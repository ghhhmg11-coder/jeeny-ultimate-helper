package com.jeenyultimate.helper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;

public class SmartOrdersAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // حماية قصوى: العمل فقط وحصرياً داخل تطبيق جيني الكابتن
        if (event.getPackageName() == null ||
            !event.getPackageName().toString().equals("com.jeeny.driver")) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences("SmartOrdersPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("auto_accept", false)) return;

        // جلب أبعاد الشاشة الحالية بدقة هندسية
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width  = dm.widthPixels;
        int height = dm.heightPixels;

        // استهداف دقيق: منتصف العرض + 88% من ارتفاع الشاشة (فوق زر قبول العرض مباشرة)
        int targetX = width / 2;
        int targetY = (int) (height * 0.88);

        // نقرة مادية سريعة 50ms
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        Path clickPath = new Path();
        clickPath.moveTo(targetX, targetY);
        gestureBuilder.addStroke(
            new GestureDescription.StrokeDescription(clickPath, 0, 50));

        dispatchGesture(gestureBuilder.build(), null, null);

        // تحديث عداد الرحلات المقبولة
        int currentCount = prefs.getInt("accepted_count", 0);
        prefs.edit().putInt("accepted_count", currentCount + 1).apply();
    }

    @Override
    public void onInterrupt() {}
}
