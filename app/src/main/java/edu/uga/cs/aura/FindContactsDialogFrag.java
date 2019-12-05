/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Picasso;

/**
 * creates the FindContactsDialogFrag
 */
public class FindContactsDialogFrag extends DialogFragment {

    private RecyclerView contactsRecyclerView;
    private EditText userSearchBar;

    private Query userDisplayNameQuery;
    private FirebaseRecyclerAdapter<User, FindContactViewHolder> adapter;
    private FirebaseRecyclerOptions<User> options;
    private DatabaseReference dbUsersRef;

    /**
     * Overrides that onCreateDialog to create a dialog for the user to find new contacts
     * @param savedInstanceState bundle for the activity
     * @return Dialog that is created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        dbUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        //Setting up firebase adapter
        options = new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(dbUsersRef, User.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<User, FindContactViewHolder>(options) {
                    /**
                     * Override for the onBindViewHolder to set up the holder for each contact
                     * @param holder
                     * @param position of the view that is being added to the contacts dialog
                     * @param model user that is being added
                     */
                    @Override
                    protected void onBindViewHolder(@NonNull FindContactViewHolder holder, final int position, @NonNull User model)
                    {
                        holder.userNameTextView.setText(model.getDisplayName());

                        final String imageUrl = model.getImageUrl();

                        Log.d("imageUrl", imageUrl);

                        if(TextUtils.isEmpty(imageUrl) || imageUrl.equals("DEFAULT")){
                            holder.profileImageView.setImageResource(R.drawable.default_profile_image);
                        }
                        else {
                            Picasso.get().load(imageUrl).placeholder(R.drawable.default_profile_image).into(holder.profileImageView);
                        }

                        holder.profileImageView.setBorderColor(model.getAura());


                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view)
                            {
                                String contactID = getRef(position).getKey();
                                //if you don't click on yourself
                                Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                profileIntent.putExtra("contactID", contactID);
                                startActivity(profileIntent);

                            }
                        });
                    }

                    /**
                     * Overrides that FindContactViewHolder that adds the layout to each item
                     * @param parent that the contact is added to
                     * @param viewType
                     * @return FindContactViewHolder holds the view
                     */
                    @NonNull
                    @Override
                    public FindContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_display, parent, false);
                        FindContactViewHolder viewHolder = new FindContactViewHolder(view);
                        return viewHolder;
                    }
                };

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View v = inflater.inflate(R.layout.recycler_view_dialog_layout,null, false);

        contactsRecyclerView = v.findViewById(R.id.dialog_recycler_view);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        contactsRecyclerView.setAdapter(adapter);
        adapter.startListening();


        userSearchBar = v.findViewById(R.id.userSearchBar);
        userSearchBar.setVisibility(View.VISIBLE);
        userSearchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            /**
             * Searches for the user
             * @param s
             */
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()){
                    searchForUser(s.toString());
                }
                else {
                    searchForUser("");
                }
            }
        });


        return new AlertDialog.Builder(getContext())
                .setNegativeButton("Close",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }
                )
                .setView(v)
                .create();
    }

    /**
     * creates the FindContactsViewHolder
     */
    public static class FindContactViewHolder extends RecyclerView.ViewHolder  {
        TextView userNameTextView;
        CircularImageView profileImageView;

        /**
         * sets up the views on the layout
         * @param itemView that the views are added to
         */
        public FindContactViewHolder(@NonNull View itemView)
        {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.username);
            profileImageView = itemView.findViewById(R.id.userProfileImage);
        }
    }

    /**
     * Searches for the user
     * @param displayName name of the user
     */
    public void searchForUser(final String displayName){
        Log.d("Searched For:", displayName);

        if (displayName != "") {
            //search for user query
            userDisplayNameQuery = dbUsersRef.orderByChild("displayName").startAt(displayName)
                    .endAt(displayName+"\uf8ff");

            //Setting up firebase adapter
            options = new FirebaseRecyclerOptions.Builder<User>()
                    .setQuery(userDisplayNameQuery, User.class)
                    .build();
        }
        else {
            //Setting up firebase adapter
            options = new FirebaseRecyclerOptions.Builder<User>()
                    .setQuery(dbUsersRef, User.class)
                    .build();
        }

        adapter = new FirebaseRecyclerAdapter<User, FindContactViewHolder>(options) {
            /**
             * overrides the onBindViewHolder method to bind the views
             * @param holder the FindContactViewHolder that holds the information about the user
             * @param position the position of the contact that will is being added
             * @param model the user for the contact
             */
            @Override
            protected void onBindViewHolder(@NonNull FindContactViewHolder holder, final int position, @NonNull User model)
            {
                holder.userNameTextView.setText(model.getDisplayName());

                final String imageUrl = model.getImageUrl();

                Log.d("imageUrl", imageUrl);

                if(TextUtils.isEmpty(imageUrl) || imageUrl.equals("DEFAULT")){
                    holder.profileImageView.setImageResource(R.drawable.default_profile_image);
                }
                else {
                    Picasso.get().load(imageUrl).placeholder(R.drawable.default_profile_image).into(holder.profileImageView);
                }

                holder.profileImageView.setBorderColor(model.getAura());


                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        String contactID = getRef(position).getKey();
                        //if you don't click on yourself
                        Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                        profileIntent.putExtra("contactID", contactID);
                        startActivity(profileIntent);

                    }
                });
            }

            /**
             * Overrides the onCreateViewHolder method to inflate the view that contains the user's contact information
             * @param parent the parent that the view is inflated into
             * @param viewType
             * @return FindContactViewHolder that is created
             */
            @NonNull
            @Override
            public FindContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_display, parent, false);
                FindContactViewHolder viewHolder = new FindContactViewHolder(view);
                return viewHolder;
            }
        };

        contactsRecyclerView.setAdapter(adapter);
        adapter.startListening();
    }

}