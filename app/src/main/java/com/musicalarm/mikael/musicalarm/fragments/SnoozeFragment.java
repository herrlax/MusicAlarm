package com.musicalarm.mikael.musicalarm.fragments;

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;

import com.musicalarm.mikael.musicalarm.R;

import static android.R.color.transparent;

/**
 * Created by mikael on 2017-06-14.
 */

public class SnoozeFragment extends Fragment {

    public interface SnoozeFragmentListener {
        void onExitClick();
        void onSnooze();
    }

    SnoozeFragmentListener listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        listener = (SnoozeFragmentListener) context;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_snooze, container, false);

        NumberPicker numberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(60);
        numberPicker.setWrapSelectorWheel(true);
        numberPicker.setValue(10);

        RelativeLayout background = (RelativeLayout) view.findViewById(R.id.top_area);
        background.setOnClickListener(view1 -> {
            listener.onExitClick();
            getFragmentManager().popBackStack();
        });

        Button snoozer = (Button) view.findViewById(R.id.snoozer);
        snoozer.setOnClickListener(view2 -> {
            listener.onSnooze();
            getFragmentManager().popBackStack();
        });



        return view;
    }
}
