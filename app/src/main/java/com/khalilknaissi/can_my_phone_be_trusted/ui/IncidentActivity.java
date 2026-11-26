package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.ChangeEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.IncidentEntity;
import com.khalilknaissi.can_my_phone_be_trusted.engine.Playbook;
import com.khalilknaissi.can_my_phone_be_trusted.model.IncidentState;
import com.khalilknaissi.can_my_phone_be_trusted.repository.SecurityRepository;
import com.khalilknaissi.can_my_phone_be_trusted.viewmodel.SecurityViewModel;

/**
 * Stage 4, "Investigate": the score and what makes it up, steps to contain it,
 * notes, "Check again" to see what was undone, and closing as Resolved or False positive.
 */
public class IncidentActivity extends BaseActivity {
    private static final String EXTRA_ID = "id";

    public static Intent intent(Context c, long id) {
        return new Intent(c, IncidentActivity.class).putExtra(EXTRA_ID, id);
    }

    private SecurityViewModel vm;
    private Views ui;
    private long id;
    private LinearLayout body, bottom;
    private EditText notes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident);
        Ui.fitTop(findViewById(R.id.header));
        Ui.fitBottom(findViewById(R.id.bottom));
        Ui.statusIcons(this, false);
        ui = new Views(this);
        id = getIntent().getLongExtra(EXTRA_ID, 0);
        body = findViewById(R.id.body);
        bottom = findViewById(R.id.bottom);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.btnReport).setOnClickListener(v -> startActivity(ReportNewActivity.intent(this, id)));

        vm = new ViewModelProvider(this).get(SecurityViewModel.class);
        vm.incident().observe(this, this::render);
        vm.busy().observe(this, busy -> {
            View b = bottom.findViewWithTag("recheck");
            if (b instanceof Button) {
                b.setEnabled(!busy);
                ((Button) b).setText(busy ? "Checking…" : "Check again");
            }
        });
        vm.message().observe(this, e -> {
            String t = e.take();
            if (t != null) Ui.toast(this, t);
        });
        vm.loadIncident(id);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (notes != null) vm.saveNotes(id, notes.getText().toString());
    }

    private void render(SecurityRepository.IncidentDetail d) {
        IncidentEntity inc = d.incident;
        if (inc == null) {
            finish();
            return;
        }
        IncidentState state = inc.state();
        boolean active = state.isActive();
        findViewById(R.id.field).setBackgroundColor(Ui.color(this,
                !active ? R.color.slate : inc.severity().isHot() ? R.color.seal_red : R.color.ink));
        ((TextView) findViewById(R.id.code)).setText(inc.code());
        Ui.pill(findViewById(R.id.statePill), state);
        ((TextView) findViewById(R.id.score)).setText(String.valueOf(inc.score));
        findViewById(R.id.score).setContentDescription("Score " + inc.score + " of 100");
        ((TextView) findViewById(R.id.severity)).setText(inc.severity().label);
        ((TextView) findViewById(R.id.incTitle)).setText(inc.title);

        body.removeAllViews();
        bottom.removeAllViews();

        // What broke the seal, with the points each change adds.
        ui.heading(body, "What broke the seal · " + d.changes.size());
        LinearLayout card = ui.card(body);
        int apps = 0;
        for (ChangeEntity c : d.changes) {
            if (!c.isSetting() && c.packageName.equals(inc.primaryPackage)) apps++;
            View row = ui.row(card, c.title(), c.source() + (c.isReverted() ? " · undone" : ""), null, null);
            ui.points(row, c.points);
            row.setAlpha(c.isReverted() ? 0.6f : 1f);
            final long changeId = c.id;
            row.setOnClickListener(v -> startActivity(ChangeActivity.intent(this, changeId)));
        }
        String why = "Linked because all " + d.changes.size() + " appeared between the "
                + Ui.when(d.windowStart) + " and " + Ui.when(d.windowEnd) + " checks"
                + (apps > 1 ? ", and " + apps + " involve " + inc.appLabel + "." : ".");
        ui.text(body, R.style.Small, why).setPadding(ui.dp(4), ui.dp(12), ui.dp(4), 0);
        if (d.undone > 0) {
            TextView undone = ui.text(body, R.style.Small_Bold, d.undone == d.changes.size()
                    ? "All " + d.changes.size() + " changes are undone. The phone matches its baseline again."
                    : d.undone + " of " + d.changes.size() + " changes are undone.");
            undone.setPadding(ui.dp(4), ui.dp(8), ui.dp(4), 0);
        }

        if (state == IncidentState.OPEN) {
            ui.primary(bottom, "Start investigating", v -> vm.setIncidentState(id, IncidentState.INVESTIGATING));
            return;
        }

        // Steps to contain it.
        int done = 0;
        for (Playbook.Step s : d.steps) if (d.done.contains(s.key)) done++;
        ui.heading(body, "Contain it · " + done + "/" + d.steps.size() + " steps");
        LinearLayout steps = ui.card(body);
        for (Playbook.Step s : d.steps) step(steps, s, d.done.contains(s.key), active);

        ui.heading(body, "Notes");
        notes = new EditText(this);
        notes.setBackground(getDrawable(R.drawable.bg_field));
        notes.setPadding(ui.dp(20), ui.dp(16), ui.dp(20), ui.dp(16));
        notes.setMinLines(3);
        notes.setHint("What do you know? For example: the phone was out of my hands on Monday night.");
        notes.setContentDescription("Notes");
        notes.setText(inc.notes == null ? "" : inc.notes);
        notes.setEnabled(active);
        body.addView(notes);

        if (active) {
            Button again = ui.secondary(bottom, "Check again", v -> vm.recheck(id));
            again.setTag("recheck");
            ui.primary(bottom, "Close…", v -> close(inc));
        } else {
            ui.text(body, R.style.Small_Muted, "Closed " + Ui.when(inc.closedAt) + " as " + state.label + ".")
                    .setPadding(ui.dp(4), ui.dp(16), ui.dp(4), 0);
            ui.secondary(bottom, "Reopen", v -> vm.setIncidentState(id, IncidentState.INVESTIGATING));
            ui.primary(bottom, "Create report", v -> startActivity(ReportNewActivity.intent(this, id)));
        }
    }

    private void step(LinearLayout parent, Playbook.Step s, boolean done, boolean active) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_step, parent, false);
        TextView t = v.findViewById(R.id.stepTitle);
        t.setText(s.title);
        if (done) t.setPaintFlags(t.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        ((TextView) v.findViewById(R.id.stepWhy)).setText(s.why);
        MaterialCheckBox box = v.findViewById(R.id.stepDone);
        box.setChecked(done);
        box.setEnabled(active);
        box.setContentDescription(s.title);
        box.setOnClickListener(x -> vm.toggleStep(id, s.key));
        Button action = v.findViewById(R.id.stepAction);
        Intent intent = Ui.forStep(s);
        if (intent == null || !active) {
            action.setVisibility(View.GONE);
        } else {
            action.setText(Ui.actionLabel(s.action));
            action.setOnClickListener(x -> Ui.open(this, intent));
        }
        parent.addView(v);
    }

    /** The close sheet: Resolved or False positive, with Undo afterwards. */
    private void close(IncidentEntity inc) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this).inflate(R.layout.sheet_close, null);
        ((TextView) v.findViewById(R.id.sheetTitle)).setText("Close " + inc.code());
        RadioGroup outcome = v.findViewById(R.id.outcome);
        Button confirm = v.findViewById(R.id.sheetConfirm);
        outcome.setOnCheckedChangeListener((g, checked) ->
                confirm.setText(checked == R.id.outFalse ? "Mark as false positive" : "Mark resolved"));
        confirm.setOnClickListener(x -> {
            IncidentState s = outcome.getCheckedRadioButtonId() == R.id.outFalse ? IncidentState.FALSE_POSITIVE : IncidentState.RESOLVED;
            if (notes != null) vm.saveNotes(id, notes.getText().toString());
            vm.setIncidentState(id, s);
            sheet.dismiss();
            Snackbar.make(findViewById(R.id.main), inc.code() + " marked " + s.label.toLowerCase(java.util.Locale.ROOT), Snackbar.LENGTH_LONG)
                    .setAction("Undo", u -> vm.setIncidentState(id, IncidentState.INVESTIGATING))
                    .show();
        });
        v.findViewById(R.id.sheetCancel).setOnClickListener(x -> sheet.dismiss());
        sheet.setContentView(v);
        sheet.show();
    }
}
