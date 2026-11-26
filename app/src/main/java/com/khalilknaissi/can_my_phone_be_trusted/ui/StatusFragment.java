package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.IncidentEntity;
import com.khalilknaissi.can_my_phone_be_trusted.repository.SecurityRepository;
import com.khalilknaissi.can_my_phone_be_trusted.viewmodel.SecurityViewModel;

/**
 * The Status tab answers one question: can I trust my phone right now?
 * Red "Seal broken" with an open incident, slate "All clear" or "Seal intact" otherwise.
 */
public class StatusFragment extends Fragment {
    private SecurityViewModel vm;
    private View root;
    private Views ui;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        root = inflater.inflate(R.layout.fragment_status, container, false);
        ui = new Views(requireContext());
        Ui.fitTop(root.findViewById(R.id.header));
        // Shared with the other tabs: the ViewModel belongs to the activity.
        vm = new ViewModelProvider(requireActivity()).get(SecurityViewModel.class);

        root.findViewById(R.id.btnCheck).setOnClickListener(v -> vm.check());
        root.findViewById(R.id.btnBell).setOnClickListener(v -> startActivity(new Intent(requireContext(), NotificationsActivity.class)));
        root.findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(requireContext(), SettingsActivity.class)));

        vm.status().observe(getViewLifecycleOwner(), this::render);
        vm.busy().observe(getViewLifecycleOwner(), busy -> {
            Button b = root.findViewById(R.id.btnCheck);
            b.setEnabled(!busy);
            b.setText(busy ? "Checking…" : "Check");
        });
        vm.message().observe(getViewLifecycleOwner(), e -> {
            String text = e.take();
            if (text != null) Ui.toast(requireContext(), text);
        });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Ui.statusIcons(requireActivity(), false);
        vm.loadStatus();
    }

    private void render(SecurityRepository.Status s) {
        TextView name = root.findViewById(R.id.deviceName);
        TextView verdict = root.findViewById(R.id.verdict);
        TextView sentence = root.findViewById(R.id.sentence);
        TextView meta = root.findViewById(R.id.meta);
        View field = root.findViewById(R.id.field);
        LinearLayout content = root.findViewById(R.id.content);
        Button primary = root.findViewById(R.id.btnPrimary);
        content.removeAllViews();

        name.setText(s.deviceName);
        TextView badge = root.findViewById(R.id.bellBadge);
        badge.setVisibility(s.unreadAlerts > 0 ? View.VISIBLE : View.GONE);
        badge.setText(String.valueOf(s.unreadAlerts));
        root.findViewById(R.id.btnBell).setContentDescription(s.unreadAlerts > 0
                ? "Notifications, " + s.unreadAlerts + " unread" : "Notifications");

        String tape = Ui.tapeText(s.baselineAt);
        ((TextView) root.findViewById(R.id.tapeWhole)).setText(tape);
        ((TextView) root.findViewById(R.id.tapeLeft)).setText(tape);
        ((TextView) root.findViewById(R.id.tapeRight)).setText(tape.substring(Math.min(tape.length(), 12)));

        if (!s.hasBaseline) {
            field.setBackgroundColor(Ui.color(requireContext(), R.color.ink));
            verdict.setText("No\nbaseline");
            sentence.setText("Record one while you trust this phone.");
            meta.setText("");
            tape(false, false);
            primary.setText("Record baseline");
            primary.setOnClickListener(v -> startActivity(new Intent(requireContext(), BaselineActivity.class)));
            return;
        }

        meta.setText("Baseline " + Ui.dayTime(s.baselineAt) + " · last check " + Ui.when(s.lastCheckAt));

        if (s.topIncident != null) {
            IncidentEntity top = s.topIncident;
            field.setBackgroundColor(Ui.color(requireContext(), R.color.seal_red));
            verdict.setText("Seal\nbroken");
            sentence.setText(Ui.plural(s.activeChanges, "change", "changes") + " since your baseline. "
                    + (s.openIncidents > 1 ? s.openIncidents + " incidents are open." : "One needs a look."));
            tape(true, true);
            incidentPanel(content, top, s);
            primary.setText("Investigate incident");
            primary.setOnClickListener(v -> startActivity(IncidentActivity.intent(requireContext(), top.id)));
        } else {
            field.setBackgroundColor(Ui.color(requireContext(), R.color.slate));
            tape(true, false);
            if (s.unreviewed > 0) {
                verdict.setText("All\nclear");
                sentence.setText("No open incidents. " + Ui.plural(s.unreviewed, "small change is", "small changes are") + " waiting for a look.");
                primary.setText("Review " + Ui.plural(s.unreviewed, "change", "changes"));
                primary.setOnClickListener(v -> ((MainActivity) requireActivity()).selectTab(R.id.nav_changes));
            } else {
                verdict.setText("Seal\nintact");
                sentence.setText(s.lastCheckAt > s.baselineAt
                        ? "The last check found nothing different from your baseline."
                        : "Your baseline is sealed. Checks compare the phone with it.");
                primary.setText("Timeline");
                primary.setOnClickListener(v -> ((MainActivity) requireActivity()).selectTab(R.id.nav_timeline));
            }
            summary(content, s);
            if (s.lastClosed != null) lastIncident(content, s.lastClosed);
        }
        ui.text(content, R.style.Small_Muted, "This covers what the app can see, not everything on the phone.")
                .setPadding(ui.dp(4), ui.dp(14), ui.dp(4), 0);
    }

    private void tape(boolean show, boolean torn) {
        root.findViewById(R.id.tapeWhole).setVisibility(show && !torn ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.tapeLeft).setVisibility(show && torn ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.tapeRight).setVisibility(show && torn ? View.VISIBLE : View.GONE);
    }

    private void incidentPanel(LinearLayout content, IncidentEntity top, SecurityRepository.Status s) {
        LinearLayout card = ui.card(content);
        View row = ui.row(card, top.title, top.code() + " · " + Ui.plural(s.topLinked, "linked change", "linked changes"),
                top.severity(), null);
        ui.points(row, top.score);
        TextView score = ui.pillOf(row);
        score.setText(String.valueOf(top.score));
        score.setTextSize(56);
        score.setTextColor(Ui.color(requireContext(), R.color.ink));
        score.setContentDescription("Score " + top.score + " of 100");
        row.setOnClickListener(v -> startActivity(IncidentActivity.intent(requireContext(), top.id)));
    }

    private void summary(LinearLayout content, SecurityRepository.Status s) {
        LinearLayout card = ui.card(content);
        ui.row(card, s.apps + " apps", null, null, s.appsChanged == 0 ? "Same" : Ui.plural(s.appsChanged, "change", "changes"));
        ui.row(card, s.permissions + " permissions", null, null, s.permissionsChanged == 0 ? "Same" : Ui.plural(s.permissionsChanged, "change", "changes"));
        ui.row(card, s.settings + " security settings", null, null, s.settingsChanged == 0 ? "Same" : Ui.plural(s.settingsChanged, "change", "changes"));
    }

    private void lastIncident(LinearLayout content, IncidentEntity last) {
        ui.heading(content, "Last incident");
        LinearLayout card = ui.card(content);
        View row = ui.row(card, last.title, last.code() + " · closed " + Ui.when(last.closedAt), null, "");
        Ui.pill(ui.pillOf(row), last.state());
        row.setOnClickListener(v -> startActivity(IncidentActivity.intent(requireContext(), last.id)));
    }
}
