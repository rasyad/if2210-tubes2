/*
TUBES 2 OOP
 */

package net.oop.raurus;

import android.app.Application;
import android.content.Context;

import net.oop.raurus.utils.PrefUtils;

public class MainApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, false); // init
    }

    public static Context getContext() {
        return context;
    }
}
