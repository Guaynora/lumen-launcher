package com.lumen.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/** Routes the Home button to Lumen. Only watches the HOME key and when the Google TV home screen appears. */
public class HomeService extends AccessibilityService {
    /** true when the user deliberately opened Google TV from Lumen. */
    static volatile boolean googleMode = false;

    @Override protected boolean onKeyEvent(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
        if (e.getAction() == KeyEvent.ACTION_UP) goHome();
        return true;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (googleMode || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence cls = e.getClassName();
        if (cls != null && cls.toString().endsWith(".home.HomeActivity")) goHome();
    }

    void goHome() {
        googleMode = false;
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
    }

    @Override public void onInterrupt() { }
}
