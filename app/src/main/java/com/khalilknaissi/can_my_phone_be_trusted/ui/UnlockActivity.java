package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.khalilknaissi.can_my_phone_be_trusted.R;

/** App lock: Android's own PIN, pattern or fingerprint prompt. The app never sees the PIN. */
public class UnlockActivity extends SimpleActivity {

    private final ActivityResultLauncher<Intent> confirm = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    markUnlocked();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViewById(R.id.back).setVisibility(android.view.View.INVISIBLE);
        title.setText("Locked");
        ui.text(body, R.style.Body, "Incidents and reports describe your phone in detail. Confirm it's you to open them.")
                .setPadding(ui.dp(4), ui.dp(12), ui.dp(4), 0);
        ui.primary(bottom, "Unlock", v -> ask());
        // Leaving without unlocking closes the app instead of showing what's behind.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
        if (savedInstanceState == null) ask();
    }

    @SuppressWarnings("deprecation")
    private void ask() {
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        Intent i = km == null ? null : km.createConfirmDeviceCredentialIntent("Unlock Trusted?", "Confirm it's you to see incidents and reports.");
        if (i == null) {
            // No screen lock on the phone: nothing to confirm with.
            markUnlocked();
            finish();
        } else {
            confirm.launch(i);
        }
    }
}
