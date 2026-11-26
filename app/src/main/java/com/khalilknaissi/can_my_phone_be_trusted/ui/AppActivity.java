package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.engine.ChangeDetector;
import com.khalilknaissi.can_my_phone_be_trusted.engine.RiskRules;
import com.khalilknaissi.can_my_phone_be_trusted.model.AppState;
import com.khalilknaissi.can_my_phone_be_trusted.repository.SecurityRepository;

/** One app: its access, where it came from, when it was installed and opened, its signing key. */
public class AppActivity extends SimpleActivity {
    private static final String EXTRA_PKG = "pkg";

    public static Intent intent(Context c, String pkg) {
        return new Intent(c, AppActivity.class).putExtra(EXTRA_PKG, pkg);
    }

    private String pkg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pkg = getIntent().getStringExtra(EXTRA_PKG);
        vm.app().observe(this, this::render);
    }

    @Override
    protected void onResume() {
        super.onResume();
        vm.loadApp(pkg); // reload after the user comes back from Android settings
    }

    private void render(SecurityRepository.AppDetail d) {
        clearBody();
        bottom.removeAllViews();
        headerExtra.removeAllViews();
        AppState a = d.now;
        if (a == null) {
            title.setText("App not found");
            return;
        }
        TextView tag = ui.text(headerExtra, R.style.Small_Bold, d.removed ? "Removed since baseline" : d.inBaseline ? "In your baseline" : "New since baseline");
        tag.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ui.dp(28)));
        tag.setGravity(android.view.Gravity.CENTER);
        tag.setPadding(ui.dp(12), 0, ui.dp(12), 0);
        tag.setTextSize(12);
        tag.setBackground(Ui.rounded(this, d.inBaseline && !d.removed ? R.color.slate : R.color.seal_red, 14));
        tag.setTextColor(Ui.color(this, R.color.white));

        title.setText(a.label);
        ui.text(body, R.style.Mono, a.packageName).setPadding(ui.dp(4), ui.dp(4), ui.dp(4), 0);

        ui.heading(body, "Access");
        LinearLayout access = ui.card(body);
        for (String s : a.specialAccess) ui.row(access, s, RiskRules.why(com.khalilknaissi.can_my_phone_be_trusted.model.ChangeKind.SPECIAL_ACCESS_ON, s), null, "On");
        for (String p : a.permissions) ui.row(access, RiskRules.permissionLabel(p), null, null, "Allowed");
        if (access.getChildCount() == 0) ui.row(access, "No sensitive permissions", "It holds no dangerous permission or special access.", null, null);

        ui.heading(body, "Origin");
        LinearLayout card = ui.card(body);
        ui.field(card, "Installed", Ui.when(a.firstInstallTime));
        ui.field(card, "From", RiskRules.installerLabel(a.installer, a.system) + (a.installer != null && !a.system ? " (" + a.installer + ")" : ""));
        ui.field(card, "Version", a.versionName + " (" + a.versionCode + ")");
        if (d.hasUsageAccess) {
            ui.field(card, "Last opened", d.lastOpened > 0 ? Ui.when(d.lastOpened) : "Not in the last 30 days");
        } else {
            ui.field(card, "Last opened", "Allow usage access to see this")
                    .setOnClickListener(v -> Ui.open(this, new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        }
        ui.field(card, "Signing key", a.signer.isEmpty() ? "Unknown" : ChangeDetector.shortKey(a.signer));
        if (d.baselineSigner != null && !d.baselineSigner.isEmpty() && !a.signer.isEmpty() && !d.baselineSigner.equals(a.signer)) {
            ui.field(card, "Signing key in baseline", ChangeDetector.shortKey(d.baselineSigner) + " (different)");
        }

        if (!d.removed) {
            ui.secondary(bottom, "Settings", v -> Ui.open(this, Ui.appSettings(pkg)));
            if (!a.system) ui.primary(bottom, "Uninstall…", v -> Ui.open(this, Ui.uninstall(pkg)));
        }
    }
}
