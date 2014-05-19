package net.oop.raurus.loader;

/**
TUBES 2 OOP
 **/

import android.content.AsyncTaskLoader;
import android.content.Context;

public abstract class BaseLoader<D> extends AsyncTaskLoader<D> {
    private D data;

    public BaseLoader(Context context) {
        super(context);
    }

    @Override
    public void deliverResult(D data) {
        if (isReset()) {
                      return;
        }

        this.data = data;

        super.deliverResult(data);
    }

    @Override
    protected void onStartLoading() {
        if (data != null) {
            deliverResult(data);
        }

        if (takeContentChanged() || data == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
     
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();

       
        onStopLoading();

        data = null;
    }
}