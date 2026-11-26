package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.viewmodel.SecurityViewModel;

/** Base for the screens built on activity_simple.xml: back button, big title, body, bottom buttons. */
public abstract class SimpleActivity extends BaseActivity {
    protected SecurityViewModel vm;
    protected Views ui;
    protected LinearLayout body;
    protected LinearLayout bottom;
    protected LinearLayout headerExtra;
    protected TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);
        Ui.fitTop(findViewById(R.id.header));
        Ui.fitBottom(findViewById(R.id.bottom));
        Ui.statusIcons(this, true);
        ui = new Views(this);
        body = findViewById(R.id.body);
        bottom = findViewById(R.id.bottom);
        headerExtra = findViewById(R.id.headerExtra);
        title = findViewById(R.id.title);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        vm = new ViewModelProvider(this).get(SecurityViewModel.class);
        vm.message().observe(this, e -> {
            String text = e.take();
            if (text != null) Ui.toast(this, text);
        });
    }

    /** Removes everything under the title, before drawing fresh data. */
    protected void clearBody() {
        body.removeViews(1, body.getChildCount() - 1);
    }
}
