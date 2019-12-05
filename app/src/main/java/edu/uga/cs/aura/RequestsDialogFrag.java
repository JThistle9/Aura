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
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Picasso;

/**
 * creates the Request Dialog Fragment
 */
public class RequestsDialogFrag extends DialogFragment {

    private RecyclerView requestsRecyclerView;
    private FirebaseRecyclerAdapter<User, RequestViewHolder> adapter;
    private FirebaseRecyclerOptions<User> options;
    private DatabaseReference dbUsersRef, dbRequestsRef, dbContactsRef;

    private String currentUserID;

    /**
     * Overrides that onCreateDialog to create a dialog for the user to interact with requests
     * @param savedInstanceState bundle for the activity
     * @return Dialog that is created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {


        currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        dbUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        dbRequestsRef = FirebaseDatabase.getInstance().getReference().child("Requests");
        dbContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        //Setting up firebase adapter
        options = new FirebaseRecyclerOptions.Builder<User>()
                .setQuery(dbRequestsRef.child(currentUserID), User.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<User, RequestViewHolder>(options) {
            /**
             * Override for the onBindViewHolder to set up the holder for each request
             * @param holder
             * @param position of the view that is being added to the request dialog
             * @param model user that is being added
             */
            @Override
            protected void onBindViewHolder(@NonNull final RequestViewHolder holder, final int position, @NonNull User model)
            {
                holder.itemView.findViewById(R.id.rejectButton).setVisibility(View.VISIBLE);
                holder.itemView.findViewById(R.id.acceptButton).setVisibility(View.VISIBLE);
                holder.profileImageView.setBorderColor(model.getAura());

                final String requestID = getRef(position).getKey();

                Log.d("requestID", requestID);

                DatabaseReference dbRequestTypeRef = getRef(position).child("requestState").getRef();

                dbRequestTypeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    /**
                     * Overrides the onDataChange to add the requests
                     * @param dataSnapshot to be watched from the database
                     */
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if (dataSnapshot.exists())
                        {
                            String type = dataSnapshot.getValue(String.class);

                            Log.d("requestType", type);

                            if (type.equals("received"))
                            {
                                dbUsersRef.child(requestID).addListenerForSingleValueEvent(new ValueEventListener() {
                                    /**
                                     * Overrides the onDataChange to add the requests
                                     * @param dataSnapshot to be watched from the database
                                     */
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot)
                                    {
                                        if (dataSnapshot.hasChild("imageUrl"))
                                        {
                                            final String requestProfileImageUrl = dataSnapshot.child("imageUrl").getValue(String.class);

                                            if(TextUtils.isEmpty(requestProfileImageUrl) || requestProfileImageUrl.equals("DEFAULT")){
                                                holder.profileImageView.setImageResource(R.drawable.default_profile_image);
                                            }
                                            else {
                                                Picasso.get().load(requestProfileImageUrl).placeholder(R.drawable.default_profile_image).into(holder.profileImageView);
                                            }

                                        }

                                        final String requestUserName = dataSnapshot.child("displayName").getValue().toString();

                                        holder.displayNameTextView.setText(requestUserName);


                                        holder.acceptButton.setOnClickListener(new View.OnClickListener(){
                                            @Override
                                            public void onClick(View v) {
                                                dbContactsRef.child(currentUserID).child(requestID).child("contactState")
                                                        .setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task)
                                                    {
                                                        if (task.isSuccessful())
                                                        {
                                                            dbContactsRef.child(requestID).child(currentUserID).child("contactState")
                                                                    .setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task)
                                                                {
                                                                    if (task.isSuccessful())
                                                                    {
                                                                        dbRequestsRef.child(currentUserID).child(requestID)
                                                                                .removeValue()
                                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                    @Override
                                                                                    public void onComplete(@NonNull Task<Void> task)
                                                                                    {
                                                                                        if (task.isSuccessful())
                                                                                        {
                                                                                            dbRequestsRef.child(requestID).child(currentUserID)
                                                                                                    .removeValue()
                                                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                        @Override
                                                                                                        public void onComplete(@NonNull Task<Void> task)
                                                                                                        {
                                                                                                            if (task.isSuccessful())
                                                                                                            {
                                                                                                                Toast.makeText(getContext(), "Contact Saved", Toast.LENGTH_SHORT).show();
                                                                                                            }
                                                                                                        }
                                                                                                    });
                                                                                        }
                                                                                    }
                                                                                });
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    }
                                                });
                                            }
                                        });


                                        holder.rejectButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                dbRequestsRef.child(currentUserID).child(requestID)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task)
                                                            {
                                                                if (task.isSuccessful())
                                                                {
                                                                    dbRequestsRef.child(requestID).child(currentUserID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task)
                                                                                {
                                                                                    if (task.isSuccessful())
                                                                                    {
                                                                                        Toast.makeText(getContext(), "Request Rejected", Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        });

                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            }

                            else if (type.equals("sent"))
                            {
                                Button requestSentButton = holder.itemView.findViewById(R.id.acceptButton);
                                requestSentButton.setText("Request Sent");
                                requestSentButton.setBackgroundColor(getResources().getColor(R.color.gray));
                                requestSentButton.setEnabled(false);

                                holder.rejectButton.setVisibility(View.GONE);

                                dbUsersRef.child(requestID).addListenerForSingleValueEvent(new ValueEventListener() {
                                    /**
                                     * Overrides the onDataChange for the requests
                                     * @param dataSnapshot to be watched from the database
                                     */
                                    @Override
                                    public void onDataChange(final DataSnapshot dataSnapshot)
                                    {
                                        if (dataSnapshot.hasChild("imageUrl"))
                                        {
                                            final String requestProfileImageUrl = dataSnapshot.child("imageUrl").getValue(String.class);

                                            if(TextUtils.isEmpty(requestProfileImageUrl) || requestProfileImageUrl.equals("DEFAULT")){
                                                holder.profileImageView.setImageResource(R.drawable.default_profile_image);
                                            }
                                            else {
                                                Picasso.get().load(requestProfileImageUrl).placeholder(R.drawable.default_profile_image).into(holder.profileImageView);
                                            }
                                        }

                                        holder.displayNameTextView.setText(dataSnapshot.child("displayName").getValue().toString());

                                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                                profileIntent.putExtra("contactID", dataSnapshot.getKey());
                                                startActivity(profileIntent);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            /**
             * Overrides that RequestViewHolder that adds the layout to each item
             * @param parent that the contact is added to
             * @param viewType
             * @return RequestViewHolder holds the view
             */
            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.request_display, parent, false);
                RequestViewHolder viewHolder = new RequestViewHolder(view);
                return viewHolder;
            }
        };

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View v = inflater.inflate(R.layout.recycler_view_dialog_layout,null, false);
        requestsRecyclerView = v.findViewById(R.id.dialog_recycler_view);
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requestsRecyclerView.setAdapter(adapter);
        adapter.startListening();

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
     * creates the RequestViewHolder
     */
    public static class RequestViewHolder extends RecyclerView.ViewHolder{
        TextView displayNameTextView;
        CircularImageView profileImageView;
        Button acceptButton, rejectButton;

        /**
         * sets up the views on the layout
         * @param itemView that the views are added to
         */
        public RequestViewHolder(@NonNull View itemView)
        {
            super(itemView);
            displayNameTextView = itemView.findViewById(R.id.requestDisplayName);
            profileImageView = itemView.findViewById(R.id.requestProfileImage);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }
    }

}
