/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

/**
 * creates the user
 */
public class User {

    private String uid, displayName, imageUrl;
    private int aura;

    /**
     * Empty constructor
     */
    public User () {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    /**
     * Constructor with all the information about the user
     * @param uid of the user
     * @param displayName of the user
     * @param aura of the user
     * @param imageUrl associated with the user
     */
    public User (String uid, String displayName, int aura, String imageUrl) {
        this.uid = uid;
        this.displayName = displayName;
        this.aura = aura;
        this.imageUrl = imageUrl;
    }

    /**
     * gets the displayName
     * @return string of the displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * gets the Aura
     * @return the aura of the user
     */
    public int getAura() {
        return aura;
    }

    /**
     * gets the imageURL
     * @return string of the imageURL
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * gets the Uid
     * @return string of the user's ID
     */
    public String getUid() {
        return uid;
    }

    /**
     * sets the displayName
     * @param displayName of the user
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * sets the aura
     * @param aura of the user
     */
    public void setAura(int aura) {
        this.aura = aura;
    }

    /**
     * sets the imageUrl
     * @param imageUrl for the user
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * sets the uid
     * @param uid of the user
     */
    public void setUid(String uid) {
        this.uid = uid;
    }
}
