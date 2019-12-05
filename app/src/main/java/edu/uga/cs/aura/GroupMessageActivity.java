/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.tone_analyzer.v3.model.ToneOptions;
import com.ibm.watson.tone_analyzer.v3.model.ToneScore;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * creates the GroupMessageActivity activity
 */
public class GroupMessageActivity extends AppCompatActivity {

    private final int [] TONE_COLORS = new int [] {
            Color.parseColor("#e82754"), //anger (disgust)
            Color.parseColor("#68f046"), //joy
            Color.parseColor("#07b0f2"), //sadness (fear)
            Color.GRAY
    };

    private ImageButton sendMessageButton;
    private EditText userMessageInput;
    private ScrollView messagesScrollView;

    private Toolbar groupDisplayToolbar;
    private ActionBar actionBar;
    private CircularImageView groupImage;
    private TextView groupName, groupDescription;

    private FirebaseAuth auth;
    private DatabaseReference dbUsersRef, dbCurrentGroupRef, dbAuraRef;

    private String currentGroupKey, currentUserID, currentUserDisplayName, currentGroupOwner;


    /**
     * Overrides the onCreate method to set up the messages in the group
     * @param savedInstanceState the bundle for the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_messages_layout);

        currentGroupKey = getIntent().getStringExtra("groupKey");

        auth = FirebaseAuth.getInstance();
        currentUserID = auth.getCurrentUser().getUid();
        dbUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        dbCurrentGroupRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(currentGroupKey);
        dbAuraRef = dbUsersRef.child(currentUserID).child("aura");

        InitializeFields();

        GetUserInfo();

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                saveMessageInfoToDatabase();
            }
        });

        dbCurrentGroupRef.addValueEventListener(new ValueEventListener() {
            /**
             * Overrides the onDataChange method that listens for changes in the information from the database
             * @param dataSnapshot to watch for changes
             */
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    final String groupImageUrl = dataSnapshot.child("groupImage").getValue(String.class);
                    final int groupAura = dataSnapshot.child("groupAura").getValue(Integer.class);
                    currentGroupOwner = dataSnapshot.child("groupOwner").getValue(String.class);

                    groupName.setText(dataSnapshot.child("groupName").getValue(String.class));
                    groupDescription.setText(dataSnapshot.child("groupDescription").getValue(String.class));
                    groupImage.setBorderColor(groupAura);

