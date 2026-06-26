package com.jeenyultimate.helper;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private Switch switchAutoAccept;
    private EditText etMinProfit, etMaxDistance;
    private TextView tvStatus, tvSaveStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("SmartOrdersPrefs", MODE_PRIVATE);

        Button btnAccessibility = findViewById(R.id.btn_open_accessibility);
        Button btnSave = findViewById(R.id.btn_save);
        switchAutoAccept = findViewById(R.id.switch_auto_accept);
        etMinProfit = findViewById(R.id.et_min_profit);
        etMaxDistance = findViewById(R.id.et_max_distance);
        tvStatus = findViewById(R.id.tv_accessibility_status);
        tvSaveStatus = findViewById(R.id.tv_save_status);

        loadSettings();

        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        btnSave.setOnClickListener(v -> saveSettings());

        switchAutoAccept.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("auto_accept", isChecked).apply();
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())), 101);
        } else {
            startService(new Intent(this, FloatingButtonService.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAccessibilityStatus();
    }

    private void updateAccessibilityStatus() {
        boolean enabled = isAccessibilityServiceEnabled();
        if (enabled) {
            tvStatus.setText("✅ الخدمة مفعّلة وتعمل");
            tvStatus.setTextColor(0xFF388E3C);
        } else {
            tvStatus.setText("⚠️ الخدمة غير مفعّلة — اضغط الزر أعلاه");
            tvStatus.setTextColor(0xFFD32F2F);
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + SmartOrdersAccessibilityService.class.getCanonicalName();
        try {
            int enabled = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, 0);
            if (enabled != 1) return false;
            String settingValue = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue == null) return false;
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
            splitter.setString(settingValue);
            while (splitter.hasNext()) {
                if (splitter.next().equalsIgnoreCase(service)) return true;
            }
        } catch (Exception e) { /* ignore */ }
        return false;
    }

    private void loadSettings() {
        float minProfit = prefs.getFloat("min_profit", 0f);
        float maxDistance = prefs.getFloat("max_distance", 0f);
        boolean autoAccept = prefs.getBoolean("auto_accept", false);

        if (minProfit > 0) etMinProfit.setText(String.valueOf(minProfit));
        if (maxDistance > 0) etMaxDistance.setText(String.valueOf(maxDistance));
        switchAutoAccept.setChecked(autoAccept);
    }

    private void saveSettings() {
        String profitStr = etMinProfit.getText().toString().trim();
        String distStr = etMaxDistance.getText().toString().trim();

        float minProfit = profitStr.isEmpty() ? 0f : Float.parseFloat(profitStr);
        float maxDistance = distStr.isEmpty() ? 0f : Float.parseFloat(distStr);

        prefs.edit()
                .putFloat("min_profit", minProfit)
                .putFloat("max_distance", maxDistance)
                .putBoolean("auto_accept", switchAutoAccept.isChecked())
                .apply();

        tvSaveStatus.setText("✅ تم حفظ الإعدادات بنجاح");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 101 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && Settings.canDrawOverlays(this)) {
            startService(new Intent(this, FloatingButtonService.class));
        }
    }
}
