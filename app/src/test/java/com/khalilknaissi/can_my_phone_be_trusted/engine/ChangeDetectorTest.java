package com.khalilknaissi.can_my_phone_be_trusted.engine;

import static com.khalilknaissi.can_my_phone_be_trusted.engine.Fixtures.app;
import static com.khalilknaissi.can_my_phone_be_trusted.engine.Fixtures.device;
import static com.khalilknaissi.can_my_phone_be_trusted.engine.Fixtures.settings;
import static com.khalilknaissi.can_my_phone_be_trusted.engine.Fixtures.withSpecial;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.khalilknaissi.can_my_phone_be_trusted.model.ChangeKind;
import com.khalilknaissi.can_my_phone_be_trusted.model.DeviceState;
import com.khalilknaissi.can_my_phone_be_trusted.model.Finding;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ChangeDetectorTest {

    private static final String STORE = "com.android.vending";

    private static List<ChangeKind> kinds(List<Finding> findings) {
        List<ChangeKind> k = new ArrayList<>();
        for (Finding f : findings) k.add(f.kind);
        return k;
    }

    @Test
    public void identicalPhoneHasNoChanges() {
        DeviceState s = device(settings(), app("com.whatsapp", 10, STORE, "AA"));
        assertTrue(ChangeDetector.diff(s, s).isEmpty());
    }

    @Test
    public void sideloadedAppWithSmsAndAccessibilityIsFullyDescribed() {
        DeviceState base = device(settings(), app("com.whatsapp", 10, STORE, "AA"));
        DeviceState now = device(settings(),
                app("com.whatsapp", 10, STORE, "AA"),
                withSpecial(app("com.flash.torch", 1, null, "BB", "READ_SMS"), RiskRules.ACCESSIBILITY));

        List<Finding> f = ChangeDetector.diff(base, now);
        List<ChangeKind> k = kinds(f);

        assertTrue(k.contains(ChangeKind.APP_INSTALLED));
        assertTrue(k.contains(ChangeKind.SIDELOADED));
        assertTrue(k.contains(ChangeKind.PERMISSION_GRANTED));
        assertTrue(k.contains(ChangeKind.SPECIAL_ACCESS_ON));
        int points = 0;
        for (Finding x : f) points += x.points;
        assertEquals(8 + 20 + 25 + 30, points);
    }

    @Test
    public void playStoreInstallIsNotFlaggedAsSideloaded() {
        DeviceState base = device(settings());
        DeviceState now = device(settings(), app("com.glovo", 1, STORE, "CC"));
        assertEquals(1, ChangeDetector.diff(base, now).size());
    }

    @Test
    public void updateIsHarmlessButDowngradeAndNewSigningKeyAreNot() {
        DeviceState base = device(settings(), app("com.a", 10, STORE, "AA"), app("com.b", 10, STORE, "AA"));
        DeviceState now = device(settings(), app("com.a", 11, STORE, "AA"), app("com.b", 9, STORE, "FF"));

        for (Finding f : ChangeDetector.diff(base, now)) {
            if (f.kind == ChangeKind.APP_UPDATED) assertEquals(0, f.points);
            if (f.kind == ChangeKind.APP_DOWNGRADED) assertEquals(RiskRules.DOWNGRADED, f.points);
            if (f.kind == ChangeKind.SIGNATURE_CHANGED) assertEquals(RiskRules.SIGNATURE, f.points);
        }
        assertEquals(3, ChangeDetector.diff(base, now).size());
    }

    @Test
    public void removedAppsAndRevokedPermissionsScoreZero() {
        DeviceState base = device(settings(), app("com.a", 1, STORE, "AA", "CAMERA"), app("com.b", 1, STORE, "AA"));
        DeviceState now = device(settings(), app("com.a", 1, STORE, "AA"));
        List<Finding> f = ChangeDetector.diff(base, now);
        assertEquals(2, f.size());
        for (Finding x : f) assertEquals(0, x.points);
    }

    @Test
    public void onlyTheRiskyDirectionOfASettingScores() {
        DeviceState base = device(settings());
        DeviceState risky = device(settings(RiskRules.ADB, RiskRules.ON, RiskRules.SCREEN_LOCK, RiskRules.OFF));
        int points = 0;
        for (Finding f : ChangeDetector.diff(base, risky)) points += f.points;
        assertEquals(8 + 30, points);

        for (Finding f : ChangeDetector.diff(risky, base)) assertEquals(0, f.points);
    }

    @Test
    public void unreadableSettingsAreIgnored() {
        DeviceState base = device(settings(RiskRules.ADB_WIFI, RiskRules.UNKNOWN));
        DeviceState now = device(settings(RiskRules.ADB_WIFI, RiskRules.ON));
        assertTrue(ChangeDetector.diff(base, now).isEmpty());
    }

    @Test
    public void olderSecurityPatchIsFlagged() {
        DeviceState base = device(settings());
        DeviceState now = device(settings(RiskRules.PATCH, "2025-01-05"));
        assertEquals(30, ChangeDetector.diff(base, now).get(0).points);
    }
}
