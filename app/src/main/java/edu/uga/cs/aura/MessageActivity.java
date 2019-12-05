/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * creates the Messages Activity
 */
public class MessageActivity extends AppCompatActivity {

    private String messageReceiverID, messageReceiverName, messageReceiverImageUrl, messageSenderID;
    private int messageReceiverAura;

    private Toolbar fromUserDisplayToolbar;
    private ActionBar actionBar;
    private CircularImageView receiverProfileImage;
    private TextView receiverDisplayName;

    private FirebaseAuth auth;
    private DatabaseReference dbRootRef, dbAuraRef;

    private ImageButton sendMessageButton;
    private EditText messageInputEditText;

    private final List<Message> messagesList = new ArrayList<Message>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;
    private RecyclerView userMessagesRecyclerView;

    /**
     * Overrides the onCreate method to set up the messages in the chat
     * @param savedInstanceState the bundle for the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messages_layout);

        auth = FirebaseAuth.getInstance();
        messageSenderID = auth.getCurrentUser().getUid();
        dbRootRef = FirebaseDatabase.getInstance().getReference();
        dbAuraRef = dbRootRef.child("Users").child(messageSenderID).child("aura");
        messageReceiverID = getIntent().getStringExtra("otherUserID");
        messageReceiverName = getIntent().getStringExtra("otherUserDisplayName");
        messageReceiverImageUrl = getIntent().getStringExtra("otherUserImageUrl");
        messageReceiverAura = getIntent().getIntExtra("otherUserAura", -1);


        fromUserDisplayToolbar = findViewById(R.id.fromUserDisplayToolbar);
        setSupportActionBar(fromUserDisplayToolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        View actionBarView = getLayoutInflater().inflate(R.layout.receiver_display_toolbar, null);
        actionBar.setCustomView(actionBarView);

        sendMessageButton = findViewById(R.id.sendMessageButton);
        messageInputEditText = findViewById(R.id.messageInput);


        receiverDisplayName = findViewById(R.id.receiverToolbar_displayName);
        receiverProfileImage = findViewById(R.id.receiverToolbar_ProfileImage);

        messageAdapter = new MessageAdapter(messagesList);
        userMessagesRecyclerView = findViewById(R.id.messageRecyclerView);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesRecyclerView.setLayoutManager(linearLayoutManager);
        userMessagesRecyclerView.setAdapter(messageAdapter);


        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                SendMessage();
            }
        });
    }


    /**
     * Overrides the onStart method to add the messages to chat
     */
    @Override
    protected void onStart() {
        super.onStart();

        receiverDisplayName.setText(messageReceiverName);

        if (messageReceiverAura == -1) {
            receiverProfileImage.setBorderColor(Color.GRAY);
        }
        else {
            receiverProfileImage.setBorderColor(messageReceiverAura);
        }
        if(TextUtils.isEmpty(messageReceiverImageUrl) || messageReceiverImageUrl.equals("DEFAULT")){
            receiverProfileImage.setImageResource(R.drawable.default_profile_image);
        }
        else {
            Picasso.get().load(messageReceiverImageUrl).placeholder(R.drawable.default_profile_image).into(receiverProfileImage);
        }

        dbRootRef.child("Messages").child(messageSenderID).child(messageReceiverID)
                .addChildEventListener(new ChildEventListener() {
                    /**
                     * Overrides the onChildAdded method to add the messages to the display and scroll down
                     * @param dataSnapshot from the database
                     * @param s the string of the message
                     */
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s)
                    {
                        Message message = dataSnapshot.getValue(Message.class);

                        messagesList.add(message);

                        messageAdapter.notifyDataSetChanged();

                        userMessagesRecyclerView.smoothScrollToPosition(View.FOCUS_DOWN);
                    }

                    /**
                     * Overrides the onChildChanged method to display the message
                     * @param dataSnapshot from the database
                     * @param s the string of the message
                     */
                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

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
     * Overrides the onCreateOptionsMenu to create the menu information for the chat page
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

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * allows the user to send a message and analyze the tone
     */
    private void SendMessage()
    {
        String messageText = messageInputEditText.getText().toString();

        messageInputEditText.setText("");

        if (TextUtils.isEmpty(messageText))
        {
            Toast.makeText(this, "first write your message...", Toast.LENGTH_SHORT).show();
        }
        else
        {
            new AnalyzeToneTask(messageText).execute();
        }
    }


    /**
     * creates the AnalyzeToneTask to analyze the tone of texts
     */
    private class AnalyzeToneTask extends AsyncTask<String,Integer,Integer>{

        private final String API_KEY = "REPLACE_WITH_API"; //I removed our api key for safety
        private final String URL = "https://gateway-wdc.watsonplatform.net/tone-analyzer/api";
        private final String VERSION = "2017-09-21";
        private IamAuthenticator authenticator;
        private ToneAnalyzer toneAnalyzer;
        private ToneOptions toneOptions;
        private ToneAnalysis toneAnalysis;
        private String text;

        /**
         * Constructor analyze the tone
         * @param messageText the tone is analyzed for this string
         */
        public AnalyzeToneTask(String messageText){
            authenticator  = new IamAuthenticator(API_KEY);
            toneAnalyzer = new ToneAnalyzer(VERSION, authenticator);
            toneAnalyzer.setServiceUrl(URL);
            text = messageText;
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

            String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
            String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

            DatabaseReference userMessageKeyRef = dbRootRef.child("Messages")
                    .child(messageSenderID).child(messageReceiverID).push();

            String messagePushID = userMessageKeyRef.getKey();

            Map messageTextBody = new HashMap();
            messageTextBody.put("message", text);
            messageTextBody.put("type", "text");
            messageTextBody.put("fromID", messageSenderID);
            messageTextBody.put("toID", messageReceiverID);
            messageTextBody.put("messageID", messagePushID);
            messageTextBody.put("tone", toneResult);

            updateUserAura(toneResult);

            Map messageBodyDetails = new HashMap();
            messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
            messageBodyDetails.put( messageReceiverRef + "/" + messagePushID, messageTextBody);

            dbRootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task)
                {
                    if (task.isSuccessful())
                    {
                        Toast.makeText(MessageActivity.this, "Message Sent", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        Toast.makeText(MessageActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                    messageInputEditText.setText("");
                }
            });

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


}
