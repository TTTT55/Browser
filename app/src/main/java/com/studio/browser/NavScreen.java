package com.studio.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.studio.browser.NavTabScroller.OnLayoutListener;
import com.studio.browser.NavTabScroller.OnRemoveListener;
import com.studio.browser.TabControl.OnThumbnailUpdatedListener;
import com.studio.browser.UI.ComboViews;

import java.util.HashMap;

public class NavScreen extends RelativeLayout
        implements OnClickListener, OnMenuItemClickListener, OnThumbnailUpdatedListener {


    UiController mUiController;
    PhoneUi mUi;
    Tab mTab;
    Activity mActivity;

    ImageButton mRefresh;
    ImageButton mForward;
    ImageButton mBookmarks;
    ImageButton mMore;
    ImageButton mNewTab;
    FrameLayout mHolder;

    TextView mTitle;
    ImageView mFavicon;
    ImageButton mCloseTab;

    NavTabScroller mScroller;
    TabAdapter mAdapter;
    int mOrientation;
    boolean mNeedsMenu;
    HashMap<Tab, View> mTabViews;

    private WindowManager wm;
    private int mOriginalNavColor;

    public NavScreen(Activity activity, UiController ctl, PhoneUi ui) {
        super(activity);
        mActivity = activity;
        mUiController = ctl;
        mUi = ui;
        mOrientation = activity.getResources().getConfiguration().orientation;
        init();

        // Store the original color when NavScreen is created
        mOriginalNavColor = ((BrowserActivity)activity).getDefaultNavigationBarColor();

        wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
    }

    public void onShow() {
        mOriginalNavColor = mActivity.getWindow().getNavigationBarColor();
        mActivity.getWindow().setNavigationBarColor(Color.BLACK);
    }

    public void onHide() {
        mActivity.getWindow().setNavigationBarColor(mOriginalNavColor);
    }

    protected void showMenu() {
        PopupMenu popup = new PopupMenu(getContext(), mMore);
        Menu menu = popup.getMenu();
        popup.getMenuInflater().inflate(R.menu.browser, menu);
        mUiController.updateMenuState(mUiController.getCurrentTab(), menu);
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return mUiController.onOptionsItemSelected(item);
    }

    protected float getToolbarHeight() {
        return mActivity.getResources().getDimension(R.dimen.toolbar_height);
    }

    @Override
    protected void onConfigurationChanged(Configuration newconfig) {
        if (newconfig.orientation != mOrientation) {
            int sv = mScroller.getScrollValue();
            removeAllViews();
            mOrientation = newconfig.orientation;
            init();
            mScroller.setScrollValue(sv);
            mAdapter.notifyDataSetChanged();
        }
    }

    public void refreshAdapter() {
        mScroller.handleDataChanged(
                mUiController.getTabControl().getTabPosition(mUi.getActiveTab()));
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.nav_screen, this);
        setContentDescription(getContext().getResources().getString(
                R.string.accessibility_transition_navscreen));
        mBookmarks = (ImageButton) findViewById(R.id.bookmarks);
        mNewTab = (ImageButton) findViewById(R.id.newtab);
        mMore = (ImageButton) findViewById(R.id.more);
        mBookmarks.setOnClickListener(this);
        mNewTab.setOnClickListener(this);
        mMore.setOnClickListener(this);
        mScroller = (NavTabScroller) findViewById(R.id.scroller);
        TabControl tc = mUiController.getTabControl();
        mTabViews = new HashMap<Tab, View>(tc.getTabCount());
        mAdapter = new TabAdapter(getContext(), tc);
        mScroller.setOrientation(mOrientation == Configuration.ORIENTATION_LANDSCAPE
                ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        // update state for active tab
        mScroller.setAdapter(mAdapter,
                mUiController.getTabControl().getTabPosition(mUi.getActiveTab()));
        mScroller.setOnRemoveListener(new OnRemoveListener() {
            public void onRemovePosition(int pos) {
                Tab tab = mAdapter.getItem(pos);
                onCloseTab(tab);
            }
        });
        mNeedsMenu = !ViewConfiguration.get(getContext()).hasPermanentMenuKey();
        if (!mNeedsMenu) {
            mMore.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (mBookmarks == v) {
            mUiController.bookmarksOrHistoryPicker(ComboViews.Bookmarks);
        } else if (mNewTab == v) {
            openNewTab();
        } else if (mMore == v) {
            showMenu();
        }
    }

    private void onCloseTab(Tab tab) {
        if (tab != null) {
            if (tab == mUiController.getCurrentTab()) {
                mUiController.closeCurrentTab();
            } else {
                mUiController.closeTab(tab);
            }
            mTabViews.remove(tab);
        }
    }

    private void openNewTab() {
        // need to call openTab explicitely with setactive false
        final Tab tab = mUiController.openTab(BrowserSettings.getInstance().getHomePage(),
                false, false, false);
        if (tab != null) {
            mUiController.setBlockEvents(true);
            final int tix = mUi.mTabControl.getTabPosition(tab);
            mScroller.setOnLayoutListener(new OnLayoutListener() {

                @Override
                public void onLayout(int l, int t, int r, int b) {
                    final int pos = mUi.mTabControl.getTabPosition(tab);
                    mUi.hideNavScreen(pos, true);
                    switchToTab(tab);
                }
            });
            mScroller.handleDataChanged(tix);
            mUiController.setBlockEvents(false);
        }
    }

    private void switchToTab(Tab tab) {
        if (tab != mUi.getActiveTab()) {
            mUiController.setActiveTab(tab);
        }
    }

    protected void close(int position) {
        close(position, true);
    }

    protected void close(int position, boolean animate) {
        mUi.hideNavScreen(position, animate);
    }

    protected NavTabView getTabView(int pos) {
        return mScroller.getTabView(pos);
    }

    class TabAdapter extends BaseAdapter {

        Context context;
        TabControl tabControl;

        public TabAdapter(Context ctx, TabControl tc) {
            context = ctx;
            tabControl = tc;
        }

        @Override
        public int getCount() {
            return tabControl.getTabCount();
        }

        @Override
        public Tab getItem(int position) {
            return tabControl.getTab(position);
        }

        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final NavTabView tabview = new NavTabView(mActivity);
            final Tab tab = getItem(position);
            tabview.setWebView(tab);
            mTabViews.put(tab, tabview.mImage);
            tabview.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (tabview.isClose(v)) {
                        mScroller.animateOut(tabview);
                        mTabViews.remove(tab);
                    } else if (tabview.isTitle(v)) {
                        switchToTab(tab);
                        mUi.getTitleBar().setSkipTitleBarAnimations(true);
                        close(position, false);
                        mUi.editUrl(false, true);
                        mUi.getTitleBar().setSkipTitleBarAnimations(false);
                    } else if (tabview.isWebView(v)) {
                        close(position);
                    }
                }
            });
            return tabview;
        }
    }

    @Override
    public void onThumbnailUpdated(Tab t) {
        View v = mTabViews.get(t);
        if (v != null) {
            v.invalidate();
        }
    }
}
