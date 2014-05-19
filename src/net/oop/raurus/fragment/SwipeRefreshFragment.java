/**
TUBES 2 OOP
 **/
package net.oop.raurus.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class SwipeRefreshFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private SwipeRefreshLayout mRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRefreshLayout = new SwipeRefreshLayout(inflater.getContext());
        inflateView(inflater, mRefreshLayout, savedInstanceState);

        return mRefreshLayout;
    }

    abstract public View inflateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRefreshLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_blue_dark,
                android.R.color.holo_blue_bright,
                android.R.color.holo_blue_dark);
        mRefreshLayout.setOnRefreshListener(this);
    }


    public void showSwipeProgress() {
        mRefreshLayout.setRefreshing(true);
    }


    public void hideSwipeProgress() {
        mRefreshLayout.setRefreshing(false);
    }

  
    public void enableSwipe() {
        mRefreshLayout.setEnabled(true);
    }


    public void disableSwipe() {
        mRefreshLayout.setEnabled(false);
    }


    public boolean isRefreshing() {
        return mRefreshLayout.isRefreshing();
    }
}