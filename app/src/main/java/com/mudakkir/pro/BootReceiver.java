package com.mudakkir.pro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        SharedPreferences prefs = context.getSharedPreferences(AlarmHelper.PREFS_NAME, Context.MODE_PRIVATE);
        String savedJson = prefs.getString(AlarmHelper.KEY_ALARMS_JSON, "[]");
        AlarmHelper.syncAlarms(context, savedJson);
    }
}
