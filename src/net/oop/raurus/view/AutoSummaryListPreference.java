/**
TUBES 2 OOP
 **/
package net.oop.raurus.view;

import android.content.Context;
import android.util.AttributeSet;

public class AutoSummaryListPreference extends android.preference.ListPreference {

    public AutoSummaryListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) setSummary(getEntry());
    }

    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        super.onSetInitialValue(restoreValue, defaultValue);
        setSummary(getEntry());
    }
}
