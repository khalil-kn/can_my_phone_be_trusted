package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.ChangeEntity;
import com.khalilknaissi.can_my_phone_be_trusted.engine.RiskRules;
import com.khalilknaissi.can_my_phone_be_trusted.model.ChangeKind;
import com.khalilknaissi.can_my_phone_be_trusted.repository.SecurityRepository;

/** One change: what it was in the baseline, what it is now, when, and why it matters. */
public class ChangeActivity extends SimpleActivity {
    private static final String EXTRA_ID = "id";

    public static Intent intent(Context c, long id) {
        return new Intent(c, ChangeActivity.class).putExtra(EXTRA_ID, id);
    }

    private long id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        id = getIntent().getLongExtra(EXTRA_ID, 0);
        vm.change().observe(this, this::render);
        vm.loadChange(id);
    }

    private void render(SecurityRepository.ChangeDetail d) {
        ChangeEntity c = d.change;
        if (c == null) {
            finish();
            return;
        }
        clearBody();
        bottom.removeAllViews();
        headerExtra.removeAllViews();
        SeverityBars bars = new SeverityBars(this);
        bars.set(c.severity());
        headerExtra.addView(bars);
        TextView sev = ui.text(headerExtra, R.style.Small_Bold, c.severity().label);
        sev.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        sev.setPadding(ui.dp(8), 0, ui.dp(8), 0);
        sev.setTextColor(Ui.color(this, c.severity().isHot() ? R.color.maroon : R.color.ink));

        title.setText(c.title());

        // The diff pair: baseline in slate, now in red and slightly tilted, as in the design.
        diff(R.color.slate, "Baseline · " + Ui.day(d.baselineAt), c.oldValue, 0, 40);
        diff(c.isReverted() ? R.color.slate : R.color.seal_red,
                (c.isReverted() ? "Back to normal · " + Ui.day(c.revertedAt) : "Now · " + Ui.day(c.windowEnd)),
                c.isReverted() ? c.oldValue : c.newValue, c.isReverted() ? 0 : -2, 52);

        ui.heading(body, "When");
        ui.text(body, R.style.Body, c.exactAt > 0 ? Ui.when(c.exactAt) : "Between " + Ui.window(c)).setPadding(ui.dp(4), 0, ui.dp(4), 0);
        if (c.exactAt == 0) {
            ui.text(body, R.style.Small_Muted, "Android doesn't log the exact time of this kind of change, so this is the window between two checks.")
                    .setPadding(ui.dp(4), ui.dp(4), ui.dp(4), 0);
        }
        ui.heading(body, "Why it matters");
        ui.text(body, R.style.Body, d.why).setPadding(ui.dp(4), 0, ui.dp(4), 0);

        LinearLayout card = ui.card(body);
        Views.margins(card, 0, ui.dp(18), 0, 0);
        if (!c.isSetting()) {
            ui.row(card, c.appLabel, c.packageName, null, null)
                    .setOnClickListener(v -> startActivity(AppActivity.intent(this, c.packageName)));
        }
        if (d.incident != null) {
            View row = ui.row(card, d.incident.title, d.incident.code(), d.incident.severity(), "");
            Ui.pill(ui.pillOf(row), d.incident.state());
            row.setOnClickListener(v -> startActivity(IncidentActivity.intent(this, d.incident.id)));
        }
        if (card.getChildCount() == 0) body.removeView(card);

        if (!c.reviewed) ui.secondary(bottom, "Reviewed", v -> vm.markReviewed(id));
        if (c.isSetting()) {
            ui.primary(bottom, "Open settings", v -> Ui.open(this, Ui.settingScreen(c.subject)));
        } else if (c.kind() == ChangeKind.SPECIAL_ACCESS_ON) {
            Intent screen = new Intent(RiskRules.ACCESSIBILITY.equals(c.subject) ? android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
                    : RiskRules.DEVICE_ADMIN.equals(c.subject) ? android.provider.Settings.ACTION_SECURITY_SETTINGS
                    : android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            ui.primary(bottom, "Open " + c.subject.toLowerCase(java.util.Locale.ROOT) + " settings", v -> Ui.open(this, screen));
        } else if (c.kind() != ChangeKind.APP_REMOVED) {
            ui.primary(bottom, "Open app permissions", v -> Ui.open(this, Ui.appSettings(c.packageName)));
        }
    }

    private void diff(int colorRes, String label, String value, float rotation, int size) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Ui.rounded(this, colorRes, 28));
        box.setPadding(ui.dp(20), ui.dp(14), ui.dp(20), ui.dp(16));
        box.setRotation(rotation);
        body.addView(box);
        Views.margins(box, 0, ui.dp(10), 0, 0);
        TextView l = ui.text(box, R.style.Small_Bold, label);
        l.setTextColor(Ui.color(this, R.color.white));
        l.setTextSize(12);
        TextView v = ui.text(box, R.style.Display, value == null || value.isEmpty() ? "—" : value);
        v.setTextColor(Ui.color(this, R.color.white));
        v.setTextSize(size);
    }
}
