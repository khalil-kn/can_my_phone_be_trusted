package com.khalilknaissi.can_my_phone_be_trusted.model;

import java.util.Map;

/** A full picture of the phone at one moment: its apps and its security settings. */
public final class DeviceState {
    /** Package name to app. */
    public final Map<String, AppState> apps;
    /** Setting key (see RiskRules) to a readable value like "On". */
    public final Map<String, String> settings;
    public final long takenAt;

    public DeviceState(Map<String, AppState> apps, Map<String, String> settings, long takenAt) {
        this.apps = apps;
        this.settings = settings;
        this.takenAt = takenAt;
    }

    public int permissionCount() {
        int n = 0;
        for (AppState a : apps.values()) n += a.permissions.size() + a.specialAccess.size();
        return n;
    }
}
