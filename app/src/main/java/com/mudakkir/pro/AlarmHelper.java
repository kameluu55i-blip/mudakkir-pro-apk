package com.mudakkir.pro;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class AlarmHelper {
    public static final String CHANNEL_ID = "mudakkir_instant_alerts";
    public static final String PREFS_NAME = "mudakkir_native_prefs";
    public static final String KEY_ALARMS_JSON = "scheduled_alarms_json";
    public static final String KEY_CODES_SET = "scheduled_request_codes";

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel existing = nm.getNotificationChannel(CHANNEL_ID);
            if (existing != null) return;

            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "تنبيهات الاشتراكات والملاحظات الفورية",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("إشعارات فورية عند حلول موعد تجديد اشتراك أو تذكير ملاحظة");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 350, 200, 450});
            channel.setSound(alarmSound, attrs);
            nm.createNotificationChannel(channel);
        }
    }

    public static void syncAlarms(Context context, String jsonStr) {
        ensureChannel(context);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        // Cancel previously scheduled PendingIntents
        Set<String> oldCodes = prefs.getStringSet(KEY_CODES_SET, new HashSet<>());
        for (String codeStr : oldCodes) {
            try {
                int reqCode = Integer.parseInt(codeStr);
                Intent cancelIntent = new Intent(context, AlarmReceiver.class);
                PendingIntent pi = PendingIntent.getBroadcast(
                        context,
                        reqCode,
                        cancelIntent,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                );
                if (pi != null) {
                    am.cancel(pi);
                    pi.cancel();
                }
            } catch (Exception ignored) {
            }
        }

        Set<String> newCodes = new HashSet<>();
        long now = System.currentTimeMillis();

        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.getJSONObject(i);
                long triggerAt = item.optLong("triggerAt", 0L);
                if (triggerAt <= now) continue;

                String id = item.optString("id", "item_" + i);
                String title = item.optString("title", "حان موعد التنبيه!");
                String body = item.optString("body", "افتح تطبيق مُذكِّر برو لمراجعة التفاصيل");
                int reqCode = Math.abs(id.hashCode() % 900000) + 1000;

                Intent alarmIntent = new Intent(context, AlarmReceiver.class);
                alarmIntent.putExtra("notif_id", reqCode);
                alarmIntent.putExtra("title", title);
                alarmIntent.putExtra("body", body);

                PendingIntent pi = PendingIntent.getBroadcast(
                        context,
                        reqCode,
                        alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                    } else {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                    }
                    newCodes.add(String.valueOf(reqCode));
                } catch (SecurityException se) {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                    newCodes.add(String.valueOf(reqCode));
                }
            }
        } catch (Exception ignored) {
        }

        prefs.edit()
                .putString(KEY_ALARMS_JSON, jsonStr)
                .putStringSet(KEY_CODES_SET, newCodes)
                .apply();
    }

    public static void showInstantNotification(Context context, int notifId, String title, String body) {
        ensureChannel(context);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPi = PendingIntent.getActivity(
                context,
                notifId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(contentPi);

        nm.notify(notifId, builder.build());
    }
}
