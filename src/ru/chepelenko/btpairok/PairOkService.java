package ru.chepelenko.btpairok;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Xiaomi Mi TV (Android TV 10, TvSettingsPlus) bug: in BluetoothPairingDialog the focus
 * stays on the title TextView and the action buttons are not focusable, so the remote's
 * OK key is dropped as "Unhandled key" and pairing times out. Only a mouse click works.
 *
 * While that dialog is in front, this service turns the remote's OK into a click on the
 * dialog's "Pair" action. Every other key, and OK on every other screen, passes through.
 */
public class PairOkService extends AccessibilityService {
    private static final String TAG = "PairOkFix";
    private static final String SETTINGS_PKG = "com.android.tv.settings";
    private static final String DIALOG_CLS = SETTINGS_PKG + ".accessories.BluetoothPairingDialog";

    private boolean mDialogInFront;
    private boolean mConsumedDown;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || event.getPackageName() == null || event.getClassName() == null) {
            return;
        }
        ComponentName cn = new ComponentName(event.getPackageName().toString(),
                event.getClassName().toString());
        // Fragments and views also send this event; only an activity change moves the flag.
        if (!isActivity(cn)) {
            return;
        }
        mDialogInFront = DIALOG_CLS.equals(cn.getClassName());
        Log.d(TAG, "front activity " + cn.flattenToShortString() + " dialog=" + mDialogInFront);
    }

    private boolean isActivity(ComponentName cn) {
        try {
            getPackageManager().getActivityInfo(cn, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (code != KeyEvent.KEYCODE_DPAD_CENTER && code != KeyEvent.KEYCODE_ENTER
                && code != KeyEvent.KEYCODE_NUMPAD_ENTER) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            mConsumedDown = mDialogInFront && findPairNode() != null;
            return mConsumedDown;
        }
        if (event.getAction() == KeyEvent.ACTION_UP && mConsumedDown) {
            mConsumedDown = false;
            AccessibilityNodeInfo node = findPairNode();
            if (node != null) {
                click(node);
            }
            return true;
        }
        return false;
    }

    private AccessibilityNodeInfo findPairNode() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null
                || !SETTINGS_PKG.equals(root.getPackageName().toString())) {
            return null;
        }
        for (String label : pairLabels()) {
            for (AccessibilityNodeInfo n : root.findAccessibilityNodeInfosByText(label)) {
                if (n.getText() != null && label.equalsIgnoreCase(n.getText().toString().trim())) {
                    return n;
                }
            }
        }
        return null;
    }

    /** The dialog's own "Pair" string in the current locale, plus known fallbacks. */
    private List<String> pairLabels() {
        List<String> labels = new ArrayList<>();
        try {
            Context settings = createPackageContext(SETTINGS_PKG, 0);
            int id = settings.getResources().getIdentifier("bluetooth_pair", "string", SETTINGS_PKG);
            if (id != 0) {
                labels.add(settings.getString(id));
            }
        } catch (Exception e) {
            Log.w(TAG, "cannot read settings strings", e);
        }
        labels.add("ОК");
        labels.add("OK");
        labels.add("Подключить");
        labels.add("Pair");
        return labels;
    }

    private void click(AccessibilityNodeInfo node) {
        for (AccessibilityNodeInfo n = node; n != null; n = n.getParent()) {
            if (n.isClickable() && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                Log.i(TAG, "clicked pair action");
                return;
            }
        }
        // No clickable ancestor: tap the label like a mouse would.
        Rect r = new Rect();
        node.getBoundsInScreen(r);
        Path p = new Path();
        p.moveTo(r.exactCenterX(), r.exactCenterY());
        dispatchGesture(new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, 50)).build(), null, null);
        Log.i(TAG, "tapped pair action at " + r.centerX() + "," + r.centerY());
    }

    @Override
    public void onInterrupt() {
    }
}
