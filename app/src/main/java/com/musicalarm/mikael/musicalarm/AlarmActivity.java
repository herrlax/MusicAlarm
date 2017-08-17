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

public class AlarmActivity extends Activity
        implements ConnectionStateCallback, PlayerNotificationCallback {

    private ImageView image;
    private LinearLayout background;

    private AlarmItem alarmItem;
    private Player mPlayer;

    private final String REDIRECT_URI = "musicalarm://callback";
    private final String CLIENT_ID = "22a32c3cb52747b0912c3701637d53db";
    private final int REQUEST_CODE = 1337;

    private int numOfAuthTries = 0;

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

        numOfAuthTries = 0;

        // SharedPreferences prefs = this.getSharedPreferences(getString(R.string.tag_sharedprefs), Context.MODE_PRIVATE);
        // Config config = new Config(this, prefs.getString(getString(R.string.tag_sharedpref_token), ""), CLIENT_ID);
        // config.useCache(true);

        // initPlayer(config);
        authSpotify();
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


    // authorizes user towards Spotify, this is called if initPlayer fails to fetch the player
    public void authSpotify() {

        if(numOfAuthTries > 20) {
            Log.e(getString(R.string.tag_log), "Tried to auth too many times");
            return;
        }

        numOfAuthTries++;

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(
                CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);

        builder.setScopes(new String[]{"streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    // response from openLoginActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);


        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {

            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            if (response.getType() == AuthenticationResponse.Type.TOKEN) {

                Config config = new Config(this, response.getAccessToken(), CLIENT_ID);

                Spotify.getPlayer(config, AlarmActivity.this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        Log.d(getString(R.string.tag_log), "onInitialized");

                        mPlayer = player;
                        mPlayer.addConnectionStateCallback(AlarmActivity.this);
                        mPlayer.addPlayerNotificationCallback(AlarmActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(getString(R.string.tag_log), "Could not initialize player: " + throwable.getMessage());

                        // saves error logs for debugging
                        SharedPreferences debugPrefs =
                                AlarmActivity.this.getSharedPreferences(getString(R.string.tag_debug), Context.MODE_PRIVATE);
                        debugPrefs.edit()
                                .putString(getString(R.string.tag_debug_onError), throwable.getMessage())
                                .apply();


                        // Retries to auth Spotify
                        authSpotify();
                    }
                });

            }
        }
    }

    public void setFlags() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    public void resetFlags() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    @Override
    public void onLoggedIn() {
        // Plays song as soon as auth is done and user is logged in
        mPlayer.play(alarmItem.getTrackUri());
    }

    @Override
    public void onLoggedOut() {

    }

    @Override
    public void onLoginFailed(Throwable throwable) {
        Log.e(getString(R.string.tag_log), "onLoginFailed: " + throwable.getMessage());
        authSpotify();

        // saves error logs for debugging
        SharedPreferences debugPrefs = this.getSharedPreferences(getString(R.string.tag_debug), Context.MODE_PRIVATE);
        debugPrefs.edit()
                .putString(getString(R.string.tag_debug_onLoginFailed), throwable.getMessage())
                .apply();
    }

    @Override
    public void onTemporaryError() {

        Log.e(getString(R.string.tag_log), "onTemporaryError: " + System.currentTimeMillis()+"");
        Log.d(getString(R.string.tag_log), "retrying to auth..");
        authSpotify();

        // saves error logs for debugging
        SharedPreferences debugPrefs = this.getSharedPreferences(getString(R.string.tag_debug), Context.MODE_PRIVATE);
        debugPrefs.edit()
                .putString(getString(R.string.tag_debug_onTemporaryError), System.currentTimeMillis()+"")
                .apply();
    }

    @Override
    public void onConnectionMessage(String s) {
        Log.d(getString(R.string.tag_log), "onConnectionMessage: " + s);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {

    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {
        Log.e(getString(R.string.tag_log), "onPlaybackError: " + s);

        // saves error logs for debugging
        SharedPreferences debugPrefs = this.getSharedPreferences(getString(R.string.tag_debug), Context.MODE_PRIVATE);
        debugPrefs.edit()
                .putString(getString(R.string.tag_debug_onPlaybackError), s)
                .apply();
    }


    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(AlarmActivity.this); // destroy player to avoid resource leak
        super.onDestroy();
    }
}
