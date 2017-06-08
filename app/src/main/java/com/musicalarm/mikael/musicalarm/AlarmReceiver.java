package com.musicalarm.mikael.musicalarm;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by mikael on 2017-06-08.
 */

public class AlarmReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d("MainActivity", "AlarmReceiver -> AN ALARM TRIGGERED");
        Log.d("MainActivity", intent.getStringExtra("artist") + " - " + intent.getStringExtra("name"));

        //perform your task
        Intent in = new Intent(context, MainActivity.class);
        in.putExtra("name", intent.getStringExtra("name"));
        in.putExtra("artist", intent.getStringExtra("artist"));
        in.putExtra("time", intent.getStringExtra("time"));
        in.putExtra("image", intent.getStringExtra("image"));
        in.putExtra("alarmID", intent.getStringExtra("alarmID"));

        context.startActivity(in);

        /*MainActivity m = new MainActivity();
        m.triggerAlarm(intent);*/

        /*if(MainActivity.mPlayer != null) {
            MainActivity.mPlayer.play(intent.getStringExtra("uri"));
        }*/
    }
}
