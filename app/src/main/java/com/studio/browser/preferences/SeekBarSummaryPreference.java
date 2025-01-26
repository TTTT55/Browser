package com.studio.browser.preferences;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.studio.browser.R;
import com.studio.browser.misc.SeekBarPreference;

public class SeekBarSummaryPreference extends SeekBarPreference {

    CharSequence mSummary;
    TextView mSummaryView;

    public SeekBarSummaryPreference(
            Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public SeekBarSummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SeekBarSummaryPreference(Context context) {
        super(context);
        init();
    }

    void init() {
        setWidgetLayoutResource(R.layout.font_size_widget);
    }

    @Override
    public void setSummary(CharSequence summary) {
        mSummary = summary;
        if (mSummaryView != null) {
            mSummaryView.setText(mSummary);
        }
    }

    @Override
    public CharSequence getSummary() {
        return null;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mSummaryView = (TextView) view.findViewById(R.id.text);
        if (TextUtils.isEmpty(mSummary)) {
            mSummaryView.setVisibility(View.GONE);
        } else {
            mSummaryView.setVisibility(View.VISIBLE);
            mSummaryView.setText(mSummary);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Intentionally blank - prevent super.onStartTrackingTouch from running
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Intentionally blank - prevent onStopTrackingTouch from running
    }

}
