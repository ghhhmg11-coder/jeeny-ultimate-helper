package com.jeenyultimate.helper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.util.Log;

public class SmartOrdersAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null ||
            !event.getPackageName().toString().equals("com.jeeny.driver")) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences("SmartOrdersPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("auto_accept", false)) return;

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        // الضغط في أسفل الشاشة تماماً بنسبة 90%
        int targetX = width / 2;
        int targetY = (int) (height * 0.90);

        Log.d("JeenyHelper", "Executing click at X: " + targetX + " Y: " + targetY);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        Path clickPath = new Path();
        clickPath.moveTo(targetX, targetY);
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, 50));

        dispatchGesture(gestureBuilder.build(), null, null);

        int currentCount = prefs.getInt("accepted_count", 0);
        prefs.edit().putInt("accepted_count", currentCount + 1).apply();
    }

    @Override
    public void onInterrupt() {}
}
