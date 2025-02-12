/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.studio.browser;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.appcompat.app.AppCompatActivity;

public class ShortcutActivity extends AppCompatActivity
    implements BookmarksPageCallbacks, OnClickListener {

    private BrowserBookmarksPage mBookmarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.shortcut_bookmark_title);
        setContentView(R.layout.pick_bookmark);
        mBookmarks = (BrowserBookmarksPage) getSupportFragmentManager().findFragmentById(R.id.bookmarks);
        mBookmarks.setEnableContextMenu(false);
        mBookmarks.setCallbackListener(this);
        View cancel = findViewById(R.id.cancel);
        if (cancel != null) {
            cancel.setOnClickListener(this);
        }
    }

    // BookmarksPageCallbacks

    @Override
    public boolean onBookmarkSelected(Cursor c, boolean isFolder) {
        if (isFolder) {
            return false;
        }
        Intent intent = BrowserBookmarksPage.createShortcutIntent(this, c);
        setResult(RESULT_OK, intent);
        finish();
        return true;
    }

    @Override
    public boolean onOpenInNewWindow(String... urls) {
        return false;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.cancel) {
            finish();
        }
    }
}
