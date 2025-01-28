package com.studio.browser;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.studio.browser.misc.Browser;
import com.studio.browser.misc.BrowserContract;

import java.util.List;

public class BookmarkUtils {
    private final static String LOGTAG = "BookmarkUtils";

    // XXX: There is no public string defining this intent so if Home changes the value, we
    // have to update this string.
    private static final String INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    enum BookmarkIconType {
        ICON_INSTALLABLE_WEB_APP, // Icon for an installable web app (launches WebAppRuntime).
        ICON_HOME_SHORTCUT,        // Icon for a shortcut on the home screen (launches Browser).
        ICON_WIDGET,
    }

    /**
     * Creates an icon to be associated with this bookmark. If available, the apple touch icon
     * will be used, else we draw our own depending on the type of "bookmark" being created.
     */
    static Bitmap createIcon(Context context, Bitmap touchIcon, Bitmap favicon,
            BookmarkIconType type) {
        final ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        final int iconDimension = am.getLauncherLargeIconSize();
        final int iconDensity = am.getLauncherLargeIconDensity();
        return createIcon(context, touchIcon, favicon, type, iconDimension, iconDensity);
    }

    static Drawable createListFaviconBackground(Context context) {
        PaintDrawable faviconBackground = new PaintDrawable();
        Resources res = context.getResources();
        int padding = res.getDimensionPixelSize(R.dimen.list_favicon_padding);
        faviconBackground.setPadding(padding, padding, padding, padding);
        faviconBackground.getPaint().setColor(context.getResources()
                .getColor(R.color.bookmarkListFaviconBackground));
        faviconBackground.setCornerRadius(
                res.getDimension(R.dimen.list_favicon_corner_radius));
        return faviconBackground;
    }

