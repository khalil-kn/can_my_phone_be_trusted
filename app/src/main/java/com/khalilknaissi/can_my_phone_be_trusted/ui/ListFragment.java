package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.viewmodel.SecurityViewModel;

/** What the Changes, Timeline, Incidents and Reports tabs have in common: a title, chips and a list. */
public abstract class ListFragment extends Fragment {
    protected SecurityViewModel vm;
    protected Views ui;
    protected LinearLayout list;
    protected LinearLayout bottom;
    protected ChipGroup chips;

    protected abstract String title();

    protected abstract void setUp();

    protected abstract void load();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        View root = inflater.inflate(R.layout.fragment_list, container, false);
        Ui.fitTop(root.findViewById(R.id.header));
        ((TextView) root.findViewById(R.id.title)).setText(title());
        ui = new Views(requireContext());
        list = root.findViewById(R.id.list);
        bottom = root.findViewById(R.id.bottom);
        chips = root.findViewById(R.id.chips);
        vm = new ViewModelProvider(requireActivity()).get(SecurityViewModel.class);
        setUp();
        vm.refresh().observe(getViewLifecycleOwner(), v -> load());
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Ui.statusIcons(requireActivity(), true);
        load();
    }

    protected Chip chip(String text, boolean checked) {
        Chip c = (Chip) LayoutInflater.from(requireContext()).inflate(R.layout.chip_filter, chips, false);
        c.setText(text);
        c.setCheckable(true);
        c.setChecked(checked);
        chips.addView(c);
        return c;
    }

    protected void empty(String title, String body) {
        LinearLayout card = ui.card(list);
        Views.margins(card, 0, ui.dp(16), 0, 0);
        ui.row(card, title, body, null, null);
    }
}
