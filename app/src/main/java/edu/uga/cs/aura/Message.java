/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

/**
 * creates the messages
 */
public class Message {
    private String fromID, fromName, toID, messageID, message;
    private int tone;

    /**
     * Empty constructor
     */
    public Message() {
        //default needed for DataSnapshot
    }

    /**
     * Constructor with all the information about the message
     * @param fromID the sender ID of the message
     * @param fromName name of the sender
     * @param toID the receiver ID of the message
     * @param messageID the ID of the message
     * @param tone of the message
     */
    public Message(String fromID, String fromName, String toID, String messageID, String message, int tone) {
        this.fromID = fromID;
        this.fromName = fromName;
        this.toID = toID;
        this.messageID = messageID;
        this.message = message;
        this.tone = tone;
    }

    /**
     * gets the ID of the person who sent the message
     * @return the ID
     */
    public String getFromID() {
        return fromID;
    }

    /**
     * sets the ID of the person who sent the message
     * @param fromID ID of the person who sent the message
     */
    public void setFromID(String fromID) {
        this.fromID = fromID;
    }

    /**
     * gets the name of the person who sent the message
     * @return the name
     */
    public String getFromName() {
        return fromName;
    }

    /**
     * sets the name of the person who sent the message
     * @param fromName name of the person who sent the message
     */
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    /**
     * gets the ID of the person the message was sent to
     * @return the ID
     */
    public String getToID() {
        return toID;
    }

    /**
     * sets the ID of the person who the message was sent to
     * @param toID ID of the person who the message was sent to
     */
    public void setToID(String toID) {
        this.toID = toID;
    }

    /**
     * gets the ID of the message
     * @return the ID
     */
    public String getMessageID() {
        return messageID;
    }

    /**
     * sets the ID of the message
     * @param messageID ID of the message
     */
    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    /**
     * gets the content of the message
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * sets the content of the message
     * @param message content
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * gets the tone of the message
     * @return the tone
     */
    public int getTone() {
        return tone;
    }

    /**
     * sets the tone of the message
     * @param tone of the message
     */
    public void setTone(int tone) {
        this.tone = tone;
    }
}