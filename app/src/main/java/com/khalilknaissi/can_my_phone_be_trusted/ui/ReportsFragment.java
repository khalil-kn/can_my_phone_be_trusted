package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.ReportEntity;

import java.io.File;
import java.util.List;

/** The Reports tab: saved PDF and JSON files. They stay on the phone until the user shares one. */
public class ReportsFragment extends ListFragment {

    @Override
    protected String title() {
        return "Reports";
    }

    @Override
    protected void setUp() {
        chips.setVisibility(View.GONE);
        ui.text(bottom, R.style.Small_Muted, "PDF is for reading or sending to someone. JSON keeps every field, for a technician or a tool.")
                .setPadding(ui.dp(4), ui.dp(8), ui.dp(4), ui.dp(10));
        LinearLayout row = new LinearLayout(requireContext());
        bottom.addView(row);
        ui.primary(row, "New report", v -> startActivity(new Intent(requireContext(), ReportNewActivity.class)));
        vm.reports().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    protected void load() {
        vm.loadReports();
    }

    private void render(List<ReportEntity> reports) {
        list.removeAllViews();
        ui.text(list, R.style.Small_Muted, "Kept on this phone. Sharing sends a copy to the app you choose.")
                .setPadding(ui.dp(4), ui.dp(8), ui.dp(4), ui.dp(4));
        if (reports.isEmpty()) {
            empty("No reports yet", "Create one for an incident, or for everything since your baseline.");
            return;
        }
        LinearLayout card = ui.card(list);
        Views.margins(card, 0, ui.dp(12), 0, 0);
        for (ReportEntity r : reports) {
            String sub = Ui.dayTime(r.createdAt) + " · " + (r.isPdf() ? Ui.plural(r.pages, "page", "pages") + " · " : "") + Ui.size(r.sizeBytes);
            View row = ui.row(card, r.title, sub, null, r.format);
            row.setOnClickListener(v -> choose(r));
        }
    }

    private void choose(ReportEntity r) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(r.title)
                .setItems(new String[]{"Open", "Share"}, (d, which) -> {
                    if (which == 0) open(requireContext(), r);
                    else share(requireContext(), r);
                })
                .show();
    }

    static Intent viewIntent(android.content.Context c, ReportEntity r) {
        File f = new File(r.path);
        return new Intent(Intent.ACTION_VIEW)
                .setDataAndType(FileProvider.getUriForFile(c, c.getPackageName() + ".files", f), r.isPdf() ? "application/pdf" : "application/json")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    static void open(android.content.Context c, ReportEntity r) {
        Ui.open(c, Intent.createChooser(viewIntent(c, r), "Open report"));
    }

    static void share(android.content.Context c, ReportEntity r) {
        shareFile(c, new File(r.path), r.isPdf() ? "application/pdf" : "application/json", r.title);
    }

    static void shareFile(android.content.Context c, File f, String type, String title) {
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType(type)
                .putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(c, c.getPackageName() + ".files", f))
                .putExtra(Intent.EXTRA_SUBJECT, title)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Ui.open(c, Intent.createChooser(send, "Share report"));
    }
}
