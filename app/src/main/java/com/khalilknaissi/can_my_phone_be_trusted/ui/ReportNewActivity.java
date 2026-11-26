package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.IncidentEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.ReportEntity;

import java.util.List;

/** Stage 5, "Report": choose what the report covers and its format, then share or open it. */
public class ReportNewActivity extends SimpleActivity {
    private static final String EXTRA_INCIDENT = "incident";

    public static Intent intent(Context c, long incidentId) {
        return new Intent(c, ReportNewActivity.class).putExtra(EXTRA_INCIDENT, incidentId);
    }

    private long incidentId;
    private ChipGroup scope, format;
    private Button create;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        incidentId = getIntent().getLongExtra(EXTRA_INCIDENT, 0);
        title.setText("New report");
        vm.incidents().observe(this, this::form);
        vm.createdReport().observe(this, this::done);
        vm.busy().observe(this, b -> {
            if (create != null) {
                create.setEnabled(!b);
                create.setText(b ? "Creating…" : "Create report");
            }
        });
        vm.loadIncidents();
    }

    private void form(List<IncidentEntity> incidents) {
        if (vm.createdReport().getValue() != null) return;
        clearBody();
        bottom.removeAllViews();
        ui.heading(body, "What it covers");
        scope = group();
        // Offer the incident we came from, or else the newest one.
        IncidentEntity pick = null;
        for (IncidentEntity i : incidents) if (i.id == incidentId) pick = i;
        if (pick == null && !incidents.isEmpty()) pick = incidents.get(0);
        Chip baseline = chip(scope, "Everything since baseline", pick == null);
        baseline.setTag(0L);
        if (pick != null) chip(scope, pick.code() + " · " + pick.title, true).setTag(pick.id);

        ui.heading(body, "Format");
        format = group();
        chip(format, "PDF · to read", true).setTag(true);
        chip(format, "JSON · for tools", false).setTag(false);

        ui.text(body, R.style.Small_Muted, "Includes the changes with their scores, the timeline, your investigation steps and notes. Never includes messages or files.")
                .setPadding(ui.dp(4), ui.dp(16), ui.dp(4), 0);
        create = ui.primary(bottom, "Create report", v -> {
            Chip s = findViewById(scope.getCheckedChipId());
            Chip f = findViewById(format.getCheckedChipId());
            vm.createReport((Long) s.getTag(), (Boolean) f.getTag());
        });
    }

    private ChipGroup group() {
        ChipGroup g = new ChipGroup(this);
        g.setSingleSelection(true);
        g.setSelectionRequired(true);
        body.addView(g);
        return g;
    }

    private Chip chip(ChipGroup g, String text, boolean checked) {
        Chip c = (Chip) getLayoutInflater().inflate(R.layout.chip_filter, g, false);
        c.setId(android.view.View.generateViewId());
        c.setText(text);
        c.setCheckable(true);
        g.addView(c);
        c.setChecked(checked);
        return c;
    }

    private void done(ReportEntity r) {
        if (r == null) return;
        clearBody();
        bottom.removeAllViews();
        title.setText("Saved");
        LinearLayout card = ui.card(body);
        Views.margins(card, 0, ui.dp(16), 0, 0);
        ui.row(card, r.title, r.format + " · " + (r.isPdf() ? Ui.plural(r.pages, "page", "pages") + " · " : "") + Ui.size(r.sizeBytes), null, null);
        ui.text(body, R.style.Small, "Kept on this phone in Reports. Nothing was sent anywhere.").setPadding(ui.dp(4), ui.dp(14), ui.dp(4), 0);
        ui.secondary(bottom, "Open", v -> ReportsFragment.open(this, r));
        ui.primary(bottom, "Share " + r.format, v -> ReportsFragment.share(this, r));
    }
}
