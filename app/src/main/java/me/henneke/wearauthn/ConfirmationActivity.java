package me.henneke.wearauthn;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseIntArray;

@TargetApi(21)
public class ConfirmationActivity extends Activity implements ConfirmationOverlay.OnAnimationFinishedListener {
    public static final String EXTRA_MESSAGE = "android.support.wearable.activity.extra.MESSAGE";
    public static final String EXTRA_ANIMATION_TYPE = "android.support.wearable.activity.extra.ANIMATION_TYPE";
    public static final int SUCCESS_ANIMATION = 1;
    public static final int OPEN_ON_PHONE_ANIMATION = 2;
    public static final int FAILURE_ANIMATION = 3;
    private static final SparseIntArray CONFIRMATION_OVERLAY_TYPES = new SparseIntArray();

    public ConfirmationActivity() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.setTheme(style.ConfirmationActivity);
        Intent intent = this.getIntent();
        int requestedType = intent.getIntExtra("android.support.wearable.activity.extra.ANIMATION_TYPE", 1);
        if (CONFIRMATION_OVERLAY_TYPES.indexOfKey(requestedType) < 0) {
            throw new IllegalArgumentException((new StringBuilder(38)).append("Unknown type of animation: ").append(requestedType).toString());
        } else {
            int type = CONFIRMATION_OVERLAY_TYPES.get(requestedType);
            String message = intent.getStringExtra("android.support.wearable.activity.extra.MESSAGE");
            (new ConfirmationOverlay()).setType(type).setMessage(message).setFinishedAnimationListener(this).showOn(this);
        }
    }

    public void onAnimationFinished() {
        this.finish();
    }

    static {
        CONFIRMATION_OVERLAY_TYPES.append(1, 0);
        CONFIRMATION_OVERLAY_TYPES.append(2, 2);
        CONFIRMATION_OVERLAY_TYPES.append(3, 1);
    }
}
