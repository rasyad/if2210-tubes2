/**
TUBES 2 OOP
 **/

package net.oop.raurus.adapter;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import net.oop.raurus.Constants;


public abstract class CursorLoaderExpandableListAdapter extends BaseExpandableListAdapter {
    private static final String URI_ARG = "uri";
    private final Activity mActivity;
    private final LoaderManager mLoaderMgr;
    private final Uri mGroupUri;

    private final int mCollapsedGroupLayout;
    private final int mExpandedGroupLayout;
    private final int mChildLayout;
    private final LayoutInflater mInflater;

    private Cursor mGroupCursor;
  
    private final SparseArray<Pair<Cursor, Boolean>> mChildrenCursors = new SparseArray<Pair<Cursor, Boolean>>();

    private final LoaderManager.LoaderCallbacks<Cursor> mGroupLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            CursorLoader cursorLoader = new CursorLoader(mActivity, mGroupUri, null, null, null, null) {

                @Override
                public Cursor loadInBackground() {
                    Cursor c = super.loadInBackground();
                    onCursorLoaded(mActivity, c);
                    return c;
                }

            };
            cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mGroupCursor = data;
            setAllChildrenCursorsAsObsolete();
            notifyDataSetChanged();
            notifyDataSetChanged(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mGroupCursor = null;
            setAllChildrenCursorsAsObsolete();
            notifyDataSetInvalidated();
        }
    };

    private void setAllChildrenCursorsAsObsolete() {
        int key;
        for (int i = 0; i < mChildrenCursors.size(); i++) {
            key = mChildrenCursors.keyAt(i);
            mChildrenCursors.put(key, new Pair<Cursor, Boolean>(mChildrenCursors.get(key).first, true));
        }
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mChildrenLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            CursorLoader cursorLoader = new CursorLoader(mActivity, (Uri) args.getParcelable(URI_ARG), null, null, null, null) {

                @Override
                public Cursor loadInBackground() {
                    Cursor c = super.loadInBackground();
                    onCursorLoaded(mActivity, c);
                    return c;
                }

            };
            cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mChildrenCursors.put(loader.getId() - 1, new Pair<Cursor, Boolean>(data, false));
            notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mChildrenCursors.delete(loader.getId() - 1);
            notifyDataSetInvalidated();
        }
    };


    public CursorLoaderExpandableListAdapter(Activity activity, Uri groupUri, int collapsedGroupLayout, int expandedGroupLayout, int childLayout) {
        mActivity = activity;
        mLoaderMgr = activity.getLoaderManager();
        mGroupUri = groupUri;

        mCollapsedGroupLayout = collapsedGroupLayout;
        mExpandedGroupLayout = expandedGroupLayout;
        mChildLayout = childLayout;

        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mLoaderMgr.restartLoader(0, null, mGroupLoaderCallback);
    }


    public CursorLoaderExpandableListAdapter(Activity activity, Uri groupUri, int groupLayout, int childLayout) {
        this(activity, groupUri, groupLayout, groupLayout, childLayout);
    }

    public View newChildView(ViewGroup parent) {
        return mInflater.inflate(mChildLayout, parent, false);
    }

    public View newGroupView(boolean isExpanded, ViewGroup parent) {
        return mInflater.inflate((isExpanded) ? mExpandedGroupLayout : mCollapsedGroupLayout, parent, false);
    }

    abstract protected Uri getChildrenUri(Cursor groupCursor);

    @Override
    public Cursor getChild(int groupPosition, int childPosition) {
        // Return this group's children Cursor pointing to the particular child
        Pair<Cursor, Boolean> childCursor = mChildrenCursors.get(groupPosition);
        if (childCursor != null && !childCursor.first.isClosed()) {
            childCursor.first.moveToPosition(childPosition);
            return childCursor.first;
        }

        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        Pair<Cursor, Boolean> childrenCursor = mChildrenCursors.get(groupPosition);
        if (childrenCursor != null && !childrenCursor.first.isClosed() && childrenCursor.first.moveToPosition(childPosition)) {
            return childrenCursor.first.getLong(childrenCursor.first.getColumnIndex("_id"));
        }

        return 0;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Pair<Cursor, Boolean> cursor = mChildrenCursors.get(groupPosition);
        if (cursor == null || cursor.first.isClosed() || !cursor.first.moveToPosition(childPosition)) {
            throw new IllegalStateException("cuman kalo kursornya valid");
        }

        View v;
        if (convertView == null) {
            v = newChildView(parent);
        } else {
            v = convertView;
        }
        bindChildView(v, mActivity, cursor.first);
        return v;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        Pair<Cursor, Boolean> childCursor = mChildrenCursors.get(groupPosition);

        // We need to restart the loader
        if ((childCursor == null || childCursor.second) && mGroupCursor != null && !mGroupCursor.isClosed() && mGroupCursor.moveToPosition(groupPosition)) {
            Bundle args = new Bundle();
            args.putParcelable(URI_ARG, getChildrenUri(mGroupCursor));
            mLoaderMgr.restartLoader(groupPosition + 1, args, mChildrenLoaderCallback);
        }

        if (childCursor != null && !childCursor.first.isClosed()) {
            return childCursor.first.getCount();
        }

        return 0;
    }

    @Override
    public Cursor getGroup(int groupPosition) {
        // Return the group Cursor pointing to the given group
        if (mGroupCursor != null && !mGroupCursor.isClosed()) {
            mGroupCursor.moveToPosition(groupPosition);
        }
        return mGroupCursor;
    }

    @Override
    public int getGroupCount() {
        if (mGroupCursor != null && !mGroupCursor.isClosed()) {
            return mGroupCursor.getCount();
        }

        return 0;
    }

    @Override
    public long getGroupId(int groupPosition) {
        if (mGroupCursor != null && !mGroupCursor.isClosed() && mGroupCursor.moveToPosition(groupPosition)) {
            return mGroupCursor.getLong(mGroupCursor.getColumnIndex("_id"));
        }

        return 0;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (mGroupCursor == null || mGroupCursor.isClosed() || !mGroupCursor.moveToPosition(groupPosition)) {
            throw new IllegalStateException("cuman kalo kurosr valid");
        }

        View v;
        if (convertView == null) {
            v = newGroupView(isExpanded, parent);
        } else {
            v = convertView;
        }
        bindGroupView(v, mActivity, mGroupCursor, isExpanded);
        return v;
    }


    protected abstract void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded);


    protected abstract void bindChildView(View view, Context context, Cursor cursor);


    protected abstract void onCursorLoaded(Context context, Cursor cursor);

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        mLoaderMgr.destroyLoader(groupPosition + 1);
        mChildrenCursors.delete(groupPosition);
    }


    public void notifyDataSetChanged(Cursor data) {
    }
}
