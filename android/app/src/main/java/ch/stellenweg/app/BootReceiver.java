package ch.stellenweg.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Nach einem Neustart oder Update gehen geplante Alarme verloren; hier werden sie neu geplant. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        Reminders.reschedule(c);
    }
}
