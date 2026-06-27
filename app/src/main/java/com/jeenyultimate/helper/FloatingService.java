package com.jeenyultimate.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;

public class FloatingService extends Service {

    private static final String CHANNEL_ID = "FloatingServiceChannel";
    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_panel, null);

        // إعدادات النافذة العائمة TYPE_APPLICATION_OVERLAY لتظهر فوق كل شيء
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 80;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, params);

        setupPanelControls();
    }

    private void setupPanelControls() {
        SharedPreferences prefs = getSharedPreferences("SmartOrdersPrefs", MODE_PRIVATE);

        Switch swAuto = floatingView.findViewById(R.id.sw_floating_auto);
        EditText etMinPrice = floatingView.findViewById(R.id.et_float_min_price);
        EditText etMaxWait = floatingView.findViewById(R.id.et_float_max_wait);
        Switch swNoDestination = floatingView.findViewById(R.id.sw_no_destination);
        Button btnSave = floatingView.findViewById(R.id.btn_float_save);
        Button btnClose = floatingView.findViewById(R.id.btn_float_close);
        TextView tvStatus = floatingView.findViewById(R.id.tv_status);

        // تحميل الإعدادات المحفوظة
        swAuto.setChecked(prefs.getBoolean("auto_accept", false));
        int savedMinPrice = prefs.getInt("min_price", 0);
        int savedMaxWait = prefs.getInt("max_wait", 10);
        boolean savedNoDestination = prefs.getBoolean("no_destination", false);

        if (savedMinPrice > 0) etMinPrice.setText(String.valueOf(savedMinPrice));
        etMaxWait.setText(String.valueOf(savedMaxWait));
        swNoDestination.setChecked(savedNoDestination);

        updateStatusText(tvStatus, swAuto.isChecked());

        swAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("auto_accept", isChecked).apply();
            updateStatusText(tvStatus, isChecked);
        });

        swNoDestination.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("no_destination", isChecked).apply());

        btnSave.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();

            String minPriceStr = etMinPrice.getText().toString().trim();
            if (!minPriceStr.isEmpty()) {
                editor.putInt("min_price", Integer.parseInt(minPriceStr));
            }

            String maxWaitStr = etMaxWait.getText().toString().trim();
            if (!maxWaitStr.isEmpty()) {
                editor.putInt("max_wait", Integer.parseInt(maxWaitStr));
            }

            editor.apply();
            tvStatus.setText("✅ تم حفظ الإعدادات");
        });

        btnClose.setOnClickListener(v -> stopSelf());

        // سحب وتحريك اللوحة العائمة على الشاشة
        floatingView.findViewById(R.id.panel_header).setOnTouchListener(new View.OnTouchListener() {
            private int initialY;
            private float initialTouchY;
            private long lastClickTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialY = params.y;
                        initialTouchY = event.getRawY();
                        lastClickTime = System.currentTimeMillis();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(deltaY) > 5) {
                            params.y = initialY + deltaY;
                            windowManager.updateViewLayout(floatingView, params);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                }
                return false;
            }
        });
    }

    private void updateStatusText(TextView tv, boolean isActive) {
        if (isActive) {
            tv.setText("🟢 النظام نشط - يراقب طلبات جيني");
            tv.setTextColor(0xFF03DAC6);
        } else {
            tv.setText("🔴 النظام متوقف");
            tv.setTextColor(0xFFCF6679);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "لوحة تحكم جيني",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("خدمة القبول التلقائي لطلبات جيني");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("لوحة تحكم جيني الذكية")
                .setContentText("النظام يعمل في الخلفية")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }
}
