package com.musicalarm.mikael.musicalarm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;
import com.squareup.picasso.Picasso;

/**
 * Created by mikael on 2017-06-11.
 */

public class AlarmActivity extends Activity implements ConnectionStateCallback, PlayerNotificationCallback, Player.InitializationObserver{

    private ImageView image;
    private LinearLayout background;

    private AlarmItem alarmItem;
    private Player mPlayer;

    private final String REDIRECT_URI = "musicalarm://callback";
    private final String CLIENT_ID = "22a32c3cb52747b0912c3701637d53db";
    private final int REQUEST_CODE = 1337;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_alarm);

        Intent intent = getIntent();

        alarmItem = new AlarmItem(intent.getStringExtra("uri"),
                intent.getStringExtra("imageUrl"),
                intent.getStringExtra("name"),
                intent.getStringExtra("artist"),
                intent.getIntExtra("hour", 15),
                intent.getIntExtra("minute", 30),
                true,
                intent.getIntExtra("alarmID", 0));

        initUI();
        setFlags(); // used to unlock device

        SharedPreferences prefs = this.getSharedPreferences("com.musicalarm.mikael.musicalarm", Context.MODE_PRIVATE);
        Config config = new Config(this, prefs.getString("com.musicalarm.mikael.musicalarm.token", ""), CLIENT_ID);

        mPlayer = Spotify.getPlayer(config, this, this);
    }

    @Override
    public void onInitialized(Player player) {
        mPlayer = player;
        mPlayer.addConnectionStateCallback(this);
        mPlayer.addPlayerNotificationCallback(this);
        mPlayer.play(alarmItem.getTrackUri());
    }

    public void initUI() {
        background = (LinearLayout) findViewById(R.id.alarm_background);
        TextView name = (TextView) findViewById(R.id.alarm_name);
        TextView artist = (TextView) findViewById(R.id.alarm_artist);
        TextView time = (TextView) findViewById(R.id.alarm_time);
        image = (ImageView) findViewById(R.id.alarm_image);

        // setting thumbnail ..
        Picasso.with(this) // context
                .load(alarmItem.getImageUrl())
                .fit()
                .centerCrop()
                .into(image, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {

                        Palette.PaletteAsyncListener paletteListener = p -> {

                            int defaultColor = ContextCompat.getColor(AlarmActivity.this,
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

        RelativeLayout dismissButton = (RelativeLayout) findViewById(R.id.dismiss_layout);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetFlags();
                mPlayer.pause();

                Intent in = new Intent(AlarmActivity.this, MainActivity.class);
                in.putExtra("alarmID", alarmItem.getAlarmID());
                in.putExtra("snooze", false);

                startActivity(in);
                AlarmActivity.this.finish();
            }
        });

        RelativeLayout snoozeButton = (RelativeLayout) findViewById(R.id.snooze_layout);
        snoozeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetFlags();
                mPlayer.pause();

                Intent in = new Intent(AlarmActivity.this, MainActivity.class);
                in.putExtra("alarmID", alarmItem.getAlarmID());
                in.putExtra("snooze", true);

                startActivity(in);
                AlarmActivity.this.finish();
            }
        });
    }

    public void setFlags() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    public void resetFlags() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    @Override
    public void onLoggedIn() {

    }

    @Override
    public void onLoggedOut() {

    }

    @Override
    public void onLoginFailed(Throwable throwable) {

    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onConnectionMessage(String s) {

    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {

    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {

    }
}
