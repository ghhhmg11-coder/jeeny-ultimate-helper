package com.jeenyultimate.helper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.SharedPreferences;

public class MainActivity extends Activity {
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("SmartOrdersPrefs", MODE_PRIVATE);

        EditText etMinDist  = findViewById(R.id.et_min_dist);
        EditText etMaxDist  = findViewById(R.id.et_max_dist);
        EditText etMinPrice = findViewById(R.id.et_min_price);
        EditText etMaxPrice = findViewById(R.id.et_max_price);
        TextView tvCount    = findViewById(R.id.tv_accepted_count);
        Button   btnAcc     = findViewById(R.id.btn_accessibility);
        Button   btnFloat   = findViewById(R.id.btn_start_floating);

        if (prefs.contains("min_dist"))
            etMinDist.setText(String.valueOf(prefs.getFloat("min_dist", 0f)));
        if (prefs.contains("max_dist"))
            etMaxDist.setText(String.valueOf(prefs.getFloat("max_dist", 10f)));
        if (prefs.contains("min_price"))
            etMinPrice.setText(String.valueOf(prefs.getInt("min_price", 0)));
        if (prefs.contains("max_price"))
            etMaxPrice.setText(String.valueOf(prefs.getInt("max_price", 999)));

        tvCount.setText("إجمالي الرحلات المقبولة تلقائياً: " + prefs.getInt("accepted_count", 0));

        btnAcc.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        btnFloat.setOnClickListener(v -> {
            String minDistStr  = etMinDist.getText().toString().trim();
            String maxDistStr  = etMaxDist.getText().toString().trim();
            String minPriceStr = etMinPrice.getText().toString().trim();
            String maxPriceStr = etMaxPrice.getText().toString().trim();

            prefs.edit()
                .putFloat("min_dist",  minDistStr.isEmpty()  ? 0f  : Float.parseFloat(minDistStr))
                .putFloat("max_dist",  maxDistStr.isEmpty()  ? 10f : Float.parseFloat(maxDistStr))
                .putInt("min_price",   minPriceStr.isEmpty() ? 0   : Integer.parseInt(minPriceStr))
                .putInt("max_price",   maxPriceStr.isEmpty() ? 999 : Integer.parseInt(maxPriceStr))
                .apply();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())), 101);
            } else {
                startService(new Intent(this, FloatingButtonService.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((TextView) findViewById(R.id.tv_accepted_count))
            .setText("إجمالي الرحلات المقبولة تلقائياً: " + prefs.getInt("accepted_count", 0));
    }
}
