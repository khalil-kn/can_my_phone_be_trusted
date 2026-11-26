package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.Prefs;
import com.khalilknaissi.can_my_phone_be_trusted.repository.SecurityRepository;
import com.khalilknaissi.can_my_phone_be_trusted.viewmodel.SecurityViewModel;

/** The app's home: five tabs (Status, Changes, Timeline, Incidents, Reports) under a bottom bar. */
public class MainActivity extends BaseActivity {
    public static final String EXTRA_INCIDENT = "incident";
    public static final String EXTRA_TAB = "tab";
    public static final int TAB_CHANGES = R.id.nav_changes;

    private BottomNavigationView nav;
    private SecurityViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Prefs.get(this).onboardingDone()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        nav = findViewById(R.id.nav);
        nav.setOnItemSelectedListener(item -> {
            show(item.getItemId());
            return true;
        });

        vm = new ViewModelProvider(this).get(SecurityViewModel.class);
        vm.status().observe(this, this::badges);
        vm.refresh().observe(this, v -> vm.loadStatus());

        // Back from another tab goes to Status first, then leaves the app.
        // Inside this anonymous class "this" is the callback, so the activity is MainActivity.this.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (nav.getSelectedItemId() != R.id.nav_status) {
                    nav.setSelectedItemId(R.id.nav_status);
                } else {
                    setEnabled(false);
                    MainActivity.this.getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        if (savedInstanceState == null) {
            nav.setSelectedItemId(R.id.nav_status);
            handle(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handle(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vm != null) vm.loadStatus();
    }

    /** Opened from a notification: go straight to the incident or tab. */
    private void handle(Intent intent) {
        long incident = intent.getLongExtra(EXTRA_INCIDENT, 0);
        if (incident > 0) startActivity(IncidentActivity.intent(this, incident));
        int tab = intent.getIntExtra(EXTRA_TAB, 0);
        if (tab != 0) nav.setSelectedItemId(tab);
    }

    public void selectTab(int id) {
        nav.setSelectedItemId(id);
    }

    private void show(int id) {
        Fragment f;
        if (id == R.id.nav_changes) f = new ChangesFragment();
        else if (id == R.id.nav_timeline) f = new TimelineFragment();
        else if (id == R.id.nav_incidents) f = new IncidentsFragment();
        else if (id == R.id.nav_reports) f = new ReportsFragment();
        else f = new StatusFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.container, f).commit();
    }

    private void badges(SecurityRepository.Status s) {
        setBadge(R.id.nav_incidents, s.openIncidents);
        setBadge(R.id.nav_changes, s.unreviewed);
    }

    private void setBadge(int item, int count) {
        if (count <= 0) {
            nav.removeBadge(item);
            return;
        }
        BadgeDrawable b = nav.getOrCreateBadge(item);
        b.setNumber(count);
        b.setBackgroundColor(Ui.color(this, item == R.id.nav_incidents ? R.color.seal_red : R.color.ink));
        b.setBadgeTextColor(Ui.color(this, R.color.white));
        b.setContentDescriptionQuantityStringsResource(item == R.id.nav_incidents ? R.plurals.badge_incidents : R.plurals.badge_changes);
    }
}
