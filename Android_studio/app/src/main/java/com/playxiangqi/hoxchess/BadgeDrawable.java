/**
 * See blog: http://www.jmhend.me/layerdrawable-menuitems
 * Also, he posted his answer here also:
 *  http://stackoverflow.com/questions/18156477/how-to-make-an-icon-in-the-action-bar-with-the-number-of-notification
 */

package com.playxiangqi.hoxchess;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.MenuItem;

public class BadgeDrawable extends Drawable {

    private float mTextSize;
    private Paint mBadgePaint;
    private Paint mTextPaint;
    private Rect mTxtRect = new Rect();

    private String mCount = "";
    private boolean mWillDraw = false;

    public BadgeDrawable(Context context) {
        mTextSize = context.getResources().getDimension(R.dimen.badge_text_size);

        mBadgePaint = new Paint();
        mBadgePaint.setColor(Color.RED);
        mBadgePaint.setAntiAlias(true);
        mBadgePaint.setStyle(Paint.Style.FILL);

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void draw(Canvas canvas) {
        if (!mWillDraw) {
            return;
        }

        Rect bounds = getBounds();
        float width = bounds.right - bounds.left;
        float height = bounds.bottom - bounds.top;

        // Position the badge in the top-right quadrant of the icon.
        float radius = ((Math.min(width, height) / 2) - 1) / 2;
        float centerX = width - radius - 1;
        float centerY = radius + 1;

        // Draw badge circle.
        canvas.drawCircle(centerX, centerY, radius, mBadgePaint);

        // Draw badge count text inside the circle.
        mTextPaint.getTextBounds(mCount, 0, mCount.length(), mTxtRect);
        float textHeight = mTxtRect.bottom - mTxtRect.top;
        float textY = centerY + (textHeight / 2f);
        canvas.drawText(mCount, centerX, textY, mTextPaint);
    }

    /*
    Sets the count (i.e notifications) to display.
     */
    public void setCount(int count) {
        mCount = Integer.toString(count);

        // Only draw a badge if there are notifications.
        mWillDraw = count > 0;
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        // do nothing
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // do nothing
    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    /**
     * A helper function to update LayerDrawable's BadgeDrawable
     */
    private static void setBadgeCount(Context context, LayerDrawable icon, int count) {
        BadgeDrawable badge;

        // Reuse drawable if possible
        Drawable reuse = icon.findDrawableByLayerId(R.id.ic_badge);
        if (reuse != null && reuse instanceof BadgeDrawable) {
            badge = (BadgeDrawable) reuse;
        } else {
            badge = new BadgeDrawable(context);
        }

        badge.setCount(count);
        icon.mutate();
        icon.setDrawableByLayerId(R.id.ic_badge, badge);
    }

    public static void setBadgeCount(Context context, MenuItem menuItem, int count) {
        if (menuItem != null) {
            LayerDrawable icon = (LayerDrawable) menuItem.getIcon();
            BadgeDrawable.setBadgeCount(context, icon, count);
        }
    }
}
