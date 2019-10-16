package hft.wiinf.de.horario.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.controller.PersonController;
import hft.wiinf.de.horario.model.Person;

/**
 * Fragment displaying links to several points of interest in the app such as settings, support, legal disclosures, and feedback
 */
public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingFragmentActivity";
    private Button button_settings;
    private Button button_support;
    private Button button_copyright;
    private Button button_feedback;
    private Person person;

    /**
     * Inflates the fragment_settings.xml layout into views
     *
     * @param inflater           a LayoutInflater used for inflating layouts into views
     * @param container          the parent view of the fragment
     * @param savedInstanceState the saved state of the fragment from before a system event changed it
     * @return the view created from inflating the layout
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        super.onCreateView(inflater, container, savedInstanceState);
        return view;
    }


    /**
     * Initializes the view variables, then sets OnClickListeners for every button that replace the
     * current fragment with the designated functions
     *
     * @param view               the view created from the layout in onCreateView()
     * @param savedInstanceState the saved state of the fragment from before a system event changed it
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull final View view, Bundle savedInstanceState) {
        try {
            person = PersonController.getPersonWhoIam();
        } catch (NullPointerException e) {
            Log.d(TAG, "SettingsActivity:" + e.getMessage());
        }

        //Initialize all Gui-Elements
        button_settings = view.findViewById(R.id.settings_button_settings);
        button_support = view.findViewById(R.id.settings_button_support);
        button_copyright = view.findViewById(R.id.settings_button_copyright);
        button_feedback = view.findViewById(R.id.settings_button_feedback);


        //onCLickListener for every button in settings fragment
        button_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction fr = getFragmentManager().beginTransaction();
                //settings_relativeLayout_helper: in this Layout all other layouts will be uploaded
                fr.replace(R.id.settings_frameLayout, new SettingsSettingsFragment(), "SettingsSettings");
                fr.addToBackStack("SettingsSettings");
                fr.commit();
            }
        });

        button_support.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction fr = getFragmentManager().beginTransaction();
                fr.replace(R.id.settings_frameLayout, new SettingsSupportFragment(), "SettingsSupport");
                fr.addToBackStack("SettingsSupport");
                fr.commit();
            }
        });

        button_copyright.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction fr = getFragmentManager().beginTransaction();
                fr.replace(R.id.settings_frameLayout, new SettingsCopyrightFragment(), "SettingsCopyright");
                fr.addToBackStack("SettingsCopyright");
                fr.commit();
            }
        });

        button_feedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction fr = getFragmentManager().beginTransaction();
                fr.replace(R.id.settings_frameLayout, new SettingsFeedbackFragment(), "SettingsFeedback");
                fr.addToBackStack("SettingsFeedback");
                fr.commit();
            }
        });
    }
}