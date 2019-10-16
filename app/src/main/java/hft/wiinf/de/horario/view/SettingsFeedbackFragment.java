package hft.wiinf.de.horario.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import hft.wiinf.de.horario.R;

/**
 * A fragment containing a nice message from the original developers telling the user that they appreciate
 * comments and feedback but are going to ignore them because it's none of their business, then telling
 * them to just do it themselves and hinting at the existence of a github repository
 */
public class SettingsFeedbackFragment extends Fragment {
    private WebView settings_feedback_webView;

    public SettingsFeedbackFragment() {
        // Required empty public constructor
    }


    /**
     * Inflates the fragment_settings_feedback.xml layout then loads the feedback document into its WebView
     *
     * @param inflater           a LayoutInflater used for inflating layouts into views
     * @param container          the parent view of the fragment
     * @param savedInstanceState the saved state of the fragment from before a system event changed it
     * @return the view created from inflating the layout
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings_feedback, container, false);

        // Initial GUI
        settings_feedback_webView = view.findViewById(R.id.settings_feedback_webView);

        // Load the HTML file from the Assets Folder into a WebView.
        settings_feedback_webView.loadUrl("file:///android_asset/settings_feedback");

        return view;
    }

}
