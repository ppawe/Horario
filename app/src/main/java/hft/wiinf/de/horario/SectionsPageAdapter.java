package hft.wiinf.de.horario;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * FragmentPagerAdapter managing the tabs ({@link hft.wiinf.de.horario.view.CalendarActivity}, {@link hft.wiinf.de.horario.view.EventOverviewActivity},
 * {@link hft.wiinf.de.horario.view.SettingsActivity})
 * Fragments are added to a list along with a title for them and they are retrieved according to their position
 */
class SectionsPageAdapter extends FragmentPagerAdapter {

    private final List<Fragment> mFragmentList = new ArrayList<>();
    private final List<String> mFragmentTitleList = new ArrayList<>();

    public SectionsPageAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    /**
     * adds a fragment to the list of tabs
     *
     * @param fragment a fragment representing a tab of the app
     * @param title    a title for the fragment
     */
    public void addFragment(Fragment fragment, String title) {
        mFragmentList.add(fragment);
        mFragmentTitleList.add(title);

    }

    /**
     * gets the title of the fragment at the specified position
     *
     * @param position the position of the fragment
     * @return the title
     */
    @Override
    public CharSequence getPageTitle(int position) {
        return mFragmentTitleList.get(position);
    }


    /**
     * Return the Fragment associated with a specified position.
     *
     * @param position
     */
    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    /**
     * Return the number of views available.
     */
    @Override
    public int getCount() {
        return mFragmentList.size();
    }
}