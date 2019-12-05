/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * creates the Settings Activity
 */
public class SettingsActivity extends AppCompatActivity {

    private LinearLayout backgroundLayout;
    private EditText editName;
    private CircularImageView profilePic;
    private Bitmap profileBitmap;
    private Button submitButton, cancelButton;


    private FirebaseAuth auth;
    private FirebaseUser fbUser;
    private DatabaseReference dbUsersRef, dbCurrentUserRef;
    private StorageReference storageUserImagesRef;

    /**
     * Overrides the onCreate method to set up the display
     * @param savedInstanceState bundle for the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        backgroundLayout = findViewById(R.id.settings_bgLayout);
        editName = findViewById(R.id.editName);
        InputFilter[] filter20 = {new InputFilter.LengthFilter(20)};
        editName.setFilters(filter20);
        profileBitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.default_profile_image)).getBitmap();
        profilePic = findViewById(R.id.profilePic);
        profilePic.setImageBitmap(profileBitmap);
        profilePic.setOnClickListener(new UploadPhotoClickListener());
        submitButton = findViewById(R.id.submitChanges);
        cancelButton = findViewById(R.id.cancelButton);

        auth = FirebaseAuth.getInstance();
        fbUser = auth.getCurrentUser();
        dbUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        dbCurrentUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fbUser.getUid());
        storageUserImagesRef = FirebaseStorage.getInstance().getReference().child("userImages");

        String userID = getIntent().getStringExtra("userID");

        //set the onClick listener for the button
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUserSettings();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { finish();
            }
        });


        dbUsersRef.child(userID).addValueEventListener(new ValueEventListener() {
            /**
             * Overrides the onDataChange method to get the settings information
             * @param dataSnapshot the information that is being watched in the database
             */
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if ((dataSnapshot.child("displayName").exists())){
                    editName.setText(dataSnapshot.child("displayName").getValue().toString());
                }
                if ((dataSnapshot.child("aura").exists())){
                    profilePic.setBorderColor(dataSnapshot.child("aura").getValue(Integer.class));
                    GradientDrawable bgGradient = new GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[] {Color.WHITE, dataSnapshot.child("aura").getValue(Integer.class)}
                    );
                    backgroundLayout.setBackground(bgGradient);
                }
                if ((dataSnapshot.child("imageUrl").exists())){
                    String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);

                    Picasso.get().load(imageUrl).placeholder(R.drawable.default_profile_image).into(profilePic);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


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
        return true;
    }

    /**
     * Overrides the onOptionsItemSelected to perform a certain action when each item in the menu is clicked
     * @param item from the menu
     * @return boolean to add the actions to the menu items
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        InputMethodManager imm;
        View v;

        switch (item.getItemId()) {
            case R.id.menu_home:
            case android.R.id.home:
                imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                v = this.getCurrentFocus();
                if (v == null) {
                    v = new View(this);
                }
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                finish();
                return true;

            case R.id.menu_help:
                Intent helpIntent = new Intent(this, HelpActivity.class);
                helpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(helpIntent);
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Updates the user settings
     */
    private void updateUserSettings() {
        String displayName = editName.getText().toString();
        if (TextUtils.isEmpty(displayName)){
            editName.setError("Required.");
            return;
        }

        dbCurrentUserRef.child("uid").setValue(fbUser.getUid());
        dbCurrentUserRef.child("displayName").setValue(displayName);
        dbCurrentUserRef.child("imageUrl").setValue("DEFAULT");

        profileBitmap = ((BitmapDrawable)profilePic.getDrawable()).getBitmap();
        if(profileBitmap != ((BitmapDrawable)getResources().getDrawable(R.drawable.default_profile_image)).getBitmap()) {
            final StorageReference storageCurrentUserRef = storageUserImagesRef.child(fbUser.getUid());
            byte[] imgAsBytes = getByteArrayFromBitmap(profileBitmap);
            UploadTask uploadPhotoUploadTask = storageCurrentUserRef.putBytes(imgAsBytes);
            uploadPhotoUploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(SettingsActivity.this, "Photo Upload Failed :(", Toast.LENGTH_SHORT).show();
                }
            }).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return storageCurrentUserRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(SettingsActivity.this, "Photo Upload Succeeded :)", Toast.LENGTH_SHORT).show();
                        Uri imgDownloadUri = task.getResult();
                        dbCurrentUserRef.child("imageUrl").setValue(imgDownloadUri.toString());
                    }
                }
            });
        }

        settingsToMainActivityIntent();
    }

    /**
     * Creates an intent from the settings to main activity
     */
    void settingsToMainActivityIntent(){
        //go to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * creates the UploadPhotoClickListener to upload photos
     */
    private class UploadPhotoClickListener implements View.OnClickListener{

        /**
         * Overrides the onClick method to so the user can set their profile image
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
     * Overrides the onActivityResult to set the image of the user
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
                    profileBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgURI);

                } else {
                    ImageDecoder.Source imgSource = ImageDecoder.createSource(this.getContentResolver(), imgURI);
                    profileBitmap = ImageDecoder.decodeBitmap(imgSource);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            profilePic.setImageBitmap(profileBitmap);
        }

    }

    // This functions converts Bitmap picture to a JPEG Byte Array
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
