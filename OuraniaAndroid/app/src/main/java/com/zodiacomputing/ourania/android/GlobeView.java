package com.zodiacomputing.ourania.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.gui.Globe;
import com.zodiacomputing.ourania.gui.GlobeRenderer;
import com.zodiacomputing.ourania.gui.Ink;
import com.zodiacomputing.ourania.gui.WheelLayout;

/**
 * The chart as a globe, on the phone: drag to turn it, pinch to come closer, tap a planet.
 *
 * <h3>There is no drawing in this file, and that is the point</h3>
 *
 * <p>The globe was drawn by {@code GlobeRenderer}, which M11 stage 4 took its last seventy
 * references into {@code SkymapPanel} away from so it could draw through a {@link
 * com.zodiacomputing.ourania.gui.Pen} instead of a {@code Graphics2D}. {@code AndroidPen} is
 * that pen on Android's canvas, and {@code PhoneGlobeSource} answers the renderer's thirty-one
 * questions from a {@code ChartFrame}. All three were finished, tested and wired to nothing:
 * the phone carried a globe renderer it never called.
 *
 * <p>So this class is the wiring and only the wiring. It owns a camera, forwards touches to
 * it, and calls {@code paint}. If the phone's globe ever differs from the desktop's it will be
 * because the source answered differently, not because a second painter drifted - which is the
 * whole reason stage 4 was done the way it was.
 *
 * <h3>The camera is the desktop's too</h3>
 *
 * <p>{@link Globe} already clamps its own pitch and distance, coasts after a flick and settles.
 * Re-deriving any of that here would have been a second set of limits to disagree with the
 * first, so a drag is {@code cam.drag}, a pinch is {@code cam.zoom}, and the inertia is
 * {@code cam.settle} driven by {@code postInvalidateOnAnimation} where the desktop uses a
 * sixteen-millisecond Swing timer.
 */
final class GlobeView extends View {

    /** The camera. Its own limits; this class never clamps yaw, pitch or distance itself. */
    private final Globe cam = new Globe();

    private PhoneGlobeSource source;

    /**
     * True while the globe is being moved or is still coasting.
     *
     * <p>It is handed to {@code paint} as its {@code turning} flag, which is the renderer's cue
     * to drop the detail that costs most and reads least mid-motion. The desktop sets it the
     * same way.
     */
    private boolean turning;

    private final ScaleGestureDetector pinch;
    private final GestureDetector gestures;

    /** What the last tap selected, so the caller can name it under the globe. */
    private OnBody listener;

    interface OnBody {
        void tapped(int body);
    }

    GlobeView(Context context) {
        super(context);
        this.pinch = new ScaleGestureDetector(context,
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector d) {
                    // A pinch factor is a ratio and zoom() takes wheel ticks, so the log turns
                    // one into the other: pinching to twice the size is the same number of
                    // ticks whatever size it started at, which is what makes it feel linear.
                    GlobeView.this.cam.zoom(-Math.log(Math.max(0.01, d.getScaleFactor())) * 6.0);
                    GlobeView.this.moving();
                    return true;
                }
            });
        this.gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent down, MotionEvent now, float dx, float dy) {
                if (GlobeView.this.pinch.isInProgress()) {
                    return false;       // a two-finger move is a zoom, not a turn
                }
                GlobeView.this.cam.drag(-dx, -dy, Math.max(1, getHeight()));
                GlobeView.this.moving();
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                GlobeView.this.tap(e.getX(), e.getY());
                return true;
            }
        });
    }

    void setOnBody(OnBody listener) {
        this.listener = listener;
    }

    /** Show this chart, with the sky on its own shell when there is one. */
    void show(ChartFrame frame, ChartFrame sky) {
        this.source = frame == null ? null : PhoneGlobeSource.of(frame, sky);
        invalidate();
    }

    /** The body the last tap landed on, or -1. */
    int selected() {
        return this.source == null ? -1 : this.source.focusedBody();
    }

    private void moving() {
        this.turning = true;
        invalidate();
    }

    private void tap(float x, float y) {
        if (this.source == null) {
            return;
        }
        // The renderer's own hit test, not a second one. It reads the same shells the painter
        // does and applies the same stack; anything written here would be the defect its own
        // comment warns about, in the view where a reader is least able to notice it.
        int hit = GlobeRenderer.bodyAt(this.cam, getWidth(), getHeight(), this.source,
            Math.round(x), Math.round(y));
        // A hit is packed - the body in the low bits, the ring in TRANSIT_BIT and SKY_BIT -
        // and WheelLayout.hitBody is the reader that matches the packHit that wrote it.
        int body = hit < 0 ? -1 : WheelLayout.hitBody(hit);
        if (this.source.focusBody(body)) {
            invalidate();
        }
        if (this.listener != null) {
            this.listener.tapped(body);
        }
    }

    @Override
    protected void onMeasure(int w, int h) {
        // Square, as the wheel is, and for a stronger reason: the camera's projection divides
        // by the smaller of the two sides, so a short wide box would letterbox the globe and
        // waste the width. A custom View asked for WRAP_CONTENT measures to nothing, so this
        // is not optional - without it the globe is present, correct and zero pixels tall.
        int width = MeasureSpec.getSize(w);
        setMeasuredDimension(width, width);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        this.pinch.onTouchEvent(e);
        this.gestures.onTouchEvent(e);
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        Ink ground = PhonePalette.background();
        c.drawColor(Color.rgb(ground.getRed(), ground.getGreen(), ground.getBlue()));
        if (this.source == null || getWidth() == 0) {
            return;
        }
        GlobeRenderer.paint(new AndroidPen(c), this.cam, getWidth(), getHeight(),
            this.source, this.turning);

        // The flick's inertia, on the frame clock rather than a timer. settle() reports whether
        // the camera is still moving, so the loop stops by itself when it comes to rest - and
        // `turning` goes false on the same frame, so the last frame drawn is the detailed one.
        if (this.cam.coasting()) {
            this.cam.settle();
            postInvalidateOnAnimation();
        } else {
            if (this.turning) {
                this.turning = false;
                postInvalidateOnAnimation();
            }
        }
    }
}
