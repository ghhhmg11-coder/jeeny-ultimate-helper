package com.jeenyultimate.helper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * SmartOrdersAccessibilityService
 * يراقب تطبيق جيني (com.jeeny.driver) حصرياً، وعند رصد نافذة جديدة
 * يضغط بالإحداثيات المئوية: 50% أفقياً، 90% رأسياً (موقع زر القبول البنفسجي).
 */
public class SmartOrdersAccessibilityService extends AccessibilityService {

    private static final String JEENY_PACKAGE = "com.jeeny.driver";
    private static final long CLICK_DELAY_MS = 800;

    private int screenWidth = 0;
    private int screenHeight = 0;
    private long lastClickTime = 0;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        loadScreenDimensions();
    }

    private void loadScreenDimensions() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (wm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.view.Display display = wm.getCurrentWindowMetrics().getBounds().width() > 0
                        ? null : null;
                android.graphics.Rect bounds = wm.getCurrentWindowMetrics().getBounds();
                screenWidth = bounds.width();
                screenHeight = bounds.height();
            } else {
                DisplayMetrics metrics = new DisplayMetrics();
                wm.getDefaultDisplay().getRealMetrics(metrics);
                screenWidth = metrics.widthPixels;
                screenHeight = metrics.heightPixels;
            }
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        // تأكد من أن الحدث من تطبيق جيني حصراً
        CharSequence packageName = event.getPackageName();
        if (packageName == null || !JEENY_PACKAGE.contentEquals(packageName)) return;

        // التحقق من نوع الحدث (نافذة جديدة أو تغيير في النافذة)
        int eventType = event.getEventType();
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences("SmartOrdersPrefs", MODE_PRIVATE);
        boolean autoAccept = prefs.getBoolean("auto_accept", false);
        if (!autoAccept) return;

        // منع الضغط المتكرر السريع
        long now = System.currentTimeMillis();
        if (now - lastClickTime < CLICK_DELAY_MS) return;

        // التحقق من أبعاد الشاشة
        if (screenWidth == 0 || screenHeight == 0) {
            loadScreenDimensions();
            if (screenWidth == 0 || screenHeight == 0) return;
        }

        int minPrice = prefs.getInt("min_price", 0);
        // ملاحظة: يمكن إضافة منطق لقراءة سعر الرحلة من شجرة العقدة هنا
        // حالياً يضغط تلقائياً عند رصد أي نشاط من جيني

        performPercentageClick(50, 90);
        lastClickTime = now;
    }

    /**
     * ينفذ ضغطة مادية على نقطة محددة بالنسبة المئوية من أبعاد الشاشة.
     *
     * @param xPercent النسبة المئوية الأفقية (0-100)
     * @param yPercent النسبة المئوية الرأسية (0-100) — 90% = أسفل الشاشة
     */
    private void performPercentageClick(int xPercent, int yPercent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;

        float x = screenWidth * (xPercent / 100f);
        float y = screenHeight * (yPercent / 100f);

        Path clickPath = new Path();
        clickPath.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(clickPath, 0, 100);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(stroke);

        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                // الضغط تم بنجاح
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                // فشل الضغط - يمكن إضافة إعادة المحاولة هنا
            }
        }, null);
    }

    @Override
    public void onInterrupt() {
        // الخدمة توقفت
    }
}
