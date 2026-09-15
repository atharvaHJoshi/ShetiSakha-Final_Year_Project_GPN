package com.shetisakha.classes;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.shetisakha.R;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefrences);
    }
}
