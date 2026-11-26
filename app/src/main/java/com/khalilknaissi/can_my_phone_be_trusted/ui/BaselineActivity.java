package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.Prefs;
import com.khalilknaissi.can_my_phone_be_trusted.repository.SecurityRepository;
import com.khalilknaissi.can_my_phone_be_trusted.viewmodel.SecurityViewModel;

/** Stage 1, "Baseline": review, record with a real progress count, then "Sealed". */
public class BaselineActivity extends BaseActivity {
    private SecurityViewModel vm;
    private Views ui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baseline);
        Ui.fitTop(findViewById(R.id.reviewTop));
        Ui.fitBottom(findViewById(R.id.reviewBottom));
        Ui.fitTop(findViewById(R.id.doneTop));
        Ui.fitBottom(findViewById(R.id.groupDone));
        Ui.statusIcons(this, true);
        ui = new Views(this);
        Prefs prefs = Prefs.get(this);

        EditText name = findViewById(R.id.name);
        name.setText(prefs.deviceName());
        ((TextView) findViewById(R.id.model)).setText(Build.MANUFACTURER + " " + Build.MODEL + " · Android " + Build.VERSION.RELEASE
                + " · security patch " + Build.VERSION.SECURITY_PATCH);
        ((TextView) findViewById(R.id.reviewTape)).setText(("SEALED  ◆  " + prefs.deviceName() + "  ◆  BASELINE  ◆  SEALED").toUpperCase(java.util.Locale.ROOT));

        vm = new ViewModelProvider(this).get(SecurityViewModel.class);
        findViewById(R.id.btnRecord).setOnClickListener(v -> {
            prefs.setDeviceName(name.getText().toString().trim());
            showOnly(R.id.groupScan);
            Ui.statusIcons(this, false);
            vm.seal();
        });
        vm.sealProgress().observe(this, p -> {
            ((TextView) findViewById(R.id.scanCount)).setText(String.valueOf(p[0]));
            ((TextView) findViewById(R.id.scanTotal)).setText("of " + p[1] + " apps");
            LinearProgressIndicator bar = findViewById(R.id.scanBar);
            bar.setMax(p[1]);
            bar.setProgressCompat(p[0], true);
            bar.setContentDescription(p[0] + " of " + p[1] + " apps recorded");
        });
        vm.sealCurrent().observe(this, pkg -> ((TextView) findViewById(R.id.scanCurrent)).setText("Reading " + pkg));
        vm.sealed().observe(this, this::sealed);

        findViewById(R.id.btnGoStatus).setOnClickListener(v -> {
            prefs.setOnboardingDone(true);
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });
    }

    private void showOnly(int group) {
        for (int id : new int[]{R.id.groupReview, R.id.groupScan, R.id.groupDone}) {
            findViewById(id).setVisibility(id == group ? View.VISIBLE : View.GONE);
        }
    }

    private void sealed(SecurityRepository.SealSummary s) {
        Prefs.get(this).setOnboardingDone(true);
        showOnly(R.id.groupDone);
        Ui.statusIcons(this, false);
        ((TextView) findViewById(R.id.sealedAt)).setText(android.text.format.DateFormat.format("EEEE d MMM yyyy, HH:mm", s.sealedAt));
        ((TextView) findViewById(R.id.doneTape)).setText(Ui.tapeText(s.sealedAt));
        GridLayout grid = findViewById(R.id.doneStats);
        grid.removeAllViews();
        tile(grid, "Apps", String.valueOf(s.apps), s.userApps + " yours · " + (s.apps - s.userApps) + " system");
        tile(grid, "Permissions", String.valueOf(s.permissions), "held by those apps");
        tile(grid, "Settings", String.valueOf(s.settings), "watched every check");
        tile(grid, "Android", s.android, "patch " + s.patch);
        ((TextView) findViewById(R.id.doneNote)).setText(Prefs.get(this).autoCheck()
                ? "Checks run about every 12 hours from now. You can also press Check on the Status screen."
                : "Automatic checks are off. Press Check on the Status screen.");
    }

    private void tile(GridLayout grid, String label, String value, String sub) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setBackgroundResource(R.drawable.bg_card);
        t.setPadding(ui.dp(18), ui.dp(14), ui.dp(18), ui.dp(14));
        ui.text(t, R.style.Small_Bold, label);
        TextView v = ui.text(t, R.style.Display, value);
        v.setTextSize(52);
        ui.text(t, R.style.Tiny, sub);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        lp.width = 0;
        lp.setMargins(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4));
        grid.addView(t, lp);
    }
}
