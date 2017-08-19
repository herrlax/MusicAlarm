package com.musicalarm.mikael.musicalarm;

import android.app.AlarmManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Context;
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
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.musicalarm.mikael.musicalarm.fragments.AddFragment;
import com.musicalarm.mikael.musicalarm.fragments.HomeFragment;
import com.musicalarm.mikael.musicalarm.fragments.RecycleUtils.RecyclerViewAdapter;
import com.musicalarm.mikael.musicalarm.fragments.SnoozeFragment;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Spotify;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class MainActivity extends FragmentActivity
        implements AddFragment.AddFragmentListener, HomeFragment.HomeFragmentListener,
        RecyclerViewAdapter.AdapterListener,
        SnoozeFragment.SnoozeFragmentListener{

    private final String CLIENT_ID = "22a32c3cb52747b0912c3701637d53db";
    private final String REDIRECT_URI = "musicalarm://callback";
    private final String SHARED_PREFS = "com.musicalarm.mikael.musicalarm";
    private final String SAVED_ALARMS_JSON = "SAVED_ALARMS";

    private AlarmManager alarmManager;

    private String token;

    private final int REQUEST_CODE = 1337;

    private List<AlarmItem> alarms = new ArrayList<>();

    private HomeFragment homeFragment;
    private AddFragment addFragment;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(getString(R.string.tag_log), "onCreate");

        initHomeFragment();
        loadAlarms();

        // allows drawing under navigation bar and status bar
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        authSpotify();

        if(getIntent().getIntExtra("alarmID", 0) != 0) {
            AlarmItem triggeredAlarm = alarms
                    .stream()
                    .filter(x -> x.getAlarmID() == getIntent().getIntExtra("alarmID", 0))
                    .findFirst()
                    .get();

            if(getIntent().getBooleanExtra("snooze", false)) {
                scheduleAlarm(triggeredAlarm, System.currentTimeMillis() + 600000);
            } else {
                scheduleAlarm(triggeredAlarm, null);
            }

        }

        printDebugLogs();
    }

    // authorizes user towards Spotify
    public void authSpotify() {

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

                token = response.getAccessToken();

                // saves auth token locally for use in AlarmActivity
                SharedPreferences prefs = this.getSharedPreferences(getString(R.string.tag_sharedprefs), Context.MODE_PRIVATE);
                prefs.edit()
                        .putString(getString(R.string.tag_sharedpref_token), token)
                        .apply();

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
                .addToBackStack("addFragment")
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
        displayUndo(item, alarms.indexOf(item));
        removeAlarm(item);

        saveAlarms();
        homeFragment.refreshList();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void updateAlarm(AlarmItem oldAlarm, AlarmItem newAlarm) {
        removeAlarm(oldAlarm);
        alarms.add(newAlarm);
        scheduleAlarm(newAlarm, null);

        saveAlarms();
        homeFragment.refreshList();
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

        PendingIntent pi;

        Calendar calendar = new GregorianCalendar();

        // if a specific alarm time is stated, used this, instead of the one
        // set in alarmTime
        if(alarmTime != null) {
            calendar.setTimeInMillis(alarmTime);

            // notifies the user of the scheduled alarm
            notifyUserOfSchedule(alarmTime - System.currentTimeMillis()  + 1000, // adds a second of visual reasons
                    "snoozed",
                    alarmItem);

            // sets new time for snooze
            AlarmItem snoozeAlarm = alarmItem.clone();
            snoozeAlarm.setMinute((int) ((alarmTime / (1000*60)) % 60));
            snoozeAlarm.setHour((int) ((alarmTime / (1000*60*60)) % 24));

            pi = alarmItemToPendingIntent(snoozeAlarm);

        } else { // use alarm time set in alarm

            calendar.set(Calendar.HOUR_OF_DAY, alarmItem.getHour());
            calendar.set(Calendar.MINUTE, alarmItem.getMinute());
            calendar.set(Calendar.SECOND, 0);

            // if alarm is set to a time earlier than now, assume it's for tomorrow (in +86400000 ms)
            if(calendar.getTimeInMillis() < System.currentTimeMillis())
                calendar.setTimeInMillis(calendar.getTimeInMillis() + 86400000);

            // notifies the user of the scheduled alarm
            notifyUserOfSchedule(calendar.getTimeInMillis() - System.currentTimeMillis(),
                    "scheduled",
                    alarmItem);

            pi = alarmItemToPendingIntent(alarmItem);
        }

        if(alarmManager == null)
            alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);


        alarmManager.setAlarmClock(
                new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pi),
                pi);
    }

    /**
     * Notifies the user of the scheduled alarm
     * @param time amount of time until scheduled alarm
     */
    public void notifyUserOfSchedule(long time, String type, AlarmItem alarmItem) {

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

                if(alarmManager == null)
                    alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

                // cancels alarm, so it wont go off while editing..
                alarmManager.cancel(
                        alarmItemToPendingIntent(alarmItem));

                fadeBackground(1.0f, 0.25f);
                SnoozeFragment snoozeFragment = new SnoozeFragment();
                snoozeFragment.setAlarmItem(alarmItem);
                snoozeFragment.setTimeValue(minutes);

                getFragmentManager().beginTransaction()
                        .setCustomAnimations(R.animator.slide_up, 0)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(R.id.fragment_container, snoozeFragment)
                        .addToBackStack("snoozeFragment")
                        .commit();

            });
        } else if(type.equals("scheduled")) {

            // TODO let user undo
        }

        sn.show();
    }

    /**
     * Constructs a PendingIntent from an AlarmItem
     */
    public PendingIntent alarmItemToPendingIntent(AlarmItem item) {

        Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
        intent.putExtra("uri", item.getTrackUri());
        intent.putExtra("name", item.getName());
        intent.putExtra("artist", item.getArtist());
        intent.putExtra("imageUrl", item.getImageUrl());
        intent.putExtra("hour", item.getHour());
        intent.putExtra("minute", item.getMinute());
        intent.putExtra("alarmID", item.getAlarmID());

        return PendingIntent.getBroadcast(MainActivity.this,
                item.getAlarmID(), intent, FLAG_UPDATE_CURRENT);
    }


    /**
     * Deletes and alarms and cancels it in the alarm manager
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void removeAlarm(AlarmItem item) {

        alarms.remove(item);

        if(alarmManager == null)
            alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        alarmManager.cancel(
                alarmItemToPendingIntent(item));
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
                .addToBackStack("addFragment")
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
            scheduleAlarm(removedItem, null);
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
    public void onExitSnoozeDialog() {
        fadeBackground(0.25f, 1.0f);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onSnoozeChanged(AlarmItem alarmItem, Long snoozeTime) {
        fadeBackground(0.25f, 1.0f);
        scheduleAlarm(alarmItem, System.currentTimeMillis() + snoozeTime);
    }

    public void fadeBackground(float start, float end) {

        AlphaAnimation animation1 = new AlphaAnimation(start, end);
        animation1.setDuration(500);
        animation1.setFillAfter(true);
        homeFragment.getView().startAnimation(animation1);
    }

    // prints logs of errors for debugging
    public void printDebugLogs() {
        SharedPreferences debugPrefs =
                this.getSharedPreferences(getString(R.string.tag_debug), Context.MODE_PRIVATE);

        Log.e(getString(R.string.tag_log), "onPlaybackError: " + debugPrefs.getString(getString(R.string.tag_debug_onPlaybackError), ""));
        Log.e(getString(R.string.tag_log), "onTemporaryError: " + debugPrefs.getString(getString(R.string.tag_debug_onTemporaryError), ""));
        Log.e(getString(R.string.tag_log), "onError: " + debugPrefs.getString(getString(R.string.tag_debug_onError), ""));
        Log.e(getString(R.string.tag_log), "onLoginFailed: " + debugPrefs.getString(getString(R.string.tag_debug_onLoginFailed), ""));

    }
}
