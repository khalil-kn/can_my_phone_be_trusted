package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.ChangeEntity;
import com.khalilknaissi.can_my_phone_be_trusted.engine.Playbook;
import com.khalilknaissi.can_my_phone_be_trusted.engine.RiskRules;
import com.khalilknaissi.can_my_phone_be_trusted.model.IncidentState;

import java.util.Locale;

/** Small helpers shared by every screen: insets, dates, colours, opening Android settings. */
public final class Ui {
    private Ui() {}

    /** Pushes a view's content below the status bar, while its background still fills behind it. */
    public static void fitTop(View v) {
        final int top = v.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(view.getPaddingLeft(), top + bars.top, view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
    }

    /** Keeps buttons above the gesture bar and the keyboard. */
    public static void fitBottom(View v) {
        final int bottom = v.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), bottom + bars.bottom);
            return insets;
        });
    }

    /** Dark status bar icons on light screens, light icons on red, slate and ink screens. */
    public static void statusIcons(Activity a, boolean darkIcons) {
        WindowCompat.getInsetsController(a.getWindow(), a.getWindow().getDecorView()).setAppearanceLightStatusBars(darkIcons);
    }

    public static int color(Context c, int res) {
        return ContextCompat.getColor(c, res);
    }

    public static GradientDrawable rounded(Context c, int colorRes, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color(c, colorRes));
        g.setCornerRadius(radiusDp * c.getResources().getDisplayMetrics().density);
        return g;
    }

    public static void toast(Context c, String text) {
        Toast.makeText(c, text, Toast.LENGTH_LONG).show();
    }

    public static String plural(int n, String one, String many) {
        return n + " " + (n == 1 ? one : many);
    }

    // ---- dates

    public static String time(long t) {
        return DateFormat.format("HH:mm", t).toString();
    }

    public static String day(long t) {
        return DateFormat.format("d MMM", t).toString();
    }

    public static String dayTime(long t) {
        return day(t) + ", " + time(t);
    }

    /** "today 08:12", "yesterday 20:10" or "26 Oct, 20:10". */
    public static String when(long t) {
        if (t <= 0) return "never";
        if (DateUtils.isToday(t)) return "today " + time(t);
        if (DateUtils.isToday(t + DateUtils.DAY_IN_MILLIS)) return "yesterday " + time(t);
        return dayTime(t);
    }

    /** Group header in lists: "Today", "Yesterday", "Monday 26 Oct". */
    public static String dayHeader(long t) {
        if (DateUtils.isToday(t)) return "Today";
        if (DateUtils.isToday(t + DateUtils.DAY_IN_MILLIS)) return "Yesterday";
        return DateFormat.format("EEEE d MMM", t).toString();
    }

    /** When a change happened: an exact time, or the window between two checks. */
    public static String window(ChangeEntity c) {
        if (c.exactAt > 0) return "at " + when(c.exactAt);
        if (day(c.windowStart).equals(day(c.windowEnd))) {
            // "today 20:10–23:40": one day word, then the two check times.
            String w = when(c.windowEnd);
            return w.substring(0, w.length() - 5) + time(c.windowStart) + "–" + time(c.windowEnd);
        }
        return when(c.windowStart) + " – " + when(c.windowEnd);
    }

    public static String size(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format(Locale.US, "%.1f MB", bytes / 1048576.0);
    }

    // ---- small pieces of the design

    public static void pill(TextView pill, IncidentState state) {
        pill.setText(state.label);
        int bg, fg;
        switch (state) {
            case OPEN: bg = R.color.seal_red; fg = R.color.white; break;
            case INVESTIGATING: bg = R.color.tape; fg = R.color.ink; break;
            case RESOLVED: bg = R.color.slate; fg = R.color.white; break;
            default: bg = R.color.sand; fg = R.color.ink; break;
        }
        pill.setBackground(rounded(pill.getContext(), bg, 14));
        pill.setTextColor(color(pill.getContext(), fg));
    }

    public static String tapeText(long sealedAt) {
        String d = sealedAt > 0 ? DateFormat.format("d MMM yyyy", sealedAt).toString().toUpperCase(Locale.ROOT) : "";
        return "SEALED  ◆  " + d + "  ◆  BASELINE  ◆  SEALED  ◆  " + d + "  ◆  BASELINE";
    }

    // ---- opening Android screens

    public static void open(Context c, Intent i) {
        try {
            c.startActivity(i);
        } catch (ActivityNotFoundException | SecurityException e) {
            toast(c, "This phone doesn't have that settings screen.");
        }
    }

    public static Intent appSettings(String pkg) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg));
    }

    public static Intent uninstall(String pkg) {
        return new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg));
    }

    public static Intent settingScreen(String key) {
        switch (key) {
            case RiskRules.ADB:
            case RiskRules.ADB_WIFI:
            case RiskRules.DEV_OPTIONS: return new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
            case RiskRules.PATCH:
            case RiskRules.ANDROID: return new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
            default: return new Intent(Settings.ACTION_SECURITY_SETTINGS);
        }
    }

    public static Intent forStep(Playbook.Step s) {
        switch (s.action) {
            case UNINSTALL: return uninstall(s.packageName);
            case APP_SETTINGS: return appSettings(s.packageName);
            case ACCESSIBILITY: return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            case SECURITY: return new Intent(Settings.ACTION_SECURITY_SETTINGS);
            case NOTIFICATION_ACCESS: return new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            case DEVELOPER: return new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
            case DEVICE_INFO: return new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
            default: return null;
        }
    }

    public static String actionLabel(Playbook.Action a) {
        switch (a) {
            case UNINSTALL: return "Uninstall…";
            case APP_SETTINGS: return "Open app permissions";
            case ACCESSIBILITY: return "Open accessibility settings";
            case SECURITY: return "Open security settings";
            case NOTIFICATION_ACCESS: return "Open notification access";
            case DEVELOPER: return "Open developer options";
            case DEVICE_INFO: return "Open system info";
            default: return "";
        }
    }
}
