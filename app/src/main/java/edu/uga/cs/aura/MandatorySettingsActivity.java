/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * creates the MandatorySettingsActivity
 */
public class MandatorySettingsActivity extends AppCompatActivity {
    private EditText requiredName;
    private ImageView profileImageView;
    private Bitmap profilePhotoBitmap;
    private Button requiredSubmitButton;
    private FirebaseAuth auth;
    private FirebaseUser fbUser;
    private DatabaseReference dbCurrentUserRef;
    private StorageReference storageUserImagesRef;

    /**
     * Overrides the onCreate method to set up the display
     * @param savedInstanceState bundle for the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mandatory_settings_layout);

        requiredName = findViewById(R.id.requiredName);
        InputFilter[] filter20 = {new InputFilter.LengthFilter(20)};
        requiredName.setFilters(filter20);
        requiredSubmitButton = findViewById(R.id.requiredSubmitButton);
        profileImageView = findViewById(R.id.mandatoryProfileImageView);
        profileImageView.setOnClickListener(new UploadPhotoClickListener());
        profilePhotoBitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.default_profile_image)).getBitmap();
        profileImageView.setImageBitmap(profilePhotoBitmap);

        auth = FirebaseAuth.getInstance();
        fbUser = auth.getCurrentUser();
        dbCurrentUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fbUser.getUid());
        storageUserImagesRef = FirebaseStorage.getInstance().getReference().child("userImages");



        requiredSubmitButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Overrides the onClick method to update the user settings
             * @param v
             */
            @Override
            public void onClick(View v) {
                updateUserSettings();
            }
        });

    }

    /**
     * updates information about the user
     */
    private void updateUserSettings() {
        String displayName = requiredName.getText().toString();
        String uid = fbUser.getUid();

        if (TextUtils.isEmpty(displayName)) {
            requiredName.setError("Required.");
            return;
        }


        User userObj = new User(uid, displayName, Color.LTGRAY, "DEFAULT");
        dbCurrentUserRef.setValue(userObj);

        if(profilePhotoBitmap != ((BitmapDrawable)getResources().getDrawable(R.drawable.default_profile_image)).getBitmap()) {
            final StorageReference storageCurrentUserRef = storageUserImagesRef.child(uid);
            byte[] imgAsBytes = getByteArrayFromBitmap(profilePhotoBitmap);
            UploadTask uploadPhotoUploadTask = storageCurrentUserRef.putBytes(imgAsBytes);
            uploadPhotoUploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MandatorySettingsActivity.this, "Photo Upload Failed :(", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(MandatorySettingsActivity.this, "Photo Upload Succeeded :)", Toast.LENGTH_SHORT).show();
                        Uri imgDownloadUri = task.getResult();
                        dbCurrentUserRef.child("imageUrl").setValue(imgDownloadUri.toString());
                    }
                }
            });
        }

        settingsToHelpScreenIntent();
    }

    /**
     * creates an intent from the settings screen to the main activity
     */
    void settingsToHelpScreenIntent(){
        //go to main activity
        Intent intent = new Intent(this, HelpActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * creates the UploadPhotoClickListener to upload photos
     */
    private class UploadPhotoClickListener implements View.OnClickListener{

        /**
         * Overrides the onClick method for the image so that the user can change the image associated
         * with their account
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
     * Overrides the onActivityResult to set the image for the user
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
                    profilePhotoBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgURI);

                } else {
                    ImageDecoder.Source imgSource = ImageDecoder.createSource(this.getContentResolver(), imgURI);
                    profilePhotoBitmap = ImageDecoder.decodeBitmap(imgSource);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            profileImageView.setImageBitmap(profilePhotoBitmap);
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
