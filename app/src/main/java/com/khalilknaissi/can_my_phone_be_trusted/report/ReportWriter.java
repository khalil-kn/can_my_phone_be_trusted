package com.khalilknaissi.can_my_phone_be_trusted.report;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.format.DateFormat;

import androidx.core.content.res.ResourcesCompat;

import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.AppSnapshotEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.ChangeEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.CheckEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.IncidentEntity;
import com.khalilknaissi.can_my_phone_be_trusted.engine.Playbook;
import com.khalilknaissi.can_my_phone_be_trusted.engine.RiskRules;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Stage 5, "Report": writes evidence files that stay on the phone until the user shares them.
 * PDF is drawn with Android's built-in PdfDocument; JSON uses the built-in org.json. No libraries.
 */
public final class ReportWriter {
    private ReportWriter() {}

    /** Everything a report can contain. The repository fills it. */
    public static class Data {
        public long createdAt, baselineAt;
        public int baselineAppCount;
        public String deviceName, owner, model, android, patch;
        public IncidentEntity incident;
        public List<ChangeEntity> changes = new ArrayList<>();
        public List<Playbook.Step> steps = new ArrayList<>();
        public Set<String> stepsDone;
        // Only for "Export everything".
        public List<ChangeEntity> allChanges;
        public List<IncidentEntity> allIncidents;
        public List<CheckEntity> checks;
        public List<AppSnapshotEntity> baselineApps;
    }

    // ------------------------------------------------------------------ JSON

    /** Returns 0 (JSON has no pages). */
    public static int writeJson(Data d, File file, boolean everything) throws JSONException, java.io.IOException {
        JSONObject root = new JSONObject();
        root.put("app", "Can My Phone Be Trusted?");
        root.put("format", everything ? "full-export" : d.incident != null ? "incident-report" : "baseline-report");
        root.put("generatedAt", iso(d.createdAt));

        JSONObject device = new JSONObject();
        device.put("name", d.deviceName);
        device.put("owner", d.owner);
        device.put("model", d.model);
        device.put("android", d.android);
        device.put("securityPatch", d.patch);
        root.put("device", device);

        JSONObject base = new JSONObject();
        base.put("sealedAt", iso(d.baselineAt));
        base.put("apps", d.baselineAppCount);
        root.put("baseline", base);

        if (d.incident != null) root.put("incident", incidentJson(d.incident, d.steps, d.stepsDone));
        root.put("changes", changesJson(d.changes));

        if (everything) {
            JSONArray incidents = new JSONArray();
            for (IncidentEntity i : d.allIncidents) incidents.put(incidentJson(i, null, null));
            root.put("allIncidents", incidents);
            root.put("allChanges", changesJson(d.allChanges));
            JSONArray checks = new JSONArray();
            for (CheckEntity c : d.checks) {
                JSONObject o = new JSONObject();
                o.put("ranAt", iso(c.ranAt));
                o.put("trigger", c.trigger);
                o.put("apps", c.appCount);
                o.put("newChanges", c.newChanges);
                checks.put(o);
            }
            root.put("checks", checks);
            JSONArray apps = new JSONArray();
            for (AppSnapshotEntity a : d.baselineApps) {
                JSONObject o = new JSONObject();
                o.put("package", a.packageName);
                o.put("label", a.appLabel);
                o.put("version", a.versionName);
                o.put("versionCode", a.versionCode);
                o.put("system", a.isSystemApp);
                o.put("installer", a.installer == null ? JSONObject.NULL : a.installer);
                o.put("signingKeySha256", a.signer);
                o.put("permissions", a.permissions);
                o.put("specialAccess", a.specialAccess);
                apps.put(o);
            }
            root.put("baselineApps", apps);
        }

        try (OutputStream out = new FileOutputStream(file)) {
            out.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        return 0;
    }

    private static JSONObject incidentJson(IncidentEntity i, List<Playbook.Step> steps, Set<String> done) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", i.code());
        o.put("title", i.title);
        o.put("score", i.score);
        o.put("severity", i.severity().label);
        o.put("state", i.state().label);
        o.put("app", i.primaryPackage == null ? JSONObject.NULL : i.primaryPackage);
        o.put("openedAt", iso(i.createdAt));
        o.put("closedAt", i.closedAt > 0 ? iso(i.closedAt) : JSONObject.NULL);
        o.put("notes", i.notes == null ? "" : i.notes);
        if (steps != null) {
            JSONArray s = new JSONArray();
            for (Playbook.Step step : steps) {
                JSONObject st = new JSONObject();
                st.put("step", step.title);
                st.put("done", done != null && done.contains(step.key));
                s.put(st);
            }
            o.put("steps", s);
        }
        return o;
    }

