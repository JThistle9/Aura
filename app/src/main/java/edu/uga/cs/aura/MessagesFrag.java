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
 * Creates a message fragment
 */
public class MessagesFrag extends Fragment {

    private View messageFragView;
    private RecyclerView messagesRecyclerView;

    private DatabaseReference dbMessagesRef, dbUsersRef;
    private FirebaseRecyclerOptions<User> options;
    private FirebaseRecyclerAdapter<User, MessageRoomViewHolder> adapter;
    private FirebaseAuth auth;
    private String currentUserID;

    /**
     * Empty constructor
     */
    public MessagesFrag() {
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
        messageFragView = inflater.inflate(R.layout.messages_fragment, container, false);

        auth = FirebaseAuth.getInstance();
        currentUserID = auth.getCurrentUser().getUid();
        dbMessagesRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        dbUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        messagesRecyclerView = messageFragView.findViewById(R.id.message_recycler_view);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        return messageFragView;
    }

    /**
     * Overrides the onStart Method to set up the groups
     */
    @Override
    public void onStart() {
        super.onStart();

        options = new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(dbMessagesRef, User.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<User, MessageRoomViewHolder>(options) {
                    /**
                     * Overrides the onBindViewHolder to bind the views
                     * @param holder of the group information
                     * @param position of the group being added
                     * @param model Group that is being added
                     */
                    @Override
                    protected void onBindViewHolder(@NonNull final MessageRoomViewHolder holder, int position, @NonNull User model)
                    {
                        final String usersIDs = getRef(position).getKey();
                        final String[] userImage = {"DEFAULT"};

                        dbUsersRef.child(usersIDs).addValueEventListener(new ValueEventListener() {
                            /**
                             * Overrides the onDataChange to add the chats to the fragment
                             * @param dataSnapshot to be watched from the database
                             */
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                if (dataSnapshot.exists())
                                {

                                    final String displayName = dataSnapshot.child("displayName").getValue(String.class);
                                    final int aura = dataSnapshot.child("aura").getValue(Integer.class);

                                    holder.userNameTextView.setText(displayName);

                                    //If image exists, display it
                                    if (dataSnapshot.hasChild("imageUrl")) {
                                        userImage[0] = dataSnapshot.child("imageUrl").getValue().toString();
                                        if(TextUtils.isEmpty(userImage[0]) || userImage[0].equals("DEFAULT")){
                                            holder.profileImageView.setImageResource(R.drawable.default_profile_image);
                                        }
                                        else {
                                            Picasso.get().load(userImage[0]).placeholder(R.drawable.default_profile_image).into(holder.profileImageView);
                                        }
                                    }

                                    if ((dataSnapshot.child("aura").exists())){
                                        holder.profileImageView.setBorderColor(dataSnapshot.child("aura").getValue(Integer.class));
                                    }

                                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view)
                                        {
                                            Intent messageIntent = new Intent(getContext(), MessageActivity.class);
                                            messageIntent.putExtra("otherUserID", usersIDs);
                                            messageIntent.putExtra("otherUserDisplayName", displayName);
                                            messageIntent.putExtra("otherUserImageUrl", userImage[0]);
                                            messageIntent.putExtra("otherUserAura", aura);
                                            startActivity(messageIntent);
                                        }
                                    });

                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }

                    /**
                     * Overrides the onCreateViewHolder to hold the views
                     * @param viewGroup that the view is added to
                     * @param i
                     */
                    @NonNull
                    @Override
                    public MessageRoomViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
                    {
                        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.user_display, viewGroup, false);
                        return new MessageRoomViewHolder(view);
                    }
                };

        messagesRecyclerView.setAdapter(adapter);
        adapter.startListening();

    }

    /**
     * creates the MessageRoomViewHolder
     */
    public static class MessageRoomViewHolder extends RecyclerView.ViewHolder  {
        TextView userNameTextView;
        CircularImageView profileImageView;

        /**
         * Constructor for the messageRoomViewHolder
         * @param itemView view that will be displayed
         */
        public MessageRoomViewHolder(@NonNull View itemView)
        {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.username);
            profileImageView = itemView.findViewById(R.id.userProfileImage);
        }
    }
}
