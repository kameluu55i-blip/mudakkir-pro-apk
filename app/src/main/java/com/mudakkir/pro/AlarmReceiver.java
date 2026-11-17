package com.mudakkir.pro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        int notifId = intent.getIntExtra("notif_id", (int) (System.currentTimeMillis() % 100000));
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        if (title == null || title.isEmpty()) {
            title = "تنبيه فوري من مُذكِّر برو 🔔";
        }
        if (body == null) {
            body = "حان الموعد المحدد للاشتراك أو الملاحظة الآن.";
        }
        AlarmHelper.showInstantNotification(context, notifId, title, body);
    }
}
