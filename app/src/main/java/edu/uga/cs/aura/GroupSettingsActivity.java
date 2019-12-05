/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

import android.app.Activity;
import android.content.DialogInterface;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
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
 * creates the GroupSettingsActivity
 */
public class GroupSettingsActivity extends AppCompatActivity {

    private String currentGroupKey;

    private LinearLayout backgroundLayout;
    private EditText editName, editDescription;
    private CircularImageView groupPic;
    private Bitmap groupImageBitmap;
    private Button submitButton, cancelButton, deleteButton;

    private DatabaseReference dbCurrentGroupRef;
    private StorageReference storageGroupImagesRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_settings_layout);

        currentGroupKey = getIntent().getStringExtra("groupKey");

        backgroundLayout = findViewById(R.id.groupSettings_bgLayout);
        editName = findViewById(R.id.editGroupName);
        editDescription = findViewById(R.id.editGroupDescription);
        InputFilter[] filter50 = {new InputFilter.LengthFilter(50)};
        editDescription.setFilters(filter50);
        InputFilter[] filter20 = {new InputFilter.LengthFilter(20)};
        editName.setFilters(filter20);
        submitButton = findViewById(R.id.submitGroupChanges);
        cancelButton = findViewById(R.id.cancelChangesButton);
        deleteButton = findViewById(R.id.deleteGroupButton);
        groupImageBitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.default_profile_image)).getBitmap();
        groupPic = findViewById(R.id.groupProfilePic);
        groupPic.setImageBitmap(groupImageBitmap);
        groupPic.setOnClickListener(new UploadPhotoClickListener());

        dbCurrentGroupRef =  FirebaseDatabase.getInstance().getReference().child("Groups").child(currentGroupKey);
        storageGroupImagesRef = FirebaseStorage.getInstance().getReference().child("groupImages");

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteGroup();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateGroupSettings();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { finish();
            }
        });


        dbCurrentGroupRef.addValueEventListener(new ValueEventListener() {
            /**
             * Overrides the onDataChange method to display the groups information if it changes in the database
             * @param dataSnapshot the information that is being watched in the database
             */
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    editName.setText(dataSnapshot.child("groupName").getValue(String.class));

                    editDescription.setText(dataSnapshot.child("groupDescription").getValue(String.class));

                    groupPic.setBorderColor(dataSnapshot.child("groupAura").getValue(Integer.class));

                    GradientDrawable bgGradient = new GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{Color.WHITE, dataSnapshot.child("groupAura").getValue(Integer.class)}
                    );
                    backgroundLayout.setBackground(bgGradient);

                    Picasso.get().load(dataSnapshot.child("groupImage").getValue(String.class)).placeholder(R.drawable.default_profile_image).into(groupPic);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Deletes a group
     */
    private void deleteGroup() {
        final AlertDialog.Builder alertBox = new AlertDialog.Builder(this);
        alertBox.setMessage("Are you sure you want to delete this group?");

        DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    dialogInterface.dismiss();
                }
                else if(i == DialogInterface.BUTTON_NEGATIVE){
                    dbCurrentGroupRef.removeValue();
                    groupSettingToMainActivityIntent();
                }
            }
        };

        alertBox.setPositiveButton("No", dialogListener);
        alertBox.setNegativeButton("Yes", dialogListener);
        alertBox.show();
    }

    /**
     * updates the information about the group
     */
    private void updateGroupSettings() {
        String groupName = editName.getText().toString();
        if (TextUtils.isEmpty(groupName)){
            editName.setError("Required.");
            return;
        }
        dbCurrentGroupRef.child("groupName").setValue(groupName);

        dbCurrentGroupRef.child("groupDescription").setValue(editDescription.getText().toString());
        dbCurrentGroupRef.child("groupImage").setValue("DEFAULT");

        groupImageBitmap = ((BitmapDrawable)groupPic.getDrawable()).getBitmap();
        if(groupImageBitmap != ((BitmapDrawable)getResources().getDrawable(R.drawable.default_profile_image)).getBitmap()) {
            final StorageReference storageCurrentGroupRef = storageGroupImagesRef.child(currentGroupKey);
            byte[] imgAsBytes = getByteArrayFromBitmap(groupImageBitmap);
            UploadTask uploadPhotoUploadTask = storageCurrentGroupRef.putBytes(imgAsBytes);
            uploadPhotoUploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(GroupSettingsActivity.this, "Photo Upload Failed :(", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(GroupSettingsActivity.this, "Photo Upload Succeeded :)", Toast.LENGTH_SHORT).show();
                        Uri imgDownloadUri = task.getResult();
                        dbCurrentGroupRef.child("groupImage").setValue(imgDownloadUri.toString());
                    }
                }
            });
        }

        groupSettingToMainActivityIntent();
    }

    /**
     * creates the UploadPhotoClickListener to upload photos
     */
    private class UploadPhotoClickListener implements View.OnClickListener{

        /**
         * Overrides the onClick method for the image so that the owner of the group can change the image
         * of the group to an image from their device
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
     * Overrides the onActivityResult to set the image of the group
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
                    groupImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgURI);

                } else {
                    ImageDecoder.Source imgSource = ImageDecoder.createSource(this.getContentResolver(), imgURI);
                    groupImageBitmap = ImageDecoder.decodeBitmap(imgSource);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            groupPic.setImageBitmap(groupImageBitmap);
        }

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
            case android.R.id.home:
                imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                v = this.getCurrentFocus();
                if (v == null) {
                    v = new View(this);
                }
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                finish();
                return true;

            case R.id.menu_home:
                imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                v = this.getCurrentFocus();
                if (v == null) {
                    v = new View(this);
                }
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                startActivity(getParentActivityIntent());
                return true;

            default:
                return super.onOptionsItemSelected(item);
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

    /**
     * creates an intent from the group settings page to the main activity
     * @param
     * @return
     */
    private void groupSettingToMainActivityIntent() {
        Intent groupSettingsIntent = new Intent(this, MainActivity.class);
        groupSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(groupSettingsIntent);
    }

}
