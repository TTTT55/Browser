package com.studio.browser;

import com.studio.browser.misc.YesNoPreference;

import android.content.Context;
import android.util.AttributeSet;

class BrowserYesNoPreference extends YesNoPreference {

    // This is the constructor called by the inflater
    public BrowserYesNoPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            setEnabled(false);

            BrowserSettings settings = BrowserSettings.getInstance();
            if (PreferenceKeys.PREF_PRIVACY_CLEAR_CACHE.equals(getKey())) {
                settings.clearCache();
                settings.clearDatabases();
            } else if (PreferenceKeys.PREF_PRIVACY_CLEAR_COOKIES.equals(getKey())) {
                settings.clearCookies();
            } else if (PreferenceKeys.PREF_PRIVACY_CLEAR_HISTORY.equals(getKey())) {
                settings.clearHistory();
            } else if (PreferenceKeys.PREF_PRIVACY_CLEAR_FORM_DATA.equals(getKey())) {
                settings.clearFormData();
            } else if (PreferenceKeys.PREF_PRIVACY_CLEAR_PASSWORDS.equals(getKey())) {
                settings.clearPasswords();
            } else if (PreferenceKeys.PREF_RESET_DEFAULT_PREFERENCES.equals(
                    getKey())) {
                settings.resetDefaultPreferences();
                setEnabled(true);
            } else if (PreferenceKeys.PREF_PRIVACY_CLEAR_GEOLOCATION_ACCESS.equals(
                    getKey())) {
                settings.clearLocationAccess();
            }
        }
    }
}
