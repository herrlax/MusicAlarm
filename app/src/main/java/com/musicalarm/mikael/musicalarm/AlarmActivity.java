package com.musicalarm.mikael.musicalarm;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * Created by mikael on 2017-06-08.
 */
public class AlarmActivity extends Activity {

    private TextView artist;
    private TextView name;
    private TextView time;
    private ImageView image;
    private RelativeLayout dismissButton;
    private RelativeLayout snoozeButton;
    private LinearLayout background;

    private KeyguardManager.KeyguardLock kgLock;

    private final int DARK_COLOR = 0x0E0E0E;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        initUI();
        unlockPhone();
    }

    public void initUI() {
        final Intent intent = getIntent();

        background = (LinearLayout) findViewById(R.id.alarm_background);
        name = (TextView) findViewById(R.id.alarm_name);
        artist = (TextView) findViewById(R.id.alarm_artist);
        time = (TextView) findViewById(R.id.alarm_time);
        image = (ImageView) findViewById(R.id.alarm_image);

        // setting thumbnail ..
        Picasso.with(this) // context
                .load(intent.getStringExtra("image"))
                .fit()
                .centerCrop()
                .into(image, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {

                        Palette.PaletteAsyncListener paletteListener = p -> {

                            GradientDrawable gd = new GradientDrawable(
                                    GradientDrawable.Orientation.TOP_BOTTOM,
                                    new int[] {p.getDarkMutedColor(DARK_COLOR), DARK_COLOR});

                            background.setBackground(gd);
                        };

                        Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
                        Palette.from(bitmap).generate(paletteListener);

                    }

                    @Override
                    public void onError() {}

                });

        name.setText(intent.getStringExtra("name"));
        artist.setText(intent.getStringExtra("artist"));
        time.setText(intent.getStringExtra("time"));

        snoozeButton = (RelativeLayout) findViewById(R.id.snooze_layout);

        dismissButton = (RelativeLayout) findViewById(R.id.dismiss_layout);
        dismissButton.setOnClickListener(view -> {
            lockPhone();
            MainActivity.mPlayer.pause();
            AlarmActivity.this.finish();
        });
    }

    public void unlockPhone() {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    public void lockPhone() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }
}