    private static JSONArray changesJson(List<ChangeEntity> changes) throws JSONException {
        JSONArray arr = new JSONArray();
        for (ChangeEntity c : changes) {
            JSONObject o = new JSONObject();
            o.put("kind", c.kind);
            o.put("title", c.title());
            o.put("app", c.packageName == null ? JSONObject.NULL : c.packageName);
            o.put("subject", c.subject == null ? JSONObject.NULL : c.subject);
            o.put("was", c.oldValue);
            o.put("now", c.newValue);
            o.put("points", c.points);
            o.put("severity", c.severity().label);
            if (c.exactAt > 0) {
                o.put("at", iso(c.exactAt));
            } else {
                o.put("between", iso(c.windowStart));
                o.put("and", iso(c.windowEnd));
            }
            o.put("undone", c.isReverted());
            o.put("incident", c.incidentId > 0 ? String.format(java.util.Locale.US, "INC-%04d", c.incidentId) : JSONObject.NULL);
            arr.put(o);
        }
        return arr;
    }

    private static String iso(long t) {
        return t <= 0 ? "" : DateFormat.format("yyyy-MM-dd'T'HH:mm:ss", t).toString();
    }

    // ------------------------------------------------------------------ PDF

    private static final int W = 595, H = 842, M = 48; // A4 in points, margin
    private static final int INK = 0xFF2A1110, MUTED = 0xFF5E4741, RED = 0xFFDC1928, SLATE = 0xFF3E6175,
            TAPE = 0xFFE0A912, BONE = 0xFFE2DCCF, SAND = 0xFFD7D0C1, MAROON = 0xFF952625;

