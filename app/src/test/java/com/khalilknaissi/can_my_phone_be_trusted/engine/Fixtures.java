package com.khalilknaissi.can_my_phone_be_trusted.engine;

import com.khalilknaissi.can_my_phone_be_trusted.model.AppState;
import com.khalilknaissi.can_my_phone_be_trusted.model.DeviceState;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

/** Small builders so each test reads like a story. */
final class Fixtures {
    private Fixtures() {}

    static AppState app(String pkg, long version, String installer, String signer, String... perms) {
        return new AppState(pkg, label(pkg), "v" + version, version, false, installer, 1000, 2000,
                signer, new TreeSet<>(Arrays.asList(perms)), new TreeSet<String>());
    }

    static AppState withSpecial(AppState a, String... special) {
        return new AppState(a.packageName, a.label, a.versionName, a.versionCode, a.system, a.installer,
                a.firstInstallTime, a.lastUpdateTime, a.signer, a.permissions, new TreeSet<>(Arrays.asList(special)));
    }

    static DeviceState device(Map<String, String> settings, AppState... apps) {
        Map<String, AppState> map = new LinkedHashMap<>();
        for (AppState a : apps) map.put(a.packageName, a);
        return new DeviceState(map, settings, 5000);
    }

    static Map<String, String> settings(String... pairs) {
        Map<String, String> s = new HashMap<>();
        s.put(RiskRules.SCREEN_LOCK, RiskRules.ON);
        s.put(RiskRules.ADB, RiskRules.OFF);
        s.put(RiskRules.DEV_OPTIONS, RiskRules.OFF);
        s.put(RiskRules.PATCH, "2026-10-05");
        for (int i = 0; i < pairs.length; i += 2) s.put(pairs[i], pairs[i + 1]);
        return s;
    }

    private static String label(String pkg) {
        String last = pkg.substring(pkg.lastIndexOf('.') + 1);
        return Character.toUpperCase(last.charAt(0)) + last.substring(1);
    }
}
