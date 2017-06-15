package com.musicalarm.mikael.musicalarm.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;

import com.musicalarm.mikael.musicalarm.AlarmItem;
import com.musicalarm.mikael.musicalarm.R;

/**
 * Created by mikael on 2017-06-14.
 */

public class SnoozeFragment extends Fragment {

    private AlarmItem alarmItem;

    private long timeValue = 10;

    public void setTimeValue(long timeValue) {
        this.timeValue = timeValue;
    }

    public interface SnoozeFragmentListener {
        void onExitSnoozeDialog();
        void onSnoozeChanged(AlarmItem alarmItem, Long snoozeTime);
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
        numberPicker.setValue((int)timeValue);

        RelativeLayout background = (RelativeLayout) view.findViewById(R.id.top_area);
        background.setOnClickListener(view1 -> {
            getFragmentManager().popBackStack();
            listener.onExitSnoozeDialog();
        });

        Button snoozer = (Button) view.findViewById(R.id.snoozer);
        snoozer.setOnClickListener(view2 -> {
            getFragmentManager().popBackStack();
            listener.onSnoozeChanged(alarmItem, numberPicker.getValue()*60000l);
        });

        return view;
    }

    public void setAlarmItem(AlarmItem alarmItem) {
        this.alarmItem = alarmItem;
    }
}
