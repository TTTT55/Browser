package com.studio.browser.misc;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.studio.browser.R;

/**
 * The expanded menu view is a list-like menu with all of the available menu items.  It is opened
 * by the user clicking no the 'More' button on the icon menu view.
 */
public final class ExpandedMenuView extends ListView implements MenuBuilder.ItemInvoker, MenuView, OnItemClickListener {
    private MenuBuilder mMenu;

    /** Default animations for this menu */
    private int mAnimations;

    /**
     * Instantiates the ExpandedMenuView that is linked with the provided MenuBuilder.
     * The model for the menu which this MenuView will display
     */
    public ExpandedMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MenuView, 0, 0);
        mAnimations = a.getResourceId(androidx.appcompat.R.styleable.MenuView_android_windowAnimationStyle, 0);
        a.recycle();

        setOnItemClickListener(this);
    }

    public void initialize(MenuBuilder menu) {
        mMenu = menu;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Clear the cached bitmaps of children
        setChildrenDrawingCacheEnabled(false);
    }

    public boolean invokeItem(MenuItemImpl item) {
        return mMenu.performItemAction(item, 0);
    }

    public void onItemClick(AdapterView parent, View v, int position, long id) {
        invokeItem((MenuItemImpl) getAdapter().getItem(position));
    }

    public int getWindowAnimations() {
        return mAnimations;
    }

}
