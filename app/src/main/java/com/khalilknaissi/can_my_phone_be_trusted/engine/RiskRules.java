package com.khalilknaissi.can_my_phone_be_trusted.engine;

import com.khalilknaissi.can_my_phone_be_trusted.model.ChangeKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All risk knowledge in one place: how many points each change is worth,
 * and the plain words used to describe it.
 */
public final class RiskRules {
    private RiskRules() {}

    // Special access an app can hold, beyond normal permissions.
    public static final String ACCESSIBILITY = "Accessibility service";
    public static final String DEVICE_ADMIN = "Device admin";
    public static final String NOTIFICATIONS = "Notification access";

    // The 8 watched security settings.
    public static final String SCREEN_LOCK = "screen_lock";
    public static final String ADB = "adb";
    public static final String ADB_WIFI = "adb_wifi";
    public static final String DEV_OPTIONS = "dev_options";
    public static final String ENCRYPTION = "encryption";
    public static final String PATCH = "patch";
    public static final String ANDROID = "android";
    public static final String ROOT = "root";

    public static final String ON = "On";
    public static final String OFF = "Off";
    public static final String FOUND = "Found";
    public static final String NOT_FOUND = "Not found";
    public static final String UNKNOWN = "Unknown";

    // Points for whole-app changes.
    public static final int SIDELOADED = 20;
    public static final int DOWNGRADED = 25;
    public static final int SIGNATURE = 40;

    private static final Map<String, String> SETTINGS = new LinkedHashMap<>();

    static {
        SETTINGS.put(SCREEN_LOCK, "Screen lock");
        SETTINGS.put(ADB, "USB debugging");
        SETTINGS.put(ADB_WIFI, "Wireless debugging");
        SETTINGS.put(DEV_OPTIONS, "Developer options");
        SETTINGS.put(ENCRYPTION, "Storage encryption");
        SETTINGS.put(PATCH, "Security patch");
        SETTINGS.put(ANDROID, "Android version");
        SETTINGS.put(ROOT, "Root tools");
    }

    public static List<String> settingKeys() {
        return new ArrayList<>(SETTINGS.keySet());
    }

    public static String settingLabel(String key) {
        String label = SETTINGS.get(key);
        return label == null ? key : label;
    }

    public static int installPoints(boolean system) {
        return system ? 3 : 8;
    }

    public static int permissionPoints(String perm) {
        if (isSms(perm)) return 25;
        if (perm.contains("CALL_LOG")) return 20;
        if (perm.equals("RECORD_AUDIO") || perm.equals("ACCESS_BACKGROUND_LOCATION")) return 15;
        if (perm.equals("CAMERA") || perm.contains("LOCATION") || perm.contains("CONTACTS")) return 10;
        if (perm.equals("POST_NOTIFICATIONS")) return 0;
        if (perm.contains("PHONE")) return 5;
        return 3;
    }

    public static int specialPoints(String access) {
        if (ACCESSIBILITY.equals(access)) return 30;
        if (DEVICE_ADMIN.equals(access)) return 25;
        return 20;
    }

    /** Points for a setting going from oldValue to newValue. Only the risky direction scores. */
    public static int settingPoints(String key, String oldValue, String newValue) {
        switch (key) {
            case SCREEN_LOCK: return OFF.equals(newValue) ? 30 : 0;
            case ADB: return ON.equals(newValue) ? 8 : 0;
            case ADB_WIFI: return ON.equals(newValue) ? 10 : 0;
            case DEV_OPTIONS: return ON.equals(newValue) ? 3 : 0;
            case ENCRYPTION: return OFF.equals(newValue) ? 15 : 0;
            // Patch dates are yyyy-mm-dd, so text order is date order.
            case PATCH: return newValue.compareTo(oldValue) < 0 ? 30 : 0;
            case ROOT: return FOUND.equals(newValue) ? 40 : 0;
            default: return 0;
        }
    }

    public static boolean isSms(String perm) {
        return perm.endsWith("_SMS") || perm.endsWith("_MMS") || perm.equals("RECEIVE_WAP_PUSH");
    }

    public static boolean fromStore(String installer) {
        return "com.android.vending".equals(installer) || "com.google.android.feedback".equals(installer);
    }

    public static String installerLabel(String installer, boolean system) {
        if (system) return "Came with the phone";
        if (fromStore(installer)) return "Play Store";
        if (installer == null) return "Unknown source (a file or a computer)";
        if (installer.contains("packageinstaller")) return "A file, not the Play Store";
        return "Another store (" + installer + ")";
    }

    private static final Map<String, String> PERMISSION_LABELS = new LinkedHashMap<>();

