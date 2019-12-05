/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;


import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Picasso;


/**
 * Creates a group fragment
 */
public class GroupsFrag extends Fragment {

    private View groupFragView;
    private EditText groupSearchBar;
    private RecyclerView groupRecyclerView;

    private DatabaseReference dbGroupRef;

    private String currentUid;

    private FirebaseRecyclerAdapter<Group, GroupsViewHolder> adapter;
    private FirebaseRecyclerOptions<Group> options;
    private Query groupNameQuery;


    /**
     * Empty constructor
     */
    public GroupsFrag() {
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

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Inflate the layout for this fragment
        groupFragView = inflater.inflate(R.layout.groups_fragment, container, false);

        groupSearchBar = groupFragView.findViewById(R.id.groupSearchBar);
        groupSearchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            /**
             * Overrides the afterTextChanged method to search for the group by groupName
             * @param s the name of the group
             */
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()){
                    searchForGroup(s.toString());
                }
                else {
                    searchForGroup("");
                }
            }
        });
        groupRecyclerView = groupFragView.findViewById(R.id.group_recycler_view);
        groupRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        dbGroupRef = FirebaseDatabase.getInstance().getReference().child("Groups");


        return groupFragView;
    }

    /**
     * Overrides the onStart Method to set up the groups
     */
    @Override
    public void onStart() {
        super.onStart();
        options = new FirebaseRecyclerOptions.Builder<Group>()
                        .setQuery(dbGroupRef, Group.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<Group, GroupsViewHolder>(options) {
            /**
             * Overrides the onCreateViewHolder to display each group
             * @param parent that the views are being put into
             * @param viewType
             * @return GroupsViewHolder
             */
            @NonNull
            @Override
            public GroupsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_display, parent, false);
                return new GroupsViewHolder(view);
            }

            /**
             * Overrides the onBindViewHolder to bind the views
             * @param holder of the group information
             * @param position of the group being added
             * @param model Group that is being added
             */
            @Override
            protected void onBindViewHolder(@NonNull final GroupsViewHolder holder, final int position, @NonNull Group model) {
                final String groupIDs = getRef(position).getKey();
                dbGroupRef.child(groupIDs).addValueEventListener(new ValueEventListener() {
                    /**
                     * Overrides the onDataChange to add the groups to the fragment
                     * @param dataSnapshot to be watched from the database
                     */
                    @Override
                    public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                        //if exists
                        if (dataSnapshot.exists()) {
                            //get groupKey and groupName and groupDescription
                            final String groupKey = getRef(position).getKey();
                            final String groupName = dataSnapshot.child("groupName").getValue(String.class);
                            final String groupDescription = dataSnapshot.child("groupDescription").getValue(String.class);
                            final String groupImageUrl = dataSnapshot.child("groupImage").getValue(String.class);
                            holder.groupName.setText(groupName);
                            holder.groupDescription.setText(groupDescription);
                            holder.groupImage.setBorderColor(dataSnapshot.child("groupAura").getValue(Integer.class));
                            if (groupImageUrl == "DEFAULT"){
                                holder.groupImage.setImageResource(R.drawable.default_profile_image);
                            }
                            else {
                                Picasso.get().load(groupImageUrl).placeholder(R.drawable.default_profile_image).into(holder.groupImage);
                            }

                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent groupMessageIntent = new Intent(getContext(), GroupMessageActivity.class);
                                    groupMessageIntent.putExtra("groupKey" , groupKey);
                                    startActivity(groupMessageIntent);
                                }
                            });

                            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View v) {
                                    if (currentUid == dataSnapshot.child("groupOwner").getValue(String.class)){
                                        Intent groupSettingsIntent = new Intent(getContext(), GroupSettingsActivity.class);
                                        groupSettingsIntent.putExtra("groupKey", groupKey);
                                        startActivity(groupSettingsIntent);
                                    }
                                    return false;
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

        groupRecyclerView.setAdapter(adapter);
        adapter.startListening();
    }


    public static class GroupsViewHolder extends RecyclerView.ViewHolder {
        CircularImageView groupImage;
        TextView groupName, groupDescription;


        /**
         * Constructor for the groupsViewHolder
         * @param itemView view that will be displayed
         */
        public GroupsViewHolder(@NonNull View itemView) {
            super(itemView);
            groupImage = itemView.findViewById(R.id.groupImage);
            groupName = itemView.findViewById(R.id.groupName);
            groupDescription = itemView.findViewById(R.id.groupDescription);
        }
    }

    /**
     * searches for the group in the database
     * @param groupName name of the group in the database
     */
    public void searchForGroup(final String groupName){
        Log.d("Searched For:", groupName);

        if (groupName != "") {
            //search for user query
            groupNameQuery = dbGroupRef.orderByChild("groupName").startAt(groupName)
                    .endAt(groupName+"\uf8ff");

            //Setting up firebase adapter
            options = new FirebaseRecyclerOptions.Builder<Group>()
                    .setQuery(groupNameQuery, Group.class)
                    .build();
        }
        else {
            //Setting up firebase adapter
            options = new FirebaseRecyclerOptions.Builder<Group>()
                    .setQuery(dbGroupRef, Group.class)
                    .build();
        }

        adapter = new FirebaseRecyclerAdapter<Group, GroupsViewHolder>(options) {
            /**
             * Overrides the onCreateViewHolder to display each group
             * @param parent that the views are being put into
             * @param viewType
             * @return GroupsViewHolder
             */
            @NonNull
            @Override
            public GroupsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_display, parent, false);
                return new GroupsViewHolder(view);
            }

            /**
             * overrides the onBindViewHolder method to bind the views
             * @param holder the ContactViewHolder that holds the information about the card
             * @param position the position of the contact that will is being added
             * @param model the user for the contact
             */
            @Override
            protected void onBindViewHolder(@NonNull final GroupsViewHolder holder, final int position, @NonNull Group model) {
                final String groupIDs = getRef(position).getKey();
                dbGroupRef.child(groupIDs).addValueEventListener(new ValueEventListener() {
                    /**
                     * Overrides the onDataChange method to make sure that the information changes in the
                     * display if it changes in the database
                     * @param dataSnapshot that is being monitored for changes
                     */
                    @Override
                    public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                        //if exists
                        if (dataSnapshot.exists()) {
                            //get groupName and groupDescription
                            final String currentGroupKey = getRef(position).getKey();
                            final String groupName = dataSnapshot.child("groupName").getValue(String.class);
                            final String groupDescription = dataSnapshot.child("groupDescription").getValue(String.class);
                            final String groupImageUrl = dataSnapshot.child("groupImage").getValue(String.class);
                            holder.groupName.setText(groupName);
                            holder.groupDescription.setText(groupDescription);
                            holder.groupImage.setBorderColor(dataSnapshot.child("groupAura").getValue(Integer.class));
                            if (groupImageUrl == "DEFAULT"){
                                holder.groupImage.setImageResource(R.drawable.default_profile_image);
                            }
                            else {
                                Picasso.get().load(groupImageUrl).placeholder(R.drawable.default_profile_image).into(holder.groupImage);
                            }

                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent groupMessageIntent = new Intent(getContext(), GroupMessageActivity.class);
                                    groupMessageIntent.putExtra("groupKey" , currentGroupKey);
                                    startActivity(groupMessageIntent);
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

        groupRecyclerView.setAdapter(adapter);
        adapter.startListening();
    }

}
