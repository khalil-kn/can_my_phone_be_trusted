package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.khalilknaissi.can_my_phone_be_trusted.R;

/** Honest answers about what the app does and doesn't do. */
public class HelpActivity extends SimpleActivity {

    private static final String[][] QA = {
            {"Does \"Seal intact\" mean my phone is safe?",
                    "No. It means nothing changed in what the app can see since your baseline. It can't look inside other apps and it doesn't scan for viruses."},
            {"What does it watch?",
                    "Every app (version, signing key, where it came from), the dangerous permissions and special access each app holds (accessibility, device admin, notification access), and 8 security settings: screen lock, USB debugging, wireless debugging, developer options, encryption, security patch, Android version and root tools."},
            {"Why do some changes show a window?",
                    "Android records when apps are installed or updated, but not when a permission or setting changes. Those are shown as \"somewhere between two checks\"."},
            {"How is the score worked out?",
                    "Each change adds risk points, for example accessibility service +30, SMS access +25, installed outside the Play Store +20, USB debugging +8. Changes found in the same check about the same app are linked into one incident. The score is the sum, capped at 100: 80+ Critical, 60+ High, 40+ Moderate, 20+ Low."},
            {"How often does it check?",
                    "About every 12 hours in the background, when Android allows it. You can also press Check at any time."},
            {"Does anything leave my phone?",
                    "No. The app has no internet permission. Reports are only sent when you press Share and pick an app."},
            {"Why does it need to see all my apps?",
                    "Since Android 11, apps can't see each other unless they ask (QUERY_ALL_PACKAGES). Without it, a baseline would miss most apps."}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        title.setText("Help");
        for (String[] qa : QA) {
            ui.heading(body, qa[0]).setTextSize(26);
            ui.text(body, R.style.Body, qa[1]).setPadding(ui.dp(4), 0, ui.dp(4), 0);
        }
        ui.heading(body, "About");
        LinearLayout card = ui.card(body);
        String version = "1.0";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        ui.field(card, "Version", version);
        ui.field(card, "Internet access", "None requested");
        ui.field(card, "Fonts", "Bebas Neue and Atkinson Hyperlegible, SIL Open Font License");
        ui.text(body, R.style.Small_Muted, "Built by Mohamed Khalil Knaissi as an Integration Project at EPI Digital School, 2026–2027. Supervised by Mrs Maha Harzallah Triki.")
                .setPadding(ui.dp(4), ui.dp(14), ui.dp(4), 0);
    }
}
