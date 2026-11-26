package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.widget.ImageViewCompat;

import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.Prefs;

/**
 * First launch, five pages from the design: what it does, what it can and can't see,
 * alerts permission, optional usage access, optional app lock. Then the baseline.
 */
public class OnboardingActivity extends BaseActivity {
    private static final int PAGES = 5;
    private int page;
    private boolean waitingForUsage;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                Prefs.get(this).setAlertsOn(granted);
                show(page + 1);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        Ui.fitTop(findViewById(R.id.header));
        Ui.fitBottom(findViewById(R.id.bottom));
        findViewById(R.id.back).setOnClickListener(v -> show(page - 1));
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (page > 0) show(page - 1);
                else finish();
            }
        });
        show(savedInstanceState == null ? 0 : savedInstanceState.getInt("page"));
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("page", page);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Back from Android's usage access screen: move on.
        if (waitingForUsage) {
            waitingForUsage = false;
            show(4);
        }
    }

    private void show(int p) {
        if (p < 0) return;
        if (p >= PAGES) {
            startActivity(new Intent(this, BaselineActivity.class));
            return;
        }
        page = p;
        TextView eyebrow = findViewById(R.id.eyebrow), title = findViewById(R.id.title), body = findViewById(R.id.body),
                note = findViewById(R.id.note), c1t = findViewById(R.id.card1Title), c1b = findViewById(R.id.card1Body),
                c2t = findViewById(R.id.card2Title), c2b = findViewById(R.id.card2Body), dots = findViewById(R.id.dots);
        View card1 = findViewById(R.id.card1), card2 = findViewById(R.id.card2);
        Button primary = findViewById(R.id.primary), secondary = findViewById(R.id.secondary);
        findViewById(R.id.back).setVisibility(p == 0 ? View.INVISIBLE : View.VISIBLE);
        findViewById(R.id.tag).setVisibility(p == 3 ? View.VISIBLE : View.GONE);
        findViewById(R.id.lockGroup).setVisibility(p == 4 ? View.VISIBLE : View.GONE);
        card1.setVisibility(View.VISIBLE);
        card2.setVisibility(View.GONE);
        secondary.setVisibility(View.GONE);
        eyebrow.setText("");
        note.setText("");

        StringBuilder d = new StringBuilder();
        for (int i = 0; i < PAGES; i++) d.append(i == p ? "▬ " : "• ");
        dots.setText(d.toString().trim());

        switch (p) {
            case 0:
                colors(R.color.tape, R.color.ink);
                eyebrow.setText("Can My Phone Be Trusted?");
                title.setText("Seal it.\nThen watch\nthe tape.");
                card(card1, R.color.card, R.color.ink, -3);
                c1t.setText("Baseline → today's check");
                c1b.setText("Apps:  87  →  88\nUSB debugging:  Off  →  On\nApps that read SMS:  2  →  3");
                body.setText("The app records your apps, their permissions and 8 security settings as a baseline. Every check after that tells you exactly what changed.");
                primary.setText("Next");
                primary.setOnClickListener(v -> show(1));
                break;
            case 1:
                colors(R.color.bone, R.color.ink);
                title.setText("Honest about\nwhat it sees");
                card(card1, R.color.slate, R.color.white, 0);
                c1t.setText("It can see");
                c1b.setText("✓  Every app, and whether it came from the Play Store\n✓  Which permissions each app holds\n✓  8 security settings, like USB debugging");
                card2.setVisibility(View.VISIBLE);
                card(card2, R.color.ink, R.color.bone, -1.5f);
                c2t.setText("It can't see");
                c2b.setText("–  Your messages, photos, files or browsing\n–  Anything that needs root access\n–  The exact time of some changes, so it shows a window");
                body.setText("");
                note.setText("Everything stays on this phone. No account, no uploads.");
                primary.setText("Next");
                primary.setOnClickListener(v -> show(2));
                break;
            case 2:
                colors(R.color.ink, R.color.bone);
                title.setText("Hear it\nwhen the\nseal breaks");
                card(card1, R.color.card, R.color.ink, 2.5f);
                c1t.setText("Critical incident");
                c1b.setText("A sideloaded app got SMS and accessibility access. Tap to see the changes behind it.");
                body.setText("Checks run about every 12 hours in the background. You're alerted only for High and Critical incidents; the rest waits in the app. Change this any time in Settings.");
                primary.setText("Turn on alerts");
                primary.setOnClickListener(v -> {
                    if (Build.VERSION.SDK_INT >= 33) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                    } else {
                        Prefs.get(this).setAlertsOn(true);
                        show(3);
                    }
                });
                secondary.setVisibility(View.VISIBLE);
                secondary.setText("Not now");
                secondary.setOnClickListener(v -> {
                    Prefs.get(this).setAlertsOn(false);
                    show(3);
                });
                break;
            case 3:
                colors(R.color.slate, R.color.white);
                title.setText("Was the new\napp ever\nopened?");
                card(card1, R.color.card, R.color.ink, 0);
                c1t.setText("Last opened");
                c1b.setText("With usage access, an incident can show a new app was opened 3 minutes after it was installed. Without it: unknown.");
                body.setText("Android hides when apps were last used unless you allow usage access.\n\n1. Android Settings opens. Find Trusted?\n2. Turn on \"Permit usage access\", then press back.");
                primary.setText("Open usage access settings");
                primary.setOnClickListener(v -> {
                    waitingForUsage = true;
                    Ui.open(this, new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                });
                secondary.setVisibility(View.VISIBLE);
                secondary.setText("Skip");
                secondary.setOnClickListener(v -> show(4));
                break;
            default:
                colors(R.color.bone, R.color.ink);
                title.setText("Lock the\nevidence");
                card1.setVisibility(View.GONE);
                body.setText("Incidents and reports describe your phone in detail. Ask for your fingerprint, face or PIN before anyone opens them.");
                note.setText("Android shows its own unlock prompt. The app never sees your PIN or fingerprint.");
                primary.setText("Turn on app lock");
                primary.setOnClickListener(v -> turnOnLock());
                secondary.setVisibility(View.VISIBLE);
                secondary.setText("Not now");
                secondary.setOnClickListener(v -> show(5));
                break;
        }
    }

    private void turnOnLock() {
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km == null || !km.isDeviceSecure()) {
            Ui.toast(this, "Set a screen lock on the phone first. You can turn this on later in Settings.");
        } else {
            Prefs prefs = Prefs.get(this);
            int checked = ((RadioGroup) findViewById(R.id.lockGroup)).getCheckedRadioButtonId();
            prefs.setLockTimeoutMs(checked == R.id.lockNow ? 0 : checked == R.id.lock5 ? 300_000 : 60_000);
            prefs.setLockOn(true);
            markUnlocked();
        }
        show(5);
    }

    /** Page background and text colour, plus matching status bar icons. */
    private void colors(int bg, int fg) {
        findViewById(R.id.main).setBackgroundColor(Ui.color(this, bg));
        int text = Ui.color(this, fg);
        for (int id : new int[]{R.id.eyebrow, R.id.title, R.id.body, R.id.note, R.id.dots}) {
            ((TextView) findViewById(id)).setTextColor(text);
        }
        ImageViewCompat.setImageTintList((ImageButton) findViewById(R.id.back), android.content.res.ColorStateList.valueOf(text));
        boolean light = bg == R.color.tape || bg == R.color.bone;
        Ui.statusIcons(this, light);
        Button primary = findViewById(R.id.primary), secondary = findViewById(R.id.secondary);
        int btnBg = bg == R.color.ink ? R.color.tape : bg == R.color.slate ? R.color.tape : R.color.ink;
        primary.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Ui.color(this, btnBg)));
        primary.setTextColor(Ui.color(this, btnBg == R.color.ink ? R.color.bone : R.color.ink));
        secondary.setTextColor(text);
    }

    private void card(View card, int bg, int fg, float rotation) {
        card.setBackground(Ui.rounded(this, bg, 28));
        card.setRotation(rotation);
        int c = Ui.color(this, fg);
        int[] ids = card.getId() == R.id.card1 ? new int[]{R.id.card1Title, R.id.card1Body} : new int[]{R.id.card2Title, R.id.card2Body};
        for (int id : ids) ((TextView) findViewById(id)).setTextColor(c);
    }
}
