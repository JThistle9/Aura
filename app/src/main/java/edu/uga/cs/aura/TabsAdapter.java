/**
 * @author Jett Thistle
 * @author Kirtana Nidamarti
 */

package edu.uga.cs.aura;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

/**
 * creates the fragment pager adapter for the tabs to be added to
 */
public class TabsAdapter extends FragmentPagerAdapter {

    /**
     * constructor for the tabs
     */
    public TabsAdapter(FragmentManager fm) {
        super(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    /**
     * Overrides the getItem method to replace the fragment based on what tab the user selects
     * @param position of the tab that the user selects
     * @return Fragment that the user selects
     */
    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                MessagesFrag messagesFrag = new MessagesFrag();
                return messagesFrag;
            case 1:
                GroupsFrag groupsFrag = new GroupsFrag();
                return groupsFrag;
            case 2:
                ContactsFrag contactsFrag = new ContactsFrag();
                return contactsFrag;
            default:
                return null;
        }

    }

    /**
     * gets the amount of tabs
     * @return the number of tabs
     */
    @Override
    public int getCount() {
        return 3;
    }

    /**
     * Overrides the getPageTitle method to set the title based on the position
     * @param position of the tab
     * @return the String of each tab
     */
    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position){
            case 0:
                return "Messages";
            case 1:
                return "Chat Rooms";
            case 2:
                return "Contacts";
            default:
                return "";
        }
    }
}
