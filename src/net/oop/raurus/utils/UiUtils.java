/**
TUBES 2 OOP
 **/

package net.oop.raurus.utils;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LongSparseArray;
import android.util.TypedValue;

import net.oop.raurus.MainApplication;
import net.oop.raurus.R;

public class UiUtils {

    static private LongSparseArray<Bitmap> sFaviconCache = new LongSparseArray<Bitmap>();

    static public void setPreferenceTheme(Activity a) {
        if (!PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true)) {
            a.setTheme(R.style.Theme_Dark);
        }
    }

    static public int dpToPixel(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, MainApplication.getContext().getResources().getDisplayMetrics());
    }

    static public Bitmap getFaviconBitmap(long feedId, Cursor cursor, int iconCursorPos) {
        Bitmap bitmap = UiUtils.sFaviconCache.get(feedId);
        if (bitmap == null) {
            byte[] iconBytes = cursor.getBlob(iconCursorPos);
            if (iconBytes != null && iconBytes.length > 0) {
                bitmap = UiUtils.getScaledBitmap(iconBytes, 18);
                UiUtils.sFaviconCache.put(feedId, bitmap);
            }
        }
        return bitmap;
    }

    static public Bitmap getScaledBitmap(byte[] iconBytes, int sizeInDp) {
        if (iconBytes != null && iconBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
            if (bitmap != null && bitmap.getWidth() != 0 && bitmap.getHeight() != 0) {
                int bitmapSizeInDip = UiUtils.dpToPixel(sizeInDp);
                if (bitmap.getHeight() != bitmapSizeInDip) {
                    Bitmap tmp = bitmap;
                    bitmap = Bitmap.createScaledBitmap(tmp, bitmapSizeInDip, bitmapSizeInDip, false);
                    tmp.recycle();
                }

                return bitmap;
            }
        }

        return null;
    }
}
