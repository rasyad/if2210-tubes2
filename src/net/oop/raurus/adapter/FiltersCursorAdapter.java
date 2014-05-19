/**
TUBES 2 OOP
 **/

package net.oop.raurus.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import net.oop.raurus.R;
import net.oop.raurus.provider.FeedData.FilterColumns;

public class FiltersCursorAdapter extends ResourceCursorAdapter {

    private int filterTextColumnPosition;
    private int isAppliedToTitleColumnPosition;
    private int isAcceptRulePosition;

    private int mSelectedFilter = -1;

    public FiltersCursorAdapter(Context context, Cursor cursor) {
        super(context, R.layout.item_rule_list, cursor, 0);

        reinit(cursor);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView isAcceptRuleTextView = (TextView) view.findViewById(android.R.id.text1);
        TextView filterTextTextView = (TextView) view.findViewById(android.R.id.text2);
        TextView isAppliedToTitleTextView = (TextView) view.findViewById(R.id.text3);

        if (cursor.getPosition() == mSelectedFilter) {
            view.setBackgroundResource(android.R.color.holo_blue_dark);
        } else {
            view.setBackgroundResource(android.R.color.transparent);
        }

        boolean isAcceptRule = cursor.getInt(isAcceptRulePosition) == 1;
        isAcceptRuleTextView.setText(isAcceptRule ? R.string.accept : R.string.reject);
        isAcceptRuleTextView.setTextColor(isAcceptRule ? context.getResources().getColor(android.R.color.holo_green_dark) :
                context.getResources().getColor(android.R.color.holo_red_dark));
        filterTextTextView.setText(cursor.getString(filterTextColumnPosition));
        isAppliedToTitleTextView.setText(cursor.getInt(isAppliedToTitleColumnPosition) == 1 ? R.string.filter_apply_to_title : R.string.filter_apply_to_content);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        reinit(cursor);
        super.changeCursor(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        reinit(newCursor);
        return super.swapCursor(newCursor);
    }

    @Override
    public void notifyDataSetChanged() {
        reinit(null);
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        reinit(null);
        super.notifyDataSetInvalidated();
    }

    private void reinit(Cursor cursor) {
        if (cursor != null) {
            filterTextColumnPosition = cursor.getColumnIndex(FilterColumns.FILTER_TEXT);
            isAppliedToTitleColumnPosition = cursor.getColumnIndex(FilterColumns.IS_APPLIED_TO_TITLE);
            isAcceptRulePosition = cursor.getColumnIndex(FilterColumns.IS_ACCEPT_RULE);
        }
    }

    public void setSelectedFilter(int filterPos) {
        mSelectedFilter = filterPos;
    }

    public int getSelectedFilter() {
        return mSelectedFilter;
    }
}
