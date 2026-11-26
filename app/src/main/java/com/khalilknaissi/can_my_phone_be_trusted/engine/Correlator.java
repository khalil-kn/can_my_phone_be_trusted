package com.khalilknaissi.can_my_phone_be_trusted.engine;

import com.khalilknaissi.can_my_phone_be_trusted.model.ChangeKind;
import com.khalilknaissi.can_my_phone_be_trusted.model.Finding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage 3, "Correlate & Score": groups the new changes of one check into
 * candidate incidents and gives each a 0-100 score.
 *
 * Rules:
 * 1. Changes about the same app belong together.
 * 2. Setting changes found in the same check join the riskiest app group,
 *    because they happened in the same time window. With no app group, they form their own.
 * 3. A group becomes an incident only if its score reaches MIN_SCORE (Low or above).
 */
public final class Correlator {
    private Correlator() {}

    public static final int MIN_SCORE = 20;

    public static final class Group {
        /** Null for a settings-only group. */
        public final String packageName;
        public final String appLabel;
        public final List<Finding> findings = new ArrayList<>();

        Group(String packageName, String appLabel) {
            this.packageName = packageName;
            this.appLabel = appLabel;
        }

        public int score() {
            int sum = 0;
            for (Finding f : findings) sum += f.points;
            return Math.min(100, sum);
        }

        public String title() {
            return Correlator.title(this);
        }
    }

    public static List<Group> group(List<Finding> fresh) {
        Map<String, Group> byApp = new LinkedHashMap<>();
        Group settings = new Group(null, null);
        for (Finding f : fresh) {
            if (f.points <= 0) continue;
            if (f.packageName == null) {
                settings.findings.add(f);
                continue;
            }
            Group g = byApp.get(f.packageName);
            if (g == null) {
                g = new Group(f.packageName, f.appLabel);
                byApp.put(f.packageName, g);
            }
            g.findings.add(f);
        }
        List<Group> groups = new ArrayList<>(byApp.values());
        Collections.sort(groups, (a, b) -> b.score() - a.score());
        if (!settings.findings.isEmpty()) {
            if (groups.isEmpty()) groups.add(settings);
            else groups.get(0).findings.addAll(settings.findings);
        }
        for (Group g : groups) Collections.sort(g.findings, (a, b) -> b.points - a.points);
        return groups;
    }

    static String title(Group g) {
        if (g.packageName == null) {
            Finding top = g.findings.get(0);
            String t = settingHeadline(top);
            int more = g.findings.size() - 1;
            return more == 0 ? t : t + " and " + more + " more setting" + (more == 1 ? "" : "s");
        }
        List<String> access = new ArrayList<>();
        boolean sideloaded = false, signature = false, downgraded = false;
        for (Finding f : g.findings) {
            if (f.kind == ChangeKind.PERMISSION_GRANTED || f.kind == ChangeKind.SPECIAL_ACCESS_ON) {
                String word = RiskRules.accessWord(f.kind, f.subject);
                if (!access.contains(word) && access.size() < 2) access.add(word);
            }
            if (f.kind == ChangeKind.SIDELOADED) sideloaded = true;
            if (f.kind == ChangeKind.SIGNATURE_CHANGED) signature = true;
            if (f.kind == ChangeKind.APP_DOWNGRADED) downgraded = true;
        }
        if (signature) return g.appLabel + "'s signing key changed";
        String who = sideloaded ? "Sideloaded app " + g.appLabel : g.appLabel;
        if (!access.isEmpty()) {
            return who + " got " + (access.size() == 1 ? access.get(0) : access.get(0) + " and " + access.get(1)) + " access";
        }
        if (downgraded) return g.appLabel + " went back to an older version";
        if (sideloaded) return g.appLabel + " was installed from outside the Play Store";
        return "New app: " + g.appLabel;
    }

    static String settingHeadline(Finding f) {
        String label = RiskRules.settingLabel(f.subject);
        if (RiskRules.SCREEN_LOCK.equals(f.subject)) return "Screen lock was removed";
        if (RiskRules.PATCH.equals(f.subject)) return "Security patch went backwards";
        if (RiskRules.ROOT.equals(f.subject)) return "Root tools appeared";
        if (RiskRules.ENCRYPTION.equals(f.subject)) return "Storage encryption turned off";
        return label + " turned " + f.newValue.toLowerCase(java.util.Locale.ROOT);
    }
}
