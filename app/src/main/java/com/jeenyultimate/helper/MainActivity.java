package com.jeenyultimate.helper;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStartFloating = findViewById(R.id.btn_start_floating);
        Button btnOpenAccessibility = findViewById(R.id.btn_open_accessibility);

        btnStartFloating.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService();
            } else {
                requestOverlayPermission();
            }
        });

        btnOpenAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this,
                    "ابحث عن 'Smart Orders' وفعّل الخدمة",
                    Toast.LENGTH_LONG).show();
        });
    }

    private void startFloatingService() {
        Intent intent = new Intent(this, FloatingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Toast.makeText(this, "تم تشغيل لوحة التحكم العائمة", Toast.LENGTH_SHORT).show();
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService();
            } else {
                Toast.makeText(this,
                        "يجب منح صلاحية الظهور فوق التطبيقات",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
