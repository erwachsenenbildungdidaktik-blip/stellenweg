package ch.stellenweg.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Erinnerungen aus der Web-App: die Liste kommt als JSON, jeder Eintrag wird mit dem AlarmManager geplant.
 * Gezeigte Einträge werden gemerkt, damit nichts doppelt kommt. Nach einem Neustart plant BootReceiver neu.
 */
final class Reminders {
    static final String CHANNEL = "erinnerungen";
    private static final String PREFS = "reminders";

    private Reminders() { }

    static void schedule(Context c, String json) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        cancelAll(c, p);
        p.edit().putString("json", json == null ? "[]" : json).apply();
        planAll(c, p);
    }

    static void reschedule(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        cancelAll(c, p);
        planAll(c, p);
    }

    private static void planAll(Context c, SharedPreferences p) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        Set<String> shown = p.getStringSet("shown", new HashSet<>());
        Set<String> codes = new HashSet<>();
        long now = System.currentTimeMillis();
        try {
            JSONArray a = new JSONArray(p.getString("json", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                String key = o.optString("key");
                if (key.isEmpty() || shown.contains(key)) continue;
                long at = o.optLong("at");
                if (at < now - 60_000) continue;
                PendingIntent pi = pending(c, key, o.optString("title"), o.optString("body"), o.optString("go"));
                long when = Math.max(at, now + 2_000);
                if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms())
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
                else
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
                codes.add(key);
            }
        } catch (Exception ignored) { }
        p.edit().putStringSet("planned", codes).apply();
    }

    private static void cancelAll(Context c, SharedPreferences p) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        for (String key : p.getStringSet("planned", new HashSet<>()))
            am.cancel(pending(c, key, "", "", ""));
    }

    private static PendingIntent pending(Context c, String key, String title, String body, String go) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction("ch.stellenweg.REMINDER." + key)
                .putExtra("key", key).putExtra("title", title).putExtra("body", body).putExtra("go", go);
        return PendingIntent.getBroadcast(c, key.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void markShown(Context c, String key) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> s = new HashSet<>(p.getStringSet("shown", new HashSet<>()));
        s.add(key);
        if (s.size() > 400) s.clear();
        p.edit().putStringSet("shown", s).apply();
    }

    static void show(Context c, String key, String title, String body, String go) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, c.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription(c.getString(R.string.channel_desc));
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(c, MainActivity.class).putExtra("go", go)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, key.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setColor(c.getColor(R.color.accent))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(c).notify(key.hashCode(), b.build());
        } catch (SecurityException ignored) { /* Mitteilungen nicht erlaubt */ }
    }
}
