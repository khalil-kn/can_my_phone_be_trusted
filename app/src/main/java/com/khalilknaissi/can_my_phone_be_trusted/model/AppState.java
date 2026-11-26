package com.khalilknaissi.can_my_phone_be_trusted.model;

import java.util.Set;

/** Everything the app records about one installed app at one moment. */
public final class AppState {
    public final String packageName;
    public final String label;
    public final String versionName;
    public final long versionCode;
    public final boolean system;
    /** Package that installed it, e.g. com.android.vending for the Play Store. Null when unknown. */
    public final String installer;
    public final long firstInstallTime;
    public final long lastUpdateTime;
    /** SHA-256 of the signing certificate, in hex. Empty when unknown. */
    public final String signer;
    /** Dangerous permissions currently granted, short names like READ_SMS. */
    public final Set<String> permissions;
    /** Special access held, like "Accessibility service". */
    public final Set<String> specialAccess;

    public AppState(String packageName, String label, String versionName, long versionCode,
                    boolean system, String installer, long firstInstallTime, long lastUpdateTime,
                    String signer, Set<String> permissions, Set<String> specialAccess) {
        this.packageName = packageName;
        this.label = label;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.system = system;
        this.installer = installer;
        this.firstInstallTime = firstInstallTime;
        this.lastUpdateTime = lastUpdateTime;
        this.signer = signer;
        this.permissions = permissions;
        this.specialAccess = specialAccess;
    }
}
