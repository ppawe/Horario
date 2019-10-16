package hft.wiinf.de.horario.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import hft.wiinf.de.horario.R;

/**
 * A fragment displaying the legal disclosure of the app in a WebView
 */
public class SettingsCopyrightFragment extends Fragment {
    private WebView settings_copyright_privacyPolicy;

    public SettingsCopyrightFragment() {
        // Required empty public constructor
    }

    /**
     * Inflates the fragment_settings_copyright.xml layout then loads the legal disclosure document into its WebView
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
        View view = inflater.inflate(R.layout.fragment_settings_copyright, container, false);

        // Initial GUI
        settings_copyright_privacyPolicy = view.findViewById(R.id.settings_copyright_webView_privacyPolicy);

        // Load the HTML file from the Assets Folder into a WebView.
        settings_copyright_privacyPolicy.loadUrl("file:///android_asset/settings_copyright_privcyPolicy_german");

        return view;
    }

}