    /** Writes the PDF and returns its page count. */
    public static int writePdf(Context context, Data d, File file) throws java.io.IOException {
        Typeface display = ResourcesCompat.getFont(context, R.font.bebas_neue);
        Typeface body = ResourcesCompat.getFont(context, R.font.atkinson_regular);
        Typeface bold = ResourcesCompat.getFont(context, R.font.atkinson_bold);
        Pen pen = new Pen(new PdfDocument(), display, body, bold);

        // Cover band: red for an incident, slate for a baseline report.
        boolean incident = d.incident != null;
        Canvas c = pen.canvas;
        Paint fill = new Paint();
        fill.setColor(incident ? RED : SLATE);
        c.drawRect(0, 0, W, 230, fill);
        Paint white = pen.paint(body, 11, Color.WHITE);
        c.drawText(incident ? "INCIDENT REPORT" : "BASELINE REPORT", M, 56, pen.paint(bold, 11, Color.WHITE));
        if (incident) {
            c.drawText(String.valueOf(d.incident.score), M, 150, pen.paint(display, 96, Color.WHITE));
            c.drawText(d.incident.severity().label.toUpperCase(java.util.Locale.ROOT), M + 120, 130, pen.paint(display, 34, Color.WHITE));
            c.drawText("score, out of 100", M + 120, 148, white);
            pen.y = 186;
            pen.wrap(d.incident.title, pen.paint(bold, 15, Color.WHITE), M, W - 2 * M);
        } else {
            c.drawText(String.valueOf(d.changes.size()), M, 150, pen.paint(display, 96, Color.WHITE));
            c.drawText("CHANGES", M + 130, 130, pen.paint(display, 34, Color.WHITE));
            c.drawText("since the baseline of " + day(d.baselineAt), M + 130, 148, white);
        }
        c.drawText(d.deviceName + " · " + day(d.createdAt), M, 216, white);
        // The tape, as in the app.
        fill.setColor(TAPE);
        c.drawRect(0, 236, W, 262, fill);
        c.drawText("EVIDENCE  ◆  SEALED " + day(d.baselineAt).toUpperCase(java.util.Locale.ROOT)
                + "  ◆  CAN MY PHONE BE TRUSTED?  ◆  EVIDENCE", M, 255, pen.paint(display, 16, INK));
        pen.y = 296;

        pen.heading("Summary");
        pen.field("Phone", d.deviceName + " (" + d.model + ")");
        if (d.owner != null && !d.owner.isEmpty()) pen.field("Owner", d.owner);
        pen.field("Android", d.android + " · security patch " + d.patch);
        pen.field("Baseline sealed", dayTime(d.baselineAt) + " · " + d.baselineAppCount + " apps");
        pen.field("Report created", dayTime(d.createdAt));
        if (incident) {
            pen.field("Incident", d.incident.code() + " · " + d.incident.state().label);
            pen.field("Opened", dayTime(d.incident.createdAt));
            if (d.incident.closedAt > 0) pen.field("Closed", dayTime(d.incident.closedAt));
        }

        pen.heading(incident ? "What broke the seal" : "Changes since the baseline");
        if (d.changes.isEmpty()) pen.text("No changes. The phone matches its baseline.", MUTED);
        int sum = 0;
        for (ChangeEntity ch : d.changes) {
            sum += ch.points;
            String line = (ch.isSetting() ? ch.title() + ": " + ch.oldValue + " → " + ch.newValue : ch.title())
                    + (ch.isReverted() ? " (undone)" : "");
            pen.change(line, ch.source() + " · " + ch.severity().label + " · " + when(ch), ch.points);
        }
        if (incident) pen.text("Score: " + sum + " points" + (sum > 100 ? ", capped at 100." : "."), INK);

        if (incident && !d.steps.isEmpty()) {
            pen.heading("Investigation");
            for (Playbook.Step s : d.steps) {
                boolean done = d.stepsDone != null && d.stepsDone.contains(s.key);
                pen.text((done ? "[x] " : "[  ] ") + s.title, INK);
            }
            if (d.incident.notes != null && !d.incident.notes.isEmpty()) {
                pen.heading("Notes");
                pen.text(d.incident.notes, INK);
            }
        }

        pen.heading("Timeline");
        List<ChangeEntity> ordered = new ArrayList<>(d.changes);
        Collections.sort(ordered, (a, b) -> Long.compare(time(a), time(b)));
        for (ChangeEntity ch : ordered) pen.text(when(ch) + "   " + ch.title() + " · " + ch.source(), INK);

        pen.heading("What this report can and can't show");
        pen.text("It lists differences from the baseline that Android lets an app see without root: apps, "
                + "their permissions and special access, and 8 security settings. It doesn't read messages, "
                + "photos or files and doesn't scan for viruses. Permission and setting changes are shown as a "
                + "window between two checks, because Android doesn't log their exact time.", MUTED);

        int pages = pen.finish();
        try (OutputStream out = new FileOutputStream(file)) {
            pen.doc.writeTo(out);
        }
        pen.doc.close();
        return pages;
    }

    private static long time(ChangeEntity c) {
        return c.exactAt > 0 ? c.exactAt : c.windowEnd;
    }

    private static String when(ChangeEntity c) {
        if (c.exactAt > 0) return dayTime(c.exactAt);
        return dayTime(c.windowStart) + " – " + dayTime(c.windowEnd);
    }

