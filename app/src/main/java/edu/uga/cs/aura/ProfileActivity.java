/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

import java.util.HashMap;

/**
 * creates the Profile Activity
 */
public class ProfileActivity extends AppCompatActivity {

    private String receiverUserID, senderUserID, state;

    private CircularImageView profileImageView;
    private TextView displayNameTextView;
    private Button profileInteractButton, rejectRequestButton;
    private LinearLayout backgroundLayout;

    private DatabaseReference dbUserRef, dbRequestRef, dbContactsRef, dbNotificationRef;
    private FirebaseAuth auth;

    /**
     * Overrides the onCreate method to set up the display
     * @param savedInstanceState bundle for the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_layout);


        auth = FirebaseAuth.getInstance();
        dbUserRef = FirebaseDatabase.getInstance().getReference().child("Users");
        dbRequestRef = FirebaseDatabase.getInstance().getReference().child("Requests");
        dbContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        dbNotificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");


        receiverUserID = getIntent().getExtras().get("contactID").toString();
        senderUserID = auth.getCurrentUser().getUid();


        profileImageView = findViewById(R.id.profileImageView);
        displayNameTextView = findViewById(R.id.displayName);
        profileInteractButton = findViewById(R.id.profileInteractButton);
        rejectRequestButton = findViewById(R.id.rejectRequestButton);
        backgroundLayout = findViewById(R.id.bgLayout);

        state = "new";

        getProfileFromDB();
    }

    /**
     * Overrides the onCreateOptionsMenu to create the menu information for the groups page
     * @param menu that should be on the page
     * @return boolean to add the menu to the page
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_simple_action_menu, menu);
        menu.removeItem(R.id.menu_requests);
        menu.removeItem(R.id.menu_contact);
        menu.removeItem(R.id.menu_group);
        menu.removeItem(R.id.menu_logout);
        menu.removeItem(R.id.menu_settings);
        menu.removeItem(R.id.menu_groupSettings);
        menu.removeItem(R.id.menu_help);
        return true;
    }

    /**
     * Overrides the onOptionsItemSelected to perform a certain action when each item in the menu is clicked
     * @param item from the menu
     * @return boolean to add the actions to the menu items
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_home:
                finish();

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * gets the profile information from the database
     */
    private void getProfileFromDB() {

        dbUserRef.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            /**
             * Overrides the onDataChange method to get the profile information
             * @param dataSnapshot the information that is being watched in the database
             */
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
                    String displayName = dataSnapshot.child("displayName").getValue(String.class);

                    if(TextUtils.isEmpty(imageUrl) || imageUrl.equals("DEFAULT")) {
                        profileImageView.setImageResource(R.drawable.default_profile_image);
                    }
                    else {
                        Picasso.get().load(imageUrl).placeholder(R.drawable.default_profile_image).into(profileImageView);
                    }

                    displayNameTextView.setText(displayName);

                    if(dataSnapshot.hasChild("aura")) {
                        profileImageView.setBorderColor(dataSnapshot.child("aura").getValue(Integer.class));
                        GradientDrawable bgGradient = new GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM,
                                new int[] {Color.WHITE, dataSnapshot.child("aura").getValue(Integer.class)}
                        );
                        backgroundLayout.setBackground(bgGradient);
                    }
                }
                else
                {

                    String displayName = dataSnapshot.child("displayName").getValue().toString();
                    displayNameTextView.setText(displayName);
                }

                ManageRequests();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    /**
     * Manages the requests sent to other users
     */
    private void ManageRequests()
    {
        dbRequestRef.child(senderUserID)
                .addValueEventListener(new ValueEventListener() {
                    /**
                     * Overrides the onDataChange method to get the request information
                     * @param dataSnapshot the information that is being watched in the database
                     */
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if (dataSnapshot.hasChild(receiverUserID))
                        {
                            String requestState = dataSnapshot.child(receiverUserID).child("requestState").getValue().toString();

                            if (requestState.equals("sent"))
                            {
                                state = "sent";
                                profileInteractButton.setText("Cancel Chat Request");
                            }
                            else if (requestState.equals("received"))
                            {
                                state = "received";
                                profileInteractButton.setText("Accept Chat Request");

                                rejectRequestButton.setVisibility(View.VISIBLE);
                                rejectRequestButton.setEnabled(true);

                                rejectRequestButton.setOnClickListener(new View.OnClickListener() {
                                    /**
                                     * Overrides the onClick to cancel the request
                                     * @param view
                                     */
                                    @Override
                                    public void onClick(View view)
                                    {
                                        CancelRequest();
                                    }
                                });
                            }
                        }
                        else
                        {
                            dbContactsRef.child(senderUserID)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        /**
                                         * Overrides the onDataChange method
                                         * @param dataSnapshot the information that is being watched in the database
                                         */
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot)
                                        {
                                            if (dataSnapshot.hasChild(receiverUserID))
                                            {
                                                state = "friends";
                                                profileInteractButton.setText("Remove Contact");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


        if (!senderUserID.equals(receiverUserID))
        {
            profileInteractButton.setOnClickListener(new View.OnClickListener() {
                /**
                 * Overrides the onClick to set the text of the button based on what the user wants to do
                 * @param view
                 */
                @Override
                public void onClick(View view)
                {
                    profileInteractButton.setEnabled(false);

                    if (state.equals("new"))
                    {
                        SendRequest();
                    }
                    if (state.equals("sent"))
                    {
                        CancelRequest();
                    }
                    if (state.equals("received"))
                    {
                        AcceptRequest();
                    }
                    if (state.equals("friends"))
                    {
                        RemoveSpecificContact();
                    }
                }
            });
        }
        else
        {
            profileInteractButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendUserToSettingsActivity();
                }
            });

            profileInteractButton.setText("Edit Profile");
        }
    }

    /**
     * Creates an intent from the profile page to the settings activity
     */
    private void sendUserToSettingsActivity() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        settingsIntent.putExtra("userID", senderUserID);
        startActivity(settingsIntent);
    }


    /**
     * allows the user to request contact with another user
     */
    private void RemoveSpecificContact()
    {
        dbContactsRef.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            dbContactsRef.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                profileInteractButton.setEnabled(true);
                                                state = "new";
                                                profileInteractButton.setText("Request Contact");
                                            }
                                        }
                                    });
                        }
                    }
                });
    }


    /**
     * Allows the user to accept requests sent to them
     */
    private void AcceptRequest()
    {
        dbContactsRef.child(senderUserID).child(receiverUserID)
                .child("Contacts").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            dbContactsRef.child(receiverUserID).child(senderUserID)
                                    .child("Contacts").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                dbRequestRef.child(senderUserID).child(receiverUserID)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task)
                                                            {
                                                                if (task.isSuccessful())
                                                                {
                                                                    dbRequestRef.child(receiverUserID).child(senderUserID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task)
                                                                                {
                                                                                    profileInteractButton.setEnabled(true);
                                                                                    state = "friends";
                                                                                    profileInteractButton.setText("Remove Contact");

                                                                                    rejectRequestButton.setVisibility(View.GONE);
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



    /**
     * Allows the user to cancel requests that the user sent
     */
    private void CancelRequest()
    {
        dbRequestRef.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            dbRequestRef.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                profileInteractButton.setEnabled(true);
                                                state = "new";
                                                profileInteractButton.setText("Request Contact");
                                                rejectRequestButton.setVisibility(View.GONE);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }



    /**
     * Allows the user to send requests
     */
    private void SendRequest()
    {
        dbRequestRef.child(senderUserID).child(receiverUserID)
                .child("requestState").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            dbRequestRef.child(receiverUserID).child(senderUserID)
                                    .child("requestState").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                HashMap<String, String> chatNotificationMap = new HashMap<>();
                                                chatNotificationMap.put("from", senderUserID);
                                                chatNotificationMap.put("type", "request");

                                                dbNotificationRef.child(receiverUserID).push()
                                                        .setValue(chatNotificationMap)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task)
                                                            {
                                                                if (task.isSuccessful())
                                                                {
                                                                    profileInteractButton.setEnabled(true);
                                                                    state = "sent";
                                                                    profileInteractButton.setText("Cancel Request");
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
