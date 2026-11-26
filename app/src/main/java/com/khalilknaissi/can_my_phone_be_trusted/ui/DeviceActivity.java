package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.Prefs;
import com.khalilknaissi.can_my_phone_be_trusted.repository.SecurityRepository;

/** This phone: its name and owner for reports, what is stored, export and delete. */
public class DeviceActivity extends SimpleActivity {
    private EditText name, owner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        title.setText("This phone");
        vm.device().observe(this, this::render);
        vm.exported().observe(this, e -> {
            java.io.File f = e.take();
            if (f != null) ReportsFragment.shareFile(this, f, "application/json", "Can My Phone Be Trusted? export");
        });
        vm.deleted().observe(this, e -> {
            if (e.take() != null) {
                startActivity(new Intent(this, OnboardingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            }
        });
        vm.loadDevice();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Prefs p = Prefs.get(this);
        if (name != null) p.setDeviceName(name.getText().toString().trim());
        if (owner != null) p.setOwnerName(owner.getText().toString().trim());
    }

    private void render(SecurityRepository.DeviceInfo d) {
        clearBody();
        bottom.removeAllViews();
        ui.heading(body, "Name");
        name = field(d.name, InputType.TYPE_TEXT_FLAG_CAP_WORDS, "Name");
        ui.heading(body, "Owner on reports");
        owner = field(d.owner, InputType.TYPE_TEXT_FLAG_CAP_WORDS, "Owner name, optional");

        ui.heading(body, "Phone");
        LinearLayout card = ui.card(body);
        ui.field(card, "Model", d.model);
        ui.field(card, "Android", d.android + " · security patch " + d.patch);

        ui.heading(body, "Stored on this phone");
        LinearLayout stored = ui.card(body);
        ui.field(stored, "Checks · changes", d.checks + " · " + d.changes);
        ui.field(stored, "Incidents · reports", d.incidents + " · " + d.reports);
        ui.field(stored, "Space used", Ui.size(d.bytes));
        ui.text(body, R.style.Small_Muted, "No online account. Uninstalling the app removes all of this, so export first if you want a copy.")
                .setPadding(ui.dp(4), ui.dp(14), ui.dp(4), 0);

        ui.secondary(bottom, "Delete all…", v -> confirmDelete(d));
        ui.primary(bottom, "Export as JSON", v -> vm.exportEverything());
    }

    private EditText field(String value, int caps, String hint) {
        EditText e = new EditText(this);
        e.setBackground(getDrawable(R.drawable.bg_field));
        e.setPadding(ui.dp(20), 0, ui.dp(20), 0);
        e.setMinHeight(ui.dp(56));
        e.setInputType(InputType.TYPE_CLASS_TEXT | caps);
        e.setHint(hint);
        e.setContentDescription(hint);
        e.setText(value);
        body.addView(e);
        return e;
    }

    private void confirmDelete(SecurityRepository.DeviceInfo d) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete everything?")
                .setMessage("This permanently removes the baseline, " + Ui.plural(d.checks, "check", "checks") + ", "
                        + Ui.plural(d.changes, "change", "changes") + ", " + Ui.plural(d.incidents, "incident", "incidents")
                        + " with your notes, and " + Ui.plural(d.reports, "saved report", "saved reports")
                        + ".\n\nThis can't be undone. Files you already shared elsewhere aren't affected.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete everything", (dialog, w) -> vm.deleteEverything())
                .show();
    }
}
