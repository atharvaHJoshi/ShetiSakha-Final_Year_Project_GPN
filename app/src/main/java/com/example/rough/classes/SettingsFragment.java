package com.example.rough.classes;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.example.rough.R;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefrences);
    }
}
