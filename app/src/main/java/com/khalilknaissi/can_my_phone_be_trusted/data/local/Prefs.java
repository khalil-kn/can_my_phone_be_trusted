package com.khalilknaissi.can_my_phone_be_trusted.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

/** Small user choices and timestamps, kept in SharedPreferences rather than the database. */
public final class Prefs {
    private static volatile Prefs INSTANCE;
    private final SharedPreferences sp;
    private final Context context;

    private Prefs(Context context) {
        this.context = context.getApplicationContext();
        this.sp = this.context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
    }

    public static Prefs get(Context context) {
        if (INSTANCE == null) {
            synchronized (Prefs.class) {
                if (INSTANCE == null) INSTANCE = new Prefs(context);
            }
        }
        return INSTANCE;
    }

    public boolean onboardingDone() { return sp.getBoolean("onboarding_done", false); }
    public void setOnboardingDone(boolean v) { sp.edit().putBoolean("onboarding_done", v).apply(); }

    public String deviceName() {
        String saved = sp.getString("device_name", null);
        if (saved != null && !saved.trim().isEmpty()) return saved;
        String name = Settings.Global.getString(context.getContentResolver(), Settings.Global.DEVICE_NAME);
        return name != null ? name : Build.MANUFACTURER + " " + Build.MODEL;
    }
    public void setDeviceName(String v) { sp.edit().putString("device_name", v).apply(); }

    public String ownerName() { return sp.getString("owner_name", ""); }
    public void setOwnerName(String v) { sp.edit().putString("owner_name", v).apply(); }

    public boolean autoCheck() { return sp.getBoolean("auto_check", true); }
    public void setAutoCheck(boolean v) { sp.edit().putBoolean("auto_check", v).apply(); }

    public boolean chargingOnly() { return sp.getBoolean("charging_only", false); }
    public void setChargingOnly(boolean v) { sp.edit().putBoolean("charging_only", v).apply(); }

    public boolean alertsOn() { return sp.getBoolean("alerts_on", true); }
    public void setAlertsOn(boolean v) { sp.edit().putBoolean("alerts_on", v).apply(); }

    public boolean weeklySummary() { return sp.getBoolean("weekly_summary", true); }
    public void setWeeklySummary(boolean v) { sp.edit().putBoolean("weekly_summary", v).apply(); }

    public boolean lockOn() { return sp.getBoolean("lock_on", false); }
    public void setLockOn(boolean v) { sp.edit().putBoolean("lock_on", v).apply(); }

    public long lockTimeoutMs() { return sp.getLong("lock_timeout", 60_000L); }
    public void setLockTimeoutMs(long v) { sp.edit().putLong("lock_timeout", v).apply(); }

    public boolean secureScreen() { return sp.getBoolean("secure_screen", false); }
    public void setSecureScreen(boolean v) { sp.edit().putBoolean("secure_screen", v).apply(); }

    public long baselineAt() { return sp.getLong("baseline_at", 0); }
    public void setBaselineAt(long v) { sp.edit().putLong("baseline_at", v).apply(); }

    public long lastCheckAt() { return sp.getLong("last_check_at", 0); }
    public void setLastCheckAt(long v) { sp.edit().putLong("last_check_at", v).apply(); }

    public long lastSummaryAt() { return sp.getLong("last_summary_at", 0); }
    public void setLastSummaryAt(long v) { sp.edit().putLong("last_summary_at", v).apply(); }

    public void clear() { sp.edit().clear().apply(); }
}
