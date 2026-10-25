package ru.chepelenko.btpairok;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Launcher screen. An app cannot turn its own accessibility service on, so this screen shows
 * whether the fix is active and, if not, opens the Accessibility settings page directly.
 */
public class MainActivity extends Activity {
    private static final String TAG = "PairOkFix";
    private static final String STUBS_PKG = "com.google.android.tv.frameworkpackagestubs";

    private TextView mTitle;
    private TextView mBody;
    private Button mPrimary;
    private Button mSecondary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pad = dp(48);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(dp(96), pad, dp(96), pad);

        mTitle = new TextView(this);
        mTitle.setTextSize(34);
        mBody = new TextView(this);
        mBody.setTextSize(20);
        mBody.setLineSpacing(0, 1.3f);
        mBody.setPadding(0, dp(24), 0, dp(32));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        mPrimary = button();
        mSecondary = button();
        buttons.addView(mPrimary);
        buttons.addView(mSecondary);
        ((LinearLayout.LayoutParams) mSecondary.getLayoutParams()).leftMargin = dp(24);

        root.addView(mTitle);
        root.addView(mBody);
        root.addView(buttons);
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isServiceEnabled()) {
            mTitle.setText(R.string.status_on_title);
            mBody.setText(R.string.status_on_body);
            mPrimary.setText(R.string.button_done);
            mPrimary.setOnClickListener(v -> finish());
            mSecondary.setText(R.string.button_settings);
            mSecondary.setOnClickListener(v -> openAccessibilitySettings());
        } else {
            mTitle.setText(R.string.status_off_title);
            String body = getString(R.string.status_off_body);
            if (Build.VERSION.SDK_INT >= 33) {
                body += "\n\n" + getString(R.string.restricted_hint);
            }
            mBody.setText(body);
            mPrimary.setText(R.string.button_open_settings);
            mPrimary.setOnClickListener(v -> openAccessibilitySettings());
            mSecondary.setText(R.string.button_later);
            mSecondary.setOnClickListener(v -> finish());
        }
        mPrimary.requestFocus();
    }

    private boolean isServiceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) {
            return false;
        }
        ComponentName mine = new ComponentName(this, PairOkService.class);
        for (String entry : enabled.split(":")) {
            if (mine.equals(ComponentName.unflattenFromString(entry))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The standard Accessibility intent is a "not supported" stub on Xiaomi/MediaTek TVs; their
     * Settings opens the same page from MainSettings with SUB_TYPE=NRT_SRC.
     */
    private void openAccessibilitySettings() {
        Intent standard = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        ResolveInfo ri = getPackageManager().resolveActivity(standard, 0);
        Intent xiaomi = new Intent()
                .setComponent(new ComponentName("com.android.tv.settings",
                        "com.android.tv.settings.MainSettings"))
                .putExtra("SUB_TYPE", "NRT_SRC");
        Intent[] candidates = ri != null && ri.activityInfo != null
                && !STUBS_PKG.equals(ri.activityInfo.packageName)
                ? new Intent[] {standard, xiaomi, new Intent(Settings.ACTION_SETTINGS)}
                : new Intent[] {xiaomi, standard, new Intent(Settings.ACTION_SETTINGS)};
        for (Intent intent : candidates) {
            if (getPackageManager().resolveActivity(intent, 0) == null) {
                continue;
            }
            try {
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                return;
            } catch (RuntimeException e) {
                Log.w(TAG, "cannot open " + intent, e);
            }
        }
        Toast.makeText(this, R.string.settings_not_found, Toast.LENGTH_LONG).show();
    }

    private Button button() {
        Button b = new Button(this);
        b.setBackgroundResource(R.drawable.button_bg);
        b.setTextColor(0xFFFFFFFF);
        b.setTextSize(18);
        b.setPadding(dp(32), dp(12), dp(32), dp(12));
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