    private static String day(long t) {
        return t <= 0 ? "-" : DateFormat.format("d MMM yyyy", t).toString();
    }

    private static String dayTime(long t) {
        return t <= 0 ? "-" : DateFormat.format("d MMM yyyy, HH:mm", t).toString();
    }

    /** Writes lines down the page and starts a new page when one is full. */
    private static final class Pen {
        final PdfDocument doc;
        final Typeface display, body, bold;
        PdfDocument.Page page;
        Canvas canvas;
        int number;
        float y;

        Pen(PdfDocument doc, Typeface display, Typeface body, Typeface bold) {
            this.doc = doc;
            this.display = display;
            this.body = body;
            this.bold = bold;
            newPage();
        }

        Paint paint(Typeface face, float size, int color) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setTypeface(face);
            p.setTextSize(size);
            p.setColor(color);
            return p;
        }

        void newPage() {
            if (page != null) {
                footer();
                doc.finishPage(page);
            }
            number++;
            page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, number).create());
            canvas = page.getCanvas();
            Paint bg = new Paint();
            bg.setColor(BONE);
            canvas.drawRect(0, 0, W, H, bg);
            y = M;
        }

        void need(float height) {
            if (y + height > H - M) newPage();
        }

        void heading(String text) {
            need(48);
            y += 18;
            canvas.drawText(text.toUpperCase(java.util.Locale.ROOT), M, y + 20, paint(display, 24, INK));
            y += 30;
            Paint line = paint(body, 1, SAND);
            line.setStrokeWidth(1.5f);
            canvas.drawLine(M, y, W - M, y, line);
            y += 10;
        }

        void field(String label, String value) {
            need(18);
            float top = y;
            canvas.drawText(label, M, y + 11, paint(bold, 10, MUTED));
            y = wrapAt(value, paint(body, 11, INK), M + 120, W - 2 * M - 120);
            y = Math.max(y, top + 16) + 2;
        }

        void change(String title, String sub, int points) {
            need(36);
            Paint pts = paint(display, 22, points > 0 ? MAROON : MUTED);
            String p = (points > 0 ? "+" : "") + points;
            canvas.drawText(p, W - M - pts.measureText(p), y + 20, pts);
            float top = y;
            y = wrapAt(title, paint(bold, 11, INK), M, W - 2 * M - 50);
            y = wrapAt(sub, paint(body, 9.5f, MUTED), M, W - 2 * M - 50);
            y = Math.max(y, top + 26) + 6;
        }

        void text(String text, int color) {
            y = wrapAt(text, paint(body, 11, color), M, W - 2 * M) + 4;
        }

        void wrap(String text, Paint p, float x, float width) {
            y = wrapAt(text, p, x, width);
        }

        /** Draws text wrapped to a width; returns the y below the last line. */
        float wrapAt(String text, Paint p, float x, float width) {
            float lineHeight = p.getTextSize() * 1.4f;
            for (String paragraph : text.split("\n")) {
                String rest = paragraph;
                while (!rest.isEmpty()) {
                    int n = p.breakText(rest, true, width, null);
                    if (n < rest.length()) {
                        int space = rest.lastIndexOf(' ', n);
                        if (space > 0) n = space;
                    }
                    need(lineHeight);
                    canvas.drawText(rest.substring(0, n).trim(), x, y + p.getTextSize(), p);
                    y += lineHeight;
                    rest = rest.substring(n).trim();
                }
            }
            return y;
        }

        void footer() {
            Paint p = paint(body, 9, MUTED);
            canvas.drawText("Can My Phone Be Trusted? · made on this phone, nothing was uploaded", M, H - 24, p);
            String n = "Page " + number;
            canvas.drawText(n, W - M - p.measureText(n), H - 24, p);
        }

        int finish() {
            footer();
            doc.finishPage(page);
            return number;
        }
    }
}
