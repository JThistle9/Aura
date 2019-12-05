/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Picasso;


/**
 * Creates a contacts fragment
 */
public class ContactsFrag extends Fragment {


    private View contactFragView;
    private RecyclerView contactRecyclerView;


    private DatabaseReference dbContactsRef, dbUsersRef;
    private FirebaseAuth auth;
    private String currentUserID;


    /**
     * Empty constructor
     */
    public ContactsFrag() {
        // Required empty public constructor
    }


    /**
     * Overrides the onCreateView method to set up all the views in the fragment
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return the view with the elements
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        contactFragView = inflater.inflate(R.layout.contacts_fragment, container, false);

        contactRecyclerView = contactFragView.findViewById(R.id.user_contacts_recycler_view);
        contactRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        auth = FirebaseAuth.getInstance();
        currentUserID = auth.getCurrentUser().getUid();

        dbContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        dbUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        return contactFragView;
    }

    /**
     * Sets up the View to hold all the contacts in the contacts tab
     */
    @Override
    public void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<User> options =
                new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(dbContactsRef, User.class)
                        .build();

        FirebaseRecyclerAdapter<User, ContactViewHolder> adapter = new FirebaseRecyclerAdapter<User, ContactViewHolder>(options) {
            /**
             * Overrides the onCreateViewHolder method to inflate the view that contains the user's contact information
             * @param parent the parent that the view is inflated into
             * @param viewType
             * @return ContactViewHolder that is created
             */
            @NonNull
            @Override
            public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_display, parent, false);
                return new ContactViewHolder(view);
            }

            /**
             * overrides the onBindViewHolder method to bind the views
             * @param holder the ContactViewHolder that holds the information about the card
             * @param position the position of the contact that will is being added
             * @param model the user for the contact
             */
            @Override
            protected void onBindViewHolder(@NonNull final ContactViewHolder holder, final int position, @NonNull User model) {

                final String contactIDs = getRef(position).getKey();

                dbUsersRef.child(contactIDs).addValueEventListener(new ValueEventListener() {
                    /**
                     * Overrides the onDataChange method to make sure that the information changes in the
                     * display if it changes in the database
                     * @param dataSnapshot that is being monitored for changes
                     */
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //if exists
                        if (dataSnapshot.exists()) {
                            //get groupName and groupDescription
                            final User user = dataSnapshot.getValue(User.class);
                            final String imageUrl = user.getImageUrl();

                            //setting user display name for contact
                            holder.userNameTextView.setText(user.getDisplayName());

                            //setting profile image for contact
                            if(TextUtils.isEmpty(imageUrl) || imageUrl.equals("DEFAULT")){
                                holder.profileImageView.setImageResource(R.drawable.default_profile_image);
                            }
                            else {
                                Picasso.get().load(imageUrl).placeholder(R.drawable.default_profile_image).into(holder.profileImageView);
                            }

                            if ((dataSnapshot.child("aura").exists())){
                                holder.profileImageView.setBorderColor(dataSnapshot.child("aura").getValue(Integer.class));
                            }

                            //setting onClickListener for whole item view
                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view)
                                {
                                    String contactID = getRef(position).getKey();
                                    Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                    profileIntent.putExtra("contactID", contactID);
                                    startActivity(profileIntent);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        };
        contactRecyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    /**
     * creates the ContactViewHolder
     */
    public static class ContactViewHolder extends RecyclerView.ViewHolder  {
        TextView userNameTextView;
        CircularImageView profileImageView;

        /**
         * constructor for the contactView Holder that adds the users name and picture to the holder
         * @param itemView
         */
        public ContactViewHolder(@NonNull View itemView)
        {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.username);
            profileImageView = itemView.findViewById(R.id.userProfileImage);
        }
    }


}
