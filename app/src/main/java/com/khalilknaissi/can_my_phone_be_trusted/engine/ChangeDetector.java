package com.khalilknaissi.can_my_phone_be_trusted.engine;

import com.khalilknaissi.can_my_phone_be_trusted.model.AppState;
import com.khalilknaissi.can_my_phone_be_trusted.model.ChangeKind;
import com.khalilknaissi.can_my_phone_be_trusted.model.DeviceState;
import com.khalilknaissi.can_my_phone_be_trusted.model.Finding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Stage 2 of the workflow, "Snapshot & Compare": lists every difference
 * between the baseline and the phone right now. Pure Java, so it is unit-tested.
 */
public final class ChangeDetector {
    private ChangeDetector() {}

    public static List<Finding> diff(DeviceState base, DeviceState now) {
        List<Finding> out = new ArrayList<>();

        for (AppState n : now.apps.values()) {
            AppState b = base.apps.get(n.packageName);
            if (b == null) {
                newApp(n, out);
                continue;
            }
            if (n.versionCode > b.versionCode) {
                out.add(app(ChangeKind.APP_UPDATED, n, b.versionName, n.versionName, n.lastUpdateTime, 0));
            } else if (n.versionCode < b.versionCode) {
                out.add(app(ChangeKind.APP_DOWNGRADED, n, b.versionName, n.versionName, n.lastUpdateTime, RiskRules.DOWNGRADED));
            }
            if (!b.signer.isEmpty() && !n.signer.isEmpty() && !b.signer.equals(n.signer)) {
                out.add(app(ChangeKind.SIGNATURE_CHANGED, n, shortKey(b.signer), shortKey(n.signer), 0, RiskRules.SIGNATURE));
            }
            accessDiff(b.permissions, n, n.permissions, false, out);
            accessDiff(b.specialAccess, n, n.specialAccess, true, out);
        }

        for (AppState b : base.apps.values()) {
            if (!now.apps.containsKey(b.packageName)) {
                out.add(app(ChangeKind.APP_REMOVED, b, b.versionName, "Removed", 0, 0));
            }
        }

        for (String key : RiskRules.settingKeys()) {
            String was = base.settings.get(key);
            String is = now.settings.get(key);
            if (was == null || is == null || was.equals(is)
                    || RiskRules.UNKNOWN.equals(was) || RiskRules.UNKNOWN.equals(is)) continue;
            out.add(new Finding(ChangeKind.SETTING_CHANGED, null, null, key, was, is, 0,
                    RiskRules.settingPoints(key, was, is)));
        }
        return out;
    }

    private static void newApp(AppState n, List<Finding> out) {
        out.add(app(ChangeKind.APP_INSTALLED, n, "Not installed", n.versionName, n.firstInstallTime,
                RiskRules.installPoints(n.system)));
        if (!n.system && !RiskRules.fromStore(n.installer)) {
            out.add(app(ChangeKind.SIDELOADED, n, "Not installed",
                    RiskRules.installerLabel(n.installer, false), n.firstInstallTime, RiskRules.SIDELOADED));
        }
        for (String p : n.permissions) {
            out.add(new Finding(ChangeKind.PERMISSION_GRANTED, n.packageName, n.label, p,
                    "Not installed", "Allowed", 0, RiskRules.permissionPoints(p)));
        }
        for (String s : n.specialAccess) {
            out.add(new Finding(ChangeKind.SPECIAL_ACCESS_ON, n.packageName, n.label, s,
                    "Not installed", RiskRules.ON, 0, RiskRules.specialPoints(s)));
        }
    }

    private static void accessDiff(Set<String> was, AppState app, Set<String> is, boolean special, List<Finding> out) {
        for (String s : is) {
            if (was.contains(s)) continue;
            out.add(new Finding(special ? ChangeKind.SPECIAL_ACCESS_ON : ChangeKind.PERMISSION_GRANTED,
                    app.packageName, app.label, s,
                    special ? RiskRules.OFF : "Not allowed", special ? RiskRules.ON : "Allowed", 0,
                    special ? RiskRules.specialPoints(s) : RiskRules.permissionPoints(s)));
        }
        for (String s : was) {
            if (is.contains(s)) continue;
            out.add(new Finding(special ? ChangeKind.SPECIAL_ACCESS_OFF : ChangeKind.PERMISSION_REVOKED,
                    app.packageName, app.label, s,
                    special ? RiskRules.ON : "Allowed", special ? RiskRules.OFF : "Not allowed", 0, 0));
        }
    }

    private static Finding app(ChangeKind kind, AppState a, String was, String is, long exactAt, int points) {
        return new Finding(kind, a.packageName, a.label, null, was, is, exactAt, points);
    }

    /** "4F9A31C0...7E2C" becomes "4F9A 31C0 … 7E2C". */
    public static String shortKey(String hex) {
        if (hex.length() < 12) return hex;
        return hex.substring(0, 4) + " " + hex.substring(4, 8) + " … " + hex.substring(hex.length() - 4);
    }
}
