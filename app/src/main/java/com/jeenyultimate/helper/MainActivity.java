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

        EditText etPrice = findViewById(R.id.et_min_price);
        EditText etDist = findViewById(R.id.et_max_dist);
        TextView tvCount = findViewById(R.id.tv_accepted_count);
        Button btnAcc = findViewById(R.id.btn_accessibility);
        Button btnFloat = findViewById(R.id.btn_start_floating);

        tvCount.setText("إجمالي الرحلات المقبولة تلقائياً: " + prefs.getInt("accepted_count", 0));

        btnAcc.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        btnFloat.setOnClickListener(v -> {
            String priceStr = etPrice.getText().toString();
            String distStr = etDist.getText().toString();
            prefs.edit()
                .putInt("min_price", Integer.parseInt(priceStr.isEmpty() ? "0" : priceStr))
                .putInt("max_dist", Integer.parseInt(distStr.isEmpty() ? "99" : distStr))
                .apply();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                startActivityForResult(
                    new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
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