                    if (TextUtils.isEmpty(groupImageUrl) || groupImageUrl.equals("DEFAULT")) {
                        groupImage.setImageResource(R.drawable.default_profile_image);
                    } else {
                        Picasso.get().load(groupImageUrl).placeholder(R.drawable.default_profile_image).into(groupImage);
                    }
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    /**
     * Overrides the onStart method to add the messages to group
     */
    @Override
    protected void onStart() {
        super.onStart();

        dbCurrentGroupRef.addChildEventListener(new ChildEventListener() {
            /**
             * Overrides the onChildAdded method to add the messages to the display and scroll down
             * @param dataSnapshot from the database
             * @param s the string of the message
             */
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists()) {
                    DisplayMessages(dataSnapshot);
                    messagesScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            }

            /**
             * Overrides the onChildChanged method to display the message
             * @param dataSnapshot from the database
             * @param s the string of the message
             */
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists()) {
                    DisplayMessages(dataSnapshot);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

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
        menu.removeItem(R.id.menu_help);
        if (!currentUserID.equals(currentGroupOwner)){
            menu.removeItem(R.id.menu_groupSettings);
        }
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

            case R.id.menu_groupSettings:
                Intent groupSettingsIntent = new Intent(this, GroupSettingsActivity.class);
                groupSettingsIntent.putExtra("groupKey", currentGroupKey);
                startActivity(groupSettingsIntent);

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Initializes all the view that should be displayed
     */
    private void InitializeFields()
    {
        sendMessageButton = findViewById(R.id.send_message_button);
        userMessageInput = findViewById(R.id.input_group_message);
        messagesScrollView = findViewById(R.id.my_scroll_view);
        groupDisplayToolbar = findViewById(R.id.groupDisplayToolbar);
        setSupportActionBar(groupDisplayToolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        View actionBarView = getLayoutInflater().inflate(R.layout.group_display_toolbar, null);
        actionBar.setCustomView(actionBarView);


        groupName = findViewById(R.id.groupToolbar_GroupName);
        groupDescription = findViewById(R.id.groupToolbar_GroupDescription);
        groupImage = findViewById(R.id.groupToolbar_GroupImage);
    }


    /**
     * Gets information about the user
     */
    private void GetUserInfo()
    {
        dbUsersRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            /**
             * Overrides the onDataChange method to get the displayName of the current user
             * @param
             * @return
             */
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
                    currentUserDisplayName = dataSnapshot.child("displayName").getValue(String.class);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }




    /**
     * Saves the message info to the database
     */
    private void saveMessageInfoToDatabase() {
        String message = userMessageInput.getText().toString();

        userMessageInput.setText("");

        if (TextUtils.isEmpty(message))
        {
            Toast.makeText(this, "Please write message first...", Toast.LENGTH_SHORT).show();
        }
        else
        {
            new AnalyzeToneTask(message).execute();
        }
    }


    /**
     * Displays previous messages that were already sent to the group
     * @param dataSnapshot from the database
     */
    private void DisplayMessages(DataSnapshot dataSnapshot)
    {
        Iterator iterator = dataSnapshot.getChildren().iterator();

        while(iterator.hasNext())
        {
            String message = ((DataSnapshot)iterator.next()).getValue(String.class);
            int messageTone = ((DataSnapshot)iterator.next()).getValue(Integer.class);
            String senderDisplayName = ((DataSnapshot)iterator.next()).getValue(String.class);

            View linearLayoutGroup =  findViewById(R.id.linearLayoutGroup);
            LinearLayout eachMessage = new LinearLayout(this);

            eachMessage.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            eachMessage.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)eachMessage.getLayoutParams();
            params.setMargins(20, 10, 20, 10);
            eachMessage.setLayoutParams(params);
            eachMessage.setPadding(20, 10, 20, 0);

            TextView dispTextName  = new TextView(this);
            TextView dispTextMessage = new TextView(this);

            dispTextName.setText(senderDisplayName + ": ");
            dispTextName.setTextSize(18);
            dispTextName.setTextColor(Color.BLACK);

            dispTextMessage.setText(message + "\n");
            dispTextMessage.setTextSize(18);
            dispTextMessage.setTextColor(Color.BLACK);

            //this is the part that I added in for the gradient
            int[] colors = {TONE_COLORS[messageTone],Color.parseColor("#fafafa"),Color.parseColor("#fafafa"),Color.parseColor("#fafafa"),Color.parseColor("#fafafa"),Color.parseColor("#fafafa"),Color.parseColor("#fafafa")};

            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT, colors);

            gd.setCornerRadius(0f);
            eachMessage.setBackground(gd);


            eachMessage.addView(dispTextName);
            eachMessage.addView(dispTextMessage);
            eachMessage.setPadding(10, 20, 10, -40);
            ((LinearLayout) linearLayoutGroup).addView(eachMessage);
        }
    }


    /**
     * creates the AnalyzeToneTask to analyze the tone of texts
     */
    private class AnalyzeToneTask extends AsyncTask<String,Integer,Integer> {

        private final String API_KEY = "REPLACE_WITH_KEY" //Removed our API key for safety
        private final String URL = "https://gateway-wdc.watsonplatform.net/tone-analyzer/api";
        private final String VERSION = "2017-09-21";
        private IamAuthenticator authenticator;
        private ToneAnalyzer toneAnalyzer;
        private ToneOptions toneOptions;
        private ToneAnalysis toneAnalysis;
        private String text;

        /**
         * Constructor analyze the tone
         * @param message the tone is analyzed for this string
         */
        public AnalyzeToneTask(String message){
            authenticator  = new IamAuthenticator(API_KEY);
            toneAnalyzer = new ToneAnalyzer(VERSION, authenticator);
            toneAnalyzer.setServiceUrl(URL);
            text = message;
        }

        /**
         * Sends the text to the tone analyzing API
         * @param strings of the message
         * @return Integer of the tone result from the API
         */
        @Override
        protected Integer doInBackground(String... strings) {
            return sendTextToToneAPI(text);
        }

        /**
         * Overrides the onPostExecute to add the tone and other information to the database
         * @param toneResult result from the API
         */
        @Override
        protected void onPostExecute(Integer toneResult) {

            String messageKey = dbCurrentGroupRef.push().getKey();

            updateUserAura(toneResult);

            HashMap<String, Object> groupMessageKey = new HashMap<>();
            dbCurrentGroupRef.updateChildren(groupMessageKey);

            HashMap<String, Object> messageInfoMap = new HashMap<>();
            messageInfoMap.put("userDisplayName", currentUserDisplayName);
            messageInfoMap.put("tone", toneResult);
            messageInfoMap.put("message", text);
            dbCurrentGroupRef.child(messageKey).updateChildren(messageInfoMap);
        }

        /**
         * Sends the text to the API to be analyzed
         * @param text to be analyzed
         * @return int the color to be displayed for each text from the TONE_COLORS array
         */
        private int sendTextToToneAPI(String text){

            toneOptions = new ToneOptions.Builder()
                    .tones(Arrays.asList(ToneOptions.Tone.EMOTION))
                    .sentences(false)
                    .text(text)
                    .build();

            toneAnalysis = toneAnalyzer.tone(toneOptions).execute().getResult();
            List<ToneScore> toneScores = toneAnalysis.getDocumentTone().getTones();

            //if there is no tone found, just return default value (5)
            if (toneScores == null || toneScores.isEmpty() || toneScores.get(0).getScore() < .7){
                return 3;
            }

            String toneResult = toneAnalysis.getDocumentTone().getTones().get(0).getToneId();

            int toneInt;
            switch(toneResult){
                case "anger":
                case "disgust":
                    toneInt = 0; //r
                    break;

                case "joy":
                    toneInt = 1; //g
                    break;

                case "sadness":
                case "fear":
                    toneInt = 2; //b
                    break;

                default:
                    toneInt = 3;
                    break;
            }

            return toneInt;

        }
    }

    /**
     * updates the user's aura
     * @param toneResult the tone of the user
     */
    private void updateUserAura(final int toneResult) {
        //if tone not found or not strong enough
        if (toneResult == 3){
            return;
        }

        updateGroupAura(toneResult);

        dbAuraRef.addListenerForSingleValueEvent(new ValueEventListener() {
            /**
             * Overrides the onDataChange to update the user's aura if it is changed in the database
             * @param dataSnapshot to be watched from the database
             */
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    int userAura = dataSnapshot.getValue(Integer.class).intValue();
                    int [] userAuraRGB = {Color.red(userAura), Color.green(userAura), Color.blue(userAura)};

                    /*
                     * Compare r, g, and b value for overall user aura and the tone of current message
                     * shifts user aura 30 units towards (r/g/b) value of current message
                     * If user aura capped, shift less aggressively
                     */

                    for(int i = 0; i < userAuraRGB.length; i++){
                        //if not the color of tone found, then its 0 - val, if it is the color, 255 - val
                        int difference = i == toneResult ? 255 - userAuraRGB[i] : 0 - userAuraRGB[i];

                        if (userAuraRGB[i] == 255){
                            if (difference != 0){
                                userAuraRGB[i]-=60;
                            }
                        }
                        else if (userAuraRGB[i] == 0){
                            if (difference != 0){
                                userAuraRGB[i]+=60;
                            }
                        }
                        else if (difference <= -15){
                            userAuraRGB[i]-=15;
                        }
                        else if (difference >= 30){
                            userAuraRGB[i]+=30;
                        }
                        else {
                            userAuraRGB[i]+=difference;
                        }

                        dbAuraRef.setValue(Color.rgb(userAuraRGB[0], userAuraRGB[1],userAuraRGB[2]));

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    /**
     * Updates the group aura everytime a message is sent to the group
     * @param
     * @return
     */
    private void updateGroupAura(final int toneResult) {

        dbCurrentGroupRef.child("groupAura").addListenerForSingleValueEvent(new ValueEventListener() {
            /**
             * Overrides the onDataChange method to display the groups aura if it changes in the database
             * @param dataSnapshot the information that is being watched in the database
             */
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    int groupAura = dataSnapshot.getValue(Integer.class);
                    int [] groupAuraRGB = {Color.red(groupAura), Color.green(groupAura), Color.blue(groupAura)};

                    /*
                     * Compare r, g, and b value for overall user aura and the tone of current message
                     * shifts user aura 30 units towards (r/g/b) value of current message
                     * If user aura capped, shift less aggressively
                     */

                    for(int i = 0; i < groupAuraRGB.length; i++){
                        //if not the color of tone found, then its 0 - val, if it is the color, 255 - val
                        int difference = (i == toneResult) ? 255 - groupAuraRGB[i] : 0 - groupAuraRGB[i];

                        if (groupAuraRGB[i] == 255){
                            if (difference != 0){
                                groupAuraRGB[i]-=10;
                            }
                        }
                        else if (groupAuraRGB[i] == 0){
                            if (difference != 0){
                                groupAuraRGB[i]+=10;
                            }
                        }
                        else if (difference <= -30){
                            groupAuraRGB[i]-=30;
                        }
                        else if (difference >= 30){
                            groupAuraRGB[i]+=30;
                        }
                        else {
                            groupAuraRGB[i]+=difference;
                        }

                        dbCurrentGroupRef.child("groupAura").setValue(Color.rgb(groupAuraRGB[0], groupAuraRGB[1], groupAuraRGB[2]));

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

}
