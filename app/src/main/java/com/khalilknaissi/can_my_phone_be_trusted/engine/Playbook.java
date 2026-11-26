package com.khalilknaissi.can_my_phone_be_trusted.engine;

import com.khalilknaissi.can_my_phone_be_trusted.model.ChangeKind;
import com.khalilknaissi.can_my_phone_be_trusted.model.Finding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stage 4, "Investigate": turns an incident's changes into concrete steps to contain it. */
public final class Playbook {
    private Playbook() {}

    /** Which Android screen a step opens. */
    public enum Action { NONE, UNINSTALL, APP_SETTINGS, ACCESSIBILITY, SECURITY, NOTIFICATION_ACCESS, DEVELOPER, DEVICE_INFO }

    public static final class Step {
        public final String key;
        public final String title;
        public final String why;
        public final Action action;
        public final String packageName;

        Step(String key, String title, String why, Action action, String packageName) {
            this.key = key;
            this.title = title;
            this.why = why;
            this.action = action;
            this.packageName = packageName;
        }
    }

    public static List<Step> steps(List<Finding> findings) {
        Map<String, Step> steps = new LinkedHashMap<>();
        boolean sms = false;
        for (Finding f : findings) {
            if (f.kind == ChangeKind.PERMISSION_GRANTED && RiskRules.isSms(f.subject)) sms = true;
        }
        for (Finding f : findings) {
            if (f.points <= 0) continue;
            String pkg = f.packageName;
            switch (f.kind) {
                case SIDELOADED:
                case SIGNATURE_CHANGED:
                case APP_DOWNGRADED:
                case APP_INSTALLED:
                    String reason = f.kind == ChangeKind.SIDELOADED ? "It came from outside the Play Store"
                            : f.kind == ChangeKind.SIGNATURE_CHANGED ? "Someone else's build replaced it"
                            : f.kind == ChangeKind.APP_DOWNGRADED ? "An older, weaker version replaced it"
                            : "Make sure you installed it yourself";
                    put(steps, new Step("uninstall:" + pkg, "Uninstall " + f.appLabel,
                            reason + (sms ? ", and it can read your SMS." : "."), Action.UNINSTALL, pkg));
                    break;
                case PERMISSION_GRANTED:
                    put(steps, new Step("perms:" + pkg, "Review " + f.appLabel + "'s permissions",
                            "Take away any access it doesn't need.", Action.APP_SETTINGS, pkg));
                    break;
                case SPECIAL_ACCESS_ON:
                    if (RiskRules.ACCESSIBILITY.equals(f.subject)) {
                        put(steps, new Step("a11y:" + pkg, "Turn off its accessibility service",
                                "It can read your screen until you do.", Action.ACCESSIBILITY, pkg));
                    } else if (RiskRules.DEVICE_ADMIN.equals(f.subject)) {
                        put(steps, new Step("admin:" + pkg, "Remove it as a device admin",
                                "Device admin apps can't be uninstalled until you do this.", Action.SECURITY, pkg));
                    } else {
                        put(steps, new Step("notif:" + pkg, "Remove its notification access",
                                "It can read your messages as they arrive.", Action.NOTIFICATION_ACCESS, pkg));
                    }
                    break;
                case SETTING_CHANGED:
                    settingStep(steps, f);
                    break;
                default:
                    break;
            }
        }
        if (sms) {
            put(steps, new Step("sms", "Look for SMS codes you didn't ask for",
                    "If you find one, change that account's password.", Action.NONE, null));
        }
        if (steps.isEmpty()) {
            put(steps, new Step("self", "Check that you made this change yourself",
                    "If you did, close the incident as a false positive.", Action.NONE, null));
        }
        return new ArrayList<>(steps.values());
    }

    private static void settingStep(Map<String, Step> steps, Finding f) {
        switch (f.subject) {
            case RiskRules.ADB:
            case RiskRules.ADB_WIFI:
            case RiskRules.DEV_OPTIONS:
                put(steps, new Step("dev", "Turn off developer options",
                        "This also turns off USB and wireless debugging.", Action.DEVELOPER, null));
                break;
            case RiskRules.SCREEN_LOCK:
                put(steps, new Step("lock", "Set a screen lock again",
                        "A PIN, pattern or password stops someone else opening the phone.", Action.SECURITY, null));
                break;
            case RiskRules.ENCRYPTION:
                put(steps, new Step("enc", "Check encryption in security settings",
                        "Phones encrypt by default. Turning it off is very unusual.", Action.SECURITY, null));
                break;
            case RiskRules.PATCH:
                put(steps, new Step("patch", "Check for a system update",
                        "The security patch went backwards, which normal updates never do.", Action.DEVICE_INFO, null));
                break;
            case RiskRules.ROOT:
                put(steps, new Step("root", "Find out where the root tools came from",
                        "If you didn't root the phone yourself, back up your data and reset it.", Action.NONE, null));
                break;
            default:
                break;
        }
    }

    private static void put(Map<String, Step> steps, Step s) {
        if (!steps.containsKey(s.key)) steps.put(s.key, s);
    }
}
