package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.IncidentEntity;
import com.khalilknaissi.can_my_phone_be_trusted.model.IncidentState;

import java.util.ArrayList;
import java.util.List;

/** The Incidents tab: open incidents as red cards on top, closed ones listed below. */
public class IncidentsFragment extends ListFragment {
    private Chip all, open, resolved, falsePositive;
    private List<IncidentEntity> incidents = new ArrayList<>();

    @Override
    protected String title() {
        return "Incidents";
    }

    @Override
    protected void setUp() {
        chips.setSingleSelection(true);
        chips.setSelectionRequired(true);
        all = chip("All", true);
        open = chip("Open", false);
        resolved = chip("Resolved", false);
        falsePositive = chip("False positive", false);
        for (Chip c : new Chip[]{all, open, resolved, falsePositive}) c.setOnClickListener(v -> render());
        vm.incidents().observe(getViewLifecycleOwner(), list -> {
            incidents = list;
            render();
        });
    }

    @Override
    protected void load() {
        vm.loadIncidents();
    }

    private boolean shown(IncidentEntity i) {
        if (open.isChecked()) return i.state().isActive();
        if (resolved.isChecked()) return i.state() == IncidentState.RESOLVED;
        if (falsePositive.isChecked()) return i.state() == IncidentState.FALSE_POSITIVE;
        return true;
    }

    private void render() {
        int nOpen = 0, nRes = 0, nFp = 0;
        for (IncidentEntity i : incidents) {
            if (i.state().isActive()) nOpen++;
            else if (i.state() == IncidentState.RESOLVED) nRes++;
            else nFp++;
        }
        all.setText("All  " + incidents.size());
        open.setText("Open  " + nOpen);
        resolved.setText("Resolved  " + nRes);
        falsePositive.setText("False positive  " + nFp);

        list.removeAllViews();
        LinearLayout closed = null;
        int shown = 0;
        for (IncidentEntity i : incidents) {
            if (!shown(i)) continue;
            shown++;
            final long id = i.id;
            if (i.state().isActive()) {
                openCard(i).setOnClickListener(v -> startActivity(IncidentActivity.intent(requireContext(), id)));
                continue;
            }
            if (closed == null) {
                ui.heading(list, "Closed");
                closed = ui.card(list);
            }
            View row = ui.row(closed, i.title, i.code() + " · " + Ui.day(i.closedAt), i.severity(), "");
            Ui.pill(ui.pillOf(row), i.state());
            row.setOnClickListener(v -> startActivity(IncidentActivity.intent(requireContext(), id)));
        }
        if (shown == 0) empty("No incidents here", "Incidents open when risky changes appear together.");
    }

    /** A red card per open incident, with its score in display type. */
    private View openCard(IncidentEntity i) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackground(Ui.rounded(requireContext(), i.severity().isHot() ? R.color.seal_red : R.color.ink, 28));
        card.setPadding(ui.dp(20), ui.dp(16), ui.dp(20), ui.dp(18));
        card.setClickable(true);
        card.setFocusable(true);
        list.addView(card);
        Views.margins(card, 0, ui.dp(16), 0, 0);

        LinearLayout left = new LinearLayout(requireContext());
        left.setOrientation(LinearLayout.VERTICAL);
        card.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        white(ui.text(left, R.style.Small_Bold, i.state().label + " · " + i.severity().label + " · " + Ui.when(i.createdAt))).setTextSize(12);
        white(ui.text(left, R.style.Body_Bold, i.title)).setTextSize(18);
        white(ui.text(left, R.style.Small, i.code()));

        TextView score = new TextView(requireContext(), null, 0, R.style.Display);
        score.setText(String.valueOf(i.score));
        score.setTextSize(72);
        white(score);
        score.setContentDescription("Score " + i.score);
        card.addView(score);
        return card;
    }

    private TextView white(TextView t) {
        t.setTextColor(Ui.color(requireContext(), R.color.white));
        return t;
    }
}
