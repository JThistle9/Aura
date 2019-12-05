/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Picasso;

import java.util.List;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * creates the Message Adapter
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final int [] TONE_COLORS = new int [] {
            Color.parseColor("#e82754"), //anger (disgust)
            Color.parseColor("#68f046"), //joy
            Color.parseColor("#07b0f2"), //sadness (fear)
            Color.GRAY
    };

    private List<Message> userMessagesList;
    private FirebaseAuth auth;
    private DatabaseReference dbFromUserRef;


    /**
     * Constructor to add the messages for the list
     * @param userMessagesList list of the messages in the chat
     */
    public MessageAdapter (List<Message> userMessagesList)
    {
        this.userMessagesList = userMessagesList;
    }

    /**
     * creates the MessageViewHolder
     */
    public class MessageViewHolder extends RecyclerView.ViewHolder
    {
        public TextView senderMessageText, receiverMessageText;

        public CircularImageView receiverProfileImage;


        /**
         * Constructor foe the holder that has the information about each text
         * @param itemView that the information is added to
         */
        public MessageViewHolder(@NonNull View itemView)
        {
            super(itemView);

            senderMessageText = itemView.findViewById(R.id.sender_messsage_text);
            receiverMessageText = itemView.findViewById(R.id.receiver_message_text);
            receiverProfileImage = itemView.findViewById(R.id.message_profile_image);
        }
    }

    /**
     * Overrides the onCreateViewHolder to hold the information
     * @param parent that the information is added to
     * @param i
     * @return the MessageViewHolder
     */
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.receiver_sender_message_layout, parent, false);

        auth = FirebaseAuth.getInstance();

        return new MessageViewHolder(view);
    }


    /**
     * Overrides the onBindViewHolder to bind the views
     * @param messageViewHolder that holds the information
     * @param i
     */
    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder messageViewHolder, int i)
    {
        String messageSenderId = auth.getCurrentUser().getUid();
        Message message = userMessagesList.get(i);

        String fromUserID = message.getFromID();

        dbFromUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserID);

        dbFromUserRef.addValueEventListener(new ValueEventListener() {
            /**
             * Overrides the onDataChange to add the texts
             * @param dataSnapshot to be watched from the database
             */
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.hasChild("imageUrl"))
                {
                    String receiverImageUrl = dataSnapshot.child("imageUrl").getValue().toString();

                    if(TextUtils.isEmpty(receiverImageUrl) || receiverImageUrl.equals("DEFAULT")){
                        messageViewHolder.receiverProfileImage.setImageResource(R.drawable.default_profile_image);
                    }
                    else {
                        Picasso.get().load(receiverImageUrl).placeholder(R.drawable.default_profile_image).into(messageViewHolder.receiverProfileImage);
                    }

                }

                if (dataSnapshot.hasChild("aura")){
                    messageViewHolder.receiverProfileImage.setBorderColor(dataSnapshot.child("aura").getValue(Integer.class));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        messageViewHolder.receiverMessageText.setVisibility(View.GONE);
        messageViewHolder.receiverProfileImage.setVisibility(View.GONE);
        messageViewHolder.senderMessageText.setVisibility(View.GONE);

        if (fromUserID.equals(messageSenderId)) {
            messageViewHolder.senderMessageText.setVisibility(View.VISIBLE);
            messageViewHolder.senderMessageText.setBackgroundColor(Color.WHITE);
            setColorFromTone(message.getTone(), messageViewHolder.senderMessageText);
            messageViewHolder.senderMessageText.setPadding(60, 20, 60, 20);
            messageViewHolder.senderMessageText.setTextColor(Color.BLACK);
            messageViewHolder.receiverMessageText.getLayoutParams().width = WRAP_CONTENT;
            messageViewHolder.senderMessageText.setText(message.getMessage());
        }
        else {
            messageViewHolder.receiverProfileImage.setVisibility(View.VISIBLE);
            messageViewHolder.receiverMessageText.setVisibility(View.VISIBLE);
            messageViewHolder.receiverMessageText.setBackgroundColor(Color.WHITE);
            setColorFromTone(message.getTone(), messageViewHolder.receiverMessageText);
            messageViewHolder.receiverMessageText.setPadding(60, 20, 60, 20);
            messageViewHolder.receiverMessageText.setTextColor(Color.BLACK);
            messageViewHolder.receiverMessageText.getLayoutParams().width = WRAP_CONTENT;
            messageViewHolder.receiverMessageText.setText(message.getMessage());
        }

    }


    /**
     * Overrides the getItemCount to return the size of the messages list
     * @return the number of messages in the list
     */
    @Override
    public int getItemCount()
    {
        return userMessagesList.size();
    }

    /**
     * sets the shape that goes in the background of each text that is sent
     * @param tone of the message
     * @param view the view to set the background to
     */
    private void setColorFromTone(int tone, View view){
        GradientDrawable shape = new GradientDrawable();

        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(Color.WHITE);
        shape.setCornerRadius(80);
        shape.setStroke(10, TONE_COLORS[tone]);
        view.setBackground(shape);
    }

}
