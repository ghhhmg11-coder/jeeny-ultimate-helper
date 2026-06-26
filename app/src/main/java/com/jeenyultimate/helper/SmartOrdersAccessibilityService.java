package com.jeenyultimate.helper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class SmartOrdersAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null &&
                event.getPackageName().toString().equals("com.android.systemui")) return;

        SharedPreferences prefs = getSharedPreferences("SmartOrdersPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("auto_accept", false)) return;

        float minProfit   = prefs.getFloat("min_profit", 0f);
        float maxDistance = prefs.getFloat("max_distance", 0f);

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        // ── 1. تحقق من السعر ──────────────────────────────────────────────
        if (minProfit > 0) {
            float detectedProfit = extractNumber(root, new String[]{
                "ريال", "SAR", "ر.س", "الأرباح", "السعر", "الإجمالي", "التوصيل"
            });
            if (detectedProfit > 0 && detectedProfit < minProfit) {
                root.recycle();
                return;   // الربح أقل من الحد الأدنى — تجاهل
            }
        }

        // ── 2. تحقق من المسافة ────────────────────────────────────────────
        if (maxDistance > 0) {
            float detectedDistance = extractNumber(root, new String[]{
                "كم", "km", "KM", "المسافة", "distance"
            });
            if (detectedDistance > 0 && detectedDistance > maxDistance) {
                root.recycle();
                return;   // المسافة أبعد من الحد الأقصى — تجاهل
            }
        }

        // ── 3. ابحث عن زر "قبول العرض" واضغطه ───────────────────────────
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText("قبول العرض");
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node.getText() != null && node.getText().toString().contains("قبول العرض")) {
                    clickNode(node);
                    node.recycle();
                }
            }
        }
        root.recycle();
    }

    /** يستخرج أول رقم من أي عنصر يحتوي على إحدى الكلمات المفتاحية */
    private float extractNumber(AccessibilityNodeInfo root, String[] keywords) {
        for (String keyword : keywords) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(keyword);
            if (nodes == null) continue;
            for (AccessibilityNodeInfo node : nodes) {
                CharSequence text = node.getText();
                if (text == null) text = node.getContentDescription();
                if (text != null) {
                    String raw = text.toString().replaceAll("[^0-9.]", "");
                    if (!raw.isEmpty()) {
                        try { return Float.parseFloat(raw); } catch (NumberFormatException ignored) {}
                    }
                }
                node.recycle();
            }
        }
        return -1f;
    }

    /** ينقر على عنصر عبر إيماءة فيزيائية مع صعود للآباء إن كانت المنطقة فارغة */
    private void clickNode(AccessibilityNodeInfo node) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.isEmpty() || bounds.width() <= 0) {
            AccessibilityNodeInfo parent = node.getParent();
            for (int i = 0; i < 4 && parent != null; i++) {
                parent.getBoundsInScreen(bounds);
                if (!bounds.isEmpty() && bounds.width() > 0) break;
                parent = parent.getParent();
            }
        }
        if (!bounds.isEmpty() && bounds.centerX() > 0 && bounds.centerY() > 0) {
            Path p = new Path();
            p.moveTo(bounds.centerX(), bounds.centerY());
            GestureDescription.Builder gb = new GestureDescription.Builder();
            gb.addStroke(new GestureDescription.StrokeDescription(p, 0, 100));
            dispatchGesture(gb.build(), null, null);
        }
    }

    @Override public void onInterrupt() {}
}
