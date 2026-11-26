package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.AlertEntity;

import java.util.List;

/** Alerts from the last 30 days: High and Critical incidents, and Sunday summaries. */
public class NotificationsActivity extends SimpleActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        title.setText("Notifications");
        ui.secondary(headerExtra, "Mark all read", v -> vm.markAlertsRead());
        vm.alerts().observe(this, this::render);
        vm.loadAlerts();
    }

    private void render(List<AlertEntity> alerts) {
        clearBody();
        bottom.removeAllViews();
        if (alerts.isEmpty()) {
            LinearLayout card = ui.card(body);
            Views.margins(card, 0, ui.dp(16), 0, 0);
            ui.row(card, "No alerts", "Nothing High or Critical in the last 30 days.", null, null);
        } else {
            LinearLayout card = ui.card(body);
            Views.margins(card, 0, ui.dp(16), 0, 0);
            for (AlertEntity a : alerts) {
                View row = ui.row(card, a.title, a.body + " " + Ui.when(a.at), null, a.read ? null : "New");
                final long incident = a.incidentId;
                row.setOnClickListener(v -> {
                    if (incident > 0) startActivity(IncidentActivity.intent(this, incident));
                    else startActivity(new Intent(this, MainActivity.class).putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_CHANGES));
                });
            }
        }
        ui.text(body, R.style.Small_Muted, "You're alerted for High and Critical incidents, plus a summary on Sundays.")
                .setPadding(ui.dp(4), ui.dp(14), ui.dp(4), 0);
        ui.primary(bottom, "Change what you're alerted about", v -> startActivity(new Intent(this, SettingsActivity.class)));
    }
}
