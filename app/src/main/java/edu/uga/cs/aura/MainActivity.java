/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * creates the Main page that the user sees
 */
public class MainActivity extends AppCompatActivity {

    private String currentUid;
    private Boolean hasAsserted;

    private ViewPager pager;
    private TabLayout tabs;
    private TabsAdapter tabsAdap;

    private FirebaseAuth auth;
    private DatabaseReference dbRootRef, dbUsersRef, dbGroupsRef;
    private StorageReference storageRootRef, storageGroupImagesRef;


    private ImageView groupImageView;
    private Bitmap groupPhotoBitmap;

    /**
     * Overrides the onCreate method to set up the display
     * @param savedInstanceState bundle for the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null){
            hasAsserted = savedInstanceState.getBoolean("hasAsserted");
        }
        else {
            hasAsserted = false;
        }

        setContentView(R.layout.main_layout);

        auth = FirebaseAuth.getInstance();
        currentUid = auth.getCurrentUser().getUid();
        dbRootRef = FirebaseDatabase.getInstance().getReference();
        dbUsersRef = dbRootRef.child("Users");
        dbGroupsRef = dbRootRef.child("Groups");

        storageRootRef = FirebaseStorage.getInstance().getReference();
        storageGroupImagesRef = storageRootRef.child("groupImages");

        pager = findViewById(R.id.pager);
        tabs = findViewById(R.id.tabs);

        tabsAdap = new TabsAdapter(getSupportFragmentManager());
        pager.setAdapter(tabsAdap);

        tabs.setupWithViewPager(pager);

    }

    /**
     * Overrides the onSaveInstanceState method to save information if the state changes
     * @param outState bundle
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("hasAsserted", hasAsserted);
    }

    /**
     * Overrides the onStart method
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (!hasAsserted) {
            assertUserExists();
        }
    }

    /**
     * Logs the user in of they have logged in previously
     */
    private void assertUserExists() {
        dbUsersRef.child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            /**
             * Overrides the onDataChange method to display the user's name
             * @param dataSnapshot the information that is being watched in the database
             */
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if ((dataSnapshot.child("displayName").exists())){
                    Toast.makeText(getBaseContext(), "Welcome Back, "+dataSnapshot.child("displayName").getValue(),
                            Toast.LENGTH_LONG).show();
                    hasAsserted = true;
                }
                else {
                    mandatorySettingsIntent();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Creates an intent to the mandatory settings page if the user has not set a name for thier account
     */
    private void mandatorySettingsIntent() {
        Intent intent = new Intent(this, MandatorySettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
        menu.removeItem(R.id.menu_home);
        menu.removeItem(R.id.menu_groupSettings);
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

            case R.id.menu_requests:
                new RequestsDialogFrag().show(getSupportFragmentManager(), "requests");
                return true;

            case R.id.menu_contact:
                new FindContactsDialogFrag().show(getSupportFragmentManager(), "find contacts");
                return true;

            case R.id.menu_home:
                return true;

            case R.id.menu_group:
                showNewGroupAlertDialog();
                return true;

            case R.id.menu_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                settingsIntent.putExtra("userID", currentUid);
                startActivity(settingsIntent);
                return true;

            case R.id.menu_help:
                Intent helpIntent = new Intent(this, HelpActivity.class);
                helpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(helpIntent);
                finish();
                return true;

            case R.id.menu_logout:
                final Intent logoutIntent = new Intent(this, SplashScreen.class);
                final AlertDialog.Builder alertBox = new AlertDialog.Builder(this);
                alertBox.setMessage("Are you sure you want to log out?");

                DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(i == DialogInterface.BUTTON_POSITIVE){
                            dialogInterface.dismiss();
                        }
                        else if(i == DialogInterface.BUTTON_NEGATIVE){
                            auth.signOut();
                            logoutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(logoutIntent);
                        }
                    }
                };

                alertBox.setPositiveButton("No", dialogListener);
                alertBox.setNegativeButton("Yes", dialogListener);
                alertBox.show();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Dialog that appears if the user wants to create a new group that is used by Create new group button
     */
    private void showNewGroupAlertDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.BasicAlertDialog);
        LayoutInflater factory = LayoutInflater.from(MainActivity.this);
        final View view = factory.inflate(R.layout.create_group_dialog_layout,null);

        final EditText groupNameEditText = view.findViewById(R.id.groupNameEditText);
        final EditText groupDescriptionEditText = view.findViewById(R.id.groupDescriptionEditText);
        InputFilter [] filter50 = {new InputFilter.LengthFilter(50)};
        groupDescriptionEditText.setFilters(filter50);
        InputFilter[] filter20 = {new InputFilter.LengthFilter(20)};
        groupNameEditText.setFilters(filter20);
        groupNameEditText.setHint("e.g. All About Cats");
        groupDescriptionEditText.setHint("e.g. This group really loves cats! (50 char limit)");

        groupImageView = view.findViewById(R.id.groupImageView);
        groupImageView.setOnClickListener(new UploadPhotoClickListener());
        groupPhotoBitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.default_profile_image)).getBitmap();
        groupImageView.setImageBitmap(groupPhotoBitmap);

        builder.setView(view);

        builder.setNegativeButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String groupName = groupNameEditText.getText().toString();
                final String groupDescription = groupDescriptionEditText.getText().toString();
                if (TextUtils.isEmpty(groupName)){
                    //demand a name for the group
                    Toast.makeText(getBaseContext(), "Please Enter a Group Name", Toast.LENGTH_SHORT).show();
                }
                else {
                    //add the newly created group to the db
                    final String groupID = dbGroupsRef.push().getKey();
                    HashMap<String, Object> groupData = new HashMap<>();
                    groupData.put("groupName", groupName);
                    groupData.put("groupDescription", groupDescription);
                    groupData.put("groupAura", Color.GRAY);
                    groupData.put("groupOwner", currentUid);

                    if(groupPhotoBitmap == ((BitmapDrawable)getResources().getDrawable(R.drawable.default_profile_image)).getBitmap()) {
                        groupData.put("groupImage", "DEFAULT");
                    }
                    else {
                        final StorageReference storageCurrentGroupRef = storageGroupImagesRef.child(groupID);
                        byte[] imgAsBytes = getByteArrayFromBitmap(groupPhotoBitmap);
                        UploadTask uploadPhotoUploadTask = storageCurrentGroupRef.putBytes(imgAsBytes);
                        uploadPhotoUploadTask.addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, "Photo Upload Failed :(", Toast.LENGTH_SHORT).show();
                            }
                        }).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }
                                return storageCurrentGroupRef.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "Photo Upload Succeeded :)", Toast.LENGTH_SHORT).show();
                                    Uri imgDownloadUri = task.getResult();
                                    dbGroupsRef.child(groupID).child("groupImage").setValue(imgDownloadUri.toString());
                                }
                            }
                        });
                    }

                    dbGroupsRef.child(groupID).setValue(groupData).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(MainActivity.this, groupName + " was created successfully", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * creates the UploadPhotoClickListener to upload photos
     */
    private class UploadPhotoClickListener implements View.OnClickListener{

        /**
         * Overrides the onClick method for the image so that the user can change their profile image
         *  to an image from their device
         * @param v
         */
        @Override
        public void onClick(View v){
            dispatchUploadPictureIntent();
        }


        private void dispatchUploadPictureIntent() {
            Intent uploadPictureIntent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            if (uploadPictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(uploadPictureIntent, 0);
            }
        }

    }

    /**
     * Overrides the onActivityResult to set the image the user
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Uri imgURI = data.getData();
            try {
                if(Build.VERSION.SDK_INT < 28) {
                        groupPhotoBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgURI);

                } else {
                    ImageDecoder.Source imgSource = ImageDecoder.createSource(this.getContentResolver(), imgURI);
                    groupPhotoBitmap = ImageDecoder.decodeBitmap(imgSource);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            groupImageView.setImageBitmap(groupPhotoBitmap);
        }

    }

    /**
     * Converts Bitmap picture to a JPEG Byte Array
     * @param bitmapPicture
     * @return byte[]
     */
    private byte[] getByteArrayFromBitmap(Bitmap bitmapPicture) {
        final int COMPRESSION_QUALITY = 100;
        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
        bitmapPicture.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY,
                byteArrayBitmapStream);
        byte [] b = byteArrayBitmapStream.toByteArray();
        return b;
    }

}
