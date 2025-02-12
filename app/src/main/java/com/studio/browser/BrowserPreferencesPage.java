package com.studio.browser;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.studio.browser.preferences.AccessibilityPreferencesFragment;
import com.studio.browser.preferences.AdvancedPreferencesFragment;
import com.studio.browser.preferences.BandwidthPreferencesFragment;
import com.studio.browser.preferences.DebugPreferencesFragment;
import com.studio.browser.preferences.GeneralPreferencesFragment;
import com.studio.browser.preferences.LabPreferencesFragment;
import com.studio.browser.preferences.PrivacySecurityPreferencesFragment;
import com.studio.browser.preferences.WebsiteSettingsFragment;

import java.util.List;

public class BrowserPreferencesPage extends PreferenceActivity {

    public static final String CURRENT_PAGE = "currentPage";
    private List<Header> mHeaders;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(
                    ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        }
        getWindow().setNavigationBarColor(Color.parseColor("#ECF0FD"));
    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);

        if (BrowserSettings.getInstance().isDebugEnabled()) {
            Header debug = new Header();
            debug.title = getText(R.string.pref_development_title);
            debug.fragment = DebugPreferencesFragment.class.getName();
            target.add(debug);
        }
        mHeaders = target;
    }

    @Override
    public Header onGetInitialHeader() {
        String action = getIntent().getAction();
        if (Intent.ACTION_MANAGE_NETWORK_USAGE.equals(action)) {
            String fragName = BandwidthPreferencesFragment.class.getName();
            for (Header h : mHeaders) {
                if (fragName.equals(h.fragment)) {
                    return h;
                }
            }
        }
        return super.onGetInitialHeader();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                } else {
                    finish();
                }
                return true;
        }

        return false;
    }

    @Override
    public Intent onBuildStartFragmentIntent(String fragmentName, Bundle args,
            int titleRes, int shortTitleRes) {
        Intent intent = super.onBuildStartFragmentIntent(fragmentName, args,
                titleRes, shortTitleRes);
        String url = getIntent().getStringExtra(CURRENT_PAGE);
        intent.putExtra(CURRENT_PAGE, url);
        return intent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return AccessibilityPreferencesFragment.class.getName().equals(fragmentName) ||
                AdvancedPreferencesFragment.class.getName().equals(fragmentName) ||
                BandwidthPreferencesFragment.class.getName().equals(fragmentName) ||
                DebugPreferencesFragment.class.getName().equals(fragmentName) ||
                GeneralPreferencesFragment.class.getName().equals(fragmentName) ||
                LabPreferencesFragment.class.getName().equals(fragmentName) ||
                PrivacySecurityPreferencesFragment.class.getName().equals(fragmentName) ||
                WebsiteSettingsFragment.class.getName().equals(fragmentName);

    }
}
