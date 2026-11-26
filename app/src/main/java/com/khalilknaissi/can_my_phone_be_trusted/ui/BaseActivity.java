package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.khalilknaissi.can_my_phone_be_trusted.data.local.Prefs;

/**
 * Every screen extends this. It draws edge to edge, hides the screen from screenshots
 * when the user asked for it, and asks for the phone's PIN when the app lock is on.
 */
public abstract class BaseActivity extends AppCompatActivity {

    // static = shared by all screens of the app, not one per screen.
    private static int visibleScreens;
    private static long leftAt;
    private static boolean unlocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        if (Prefs.get(this).secureScreen()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        visibleScreens++;
    }

    @Override
    protected void onStop() {
        super.onStop();
        visibleScreens--;
        if (visibleScreens == 0) leftAt = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (needsUnlock()) startActivity(new Intent(this, UnlockActivity.class));
    }

    private boolean needsUnlock() {
        if (this instanceof UnlockActivity || this instanceof OnboardingActivity) return false;
        Prefs prefs = Prefs.get(this);
        if (!prefs.lockOn()) return false;
        if (!unlocked) return true;
        if (leftAt > 0 && System.currentTimeMillis() - leftAt >= prefs.lockTimeoutMs()) {
            unlocked = false;
            return true;
        }
        return false;
    }

    static void markUnlocked() {
        unlocked = true;
        leftAt = 0;
    }
}
