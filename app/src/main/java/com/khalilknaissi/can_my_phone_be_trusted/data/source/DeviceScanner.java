package com.khalilknaissi.can_my_phone_be_trusted.data.source;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.pm.PackageInfoCompat;

import com.khalilknaissi.can_my_phone_be_trusted.engine.RiskRules;
import com.khalilknaissi.can_my_phone_be_trusted.model.AppState;
import com.khalilknaissi.can_my_phone_be_trusted.model.DeviceState;

import java.io.File;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Stage 1, "Baseline": reads everything the app can see through public Android APIs.
 * No root and no internet. What it can't see is listed in the Help screen.
 */
public class DeviceScanner {

    /** Called after each app is read, so the Recording screen can count up. */
    public interface Progress {
        void onApp(int done, int total, String packageName);
    }

    private static final String[] SU_PATHS = {
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
            "/data/local/su", "/data/local/bin/su", "/data/local/xbin/su", "/system/app/Superuser.apk"
    };
    private static final String[] ROOT_APPS = {
            "com.topjohnwu.magisk", "eu.chainfire.supersu", "com.koushikdutta.superuser", "me.weishu.kernelsu"
    };

    private final Context context;
    private final PackageManager pm;
    private final Map<String, Boolean> dangerous = new HashMap<>();

    public DeviceScanner(Context context) {
        this.context = context.getApplicationContext();
        this.pm = this.context.getPackageManager();
    }

    public DeviceState scan(Progress progress) {
        long now = System.currentTimeMillis();
        Map<String, Set<String>> special = specialAccess();
        List<PackageInfo> installed = pm.getInstalledPackages(0);
        Map<String, AppState> apps = new TreeMap<>();
        int done = 0;
        for (PackageInfo basic : installed) {
            done++;
            AppState app = readApp(basic.packageName, special);
            if (app != null) apps.put(app.packageName, app);
            if (progress != null) progress.onApp(done, installed.size(), basic.packageName);
        }
        return new DeviceState(apps, readSettings(apps), now);
    }

    /** Reads one app, or returns null if it isn't installed. */
    public AppState readApp(String packageName) {
        return readApp(packageName, specialAccess());
    }

