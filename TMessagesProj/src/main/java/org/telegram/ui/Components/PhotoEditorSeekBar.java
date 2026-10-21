/*
 * This is the source code of Telegram for Android v. 5.x.x
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

public class PhotoEditorSeekBar extends View {

    private Paint innerPaint = new Paint();
    private Paint outerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int thumbSize = AndroidUtilities.dp(16);
    private int thumbDX = 0;
    private float progress = 0;
    private boolean pressed = false;
    private int minValue;
    private int maxValue;
    private PhotoEditorSeekBarDelegate delegate;

    public interface PhotoEditorSeekBarDelegate {
        void onProgressChanged(int i, int progress);
    }

    public PhotoEditorSeekBar(Context context) {
        super(context);

        innerPaint.setColor(0xff4d4d4d);
        outerPaint.setColor(0xffffffff);
    }

    public void setDelegate(PhotoEditorSeekBarDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }
        float x = event.getX();
        float y = event.getY();
        float thumbX = (int)((getMeasuredWidth() - thumbSize) * progress);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int additionWidth = (getMeasuredHeight() - thumbSize) / 2;
            if (thumbX - additionWidth <= x && x <= thumbX + thumbSize + additionWidth && y >= 0 && y <= getMeasuredHeight()) {
                pressed = true;
                thumbDX = (int)(x - thumbX);
                getParent().requestDisallowInterceptTouchEvent(true);
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (pressed) {
                pressed = false;
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (pressed) {
                thumbX = (int)(x - thumbDX);
                if (thumbX < 0) {
                    thumbX = 0;
                } else if (thumbX > getMeasuredWidth() - thumbSize) {
                    thumbX = getMeasuredWidth() - thumbSize;
                }
                progress = thumbX / (getMeasuredWidth() - thumbSize);
                if (delegate != null) {
                    delegate.onProgressChanged((Integer) getTag(), getProgress());
                }
                invalidate();
                return true;
            }
        }
        return false;
    }

    public void setProgress(int progress) {
        setProgress(progress, true);
    }

    public void setProgress(int progress, boolean notify) {
        if (progress < minValue) {
            progress = minValue;
        } else if (progress > maxValue) {
            progress = maxValue;
        }
        this.progress = (progress - minValue) / (float) (maxValue - minValue);
        invalidate();
        if (notify && delegate != null) {
            delegate.onProgressChanged((Integer) getTag(), getProgress());
        }
    }

    public int getProgress() {
        return (int) (minValue + progress * (maxValue - minValue));
    }

    public void setMinMax(int min, int max) {
        minValue = min;
        maxValue = max;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (org.telegram.marble.MarbleTheme.expressiveShapes()) {
            drawM3(canvas);
            return;
        }
        int y = (getMeasuredHeight() - thumbSize) / 2;
        int thumbX = (int)((getMeasuredWidth() - thumbSize) * progress);
        canvas.drawRect(thumbSize / 2, getMeasuredHeight() / 2 - AndroidUtilities.dp(1), getMeasuredWidth() - thumbSize / 2, getMeasuredHeight() / 2 + AndroidUtilities.dp(1), innerPaint);
        if (minValue == 0) {
            canvas.drawRect(thumbSize / 2, getMeasuredHeight() / 2 - AndroidUtilities.dp(1), thumbX, getMeasuredHeight() / 2 + AndroidUtilities.dp(1), outerPaint);
        } else {
            if (progress > 0.5f) {
                canvas.drawRect(getMeasuredWidth() / 2 - AndroidUtilities.dp(1), (getMeasuredHeight() - thumbSize) / 2, getMeasuredWidth() / 2, (getMeasuredHeight() + thumbSize) / 2, outerPaint);
                canvas.drawRect(getMeasuredWidth() / 2, getMeasuredHeight() / 2 - AndroidUtilities.dp(1), thumbX, getMeasuredHeight() / 2 + AndroidUtilities.dp(1), outerPaint);
            } else {
                canvas.drawRect(getMeasuredWidth() / 2, (getMeasuredHeight() - thumbSize) / 2, getMeasuredWidth() / 2 + AndroidUtilities.dp(1), (getMeasuredHeight() + thumbSize) / 2, outerPaint);
                canvas.drawRect(thumbX, getMeasuredHeight() / 2 - AndroidUtilities.dp(1), getMeasuredWidth() / 2, getMeasuredHeight() / 2 + AndroidUtilities.dp(1), outerPaint);
            }
        }
        canvas.drawCircle(thumbX + thumbSize / 2, y + thumbSize / 2, thumbSize / 2, outerPaint);
    }

    /** Marblegram: M3E slider — 12dp rounded track, 6dp gaps around a 4dp vertical pill handle; bipolar from center. */
    private void drawM3(Canvas canvas) {
        innerPaint.setAntiAlias(true);
        final float w = getMeasuredWidth(), cy = getMeasuredHeight() / 2f;
        final float left = thumbSize / 2f, right = w - thumbSize / 2f;
        final float hx = left + (right - left) * progress;
        final float th = AndroidUtilities.dp(12), r = th / 2f, gap = AndroidUtilities.dp(6);
        final float origin = minValue == 0 ? left : (left + right) / 2f;
        final float aL = Math.min(origin, hx), aR = Math.max(origin, hx);
        // inactive track (both sides of the handle)
        segment(canvas, left, Math.min(aL, hx - gap), cy, th, r, innerPaint);
        segment(canvas, Math.max(aR, hx + gap), right, cy, th, r, innerPaint);
        // active track
        if (hx >= origin) segment(canvas, aL, hx - gap, cy, th, r, outerPaint);
        else segment(canvas, hx + gap, aR, cy, th, r, outerPaint);
        // handle
        final float hw = AndroidUtilities.dpf2(2), hh = AndroidUtilities.dp(16);
        AndroidUtilities.rectTmp.set(hx - hw, cy - hh, hx + hw, cy + hh);
        canvas.drawRoundRect(AndroidUtilities.rectTmp, org.telegram.marble.MarbleTheme.m3CornerPxF(hw), org.telegram.marble.MarbleTheme.m3CornerPxF(hw), outerPaint);
    }

    private static void segment(Canvas canvas, float l, float r, float cy, float th, float rad, android.graphics.Paint p) {
        if (r - l < 1) return;
        AndroidUtilities.rectTmp.set(l, cy - th / 2f, r, cy + th / 2f);
        final float rr = Math.min(rad, (r - l) / 2f);
        canvas.drawRoundRect(AndroidUtilities.rectTmp, org.telegram.marble.MarbleTheme.m3CornerPxF(rr), org.telegram.marble.MarbleTheme.m3CornerPxF(rr), p);
    }
}
