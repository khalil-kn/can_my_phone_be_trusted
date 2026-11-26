package com.khalilknaissi.can_my_phone_be_trusted.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.model.Severity;

/**
 * Builds the repeated pieces of the design in code (cards, rows, headings, buttons),
 * so lists and simple screens don't each need their own XML file.
 */
public final class Views {
    private final Context c;
    private final LayoutInflater inflater;

    public Views(Context context) {
        this.c = context;
        this.inflater = LayoutInflater.from(context);
    }

    public int dp(float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** A TextView with one of the text styles (Body, Small.Muted, Display.S...). */
    public TextView text(ViewGroup parent, int style, CharSequence s) {
        TextView t = new TextView(c, null, 0, style);
        t.setText(s);
        parent.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return t;
    }

    /** A section heading above a card, in display type. */
    public TextView heading(ViewGroup parent, CharSequence s) {
        TextView t = text(parent, R.style.Display_S, s);
        t.setPadding(dp(4), dp(22), dp(4), dp(8));
        androidx.core.view.ViewCompat.setAccessibilityHeading(t, true);
        return t;
    }

    /** A rounded card that stacks rows with thin dividers between them. */
    public LinearLayout card(ViewGroup parent) {
        LinearLayout l = new LinearLayout(c, null, 0, R.style.CardList);
        parent.addView(l, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return l;
    }

    public View row(ViewGroup parent, CharSequence title, CharSequence sub, Severity severity, String pill) {
        View v = inflater.inflate(R.layout.item_row, parent, false);
        ((TextView) v.findViewById(R.id.rowTitle)).setText(title);
        TextView s = v.findViewById(R.id.rowSub);
        if (sub != null && sub.length() > 0) {
            s.setText(sub);
            s.setVisibility(View.VISIBLE);
        }
        if (severity != null) {
            v.findViewById(R.id.rowSev).setVisibility(View.VISIBLE);
            ((SeverityBars) v.findViewById(R.id.rowBars)).set(severity);
            TextView label = v.findViewById(R.id.rowSevLabel);
            label.setText(severity.label);
            label.setTextColor(Ui.color(c, severity.isHot() ? R.color.maroon : R.color.ink));
        }
        if (pill != null) {
            TextView p = v.findViewById(R.id.rowTrail);
            p.setText(pill);
            p.setVisibility(View.VISIBLE);
        }
        parent.addView(v);
        return v;
    }

    /** The trailing pill of a row, for custom colours. */
    public TextView pillOf(View row) {
        TextView p = row.findViewById(R.id.rowTrail);
        p.setVisibility(View.VISIBLE);
        return p;
    }

    /** Turns a row's pill into big maroon risk points, like "+30". */
    public void points(View row, int points) {
        TextView p = pillOf(row);
        p.setBackground(null);
        p.setText(points > 0 ? "+" + points : "0");
        p.setTypeface(ResourcesCompat.getFont(c, R.font.bebas_neue));
        p.setTextSize(32);
        p.setTextColor(Ui.color(c, points > 0 ? R.color.maroon : R.color.muted));
        p.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    /** A title/value row, like "Installed   26 Oct, 23:52". */
    public View field(ViewGroup parent, String label, CharSequence value) {
        return row(parent, value, label, null, null);
    }

    public MaterialSwitch toggle(ViewGroup parent, String title, String sub, boolean checked, OnToggle onToggle) {
        View v = inflater.inflate(R.layout.item_switch, parent, false);
        ((TextView) v.findViewById(R.id.swTitle)).setText(title);
        TextView s = v.findViewById(R.id.swSub);
        s.setText(sub);
        s.setVisibility(sub == null ? View.GONE : View.VISIBLE);
        MaterialSwitch sw = v.findViewById(R.id.sw);
        sw.setChecked(checked);
        sw.setContentDescription(title);
        sw.setOnCheckedChangeListener((button, isChecked) -> onToggle.changed(isChecked));
        v.setOnClickListener(x -> sw.toggle());
        parent.addView(v);
        return sw;
    }

    public interface OnToggle {
        void changed(boolean on);
    }

    /** Filled ink button that shares the bottom bar's width. */
    public Button primary(ViewGroup parent, String label, View.OnClickListener click) {
        Button b = (Button) inflater.inflate(R.layout.btn_ink, parent, false);
        b.setText(label);
        b.setOnClickListener(click);
        parent.addView(b);
        return b;
    }

    /** Outlined secondary button. */
    public Button secondary(ViewGroup parent, String label, View.OnClickListener click) {
        Button b = (Button) inflater.inflate(R.layout.btn_outline, parent, false);
        b.setText(label);
        b.setOnClickListener(click);
        parent.addView(b);
        if (parent instanceof LinearLayout && ((LinearLayout) parent).getOrientation() == LinearLayout.HORIZONTAL) {
            ((LinearLayout.LayoutParams) b.getLayoutParams()).setMarginEnd(dp(8));
        }
        return b;
    }

    public void gap(ViewGroup parent, int dp) {
        View v = new View(c);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(dp)));
    }

    public static void margins(View v, int left, int top, int right, int bottom) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        lp.setMargins(left, top, right, bottom);
        v.setLayoutParams(lp);
    }
}
