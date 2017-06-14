package com.musicalarm.mikael.musicalarm.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.musicalarm.mikael.musicalarm.R;

/**
 * Created by mikael on 2017-06-14.
 */

public class SnoozeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_snooze, container, false);
        return view;
    }
}
