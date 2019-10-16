package hft.wiinf.de.horario.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import hft.wiinf.de.horario.R;

/**
 * the fragment that contains the CalendarFragment
 */
public class CalendarActivity extends Fragment {
    private static final String TAG = "CalendarFragmentActivity";

    /**
     * inflates activity_calendar layout, then replaces the calendar_frameLayout within it with a new CalendarFragment
     * thus acting as the container for any fragments that may replace the CalendarFragment
     *
     * @param inflater           LayoutInflater used for inflating the layout into views
     * @param container          the View containing the Fragment (usually @+id/container in activity_tab.xml)
     * @param savedInstanceState the saved state of the fragment for recreating the fragment after a system event changed it
     * @return the View with the inflated layout
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_calendar, container, false);

        FragmentTransaction fr = getFragmentManager().beginTransaction();
        //settings_relativeLayout_helper: in this Layout all other layouts will be uploaded
        fr.replace(R.id.calendar_frameLayout, new CalendarFragment(), "Calendar");
        fr.commit();

        return view;
    }
}