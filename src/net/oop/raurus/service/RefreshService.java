/**
TUBES 2 OOP
 **/

package net.oop.raurus.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.os.SystemClock;

import net.oop.raurus.Constants;
import net.oop.raurus.utils.PrefUtils;

public class RefreshService extends Service {
    public static final String SIXTY_MINUTES = "3600000";

    public static class RefreshAlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            context.startService(new Intent(context, FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS).putExtra(Constants.FROM_AUTO_REFRESH, true));
        }
    }

    private final OnSharedPreferenceChangeListener listener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefUtils.REFRESH_INTERVAL.equals(key)) {
                restartTimer(false);
            }
        }
    };

    private AlarmManager alarmManager;
    private PendingIntent timerIntent;

    @Override
    public IBinder onBind(Intent intent) {
        onRebind(intent);
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true; // we want to use rebind
    }

    @Override
    public void onCreate() {
        super.onCreate();

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        PrefUtils.registerOnPrefChangeListener(listener);
        restartTimer(true);
    }

    private void restartTimer(boolean created) {
        if (timerIntent == null) {
            timerIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, RefreshAlarmReceiver.class), 0);
        } else {
            alarmManager.cancel(timerIntent);
        }

        int time = 3600000;
        try {
            time = Math.max(60000, Integer.parseInt(PrefUtils.getString(PrefUtils.REFRESH_INTERVAL, SIXTY_MINUTES)));
        } catch (Exception ignored) {
        }

        long initialRefreshTime = SystemClock.elapsedRealtime() + 10000;

        if (created) {
            long lastRefresh = PrefUtils.getLong(PrefUtils.LAST_SCHEDULED_REFRESH, 0);

            if (lastRefresh > 0) {
                // this indicates a service restart by the system
                initialRefreshTime = Math.max(SystemClock.elapsedRealtime() + 10000, lastRefresh + time);
            }
        }

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, initialRefreshTime, time, timerIntent);
    }

    @Override
    public void onDestroy() {
        if (timerIntent != null) {
            alarmManager.cancel(timerIntent);
        }
        PrefUtils.unregisterOnPrefChangeListener(listener);
        super.onDestroy();
    }
}
