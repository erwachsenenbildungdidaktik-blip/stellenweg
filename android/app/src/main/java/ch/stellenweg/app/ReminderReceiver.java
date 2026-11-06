package ch.stellenweg.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Zeigt eine geplante Erinnerung an. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String key = i.getStringExtra("key");
        if (key == null) return;
        Reminders.show(c, key, i.getStringExtra("title"), i.getStringExtra("body"), i.getStringExtra("go"));
        Reminders.markShown(c, key);
    }
}
