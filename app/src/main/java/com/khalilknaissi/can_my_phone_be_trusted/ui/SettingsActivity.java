package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.Prefs;
import com.khalilknaissi.can_my_phone_be_trusted.data.source.DeviceScanner;
import com.khalilknaissi.can_my_phone_be_trusted.repository.SecurityRepository;
import com.khalilknaissi.can_my_phone_be_trusted.work.Notifier;
import com.khalilknaissi.can_my_phone_be_trusted.work.Scheduler;

/** Settings: checks, alerts, privacy, and the phone's data. */
public class SettingsActivity extends SimpleActivity {
    private Prefs prefs;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> build());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = Prefs.get(this);
        title.setText("Settings");
        vm.status().observe(this, s -> build());
    }

    @Override
    protected void onResume() {
        super.onResume();
        vm.loadStatus(); // also redraws after returning from Android settings
    }

    private void build() {
        SecurityRepository.Status s = vm.status().getValue();
        clearBody();

        ui.heading(body, "Checks");
        LinearLayout checks = ui.card(body);
        ui.toggle(checks, "Check automatically", "About every 12 hours, when Android allows", prefs.autoCheck(), on -> {
            prefs.setAutoCheck(on);
            Scheduler.apply(this);
        });
        ui.toggle(checks, "Only while charging", "Saves battery", prefs.chargingOnly(), on -> {
            prefs.setChargingOnly(on);
            Scheduler.apply(this);
        });

        ui.heading(body, "Alerts");
        LinearLayout alerts = ui.card(body);
        ui.toggle(alerts, "Alert me about High and Critical", null, prefs.alertsOn(), on -> {
            prefs.setAlertsOn(on);
            if (on && !Notifier.allowed(this) && Build.VERSION.SDK_INT >= 33) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });
        ui.toggle(alerts, "Weekly summary", "On Sundays", prefs.weeklySummary(), prefs::setWeeklySummary);
        if (!Notifier.allowed(this)) {
            ui.row(alerts, "Notifications are blocked", "Tap to allow them in Android settings", null, null)
                    .setOnClickListener(v -> Ui.open(this, new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
        }

        ui.heading(body, "Privacy and security");
        LinearLayout privacy = ui.card(body);
        MaterialSwitch[] lock = new MaterialSwitch[1];
        lock[0] = ui.toggle(privacy, "App lock", lockLabel(), prefs.lockOn(), on -> {
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (on && (km == null || !km.isDeviceSecure())) {
                Ui.toast(this, "Set a screen lock on the phone first.");
                lock[0].setChecked(false);
                return;
            }
            prefs.setLockOn(on);
            if (on) BaseActivity.markUnlocked();
        });
        ui.row(privacy, "Lock again after", lockLabel(), null, null).setOnClickListener(v -> pickTimeout());
        ui.toggle(privacy, "Hide in recent apps", "Also blocks screenshots. Applies to screens opened next.", prefs.secureScreen(), prefs::setSecureScreen);
        boolean usage = new DeviceScanner(this).hasUsageAccess();
        ui.row(privacy, "Usage access", usage ? "Allowed: incidents can show when an app was opened" : "Not allowed. Optional.", null, null)
                .setOnClickListener(v -> Ui.open(this, new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));

        ui.heading(body, "Your data");
        LinearLayout data = ui.card(body);
        ui.row(data, "This phone", prefs.deviceName(), null, null)
                .setOnClickListener(v -> startActivity(new Intent(this, DeviceActivity.class)));
        ui.row(data, "Baseline", prefs.baselineAt() > 0 ? "Sealed " + Ui.dayTime(prefs.baselineAt()) + ". Tap to seal a new one." : "Not sealed yet", null, null)
                .setOnClickListener(v -> confirmReseal(s == null ? 0 : s.unreviewed));
        ui.row(data, "Help and about", null, null, null)
                .setOnClickListener(v -> startActivity(new Intent(this, HelpActivity.class)));
    }

    private String lockLabel() {
        long t = prefs.lockTimeoutMs();
        return t == 0 ? "Immediately" : "After " + (t / 60_000) + (t == 60_000 ? " minute" : " minutes");
    }

    private void pickTimeout() {
        long[] values = {0, 60_000, 300_000};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Lock again after I leave the app")
                .setItems(new String[]{"Immediately", "After 1 minute", "After 5 minutes"}, (d, which) -> {
                    prefs.setLockTimeoutMs(values[which]);
                    build();
                })
                .show();
    }

    /** From the design: a new baseline accepts every current change as normal. */
    private void confirmReseal(int unreviewed) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Seal a new baseline?")
                .setMessage("The phone as it is right now becomes the new normal. Changes are measured from now on."
                        + (unreviewed > 0 ? "\n\n" + Ui.plural(unreviewed, "unreviewed change", "unreviewed changes") + " will be accepted as normal." : "")
                        + "\n\nIncidents and reports stay in your history.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Seal new baseline", (d, w) -> startActivity(new Intent(this, BaselineActivity.class)))
                .show();
    }
}
