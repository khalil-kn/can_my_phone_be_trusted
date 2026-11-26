package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.model.ChangeKind;
import com.khalilknaissi.can_my_phone_be_trusted.repository.SecurityRepository;

import java.util.List;

/**
 * The forensic timeline. Three marks keep the honest difference visible:
 * a red dot when Android gave an exact time, a mustard bar when we only know the window
 * between two checks, a dashed tick when a check ran.
 */
public class TimelineFragment extends ListFragment {
    private Chip apps, perms, settings, checks, inIncident;
    private List<SecurityRepository.TimelineItem> items;

    @Override
    protected String title() {
        return "Timeline";
    }

    @Override
    protected void setUp() {
        apps = chip("Apps", true);
        perms = chip("Permissions", true);
        settings = chip("Settings", true);
        checks = chip("Checks", true);
        inIncident = chip("Only in incidents", false);
        for (Chip c : new Chip[]{apps, perms, settings, checks, inIncident}) {
            c.setOnCheckedChangeListener((b, on) -> render());
        }
        vm.timeline().observe(getViewLifecycleOwner(), t -> {
            items = t;
            render();
        });
        legend();
    }

    @Override
    protected void load() {
        vm.loadTimeline();
    }

    private boolean shown(SecurityRepository.TimelineItem i) {
        if (i.check) return checks.isChecked() && !inIncident.isChecked();
        if (inIncident.isChecked() && i.incidentId == 0) return false;
        if (i.category == ChangeKind.Category.APPS) return apps.isChecked();
        if (i.category == ChangeKind.Category.PERMISSIONS) return perms.isChecked();
        return settings.isChecked();
    }

    private void render() {
        if (items == null) return;
        list.removeAllViews();
        String day = null;
        LinearLayout card = null;
        int count = 0;
        for (SecurityRepository.TimelineItem i : items) {
            if (!shown(i)) continue;
            count++;
            String d = Ui.dayHeader(i.time);
            if (!d.equals(day)) {
                day = d;
                ui.heading(list, d);
                card = ui.card(list);
            }
            View v = LayoutInflater.from(requireContext()).inflate(R.layout.item_timeline, card, false);
            ((ImageView) v.findViewById(R.id.mark)).setImageResource(
                    i.check ? R.drawable.mark_check : i.exact ? R.drawable.mark_exact : R.drawable.mark_window);
            ((TextView) v.findViewById(R.id.tlTitle)).setText(i.title);
            ((TextView) v.findViewById(R.id.tlSub)).setText(i.sub);
            TextView time = v.findViewById(R.id.tlTime);
            time.setText(i.check || i.exact ? Ui.time(i.time) : Ui.time(i.windowStart) + "–\n" + Ui.time(i.time));
            v.setContentDescription(i.title + ", " + (i.check ? "check at " : i.exact ? "at " : "somewhere between "
                    + Ui.time(i.windowStart) + " and ") + Ui.time(i.time));
            if (i.changeId > 0) {
                final long id = i.changeId;
                v.setOnClickListener(x -> startActivity(ChangeActivity.intent(requireContext(), id)));
            }
            card.addView(v);
        }
        if (count == 0) empty("Nothing to show", "No events match these filters.");
    }

    private void legend() {
        LinearLayout card = ui.card(bottom);
        Views.margins(card, 0, ui.dp(8), 0, 0);
        TextView t = ui.text(card, R.style.Tiny, "●  exact time      ▬  somewhere in the window between two checks      ┆  a check ran");
        t.setPadding(ui.dp(16), ui.dp(10), ui.dp(16), ui.dp(10));
        t.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
}
