package com.musicalarm.mikael.musicalarm;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.musicalarm.mikael.musicalarm.fragments.AddFragment;
import com.musicalarm.mikael.musicalarm.fragments.HomeFragment;
import com.musicalarm.mikael.musicalarm.fragments.RecycleUtils.RecyclerViewAdapter;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MainActivity extends FragmentActivity
        implements ConnectionStateCallback, PlayerNotificationCallback,
        AddFragment.AddFragmentListener, HomeFragment.HomeFragmentListener, RecyclerViewAdapter.AdapterListener {

    private static final String CLIENT_ID = "22a32c3cb52747b0912c3701637d53db";
    private static final String REDIRECT_URI = "musicalarm://callback";
    private static final String SHARED_PREFS = "SHARED_PREFS";
    private static final String SAVED_ALARMS_JSON = "SAVED_ALARMS";

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

    // callback when HomeFragment is ready
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void homeFragmentReady() {
        loadAlarms();
    }


    // when a user clicks "Add alarm" in AddFragment
    @Override
    public void saveClicked(AlarmItem item) {
        alarms.add(item);
        homeFragment.refreshList();
        saveAlarms();
    }

    // loading alarms from local memory
    public void loadAlarms() {
        SharedPreferences shared = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        Set<String> jsonAlarms = shared.getStringSet(SAVED_ALARMS_JSON, new HashSet<>());

        Log.d("MainActivity", "loaded hashset of size: " + jsonAlarms.size());

        alarms = new ArrayList<>();

        alarms = jsonAlarms
                .stream()
                .map(AlarmItem::buildFromString)
                .collect(Collectors.toList());

        Log.d("MainActivity", "alarms of size: " + alarms.size());

        for(AlarmItem item : alarms) {
            Log.d("MainActivity", "LOADED: " + item.getName());
        }

        homeFragment.refreshList();
    }

    // saves all alarms in json-format
    public void saveAlarms() {
        SharedPreferences shared = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();

        Set<String> jsonAlarms = alarms
                .stream()
                .map(AlarmItem::getJson)
                .collect(Collectors.toSet());

        editor.putStringSet(SAVED_ALARMS_JSON, jsonAlarms);
        editor.commit();

    }

    @Override
    public void onDeleteClick(AlarmItem item) {

        // if deleted alarm isn't in list, do nothing
        if(!alarms.contains(item))
            return;

        displayUndo(item, alarms.indexOf(item));
        alarms.remove(item);

        saveAlarms();
        homeFragment.refreshList();
    }

    // Displays snackbar that allows user to undo delete
    public void displayUndo(final AlarmItem removedItem, final int pos) {

        Snackbar sn = Snackbar.make(findViewById(R.id.snackArea),
                "Alarm removed \n \n", Snackbar.LENGTH_LONG);

        View snackbarView = sn.getView();
        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setY(-25f);
        textView.setMaxLines(3);

        // sets height of "UNDO"-text
        snackbarView.findViewById(android.support.design.R.id.snackbar_action).setY(-65f);

        sn.setAction("UNDO", view -> {
            alarms.add(pos, removedItem);
            saveAlarms();
            homeFragment.refreshList();
        });

        sn.show();
    }

    public List<AlarmItem> getAlarms() {
        return alarms;
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this); // destroy player to avoid resource leak
        super.onDestroy();
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
