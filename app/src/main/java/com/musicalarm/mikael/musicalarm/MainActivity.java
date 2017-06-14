package com.musicalarm.mikael.musicalarm;

import android.app.AlarmManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.musicalarm.mikael.musicalarm.fragments.AddFragment;
import com.musicalarm.mikael.musicalarm.fragments.AlarmFragment;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class MainActivity extends FragmentActivity
        implements ConnectionStateCallback, PlayerNotificationCallback,
        AddFragment.AddFragmentListener, HomeFragment.HomeFragmentListener,
        AlarmFragment.AlarmListener, RecyclerViewAdapter.AdapterListener {

    private static final String CLIENT_ID = "22a32c3cb52747b0912c3701637d53db";
    private static final String REDIRECT_URI = "musicalarm://callback";
    private static final String SHARED_PREFS = "SHARED_PREFS";
    private static final String SAVED_ALARMS_JSON = "SAVED_ALARMS";

    private Player mPlayer;
    private static AlarmManager alarmManager;

    private boolean alarmTriggered;

    public static String token;

    private static final int REQUEST_CODE = 1337;

    private List<AlarmItem> alarms = new ArrayList<>();

    private HomeFragment homeFragment;
    private AddFragment addFragment;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadAlarms();

        // if an intent with an alarmID started this activity, it was AlarmReceiver
        if(getIntent().getStringExtra("alarmID") != null)
            alarmTriggered = true;

        // allows drawing under navigation bar and status bar
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
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

                token = response.getAccessToken();

                // inits the Spotify player if auth is correct
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {

                    @Override
                    public void onInitialized(Player spotifyPlayer) {
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addPlayerNotificationCallback(MainActivity.this);

                        // if an alarm was triggered, start the alarm activity
                        if(alarmTriggered) {
                            triggerAlarm(getIntent().getStringExtra("alarmID"));
                            alarmTriggered = false;
                        } else { // else go to home
                            initHomeFragment();
                        }

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
    @Override
    public void homeFragmentReady() {
        homeFragment.refreshList(); // refreshes list in HomeFragment to get loaded alarms
    }


    // when a user clicks "Add alarm" in AddFragment
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void saveClicked(AlarmItem item) {

        alarms.add(item);
        homeFragment.refreshList();
        saveAlarms();

        scheduleAlarm(item, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void deleteClicked(AlarmItem item) {
        onDeleteClick(item);
    }

    // loading alarms from local memory
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void loadAlarms() {
        SharedPreferences shared = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        Set<String> jsonAlarms = shared.getStringSet(SAVED_ALARMS_JSON, new HashSet<>());

        alarms = new ArrayList<>();
        alarms = jsonAlarms
                .stream()
                .map(AlarmItem::buildFromString)
                .collect(Collectors.toList());
    }

    // saves all alarms in json-format
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void saveAlarms() {
        SharedPreferences shared = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();

        Set<String> jsonAlarms = alarms
                .stream()
                .map(AlarmItem::getJson)
                .collect(Collectors.toSet());

        editor.putStringSet(SAVED_ALARMS_JSON, jsonAlarms);
        //editor.commit();
        editor.apply();
    }

    /**
     * Schedules an alarm to be triggered
     * @param alarmItem alarm to be triggered
     * @param alarmTime optional time (in ms) to trigger alarm,
     *                  if other than stated in alarmItem
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void scheduleAlarm(AlarmItem alarmItem, @Nullable Long alarmTime) {

        PendingIntent pi = alarmItemToPendingIntent(alarmItem);

        Calendar calendar = new GregorianCalendar();

        // if a specific alarm time is stated, used this, instead of the one
        // set in alarmTime
        if(alarmTime != null) {
            calendar.setTimeInMillis(alarmTime);

            // notifies the user of the scheduled alarm
            notifyUserOfSchedule(alarmTime - System.currentTimeMillis(),
                    "scheduled");
        } else { // use alarm time set in alarm

            calendar.set(Calendar.HOUR_OF_DAY, alarmItem.getHour());
            calendar.set(Calendar.MINUTE, alarmItem.getMinute());
            calendar.set(Calendar.SECOND, 0);

            // if alarm is set to a time earlier than now, assume it's for tomorrow (in +86400000 ms)
            if(calendar.getTimeInMillis() < System.currentTimeMillis())
                calendar.setTimeInMillis(calendar.getTimeInMillis() + 86400000);

            // notifies the user of the scheduled alarm
            notifyUserOfSchedule(calendar.getTimeInMillis() - System.currentTimeMillis(),
                    "scheduled");
        }

        alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pi);
    }

    /**
     * Notifies the user of the scheduled alarm
     * @param time amount of time until scheduled alarm
     */
    public void notifyUserOfSchedule(long time, String type) {

        long minutes = time/1000/60;
        long hours = minutes/60;

        String minutesFromNow = (hours >= 1 ? minutes - 60 * hours : minutes) + " minutes";
        String hoursFromNow = hours >= 1 ? hours + " hours and " : "";
        String sufix = type.equals("scheduled") ? " from now" : "";

        Snackbar sn = Snackbar.make(findViewById(R.id.snackArea),
                "Alarm " + type + " for " + hoursFromNow + minutesFromNow + sufix + " \n \n",
                Snackbar.LENGTH_LONG);

        View snackbarView = sn.getView();
        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setY(-25f);
        textView.setMaxLines(3);

        // sets height
        snackbarView.findViewById(android.support.design.R.id.snackbar_action).setY(-65f);

        if(type.equals("snoozed")) {

            sn.setAction("CHANGE", view -> {
                // TODO let user pick snooze time
            });
        } else if(type.equals("scheduled")) {

            sn.setAction("UNDO", view -> {
                // TODO open AddFragment and let user edit alarm
            });
        }

        sn.show();
    }

    public void triggerAlarm(String id) {

        // finds correct alarm based on the id
        AlarmItem triggeredAlarm = alarms
                .stream()
                .filter(x -> x.getAlarmID() == Integer.parseInt(id))
                .findFirst()
                .get();

        mPlayer.play(triggeredAlarm.getTrackUri());

        AlarmFragment alarmFragment = new AlarmFragment();
        alarmFragment.setAlarmItem(triggeredAlarm);

        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container, alarmFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Constructs a PendingIntent from an AlarmItem
     */
    public PendingIntent alarmItemToPendingIntent(AlarmItem item) {

        Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
        intent.putExtra("uri", item.getTrackUri());
        intent.putExtra("name", item.getName());
        intent.putExtra("artist", item.getArtist());
        intent.putExtra("image", item.getImageUrl());

        String hourPrefix = item.getHour() < 10 ? "0" : "";
        String minutePrefix = item.getMinute() < 10 ? "0" : "";

        intent.putExtra("time", hourPrefix + item.getHour() + ":" + minutePrefix + item.getMinute());
        intent.putExtra("alarmID", item.getAlarmID()+"");

        return PendingIntent.getBroadcast(MainActivity.this,
                item.getAlarmID(), intent, FLAG_UPDATE_CURRENT);
    }


    /**
     * Deletes and alarms and cancels it in the alarm manager
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDeleteClick(AlarmItem item) {

        displayUndo(item, alarms.indexOf(item));
        alarms.remove(item);
        alarmManager.cancel(
                alarmItemToPendingIntent(item));

        saveAlarms();
        homeFragment.refreshList();
    }

    @Override
    public void onItemClicked(AlarmItem item) {
        addFragment = new AddFragment();
        addFragment.setOldAlarmItem(item); // old alarm is needed to be removed
        addFragment.setAlarmItem(item);
        addFragment.setEditing();
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container, addFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    // Displays snackbar that allows user to undo delete
    @RequiresApi(api = Build.VERSION_CODES.N)
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDismiss(AlarmItem alarmItem) {
        mPlayer.pause();                // stops music
        initHomeFragment();             // opens home for user
        scheduleAlarm(alarmItem, null); // schedules alarm in 24 hrs
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onSnooze(AlarmItem alarmItem) {
        mPlayer.pause();
        initHomeFragment();
        scheduleAlarm(alarmItem, System.currentTimeMillis() + 600000); // schedules alarm in 10 min (600000 ms)
    }
}
