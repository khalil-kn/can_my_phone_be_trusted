package com.khalilknaissi.can_my_phone_be_trusted.engine;

import static com.khalilknaissi.can_my_phone_be_trusted.engine.Fixtures.app;
import static com.khalilknaissi.can_my_phone_be_trusted.engine.Fixtures.device;
import static com.khalilknaissi.can_my_phone_be_trusted.engine.Fixtures.settings;
import static com.khalilknaissi.can_my_phone_be_trusted.engine.Fixtures.withSpecial;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.khalilknaissi.can_my_phone_be_trusted.model.DeviceState;
import com.khalilknaissi.can_my_phone_be_trusted.model.Finding;
import com.khalilknaissi.can_my_phone_be_trusted.model.Severity;

import org.junit.Test;

import java.util.List;

public class CorrelatorTest {

    @Test
    public void appChangesAndSameWindowSettingsBecomeOneCriticalIncident() {
        DeviceState base = device(settings());
        DeviceState now = device(settings(RiskRules.ADB, RiskRules.ON),
                withSpecial(app("com.flash.torch", 1, null, "BB", "READ_SMS"), RiskRules.ACCESSIBILITY));

        List<Correlator.Group> groups = Correlator.group(ChangeDetector.diff(base, now));

        assertEquals(1, groups.size());
        Correlator.Group g = groups.get(0);
        assertEquals(8 + 20 + 25 + 30 + 8, g.score());
        assertEquals(Severity.CRITICAL, Severity.ofScore(g.score()));
        assertEquals("Sideloaded app Torch got accessibility and SMS access", g.title());
    }

    @Test
    public void scoreIsCappedAt100() {
        DeviceState base = device(settings());
        DeviceState now = device(settings(RiskRules.SCREEN_LOCK, RiskRules.OFF, RiskRules.ADB, RiskRules.ON),
                withSpecial(app("com.spy", 1, null, "BB", "READ_SMS", "RECORD_AUDIO"),
                        RiskRules.ACCESSIBILITY, RiskRules.DEVICE_ADMIN));
        assertEquals(100, Correlator.group(ChangeDetector.diff(base, now)).get(0).score());
    }

    @Test
    public void settingsAloneFormTheirOwnGroup() {
        DeviceState base = device(settings());
        DeviceState now = device(settings(RiskRules.SCREEN_LOCK, RiskRules.OFF, RiskRules.ADB, RiskRules.ON));
        Correlator.Group g = Correlator.group(ChangeDetector.diff(base, now)).get(0);
        assertNull(g.packageName);
        assertEquals("Screen lock was removed and 1 more setting", g.title());
    }

    @Test
    public void harmlessChangesCreateNoGroups() {
        DeviceState base = device(settings(), app("com.a", 1, "com.android.vending", "AA"));
        DeviceState now = device(settings(), app("com.a", 2, "com.android.vending", "AA"));
        assertTrue(Correlator.group(ChangeDetector.diff(base, now)).isEmpty());
    }

    @Test
    public void twoAppsMakeTwoGroupsRiskiestFirst() {
        DeviceState base = device(settings());
        DeviceState now = device(settings(),
                app("com.glovo", 1, "com.android.vending", "CC", "ACCESS_FINE_LOCATION"),
                app("com.flash.torch", 1, null, "BB", "READ_SMS"));
        List<Correlator.Group> groups = Correlator.group(ChangeDetector.diff(base, now));
        assertEquals(2, groups.size());
        assertEquals("com.flash.torch", groups.get(0).packageName);
        assertTrue(groups.get(0).score() >= Correlator.MIN_SCORE);
        assertTrue(groups.get(1).score() < Correlator.MIN_SCORE);
    }

    @Test
    public void playbookSuggestsUninstallAndSmsCheck() {
        DeviceState base = device(settings());
        DeviceState now = device(settings(RiskRules.ADB, RiskRules.ON),
                withSpecial(app("com.flash.torch", 1, null, "BB", "READ_SMS"), RiskRules.ACCESSIBILITY));
        List<Finding> f = ChangeDetector.diff(base, now);
        List<Playbook.Step> steps = Playbook.steps(f);
        assertEquals(Playbook.Action.UNINSTALL, steps.get(0).action);
        boolean sms = false, dev = false;
        for (Playbook.Step s : steps) {
            if (s.key.equals("sms")) sms = true;
            if (s.key.equals("dev")) dev = true;
        }
        assertTrue(sms);
        assertTrue(dev);
    }

    @Test
    public void severityBandsMatchTheDesign() {
        assertEquals(Severity.CRITICAL, Severity.ofScore(80));
        assertEquals(Severity.HIGH, Severity.ofScore(60));
        assertEquals(Severity.MODERATE, Severity.ofScore(40));
        assertEquals(Severity.LOW, Severity.ofScore(20));
        assertEquals(Severity.INFO, Severity.ofScore(19));
    }
}
