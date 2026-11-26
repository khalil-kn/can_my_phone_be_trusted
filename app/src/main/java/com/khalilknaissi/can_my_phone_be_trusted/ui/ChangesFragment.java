package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.chip.Chip;
import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.ChangeEntity;
import com.khalilknaissi.can_my_phone_be_trusted.engine.RiskRules;

import java.util.List;

/** The Changes tab: everything different from the baseline, newest check first. */
public class ChangesFragment extends ListFragment {
    private boolean unreviewedOnly = true;
    private Chip notReviewed, all;

    @Override
    protected String title() {
        return "Changes";
    }

    @Override
    protected void setUp() {
        chips.setSingleSelection(true);
        chips.setSelectionRequired(true);
        notReviewed = chip("Not reviewed", true);
        all = chip("All since baseline", false);
        notReviewed.setOnClickListener(v -> pick(true));
        all.setOnClickListener(v -> pick(false));
        vm.changes().observe(getViewLifecycleOwner(), this::render);
        vm.changeCounts().observe(getViewLifecycleOwner(), c -> {
            notReviewed.setText("Not reviewed  " + c[0]);
            all.setText("All since baseline  " + c[1]);
        });
    }

    private void pick(boolean unreviewed) {
        unreviewedOnly = unreviewed;
        load();
    }

    @Override
    protected void load() {
        vm.loadChanges(unreviewedOnly);
    }

    private void render(List<ChangeEntity> changes) {
        list.removeAllViews();
        if (changes.isEmpty()) {
            empty(unreviewedOnly ? "Nothing to review" : "No changes since your baseline",
                    "Run a check from Status, or wait for the next automatic one.");
            return;
        }
        long group = -1;
        LinearLayout card = null;
        for (ChangeEntity c : changes) {
            if (c.detectedAt != group) {
                group = c.detectedAt;
                ui.heading(list, Ui.dayHeader(c.detectedAt) + " · " + Ui.time(c.detectedAt) + " check");
                card = ui.card(list);
            }
            View row = ui.row(card, title(c), c.source() + " · " + (c.isReverted() ? "undone" : Ui.window(c)),
                    c.severity(), null);
            row.setAlpha(c.isReverted() ? 0.6f : 1f);
            final long id = c.id;
            row.setOnClickListener(v -> startActivity(ChangeActivity.intent(requireContext(), id)));
        }
    }

    /** For settings, "USB debugging Off On" with the old value struck through and the new one in maroon. */
    private CharSequence title(ChangeEntity c) {
        if (!c.isSetting()) return c.title();
        SpannableStringBuilder b = new SpannableStringBuilder(RiskRules.settingLabel(c.subject) + " ");
        int start = b.length();
        b.append(c.oldValue);
        b.setSpan(new StrikethroughSpan(), start, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        b.setSpan(new ForegroundColorSpan(Ui.color(requireContext(), R.color.muted)), start, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        b.append(" ");
        start = b.length();
        b.append(c.newValue);
        b.setSpan(new ForegroundColorSpan(Ui.color(requireContext(), R.color.maroon)), start, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        b.append(" ");
        return b;
    }
}
