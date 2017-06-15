package com.musicalarm.mikael.musicalarm.fragments;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.TimedText;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.musicalarm.mikael.musicalarm.AlarmItem;
import com.musicalarm.mikael.musicalarm.MainActivity;
import com.musicalarm.mikael.musicalarm.R;
import com.squareup.picasso.Picasso;

/**
 * Created by mikael on 2017-06-11.
 */

public class AlarmFragment extends Fragment {

    private ImageView image;
    private LinearLayout background;

    private AlarmItem alarmItem;

    public interface AlarmListener {
        void onDismiss(AlarmItem alarmItem);
        void onSnooze(AlarmItem alarmItem);
    }

    AlarmListener listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (AlarmListener) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_alarm, container, false);
        initUI(view);
        setFlags(); // used to unlock device

        return view;
    }

    public void initUI(View view) {
        background = (LinearLayout) view.findViewById(R.id.alarm_background);
        TextView name = (TextView) view.findViewById(R.id.alarm_name);
        TextView artist = (TextView) view.findViewById(R.id.alarm_artist);
        TextView time = (TextView) view.findViewById(R.id.alarm_time);
        image = (ImageView) view.findViewById(R.id.alarm_image);

        // setting thumbnail ..
        Picasso.with(getContext()) // context
                .load(alarmItem.getImageUrl())
                .fit()
                .centerCrop()
                .into(image, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {

                        Palette.PaletteAsyncListener paletteListener = p -> {

                            int defaultColor = ContextCompat.getColor(getContext(),
                                    R.color.colorPrimaryDark);

                            GradientDrawable gd = new GradientDrawable(
                                    GradientDrawable.Orientation.TOP_BOTTOM,
                                    new int[] {p.getDarkMutedColor(defaultColor),
                                            defaultColor});

                            background.setBackground(gd);
                        };

                        Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
                        Palette.from(bitmap).generate(paletteListener);

                    }

                    @Override
                    public void onError() {}

                });

        name.setText(alarmItem.getName());
        artist.setText(alarmItem.getArtist());
        time.setText(alarmItem.getFormatedTime());

        RelativeLayout dismissButton = (RelativeLayout) view.findViewById(R.id.dismiss_layout);
        dismissButton.setOnClickListener(view1 -> {
            resetFlags();
            listener.onDismiss(alarmItem);
            exitFragment();
        });

        RelativeLayout snoozeButton = (RelativeLayout) view.findViewById(R.id.snooze_layout);
        snoozeButton.setOnClickListener(view2 -> {
            resetFlags();
            listener.onSnooze(alarmItem);
            exitFragment();
        });
    }

    // resets alarm and returns to homefragment
    public void exitFragment() {
        getFragmentManager().popBackStack();
    }

    public void setAlarmItem(AlarmItem alarmItem) {
        this.alarmItem = alarmItem;
    }

    public void setFlags() {
        ((MainActivity) getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    public void resetFlags() {
        ((MainActivity) getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }
}