    @SuppressWarnings("deprecation")
    private AppState readApp(String packageName, Map<String, Set<String>> special) {
        try {
            int flags = PackageManager.GET_PERMISSIONS
                    | (Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES);
            PackageInfo pi = pm.getPackageInfo(packageName, flags);
            ApplicationInfo ai = pi.applicationInfo;
            if (ai == null) return null;

            Set<String> perms = new TreeSet<>();
            if (pi.requestedPermissions != null && pi.requestedPermissionsFlags != null) {
                for (int i = 0; i < pi.requestedPermissions.length; i++) {
                    boolean granted = (pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;
                    String name = pi.requestedPermissions[i];
                    if (granted && isDangerous(name)) perms.add(name.substring(name.lastIndexOf('.') + 1));
                }
            }
            Set<String> sp = special.get(packageName);
            return new AppState(
                    packageName,
                    String.valueOf(pm.getApplicationLabel(ai)),
                    pi.versionName == null ? "" : pi.versionName,
                    PackageInfoCompat.getLongVersionCode(pi),
                    (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0,
                    installer(packageName),
                    pi.firstInstallTime,
                    pi.lastUpdateTime,
                    signer(pi),
                    perms,
                    sp == null ? new TreeSet<String>() : sp);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isDangerous(String permission) {
        Boolean d = dangerous.get(permission);
        if (d == null) {
            try {
                PermissionInfo info = pm.getPermissionInfo(permission, 0);
                d = (info.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS;
            } catch (PackageManager.NameNotFoundException e) {
                d = false;
            }
            dangerous.put(permission, d);
        }
        return d;
    }

    @SuppressWarnings("deprecation")
    private String installer(String packageName) {
        try {
            if (Build.VERSION.SDK_INT >= 30) return pm.getInstallSourceInfo(packageName).getInstallingPackageName();
            return pm.getInstallerPackageName(packageName);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static String signer(PackageInfo pi) {
        Signature[] sigs;
        if (Build.VERSION.SDK_INT >= 28) {
            sigs = pi.signingInfo == null ? null : pi.signingInfo.getApkContentsSigners();
        } else {
            sigs = pi.signatures;
        }
        if (sigs == null || sigs.length == 0) return "";
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(sigs[0].toByteArray());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02X", b));
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Which apps hold accessibility, device admin or notification access. */
    private Map<String, Set<String>> specialAccess() {
        Map<String, Set<String>> map = new HashMap<>();
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null) {
            for (AccessibilityServiceInfo s : am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
                if (s.getResolveInfo() != null && s.getResolveInfo().serviceInfo != null) {
                    add(map, s.getResolveInfo().serviceInfo.packageName, RiskRules.ACCESSIBILITY);
                }
            }
        }
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        List<ComponentName> admins = dpm == null ? null : dpm.getActiveAdmins();
        if (admins != null) {
            for (ComponentName c : admins) add(map, c.getPackageName(), RiskRules.DEVICE_ADMIN);
        }
        for (String pkg : NotificationManagerCompat.getEnabledListenerPackages(context)) {
            add(map, pkg, RiskRules.NOTIFICATIONS);
        }
        return map;
    }

    private static void add(Map<String, Set<String>> map, String pkg, String access) {
        Set<String> set = map.get(pkg);
        if (set == null) {
            set = new TreeSet<>();
            map.put(pkg, set);
        }
        set.add(access);
    }

    private Map<String, String> readSettings(Map<String, AppState> apps) {
        ContentResolver cr = context.getContentResolver();
        Map<String, String> s = new LinkedHashMap<>();
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        s.put(RiskRules.SCREEN_LOCK, km != null && km.isDeviceSecure() ? RiskRules.ON : RiskRules.OFF);
        s.put(RiskRules.ADB, global(cr, Settings.Global.ADB_ENABLED));
        s.put(RiskRules.ADB_WIFI, global(cr, "adb_wifi_enabled"));
        s.put(RiskRules.DEV_OPTIONS, global(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED));
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        int enc = dpm == null ? -1 : dpm.getStorageEncryptionStatus();
        boolean encrypted = enc == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE
                || enc == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY
                || enc == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER;
        s.put(RiskRules.ENCRYPTION, encrypted ? RiskRules.ON : RiskRules.OFF);
        s.put(RiskRules.PATCH, Build.VERSION.SECURITY_PATCH);
        s.put(RiskRules.ANDROID, Build.VERSION.RELEASE);
        s.put(RiskRules.ROOT, rootFound(apps) ? RiskRules.FOUND : RiskRules.NOT_FOUND);
        return s;
    }

    /** Some settings can't be read on newer Android versions. Those become Unknown and are never compared. */
    private static String global(ContentResolver cr, String key) {
        try {
            return Settings.Global.getInt(cr, key, 0) == 1 ? RiskRules.ON : RiskRules.OFF;
        } catch (Exception e) {
            return RiskRules.UNKNOWN;
        }
    }

    private static boolean rootFound(Map<String, AppState> apps) {
        for (String path : SU_PATHS) {
            if (new File(path).exists()) return true;
        }
        for (String pkg : ROOT_APPS) {
            if (apps.containsKey(pkg)) return true;
        }
        return false;
    }

    // ---- optional usage access: "was the new app ever opened?"

    @SuppressWarnings("deprecation")
    public boolean hasUsageAccess() {
        AppOpsManager ops = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (ops == null) return false;
        int mode = Build.VERSION.SDK_INT >= 29
                ? ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName())
                : ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    /** Last time the app was in the foreground in the past 30 days, or 0 if unknown. */
    public long lastOpened(String packageName) {
        if (!hasUsageAccess()) return 0;
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return 0;
        long now = System.currentTimeMillis();
        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 30L * 24 * 3600 * 1000, now);
        long last = 0;
        if (stats != null) {
            for (UsageStats u : stats) {
                if (packageName.equals(u.getPackageName())) last = Math.max(last, u.getLastTimeUsed());
            }
        }
        return last;
    }
}
