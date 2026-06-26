package com.jeenyultimate.helper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import java.util.List;

public class SmartOrdersAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null &&
            event.getPackageName().toString().equals("com.android.systemui")) return;

        SharedPreferences prefs = getSharedPreferences("SmartOrdersPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("auto_accept", false)) return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText("قبول العرض");
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                Rect bounds = new Rect();
                node.getBoundsInScreen(bounds);

                DisplayMetrics dm = getResources().getDisplayMetrics();
                int screenHeight = dm.heightPixels;
                int screenWidth  = dm.widthPixels;

                // الضغط في أسفل الشاشة — مكان زر قبول جيني الفعلي
                int clickX = screenWidth / 2;
                int clickY = screenHeight - (screenHeight / 8);

                // استخدام الإحداثيات الفعلية للزر إن وُجدت
                if (!bounds.isEmpty() && bounds.centerX() > 0 && bounds.centerY() > 0) {
                    clickX = bounds.centerX();
                    clickY = bounds.centerY();
                }

                GestureDescription.Builder gb = new GestureDescription.Builder();
                Path p = new Path();
                p.moveTo(clickX, clickY);
                gb.addStroke(new GestureDescription.StrokeDescription(p, 0, 80));
                dispatchGesture(gb.build(), null, null);

                prefs.edit().putInt("accepted_count",
                    prefs.getInt("accepted_count", 0) + 1).apply();
                node.recycle();
                break;
            }
        }
        rootNode.recycle();
    }

    @Override
    public void onInterrupt() {}
}
