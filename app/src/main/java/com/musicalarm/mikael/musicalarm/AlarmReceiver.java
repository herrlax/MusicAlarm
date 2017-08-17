package com.musicalarm.mikael.musicalarm;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Created by mikael on 2017-06-08.
 */

public class AlarmReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        Intent in = new Intent(context.getApplicationContext(), AlarmActivity.class);

        // puts all we need to start the alarm
        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        in.putExtra("alarmID", intent.getIntExtra("alarmID", 0));
        in.putExtra("uri", intent.getStringExtra("uri"));
        in.putExtra("imageUrl", intent.getStringExtra("imageUrl"));
        in.putExtra("name", intent.getStringExtra("name"));
        in.putExtra("artist", intent.getStringExtra("artist"));
        in.putExtra("image", intent.getStringExtra("image"));
        in.putExtra("hour", intent.getIntExtra("hour", 0));
        in.putExtra("minute", intent.getIntExtra("minute", 0));

        context.startActivity(in);
    }
}
