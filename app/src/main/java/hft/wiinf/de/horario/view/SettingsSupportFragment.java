package hft.wiinf.de.horario.view;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import hft.wiinf.de.horario.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsSupportFragment extends Fragment {

    RelativeLayout rl;

    public SettingsSupportFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings_support, container, false);



        return view;
    }

}
