package com.studio.browser;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;
import android.widget.PopupMenu.OnMenuItemClickListener;

public class NavigationBarPhone extends NavigationBarBase implements
        UrlInputView.StateListener, OnMenuItemClickListener, OnDismissListener {

    private ImageView mStopButton;
    private ImageView mMagnify;
    private ImageView mClearButton;
    private ImageView mVoiceButton;
    private Drawable mStopDrawable;
    private Drawable mRefreshDrawable;
    private String mStopDescription;
    private String mRefreshDescription;
    private View mTabSwitcher;
    private View mComboIcon;
    private View mTitleContainer;
    private View mMore;
    private Drawable mTextfieldBgDrawable;
    private PopupMenu mPopupMenu;
    private boolean mOverflowMenuShowing;
    private boolean mNeedsMenu;
    private View mIncognitoIcon;

    public NavigationBarPhone(Context context) {
        super(context);
    }

    public NavigationBarPhone(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NavigationBarPhone(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mStopButton = (ImageView) findViewById(R.id.stop);
        mStopButton.setOnClickListener(this);
        mClearButton = (ImageView) findViewById(R.id.clear);
        mClearButton.setOnClickListener(this);
        mVoiceButton = (ImageView) findViewById(R.id.voice);
        mVoiceButton.setOnClickListener(this);
        mMagnify = (ImageView) findViewById(R.id.magnify);
        mTabSwitcher = findViewById(R.id.tab_switcher);
        mTabSwitcher.setOnClickListener(this);
        mMore = findViewById(R.id.more);
        mMore.setOnClickListener(this);
        mComboIcon = findViewById(R.id.iconcombo);
        mComboIcon.setOnClickListener(this);
        mTitleContainer = findViewById(R.id.title_bg);
        setFocusState(false);
        Resources res = getContext().getResources();
        mStopDrawable = res.getDrawable(R.drawable.ic_stop_holo_dark);
        mRefreshDrawable = res.getDrawable(R.drawable.ic_refresh_holo_dark);
        mStopDescription = res.getString(R.string.accessibility_button_stop);
        mRefreshDescription = res.getString(R.string.accessibility_button_refresh);
        mTextfieldBgDrawable = res.getDrawable(R.drawable.textfield_active_holo_dark);
        mUrlInput.setContainer(this);
        mUrlInput.setStateListener(this);
        mNeedsMenu = !ViewConfiguration.get(getContext()).hasPermanentMenuKey();
        mIncognitoIcon = findViewById(R.id.incognito_icon);
    }

    private int parseColor(String color) {
        if (color.startsWith("rgb")) {
            String[] components = color.substring(color.indexOf('(') + 1,
                    color.length() - 1).split(",");

            int r = Integer.parseInt(components[0].trim());
            int g = Integer.parseInt(components[1].trim());
            int b = Integer.parseInt(components[2].trim());

            return Color.rgb(r, g, b);
        } else {
            return Color.parseColor(color);
        }
    }

    @Override
    public void onProgressStarted() {
        super.onProgressStarted();
        if (mStopButton.getDrawable() != mStopDrawable) {
            mStopButton.setImageDrawable(mStopDrawable);
            mStopButton.setContentDescription(mStopDescription);
            if (mStopButton.getVisibility() != View.VISIBLE) {
                mComboIcon.setVisibility(View.GONE);
                mStopButton.setVisibility(View.VISIBLE);
            }
        }

        // WebViewClient to detect page loads
        mBaseUi.getWebView().setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String script = "javascript:(function() { " +
                        "var bodyBg = window.getComputedStyle(document.body).backgroundColor;" +
                        "AndroidInterface.onBackgroundColorReceived(bodyBg);" +
                        "})()";

                view.evaluateJavascript(script, null);
            }
        });
        mBaseUi.getWebView().addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void onBackgroundColorReceived(String color) {
                try {
                    final int navColor = parseColor(color);
                    if (getContext() instanceof BrowserActivity) {
                        final BrowserActivity activity = (BrowserActivity) getContext();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.updateNavigationBarColor(navColor);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "AndroidInterface");
    }

    @Override
    public void onProgressStopped() {
        super.onProgressStopped();
        mStopButton.setImageDrawable(mRefreshDrawable);
        mStopButton.setContentDescription(mRefreshDescription);
        if (!isEditingUrl()) {
            mComboIcon.setVisibility(View.VISIBLE);
        }
        onStateChanged(mUrlInput.getState());
    }

    /**
     * Update the text displayed in the title bar.
     * @param title String to display.  If null, the new tab string will be
     *      shown.
     */
    @Override
    void setDisplayTitle(String title) {
        mUrlInput.setTag(title);
        if (!isEditingUrl()) {
            if (title == null) {
                mUrlInput.setText(R.string.new_tab);
            } else {
                mUrlInput.setText(UrlUtils.stripUrl(title), false);
            }
            mUrlInput.setSelection(0);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mStopButton) {
            if (mTitleBar.isInLoad()) {
                mUiController.stopLoading();
            } else {
                WebView web = mBaseUi.getWebView();
                if (web != null) {
                    stopEditingUrl();
                    web.reload();
                }
            }
        } else if (v == mTabSwitcher) {
            ((PhoneUi) mBaseUi).toggleNavScreen();
        } else if (mMore == v) {
            showMenu(mMore);
        } else if (mClearButton == v) {
            mUrlInput.setText("");
        } else if (mComboIcon == v) {
            mUiController.showPageInfo();
        } else if (mVoiceButton == v) {
            mUiController.startVoiceRecognizer();
        } else {
            super.onClick(v);
        }
    }

    @Override
    public boolean isMenuShowing() {
        return super.isMenuShowing() || mOverflowMenuShowing;
    }

    public void showMenu(View anchor) {
        Activity activity = mUiController.getActivity();
        if (mPopupMenu == null) {
            mPopupMenu = new PopupMenu(getContext(), anchor);
            mPopupMenu.setOnMenuItemClickListener(this);
            mPopupMenu.setOnDismissListener(this);
            if (!activity.onCreateOptionsMenu(mPopupMenu.getMenu())) {
                mPopupMenu = null;
                return;
            }
        }
        Menu menu = mPopupMenu.getMenu();
        if (activity.onPrepareOptionsMenu(menu)) {
            mOverflowMenuShowing = true;
            mPopupMenu.show();
        }
    }

    @Override
    public void onDismiss(PopupMenu menu) {
        if (menu == mPopupMenu) {
            onMenuHidden();
        }
    }

    private void onMenuHidden() {
        mOverflowMenuShowing = false;
        mBaseUi.showTitleBarForDuration();
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view == mUrlInput) {
            if (hasFocus && !mUrlInput.getText().toString().equals(mUrlInput.getTag())) {
                // only change text if different
                mUrlInput.setText((String) mUrlInput.getTag(), false);
                mUrlInput.selectAll();
            } else {
                setDisplayTitle(mUrlInput.getText().toString());
            }
        }
        super.onFocusChange(view, hasFocus);
    }

    @Override
    public void onStateChanged(int state) {
        mVoiceButton.setVisibility(View.GONE);
        switch(state) {
        case UrlInputView.StateListener.STATE_NORMAL:
            mComboIcon.setVisibility(View.VISIBLE);
            mStopButton.setVisibility(View.GONE);
            mClearButton.setVisibility(View.GONE);
            mMagnify.setVisibility(View.GONE);
            mTabSwitcher.setVisibility(View.VISIBLE);
            mTitleContainer.setBackgroundDrawable(null);
            mMore.setVisibility(mNeedsMenu ? View.VISIBLE : View.GONE);
            break;
        case UrlInputView.StateListener.STATE_HIGHLIGHTED:
            mComboIcon.setVisibility(View.GONE);
            mStopButton.setVisibility(View.VISIBLE);
            mClearButton.setVisibility(View.GONE);
            if ((mUiController != null) && mUiController.supportsVoice()) {
                mVoiceButton.setVisibility(View.VISIBLE);
            }
            mMagnify.setVisibility(View.GONE);
            mTabSwitcher.setVisibility(View.GONE);
            mMore.setVisibility(View.GONE);
            mTitleContainer.setBackgroundDrawable(mTextfieldBgDrawable);
            break;
        case UrlInputView.StateListener.STATE_EDITED:
            mComboIcon.setVisibility(View.GONE);
            mStopButton.setVisibility(View.GONE);
            mClearButton.setVisibility(View.VISIBLE);
            mMagnify.setVisibility(View.VISIBLE);
            mTabSwitcher.setVisibility(View.GONE);
            mMore.setVisibility(View.GONE);
            mTitleContainer.setBackgroundDrawable(mTextfieldBgDrawable);
            break;
        }
    }

    @Override
    public void onTabDataChanged(Tab tab) {
        super.onTabDataChanged(tab);
        mIncognitoIcon.setVisibility(tab.isPrivateBrowsingEnabled()
                ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return mUiController.onOptionsItemSelected(item);
    }

}
