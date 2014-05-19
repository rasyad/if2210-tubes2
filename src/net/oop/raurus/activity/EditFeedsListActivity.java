/**
TUBES 2 OOP
 **/

package net.oop.raurus.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import net.oop.raurus.fragment.EditFeedsListFragment;
import net.oop.raurus.utils.UiUtils;

public class EditFeedsListActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            EditFeedsListFragment fragment = new EditFeedsListFragment();
            getFragmentManager().beginTransaction().add(android.R.id.content, fragment, fragment.getClass().getName()).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }
}
