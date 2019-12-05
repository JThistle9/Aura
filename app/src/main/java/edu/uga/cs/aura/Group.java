/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

/**
 * creates the group
 */
public class Group {
    private String groupName, groupDescription, groupImage, groupOwner;
    private int groupAura;

    /**
     * Empty constructor
     */
    public Group(){

    }

    /**
     * Constructor with all the information about the group
     * @param groupName name of the group
     * @param groupDescription description of the group
     * @param groupAura aura of the group
     * @param groupImage image associated with the group
     * @param groupOwner owner of the group
     */
    public Group (String groupName, String groupDescription, int groupAura, String groupImage, String groupOwner){
        this.groupName = groupName;
        this.groupDescription = groupDescription;
        this.groupImage = groupImage;
        this.groupAura = groupAura;
        this.groupOwner = groupOwner;
    }

    /**
     * gets the group name
     * @return string of the groups name
     */
    public String getGroupName(){
        return groupName;
    }

    /**
     * Sets the group name
     * @param groupName group's name
     */
    public void setGroupName(String groupName){
        this.groupName = groupName;
    }


    /**
     * Gets the group description
     * @return string of the group's description
     */
    public String getGroupDescription(){
        return groupDescription;
    }

    /**
     * Sets the group description
     * @param groupDescription group's description
     */
    public void setGroupDescription(String groupDescription){
        this.groupDescription = groupDescription;
    }

    /**
     * Gets the group image
     * @return string of the group's image
     */
    public String getGroupImage(){
        return groupImage;
    }

    /**
     * Sets the group image
     * @param groupImage group's image
     */
    public void setGroupImage(String groupImage){
        this.groupImage = groupImage;
    }

    /**
     * Gets the group aura
     * @return string of the group's aura
     */
    public int getGroupAura() {
        return groupAura;
    }

    /**
     * Sets the group aura
     * @param groupAura group's aura
     */
    public void setGroupAura(int groupAura) {
        this.groupAura = groupAura;
    }

    /**
     * Gets the group owner
     * @return string of the group's owner
     */
    public String getGroupOwner() {
        return groupOwner;
    }

    /**
     * Sets the group owner
     * @param groupOwner group's owner
     */
    public void setGroupOwner(String groupOwner) {
        this.groupOwner = groupOwner;
    }
}