    private static Bitmap createIcon(Context context, Bitmap touchIcon,
            Bitmap favicon, BookmarkIconType type, int iconDimension, int iconDensity) {
        Bitmap bm = Bitmap.createBitmap(iconDimension, iconDimension, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        Rect iconBounds = new Rect(0, 0, bm.getWidth(), bm.getHeight());

        // Use the apple-touch-icon if available
        if (touchIcon != null) {
            drawTouchIconToCanvas(touchIcon, canvas, iconBounds);
        } else {
            // No touch icon so create our own.
            // Set the background based on the type of shortcut (either webapp or home shortcut).
            Bitmap icon = getIconBackground(context, type, iconDensity);

            if (icon != null) {
                // Now draw the correct icon background into our new bitmap.
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                canvas.drawBitmap(icon, null, iconBounds, p);
            }

            // If we have a favicon, overlay it in a nice rounded white box on top of the
            // background.
            if (favicon != null) {
                drawFaviconToCanvas(context, favicon, canvas, iconBounds, type);
            }
        }
        canvas.setBitmap(null);
        return bm;
    }

    /**
     * Convenience method for creating an intent that will add a shortcut to the home screen.
     */
    static Bitmap vectorToBitmap(Context context, int vectorResId) {
        Drawable drawable = VectorDrawableCompat.create(context.getResources(), vectorResId, context.getTheme());
        if (drawable == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    static Intent createAddToHomeIntent(Context context, String url, String title, Bitmap touchIcon, Bitmap favicon) {
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
            // Generate a consistent ID based on the URL
            String shortcutId = "shortcut_" + url.hashCode();

            // Check if shortcut already exists by checking pinned shortcuts
            List<ShortcutInfo> pinnedShortcuts = shortcutManager.getPinnedShortcuts();
            for (ShortcutInfo existingShortcut : pinnedShortcuts) {
                if (existingShortcut.getId().equals(shortcutId)) {
                    // Show a toast and return
                    Handler mainHandler = new Handler(context.getMainLooper());
                    mainHandler.post(() -> {
                        Toast.makeText(context, R.string.shortcut_already_exists, Toast.LENGTH_SHORT).show();
                    });
                    return null;
                }
            }

            // If we get here, shortcut doesn't exist, create it
            Intent shortcutIntent = createShortcutIntent(url);
            // Check if the touchIcon is null
            if (touchIcon == null) {
                // Attempt to load the Google icon from resources
                touchIcon = vectorToBitmap(context, R.drawable.ic_google);
                if (touchIcon == null) {
                    Log.e(LOGTAG, "Failed to load Google icon bitmap. Using a default placeholder.");
                    // Create a default placeholder bitmap
                    touchIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                } else {
                    Log.d(LOGTAG, "Loaded Google icon bitmap successfully.");
                }
            }
            ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(context, shortcutId)
                    .setShortLabel(title)
                    .setLongLabel(title)
                    .setIcon(Icon.createWithBitmap(touchIcon))
                    .setIntent(shortcutIntent)
                    .build();

            // Broadcast receiver for the callback
            Intent callbackIntent = new Intent(context, ShortcutResultReceiver.class);
            callbackIntent.setAction("com.studio.browser.SHORTCUT_CREATED");
            IntentSender shortcutCallback = PendingIntent.getBroadcast(context, 0,
                    callbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE).getIntentSender();

            // Request to pin the shortcut with callback
            shortcutManager.requestPinShortcut(shortcutInfo, shortcutCallback);
            return null;  // Return null, we're handling it through the callback
        }

        // Fallback for older Android versions or when ShortcutManager is not available
        Intent addIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, createShortcutIntent(url));
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
        if (touchIcon != null) {
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, touchIcon);
        } else if (favicon != null) {
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, favicon);
        }
        return addIntent;
    }

    static Intent createShortcutIntent(String url) {
        Intent shorIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        long urlHash = url.hashCode();
        long uniqueId = (urlHash << 32) | shorIntent.hashCode();
        shorIntent.putExtra(Browser.EXTRA_APPLICATION_ID, Long.toString(uniqueId));
        return shorIntent;
    }

    public static class ShortcutResultReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, R.string.shortcut_created, Toast.LENGTH_SHORT).show();
        }
    }

    private static Bitmap getIconBackground(Context context, BookmarkIconType type, int density) {
        if (type == BookmarkIconType.ICON_HOME_SHORTCUT) {
            // Want to create a shortcut icon on the homescreen, so the icon
            // background is the red bookmark.
            Drawable drawable = context.getResources().getDrawableForDensity(
                    R.mipmap.ic_launcher_shortcut_browser_bookmark, density);
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bd = (BitmapDrawable) drawable;
                return bd.getBitmap();
            }
        } else if (type == BookmarkIconType.ICON_INSTALLABLE_WEB_APP) {
            // Use the web browser icon as the background for the icon for an installable
            // web app.
            Drawable drawable = context.getResources().getDrawableForDensity(
                    R.mipmap.ic_launcher_browser, density);
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bd = (BitmapDrawable) drawable;
                return bd.getBitmap();
            }
        }
        return null;
    }

    private static void drawTouchIconToCanvas(Bitmap touchIcon, Canvas canvas, Rect iconBounds) {
        Rect src = new Rect(0, 0, touchIcon.getWidth(), touchIcon.getHeight());

        // Paint used for scaling the bitmap and drawing the rounded rect.
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(touchIcon, src, iconBounds, paint);

        // Construct a path from a round rect. This will allow drawing with
        // an inverse fill so we can punch a hole using the round rect.
        Path path = new Path();
        path.setFillType(Path.FillType.INVERSE_WINDING);
        RectF rect = new RectF(iconBounds);
        rect.inset(1, 1);
        path.addRoundRect(rect, 8f, 8f, Path.Direction.CW);

        // Reuse the paint and clear the outside of the rectangle.
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPath(path, paint);
    }

    private static void drawFaviconToCanvas(Context context, Bitmap favicon,
            Canvas canvas, Rect iconBounds, BookmarkIconType type) {
        // Make a Paint for the white background rectangle and for
        // filtering the favicon.
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        if (type == BookmarkIconType.ICON_WIDGET) {
            p.setColor(context.getResources()
                    .getColor(R.color.bookmarkWidgetFaviconBackground));
        } else {
            p.setColor(Color.WHITE);
        }

        // Create a rectangle that is slightly wider than the favicon
        int faviconDimension = context.getResources().getDimensionPixelSize(R.dimen.favicon_size);
        int faviconPaddedRectDimension;
        if (type == BookmarkIconType.ICON_WIDGET) {
            faviconPaddedRectDimension = canvas.getWidth();
        } else {
            faviconPaddedRectDimension = context.getResources().getDimensionPixelSize(
                    R.dimen.favicon_padded_size);
        }
        float padding = (faviconPaddedRectDimension - faviconDimension) / 2;
        final float x = iconBounds.exactCenterX() - (faviconPaddedRectDimension / 2);
        float y = iconBounds.exactCenterY() - (faviconPaddedRectDimension / 2);
        if (type != BookmarkIconType.ICON_WIDGET) {
            // Note: Subtract from the y position since the box is
            // slightly higher than center. Use padding since it is already
            // device independent.
            y -= padding;
        }
        RectF r = new RectF(x, y, x + faviconPaddedRectDimension, y + faviconPaddedRectDimension);
        // Draw a white rounded rectangle behind the favicon
        canvas.drawRoundRect(r, 3, 3, p);

        // Draw the favicon in the same rectangle as the rounded
        // rectangle but inset by the padding
        // (results in a 16x16 favicon).
        r.inset(padding, padding);
        canvas.drawBitmap(favicon, null, r, null);
    }

    /* package */ static Uri getBookmarksUri(Context context) {
        return BrowserContract.Bookmarks.CONTENT_URI;
    }

    /**
     * Show a confirmation dialog to remove a bookmark.
     * @param id Id of the bookmark to remove
     * @param title Title of the bookmark, to be displayed in the confirmation method.
     * @param context Package Context for strings, dialog, ContentResolver
     * @param msg Message to send if the bookmark is deleted.
     */
    static void displayRemoveBookmarkDialog( final long id, final String title,
            final Context context, final Message msg) {

        new AlertDialog.Builder(context)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(context.getString(R.string.delete_bookmark_warning,
                        title))
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (msg != null) {
                                    msg.sendToTarget();
                                }
                                Runnable runnable = new Runnable(){
                                    @Override
                                    public void run() {
                                        Uri uri = ContentUris.withAppendedId(
                                                BrowserContract.Bookmarks.CONTENT_URI,
                                                id);
                                        context.getContentResolver().delete(uri, null, null);
                                    }
                                };
                                new Thread(runnable).start();
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
