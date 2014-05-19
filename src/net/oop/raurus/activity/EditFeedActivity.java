/**
TUBES 2 OOP
 **/
package net.oop.raurus.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.Toast;

import net.oop.raurus.Constants;
import net.oop.raurus.R;
import net.oop.raurus.adapter.FiltersCursorAdapter;
import net.oop.raurus.loader.BaseLoader;
import net.oop.raurus.provider.FeedData.FeedColumns;
import net.oop.raurus.provider.FeedData.FilterColumns;
import net.oop.raurus.provider.FeedDataContentProvider;
import net.oop.raurus.utils.NetworkUtils;
import net.oop.raurus.utils.UiUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

public class EditFeedActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String STATE_CURRENT_TAB = "STATE_CURRENT_TAB";

    static final String FEED_SEARCH_TITLE = "title";
    static final String FEED_SEARCH_URL = "url";
    static final String FEED_SEARCH_DESC = "contentSnippet";

    private static final String[] FEED_PROJECTION = new String[]{FeedColumns.NAME, FeedColumns.URL, FeedColumns.RETRIEVE_FULLTEXT};

    private TabHost mTabHost;
    private EditText mNameEditText, mUrlEditText;
    private CheckBox mRetrieveFulltextCb;
    private ListView mFiltersListView;

    private FiltersCursorAdapter mFiltersCursorAdapter;

    private final ActionMode.Callback mFilterActionModeCallback = new ActionMode.Callback() {

       
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.edit_context_menu, menu);
            return true;
        }

      
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; 
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            switch (item.getItemId()) {
                case R.id.menu_edit:
                    Cursor c = mFiltersCursorAdapter.getCursor();
                    if (c.moveToPosition(mFiltersCursorAdapter.getSelectedFilter())) {
                        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_edit, null);
                        final EditText filterText = (EditText) dialogView.findViewById(R.id.filterText);
                        final CheckBox regexCheckBox = (CheckBox) dialogView.findViewById(R.id.regexCheckBox);
                        final RadioButton applyTitleRadio = (RadioButton) dialogView.findViewById(R.id.applyTitleRadio);
                        final RadioButton applyContentRadio = (RadioButton) dialogView.findViewById(R.id.applyContentRadio);
                        final RadioButton acceptRadio = (RadioButton) dialogView.findViewById(R.id.acceptRadio);
                        final RadioButton rejectRadio = (RadioButton) dialogView.findViewById(R.id.rejectRadio);

                        filterText.setText(c.getString(c.getColumnIndex(FilterColumns.FILTER_TEXT)));
                        regexCheckBox.setChecked(c.getInt(c.getColumnIndex(FilterColumns.IS_REGEX)) == 1);
                        if (c.getInt(c.getColumnIndex(FilterColumns.IS_APPLIED_TO_TITLE)) == 1) {
                            applyTitleRadio.setChecked(true);
                        } else {
                            applyContentRadio.setChecked(true);
                        }
                        if (c.getInt(c.getColumnIndex(FilterColumns.IS_ACCEPT_RULE)) == 1) {
                            acceptRadio.setChecked(true);
                        } else {
                            rejectRadio.setChecked(true);
                        }

                        final long filterId = mFiltersCursorAdapter.getItemId(mFiltersCursorAdapter.getSelectedFilter());
                        new AlertDialog.Builder(EditFeedActivity.this) //
                                .setTitle(R.string.filter_edit_title) //
                                .setView(dialogView) //
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new Thread() {
                                            @Override
                                            public void run() {
                                                String filter = filterText.getText().toString();
                                                if (!filter.isEmpty()) {
                                                    ContentResolver cr = getContentResolver();
                                                    ContentValues values = new ContentValues();
                                                    values.put(FilterColumns.FILTER_TEXT, filter);
                                                    values.put(FilterColumns.IS_REGEX, regexCheckBox.isChecked());
                                                    values.put(FilterColumns.IS_APPLIED_TO_TITLE, applyTitleRadio.isChecked());
                                                    values.put(FilterColumns.IS_ACCEPT_RULE, acceptRadio.isChecked());
                                                    if (cr.update(FilterColumns.CONTENT_URI, values, FilterColumns._ID + '=' + filterId, null) > 0) {
                                                        cr.notifyChange(
                                                                FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(getIntent().getData().getLastPathSegment()),
                                                                null);
                                                    }
                                                }
                                            }
                                        }.start();
                                    }
                                }).setNegativeButton(android.R.string.cancel, null).show();
                    }

                    mode.finish(); 
                    return true;
                case R.id.menu_delete:
                    final long filterId = mFiltersCursorAdapter.getItemId(mFiltersCursorAdapter.getSelectedFilter());
                    new AlertDialog.Builder(EditFeedActivity.this) //
                            .setIcon(android.R.drawable.ic_dialog_alert) //
                            .setTitle(R.string.filter_delete_title) //
                            .setMessage(R.string.question_delete_filter) //
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            ContentResolver cr = getContentResolver();
                                            if (cr.delete(FilterColumns.CONTENT_URI, FilterColumns._ID + '=' + filterId, null) > 0) {
                                                cr.notifyChange(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(getIntent().getData().getLastPathSegment()),
                                                        null);
                                            }
                                        }
                                    }.start();
                                }
                            }).setNegativeButton(android.R.string.no, null).show();

                    mode.finish(); 
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mFiltersCursorAdapter.setSelectedFilter(-1);
            mFiltersListView.invalidateViews();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_feed_edit);
        setResult(RESULT_CANCELED);

        Intent intent = getIntent();

        mTabHost = (TabHost) findViewById(R.id.tabHost);
        mNameEditText = (EditText) findViewById(R.id.feed_title);
        mUrlEditText = (EditText) findViewById(R.id.feed_url);
        mRetrieveFulltextCb = (CheckBox) findViewById(R.id.retrieve_fulltext);
        mFiltersListView = (ListView) findViewById(android.R.id.list);
        View tabWidget = findViewById(android.R.id.tabs);
        View buttonLayout = findViewById(R.id.button_layout);

        mTabHost.setup();
        mTabHost.addTab(mTabHost.newTabSpec("feedTab").setIndicator(getString(R.string.tab_feed_title)).setContent(R.id.feed_tab));
        mTabHost.addTab(mTabHost.newTabSpec("filtersTab").setIndicator(getString(R.string.tab_filters_title)).setContent(R.id.filters_tab));

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String s) {
                invalidateOptionsMenu();
            }
        });

        if (savedInstanceState != null) {
            mTabHost.setCurrentTab(savedInstanceState.getInt(STATE_CURRENT_TAB));
        }

        if (intent.getAction().equals(Intent.ACTION_INSERT) || intent.getAction().equals(Intent.ACTION_SEND)) {
            setTitle(R.string.new_feed_title);

            tabWidget.setVisibility(View.GONE);

            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                mUrlEditText.setText(intent.getStringExtra(Intent.EXTRA_TEXT));
            }
        } else if (intent.getAction().equals(Intent.ACTION_EDIT)) {
            setTitle(R.string.edit_feed_title);

            buttonLayout.setVisibility(View.GONE);

            mFiltersCursorAdapter = new FiltersCursorAdapter(this, null);
            mFiltersListView.setAdapter(mFiltersCursorAdapter);
            mFiltersListView.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    startActionMode(mFilterActionModeCallback);
                    mFiltersCursorAdapter.setSelectedFilter(position);
                    mFiltersListView.invalidateViews();
                    return true;
                }
            });

            getLoaderManager().initLoader(0, null, this);

            if (savedInstanceState == null) {
                Cursor cursor = getContentResolver().query(intent.getData(), FEED_PROJECTION, null, null, null);

                if (cursor.moveToNext()) {
                    mNameEditText.setText(cursor.getString(0));
                    mUrlEditText.setText(cursor.getString(1));
                    mRetrieveFulltextCb.setChecked(cursor.getInt(2) == 1);
                    cursor.close();
                } else {
                    cursor.close();
                    Toast.makeText(EditFeedActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_TAB, mTabHost.getCurrentTab());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (getIntent().getAction().equals(Intent.ACTION_EDIT)) {
            String url = mUrlEditText.getText().toString();
            ContentResolver cr = getContentResolver();

            Cursor cursor = getContentResolver().query(FeedColumns.CONTENT_URI, FeedColumns.PROJECTION_ID,
                    FeedColumns.URL + Constants.DB_ARG, new String[]{url}, null);

            if (cursor.moveToFirst() && !getIntent().getData().getLastPathSegment().equals(cursor.getString(0))) {
                cursor.close();
                Toast.makeText(EditFeedActivity.this, R.string.error_feed_url_exists, Toast.LENGTH_LONG).show();
            } else {
                cursor.close();
                ContentValues values = new ContentValues();

                if (!url.startsWith(Constants.HTTP_SCHEME) && !url.startsWith(Constants.HTTPS_SCHEME)) {
                    url = Constants.HTTP_SCHEME + url;
                }
                values.put(FeedColumns.URL, url);

                String name = mNameEditText.getText().toString();

                values.put(FeedColumns.NAME, name.trim().length() > 0 ? name : null);
                values.put(FeedColumns.RETRIEVE_FULLTEXT, mRetrieveFulltextCb.isChecked() ? 1 : null);
                values.put(FeedColumns.FETCH_MODE, 0);
                values.putNull(FeedColumns.ERROR);

                cr.update(getIntent().getData(), values, null, null);
            }
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_feed, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mTabHost.getCurrentTab() == 0) {
            menu.findItem(R.id.menu_add_filter).setVisible(false);
            menu.findItem(R.id.menu_search_feed).setVisible(true);
        } else {
            menu.findItem(R.id.menu_search_feed).setVisible(false);
            menu.findItem(R.id.menu_add_filter).setVisible(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_add_filter: {
                final View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_edit, null);

                new AlertDialog.Builder(this) //
                        .setTitle(R.string.filter_add_title) //
                        .setView(dialogView) //
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                String filterText = ((EditText) dialogView.findViewById(R.id.filterText)).getText().toString();
                                if (filterText.length() != 0) {
                                    String feedId = getIntent().getData().getLastPathSegment();

                                    ContentValues values = new ContentValues();
                                    values.put(FilterColumns.FILTER_TEXT, filterText);
                                    values.put(FilterColumns.IS_REGEX, ((CheckBox) dialogView.findViewById(R.id.regexCheckBox)).isChecked());
                                    values.put(FilterColumns.IS_APPLIED_TO_TITLE, ((RadioButton) dialogView.findViewById(R.id.applyTitleRadio)).isChecked());
                                    values.put(FilterColumns.IS_ACCEPT_RULE, ((RadioButton) dialogView.findViewById(R.id.acceptRadio)).isChecked());

                                    ContentResolver cr = getContentResolver();
                                    cr.insert(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedId), values);
                                }
                            }
                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }).show();
                return true;
            }
            case R.id.menu_search_feed: {
                final View dialogView = getLayoutInflater().inflate(R.layout.dialog_search_feed, null);
                final EditText searchText = (EditText) dialogView.findViewById(R.id.searchText);
                if (!mUrlEditText.getText().toString().startsWith(Constants.HTTP_SCHEME) && !mUrlEditText.getText().toString().startsWith(Constants.HTTPS_SCHEME)) {
                    searchText.setText(mUrlEditText.getText());
                }
                final RadioGroup radioGroup = (RadioGroup) dialogView.findViewById(R.id.radioGroup);

                new AlertDialog.Builder(EditFeedActivity.this) //
                        .setIcon(R.drawable.action_search) //
                        .setTitle(R.string.feed_search) //
                        .setView(dialogView) //
                        .setPositiveButton(android.R.string.search_go, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (searchText.getText().length() > 0) {
                                    String tmp = searchText.getText().toString();
                                    try {
                                        tmp = URLEncoder.encode(searchText.getText().toString(), Constants.UTF8);
                                    } catch (UnsupportedEncodingException ignored) {
                                    }
                                    final String text = tmp;

                                    switch (radioGroup.getCheckedRadioButtonId()) {
                                        case R.id.byWebSearch:
                                            final ProgressDialog pd = new ProgressDialog(EditFeedActivity.this);
                                            pd.setMessage(getString(R.string.loading));
                                            pd.setCancelable(true);
                                            pd.setIndeterminate(true);
                                            pd.show();

                                            getLoaderManager().restartLoader(1, null, new LoaderManager.LoaderCallbacks<ArrayList<HashMap<String, String>>>() {

                                                @Override
                                                public Loader<ArrayList<HashMap<String, String>>> onCreateLoader(int id, Bundle args) {
                                                    return new GetFeedSearchResultsLoader(EditFeedActivity.this, text);
                                                }

                                                @Override
                                                public void onLoadFinished(Loader<ArrayList<HashMap<String, String>>> loader,
                                                                           final ArrayList<HashMap<String, String>> data) {
                                                    pd.cancel();

                                                    if (data == null) {
                                                        Toast.makeText(EditFeedActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                                                    } else if (data.isEmpty()) {
                                                        Toast.makeText(EditFeedActivity.this, R.string.no_result, Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        AlertDialog.Builder builder = new AlertDialog.Builder(EditFeedActivity.this);
                                                        builder.setTitle(R.string.feed_search);

                                                        
                                                        String[] from = new String[]{FEED_SEARCH_TITLE, FEED_SEARCH_DESC};
                                                        int[] to = new int[]{android.R.id.text1, android.R.id.text2};

                                                      
                                                        SimpleAdapter adapter = new SimpleAdapter(EditFeedActivity.this, data, R.layout.item_search_result, from,
                                                                to);
                                                        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                mNameEditText.setText(data.get(which).get(FEED_SEARCH_TITLE));
                                                                mUrlEditText.setText(data.get(which).get(FEED_SEARCH_URL));
                                                            }
                                                        });
                                                        builder.show();
                                                    }
                                                }

                                                @Override
                                                public void onLoaderReset(Loader<ArrayList<HashMap<String, String>>> loader) {
                                                }
                                            });
                                            break;

                                        case R.id.byTopic:
                                            mUrlEditText.setText("http://www.faroo.com/api?q=" + text + "&start=1&length=10&l=en&src=news&f=rss");
                                            break;

                                        case R.id.byYoutube:
                                            mUrlEditText.setText("http://www.youtube.com/rss/user/" + text.replaceAll("\\+", "") + "/videos.rss");
                                            break;
                                    }
                                }
                            }
                        }).setNegativeButton(android.R.string.cancel, null).show();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onClickOk(View view) {
        // only in insert mode

        FeedDataContentProvider.addFeed(this, mUrlEditText.getText().toString(), mNameEditText.getText().toString(), mRetrieveFulltextCb.isChecked());

        setResult(RESULT_OK);
        finish();
    }

    public void onClickCancel(View view) {
        finish();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = new CursorLoader(this, FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(getIntent().getData().getLastPathSegment()),
                null, null, null, FilterColumns.IS_ACCEPT_RULE + Constants.DB_DESC);
        cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mFiltersCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mFiltersCursorAdapter.swapCursor(null);
    }
}

