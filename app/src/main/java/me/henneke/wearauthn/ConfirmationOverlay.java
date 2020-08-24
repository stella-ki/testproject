package me.henneke.wearauthn;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

public class ConfirmationOverlay {

    /**
     * Interface for listeners to be notified when the {@link ConfirmationOverlay} animation has
     * finished and its {@link View} has been removed.
     */
    public interface OnAnimationFinishedListener {
        /**
         * Called when the confirmation animation is finished.
         */
        void onAnimationFinished();
    }

    /** Default animation duration in ms. **/
    public static final int DEFAULT_ANIMATION_DURATION_MS = 1000;

    /** Types of animations to display in the overlay. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SUCCESS_ANIMATION, FAILURE_ANIMATION, OPEN_ON_PHONE_ANIMATION})
    public @interface OverlayType {
    }

    /** {@link OverlayType} indicating the success animation overlay should be displayed. */
    public static final int SUCCESS_ANIMATION = 0;

    /**
     * {@link OverlayType} indicating the failure overlay should be shown. The icon associated with
     * this type, unlike the others, does not animate.
     */
    public static final int FAILURE_ANIMATION = 1;

    /** {@link OverlayType} indicating the "Open on Phone" animation overlay should be displayed. */
    public static final int OPEN_ON_PHONE_ANIMATION = 2;

    @OverlayType
    private int mType = SUCCESS_ANIMATION;
    private int mDurationMillis = DEFAULT_ANIMATION_DURATION_MS;
    private OnAnimationFinishedListener mListener;
    private String mMessage;
    private View mOverlayView;
    private Drawable mOverlayDrawable;
    private boolean mIsShowing = false;

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private final Runnable mHideRunnable =
            new Runnable() {
                @Override
                public void run() {
                    hide();
                }
            };

    /**
     * Sets a message which will be displayed at the same time as the animation.
     *
     * @return {@code this} object for method chaining.
     */
    public ConfirmationOverlay setMessage(String message) {
        mMessage = message;
        return this;
    }

    /**
     * Sets the {@link OverlayType} which controls which animation is displayed.
     *
     * @return {@code this} object for method chaining.
     */
    public ConfirmationOverlay setType(@OverlayType int type) {
        mType = type;
        return this;
    }

    /**
     * Sets the duration in milliseconds which controls how long the animation will be displayed.
     * Default duration is {@link #DEFAULT_ANIMATION_DURATION_MS}.
     *
     * @return {@code this} object for method chaining.
     */
    public ConfirmationOverlay setDuration(int millis) {
        mDurationMillis = millis;
        return this;
    }

    /**
     * Sets the {@link OnAnimationFinishedListener} which will be invoked once the overlay is no
     * longer visible.
     *
     * @return {@code this} object for method chaining.
     */
    public ConfirmationOverlay setFinishedAnimationListener(
            @Nullable OnAnimationFinishedListener listener) {
        mListener = listener;
        return this;
    }

    /**
     * Adds the overlay as a child of {@code view.getRootView()}, removing it when complete. While
     * it is shown, all touches will be intercepted to prevent accidental taps on obscured views.
     */
    @MainThread
    public void showAbove(View view) {
        if (mIsShowing) {
            return;
        }
        mIsShowing = true;

        updateOverlayView(view.getContext());
        ((ViewGroup) view.getRootView()).addView(mOverlayView);
        animateAndHideAfterDelay();
    }

    /**
     * Adds the overlay as a content view to the {@code activity}, removing it when complete. While
     * it is shown, all touches will be intercepted to prevent accidental taps on obscured views.
     */
    @MainThread
    public void showOn(Activity activity) {
        if (mIsShowing) {
            return;
        }
        mIsShowing = true;

        updateOverlayView(activity);
        activity.getWindow().addContentView(mOverlayView, mOverlayView.getLayoutParams());
        animateAndHideAfterDelay();
    }

    @MainThread
    private void animateAndHideAfterDelay() {
        if (mOverlayDrawable instanceof Animatable) {
            Animatable animatable = (Animatable) mOverlayDrawable;
            animatable.start();
        }
        mMainThreadHandler.postDelayed(mHideRunnable, mDurationMillis);
    }

    /**
     * Starts a fadeout animation and removes the view once finished. This is invoked by {@link
     * #mHideRunnable} after {@link #mDurationMillis} milliseconds.
     *
     * @hide
     */
    @MainThread
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void hide() {
        Animation fadeOut =
                AnimationUtils.loadAnimation(mOverlayView.getContext(), android.R.anim.fade_out);
        fadeOut.setAnimationListener(
                new AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        mOverlayView.clearAnimation();
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        ((ViewGroup) mOverlayView.getParent()).removeView(mOverlayView);
                        mIsShowing = false;
                        if (mListener != null) {
                            mListener.onAnimationFinished();
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
        mOverlayView.startAnimation(fadeOut);
    }

    @MainThread
    private void updateOverlayView(Context context) {
        if (mOverlayView == null) {
            //noinspection InflateParams
            mOverlayView =
                    LayoutInflater.from(context).inflate(R.layout.ws_overlay_confirmation, null);
        }
        mOverlayView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mOverlayView.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        updateImageView(context, mOverlayView);
        updateMessageView(context, mOverlayView);
    }

    @MainThread
    private void updateMessageView(Context context, View overlayView) {
        TextView messageView =
                overlayView.findViewById(R.id.wearable_support_confirmation_overlay_message);

        if (mMessage != null) {
            /*int screenWidthPx = ResourcesUtil.getScreenWidthPx(context);
            int topMarginPx = ResourcesUtil.getFractionOfScreenPx(
                    context, screenWidthPx, R.fraction.confirmation_overlay_margin_above_text);
            int sideMarginPx =
                    ResourcesUtil.getFractionOfScreenPx(
                            context, screenWidthPx, R.fraction.confirmation_overlay_margin_side);

            MarginLayoutParams layoutParams = (MarginLayoutParams) messageView.getLayoutParams();
            layoutParams.topMargin = topMarginPx;
            layoutParams.leftMargin = sideMarginPx;
            layoutParams.rightMargin = sideMarginPx;

            messageView.setLayoutParams(layoutParams);*/
            messageView.setText(mMessage);
            messageView.setVisibility(View.VISIBLE);

        } else {
            messageView.setVisibility(View.GONE);
        }
    }

    @MainThread
    private void updateImageView(Context context, View overlayView) {
        switch (mType) {
            case SUCCESS_ANIMATION:
                mOverlayDrawable = ContextCompat.getDrawable(context,
                        R.drawable.accept_deny_dialog_negative_bg);
                break;
            case FAILURE_ANIMATION:
                mOverlayDrawable = ContextCompat.getDrawable(context, R.drawable.common_google_signin_btn_icon_dark_focused);
                break;
            case OPEN_ON_PHONE_ANIMATION:
                mOverlayDrawable = ContextCompat.getDrawable(context, R.drawable.common_google_signin_btn_icon_dark);
                break;
            default:
                String errorMessage =
                        String.format(Locale.US, "Invalid ConfirmationOverlay type [%d]", mType);
                throw new IllegalStateException(errorMessage);
        }

        ImageView imageView =
                overlayView.findViewById(R.id.wearable_support_confirmation_overlay_image);
        imageView.setImageDrawable(mOverlayDrawable);
    }
}