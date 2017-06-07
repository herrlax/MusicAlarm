package com.musicalarm.mikael.musicalarm;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.WindowManager;

import com.musicalarm.mikael.musicalarm.fragments.AddFragment;
import com.musicalarm.mikael.musicalarm.fragments.HomeFragment;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity
        implements ConnectionStateCallback, PlayerNotificationCallback, AddFragment.AddFragmentListener, HomeFragment.HomeFragmentListener {

    private static final String CLIENT_ID = "22a32c3cb52747b0912c3701637d53db";
    private static final String REDIRECT_URI = "musicalarm://callback";

    public static Player mPlayer;
    public static String token;

    private static final int REQUEST_CODE = 1337;

    private List<AlarmItem> alarms = new ArrayList<>();


    private HomeFragment homeFragment;
    private AddFragment addFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // allows drawing under navigation bar and status bar
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        authSpotify();

    }

    // authorizes user towards Spotify
    public void authSpotify() {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(
                CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {

            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            if (response.getType() == AuthenticationResponse.Type.TOKEN) {

                Log.d("MainActivity", "token: " + response.getAccessToken());
                token = response.getAccessToken();

                // inits the Spotify player if auth is correct
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {

                    @Override
                    public void onInitialized(Player spotifyPlayer) {
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addPlayerNotificationCallback(MainActivity.this);

                        initHomeFragment();
                        Log.d("MainActivity", "Init player correctly");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    // opens the home fragment
    public void initHomeFragment() {

        homeFragment = new HomeFragment();

        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container, homeFragment)
                .commit();
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this); // destroy player to avoid resource leak
        super.onDestroy();
    }

    // when a user clicks "Add new" in HomeFragment
    @Override
    public void addButtonClicked() {
        addFragment = new AddFragment();
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container, addFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }


    // when a user clicks "Add alarm" in AddFragment
    @Override
    public void saveClicked(AlarmItem item) {
        Log.d("MainActivity", item.getArtist() + ":" + item.getName() + ":" + item.getHour());
        alarms.add(item);
        homeFragment.refreshList();
    }

    public List<AlarmItem> getAlarms() {
        return alarms;
    }

    @Override
    public void onLoggedIn() {}
    @Override
    public void onLoggedOut() {}
    @Override
    public void onLoginFailed(Throwable throwable) {}
    @Override
    public void onTemporaryError() {}
    @Override
    public void onConnectionMessage(String s) {}
    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {}
    @Override
    public void onPlaybackError(ErrorType errorType, String s) {}
}