/**
 * A custom Loader that loads feed search results from the google WS.
 */
class GetFeedSearchResultsLoader extends BaseLoader<ArrayList<HashMap<String, String>>> {

    private final String mSearchText;

    public GetFeedSearchResultsLoader(Context context, String searchText) {
        super(context);
        mSearchText = searchText;
    }


    @Override
    public ArrayList<HashMap<String, String>> loadInBackground() {
        try {
            HttpURLConnection conn = NetworkUtils.setupConnection("https://ajax.googleapis.com/ajax/services/feed/find?v=1.0&q=" + mSearchText);
            try {
                String jsonStr = new String(NetworkUtils.getBytes(conn.getInputStream()));

                // Parse results
                final ArrayList<HashMap<String, String>> results = new ArrayList<HashMap<String, String>>();
                JSONObject response = new JSONObject(jsonStr).getJSONObject("responseData");
                JSONArray entries = response.getJSONArray("entries");
                for (int i = 0; i < entries.length(); i++) {
                    try {
                        JSONObject entry = (JSONObject) entries.get(i);
                        String url = entry.get(EditFeedActivity.FEED_SEARCH_URL).toString();
                        if (!url.isEmpty()) {
                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put(EditFeedActivity.FEED_SEARCH_TITLE, Html.fromHtml(entry.get(EditFeedActivity.FEED_SEARCH_TITLE).toString())
                                    .toString());
                            map.put(EditFeedActivity.FEED_SEARCH_URL, url);
                            map.put(EditFeedActivity.FEED_SEARCH_DESC, Html.fromHtml(entry.get(EditFeedActivity.FEED_SEARCH_DESC).toString()).toString());

                            results.add(map);
                        }
                    } catch (Exception ignored) {
                    }
                }

                return results;
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