    static {
        PERMISSION_LABELS.put("READ_SMS", "Read SMS");
        PERMISSION_LABELS.put("RECEIVE_SMS", "Receive SMS");
        PERMISSION_LABELS.put("SEND_SMS", "Send SMS");
        PERMISSION_LABELS.put("RECEIVE_MMS", "Receive MMS");
        PERMISSION_LABELS.put("READ_CALL_LOG", "Read call log");
        PERMISSION_LABELS.put("WRITE_CALL_LOG", "Change call log");
        PERMISSION_LABELS.put("RECORD_AUDIO", "Microphone");
        PERMISSION_LABELS.put("CAMERA", "Camera");
        PERMISSION_LABELS.put("ACCESS_FINE_LOCATION", "Precise location");
        PERMISSION_LABELS.put("ACCESS_COARSE_LOCATION", "Approximate location");
        PERMISSION_LABELS.put("ACCESS_BACKGROUND_LOCATION", "Location in the background");
        PERMISSION_LABELS.put("READ_CONTACTS", "Read contacts");
        PERMISSION_LABELS.put("WRITE_CONTACTS", "Change contacts");
        PERMISSION_LABELS.put("READ_PHONE_STATE", "Phone status");
        PERMISSION_LABELS.put("CALL_PHONE", "Make phone calls");
        PERMISSION_LABELS.put("READ_CALENDAR", "Read calendar");
        PERMISSION_LABELS.put("BODY_SENSORS", "Body sensors");
        PERMISSION_LABELS.put("READ_EXTERNAL_STORAGE", "Files");
        PERMISSION_LABELS.put("READ_MEDIA_IMAGES", "Photos");
        PERMISSION_LABELS.put("READ_MEDIA_VIDEO", "Videos");
        PERMISSION_LABELS.put("POST_NOTIFICATIONS", "Send notifications");
    }

    public static String permissionLabel(String perm) {
        String label = PERMISSION_LABELS.get(perm);
        if (label != null) return label;
        String s = perm.replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Short word for incident titles: "SMS", "accessibility", "microphone". */
    public static String accessWord(ChangeKind kind, String subject) {
        if (kind == ChangeKind.SPECIAL_ACCESS_ON) {
            if (ACCESSIBILITY.equals(subject)) return "accessibility";
            if (DEVICE_ADMIN.equals(subject)) return "device admin";
            return "notification";
        }
        if (isSms(subject)) return "SMS";
        if (subject.contains("CALL_LOG")) return "call log";
        if (subject.equals("RECORD_AUDIO")) return "microphone";
        if (subject.contains("LOCATION")) return "location";
        if (subject.contains("CONTACTS")) return "contacts";
        return permissionLabel(subject).toLowerCase(java.util.Locale.ROOT);
    }

    /** Short title for one change, as shown in lists. */
    public static String title(ChangeKind kind, String appLabel, String subject) {
        switch (kind) {
            case APP_INSTALLED: return appLabel + " installed";
            case APP_REMOVED: return appLabel + " removed";
            case APP_UPDATED: return appLabel + " updated";
            case APP_DOWNGRADED: return appLabel + " went back to an older version";
            case SIGNATURE_CHANGED: return appLabel + "'s signing key changed";
            case SIDELOADED: return "Installed outside the Play Store";
            case PERMISSION_GRANTED: return permissionLabel(subject) + " allowed";
            case PERMISSION_REVOKED: return permissionLabel(subject) + " taken away";
            case SPECIAL_ACCESS_ON: return subject + " on";
            case SPECIAL_ACCESS_OFF: return subject + " off";
            default: return settingLabel(subject);
        }
    }

    /** One or two sentences explaining why a change matters, for a non-expert. */
    public static String why(ChangeKind kind, String subject) {
        switch (kind) {
            case APP_INSTALLED: return "A new app is the most common way new risks arrive. Check that you installed it yourself.";
            case APP_REMOVED: return "Usually you did this. It only matters if you didn't.";
            case APP_UPDATED: return "Normal. Apps update often.";
            case APP_DOWNGRADED: return "Going back to an older version can bring back security holes that were already fixed.";
            case SIGNATURE_CHANGED: return "The same app with a different signing key means it was replaced by someone else's build.";
            case SIDELOADED: return "Apps from outside the Play Store skip Google's checks. Most Android malware arrives this way.";
            case PERMISSION_REVOKED:
            case SPECIAL_ACCESS_OFF: return "Less access than before. Usually a good thing.";
            case SPECIAL_ACCESS_ON:
                if (ACCESSIBILITY.equals(subject)) return "An accessibility service can read the screen and tap buttons for you. Spyware uses it to read messages.";
                if (DEVICE_ADMIN.equals(subject)) return "A device admin app can lock or wipe the phone and is hard to uninstall.";
                return "It can read every notification, including messages and login codes.";
            case PERMISSION_GRANTED:
                if (isSms(subject)) return "An app that reads SMS can read the login codes your bank texts you.";
                if (subject.contains("CALL_LOG")) return "It can see who you call and when.";
                if (subject.equals("RECORD_AUDIO")) return "It can record sound through the microphone.";
                if (subject.contains("LOCATION")) return "It can track where you are.";
                if (subject.equals("CAMERA")) return "It can take photos and video.";
                if (subject.contains("CONTACTS")) return "It can copy everyone in your contacts.";
                return "It gives the app access to personal data.";
            default:
                switch (subject) {
                    case SCREEN_LOCK: return "Without a screen lock, anyone holding the phone can open it.";
                    case ADB: return "USB debugging lets a computer install apps and read data over a cable.";
                    case ADB_WIFI: return "Wireless debugging lets a computer on the same network control the phone.";
                    case DEV_OPTIONS: return "Developer options unlock settings that weaken security.";
                    case ENCRYPTION: return "Without encryption, data can be read if the phone is taken apart.";
                    case PATCH: return "Security patches fix known holes. A patch going backwards is a red flag.";
                    case ANDROID: return "The Android version changed, usually after a system update.";
                    case ROOT: return "Root tools let apps bypass Android's protections.";
                    default: return "A security setting changed.";
                }
        }
    }
}
