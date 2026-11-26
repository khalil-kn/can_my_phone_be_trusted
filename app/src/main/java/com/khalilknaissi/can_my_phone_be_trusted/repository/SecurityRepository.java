package com.khalilknaissi.can_my_phone_be_trusted.repository;

import android.content.Context;
import android.os.Build;

import com.khalilknaissi.can_my_phone_be_trusted.data.local.AlertEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.AppDatabase;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.AppSnapshotDao;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.AppSnapshotEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.ChangeEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.CheckEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.IncidentEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.Prefs;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.ReportEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.SecurityDao;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.SettingSnapshotEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.source.DeviceScanner;
import com.khalilknaissi.can_my_phone_be_trusted.engine.ChangeDetector;
import com.khalilknaissi.can_my_phone_be_trusted.engine.Correlator;
import com.khalilknaissi.can_my_phone_be_trusted.engine.Playbook;
import com.khalilknaissi.can_my_phone_be_trusted.engine.RiskRules;
import com.khalilknaissi.can_my_phone_be_trusted.model.AppState;
import com.khalilknaissi.can_my_phone_be_trusted.model.ChangeKind;
import com.khalilknaissi.can_my_phone_be_trusted.model.DeviceState;
import com.khalilknaissi.can_my_phone_be_trusted.model.Finding;
import com.khalilknaissi.can_my_phone_be_trusted.model.IncidentState;
import com.khalilknaissi.can_my_phone_be_trusted.model.Severity;
import com.khalilknaissi.can_my_phone_be_trusted.report.ReportWriter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The single source of truth. Screens and the background job never touch the database
 * or Android's package manager directly; they ask this class.
 *
 * Every method here is slow (disk or package scans) and must run on a background thread.
 */
public class SecurityRepository {

    private static volatile SecurityRepository INSTANCE;

    public static SecurityRepository get(Context context) {
        if (INSTANCE == null) {
            synchronized (SecurityRepository.class) {
                if (INSTANCE == null) INSTANCE = new SecurityRepository(context.getApplicationContext());
            }
        }
        return INSTANCE;
    }

    private final Context context;
    private final AppDatabase db;
    private final AppSnapshotDao baseline;
    private final SecurityDao dao;
    private final DeviceScanner scanner;
    private final Prefs prefs;

    private SecurityRepository(Context context) {
        this.context = context;
        this.db = AppDatabase.getInstance(context);
        this.baseline = db.appSnapshotDao();
        this.dao = db.securityDao();
        this.scanner = new DeviceScanner(context);
        this.prefs = Prefs.get(context);
    }

    // =====================================================================
    // Stage 1: Baseline
    // =====================================================================

    public boolean hasBaseline() {
        return baseline.count() > 0;
    }

    /** Scans the phone and makes the result the new normal. */
    public synchronized SealSummary sealBaseline(DeviceScanner.Progress progress) {
        final DeviceState state = scanner.scan(progress);
        final long now = state.takenAt;
        db.runInTransaction(() -> {
            baseline.deleteAll();
            baseline.insertAll(toEntities(state));
            dao.deleteSettings();
            dao.insertSettings(toSettings(state));
            dao.acceptAll(now);
            CheckEntity c = new CheckEntity();
            c.ranAt = now;
            c.trigger = CheckEntity.BASELINE;
            c.appCount = state.apps.size();
            dao.insertCheck(c);
        });
        prefs.setBaselineAt(now);
        prefs.setLastCheckAt(now);

        SealSummary s = new SealSummary();
        s.sealedAt = now;
        s.apps = state.apps.size();
        for (AppState a : state.apps.values()) if (!a.system) s.userApps++;
        s.permissions = state.permissionCount();
        s.settings = state.settings.size();
        s.android = Build.VERSION.RELEASE;
        s.patch = Build.VERSION.SECURITY_PATCH;
        return s;
    }

    private DeviceState loadBaseline() {
        Map<String, AppState> apps = new TreeMap<>();
        for (AppSnapshotEntity e : baseline.getAll()) {
            apps.put(e.packageName, new AppState(e.packageName, e.appLabel, e.versionName == null ? "" : e.versionName,
                    e.versionCode, e.isSystemApp, e.installer, e.firstInstallTime, e.lastUpdateTime,
                    e.signer == null ? "" : e.signer, split(e.permissions), split(e.specialAccess)));
        }
        Map<String, String> settings = new HashMap<>();
        for (SettingSnapshotEntity s : dao.settings()) settings.put(s.key, s.value);
        return new DeviceState(apps, settings, prefs.baselineAt());
    }

    private static List<AppSnapshotEntity> toEntities(DeviceState s) {
        List<AppSnapshotEntity> out = new ArrayList<>();
        for (AppState a : s.apps.values()) {
            AppSnapshotEntity e = new AppSnapshotEntity(a.packageName, a.label, a.versionName, a.versionCode, a.system, s.takenAt);
            e.installer = a.installer;
            e.firstInstallTime = a.firstInstallTime;
            e.lastUpdateTime = a.lastUpdateTime;
            e.signer = a.signer;
            e.permissions = join(a.permissions);
            e.specialAccess = join(a.specialAccess);
            out.add(e);
        }
        return out;
    }

    private static List<SettingSnapshotEntity> toSettings(DeviceState s) {
        List<SettingSnapshotEntity> out = new ArrayList<>();
        for (Map.Entry<String, String> e : s.settings.entrySet()) {
            SettingSnapshotEntity row = new SettingSnapshotEntity();
            row.key = e.getKey();
            row.value = e.getValue();
            out.add(row);
        }
        return out;
    }

    // =====================================================================
    // Stages 2 and 3: Snapshot & Compare, then Correlate & Score
    // =====================================================================

    /**
     * Compares the phone with the baseline. New differences become changes; changes that
     * disappeared are marked undone; risky new changes are grouped into incidents.
     */
    public synchronized CheckResult runCheck(String trigger) {
        final CheckResult result = new CheckResult();
        if (!hasBaseline()) return result;

        DeviceState base = loadBaseline();
        final DeviceState now = scanner.scan(null);
        final List<Finding> findings = ChangeDetector.diff(base, now);
        final long t = now.takenAt;
        long previous = prefs.lastCheckAt();
        if (previous <= 0 || previous > t) previous = prefs.baselineAt();
        final long windowStart = previous;

        db.runInTransaction(() -> {
            Map<String, ChangeEntity> active = new HashMap<>();
            for (ChangeEntity c : dao.activeChanges()) active.put(c.fingerprint, c);

            List<Finding> fresh = new ArrayList<>();
            Map<String, Long> newIds = new HashMap<>();
            for (Finding f : findings) {
                ChangeEntity known = active.remove(f.key());
                if (known != null) {
                    // Same change as before; keep its first-seen time, refresh its values.
                    if (!f.newValue.equals(known.newValue) || known.points != f.points) {
                        known.newValue = f.newValue;
                        known.points = f.points;
                        dao.updateChange(known);
                    }
                    continue;
                }
                ChangeEntity c = ChangeEntity.from(f, t, windowStart);
                newIds.put(f.key(), dao.insertChange(c));
                fresh.add(f);
            }
            // Whatever is left was there last time but not any more: back to the baseline value.
            for (ChangeEntity gone : active.values()) {
                gone.revertedAt = t;
                dao.updateChange(gone);
                result.undone++;
            }

            for (Correlator.Group g : Correlator.group(fresh)) {
                IncidentEntity inc = dao.activeIncidentFor(g.packageName);
                if (inc == null) {
                    if (g.score() < Correlator.MIN_SCORE) continue;
                    inc = new IncidentEntity();
                    inc.title = g.title();
                    inc.primaryPackage = g.packageName;
                    inc.appLabel = g.appLabel;
                    inc.state = IncidentState.OPEN.name();
                    inc.createdAt = t;
                    inc.id = dao.insertIncident(inc);
                    result.opened.add(inc);
                }
                for (Finding f : g.findings) {
                    Long id = newIds.get(f.key());
                    if (id != null) dao.linkChange(id, inc.id);
                }
                inc.score = Math.min(100, dao.pointsFor(inc.id));
                inc.updatedAt = t;
                dao.updateIncident(inc);
            }

            for (IncidentEntity inc : result.opened) {
                if (!inc.severity().isHot()) continue;
                AlertEntity a = new AlertEntity();
                a.at = t;
                a.title = inc.severity().label + " incident opened";
                a.body = inc.title + ".";
                a.incidentId = inc.id;
                dao.insertAlert(a);
            }

            CheckEntity c = new CheckEntity();
            c.ranAt = t;
            c.trigger = trigger;
            c.appCount = now.apps.size();
            c.newChanges = fresh.size();
            dao.insertCheck(c);
            result.newChanges = fresh.size();
        });
        prefs.setLastCheckAt(t);
        return result;
    }

    /** A Sunday summary of the past week, or null if one was sent in the last 6 days. */
    public AlertEntity weeklySummaryIfDue() {
        long now = System.currentTimeMillis();
        if (now - prefs.lastSummaryAt() < 6L * 24 * 3600 * 1000) return null;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SUNDAY) return null;
        int changes = dao.changesSinceCount(now - 7L * 24 * 3600 * 1000);
        AlertEntity a = new AlertEntity();
        a.at = now;
        a.title = "Sunday summary";
        a.body = changes == 0 ? "No changes since last week." : changes + (changes == 1 ? " change" : " changes") + " last week.";
        dao.insertAlert(a);
        prefs.setLastSummaryAt(now);
        return a;
    }

    // =====================================================================
    // Reading data for the screens
    // =====================================================================

    public Status status() {
        Status s = new Status();
        s.hasBaseline = hasBaseline();
        s.deviceName = prefs.deviceName();
        s.baselineAt = prefs.baselineAt();
        s.lastCheckAt = prefs.lastCheckAt();
        s.unreadAlerts = dao.unreadAlerts();
        if (!s.hasBaseline) return s;

        List<IncidentEntity> active = dao.activeIncidents();
        s.openIncidents = active.size();
        if (!active.isEmpty()) {
            s.topIncident = active.get(0);
            s.topLinked = dao.changesFor(s.topIncident.id).size();
        } else {
            s.lastClosed = dao.lastClosedIncident();
        }
        for (ChangeEntity c : dao.activeChanges()) {
            s.activeChanges++;
            if (!c.reviewed) s.unreviewed++;
            switch (c.kind().category) {
                case APPS: s.appsChanged++; break;
                case PERMISSIONS: s.permissionsChanged++; break;
                default: s.settingsChanged++; break;
            }
        }
        s.apps = baseline.count();
        for (AppSnapshotEntity e : baseline.getAll()) {
            s.permissions += split(e.permissions).size() + split(e.specialAccess).size();
        }
        s.settings = dao.settings().size();
        return s;
    }

    public List<ChangeEntity> changes(boolean unreviewedOnly) {
        return unreviewedOnly ? dao.unreviewedChanges() : dao.changesSince(prefs.baselineAt());
    }

    public int[] changeCounts() {
        return new int[]{dao.unreviewedChanges().size(), dao.changesSince(prefs.baselineAt()).size()};
    }

    public ChangeDetail change(long id) {
        ChangeDetail d = new ChangeDetail();
        d.change = dao.change(id);
        if (d.change == null) return d;
        d.baselineAt = prefs.baselineAt();
        if (d.change.incidentId > 0) d.incident = dao.incident(d.change.incidentId);
        d.why = RiskRules.why(d.change.kind(), d.change.subject);
        return d;
    }

    public AppDetail app(String packageName) {
        AppDetail d = new AppDetail();
        d.packageName = packageName;
        d.now = scanner.readApp(packageName);
        for (AppSnapshotEntity e : baseline.getAll()) {
            if (e.packageName.equals(packageName)) {
                d.inBaseline = true;
                d.baselineSigner = e.signer;
                if (d.now == null) {
                    d.now = new AppState(e.packageName, e.appLabel, e.versionName, e.versionCode, e.isSystemApp,
                            e.installer, e.firstInstallTime, e.lastUpdateTime, e.signer == null ? "" : e.signer,
                            split(e.permissions), split(e.specialAccess));
                    d.removed = true;
                }
            }
        }
        d.hasUsageAccess = scanner.hasUsageAccess();
        if (!d.removed) d.lastOpened = scanner.lastOpened(packageName);
        return d;
    }

    public List<IncidentEntity> incidents() {
        return dao.incidents();
    }

    public IncidentDetail incident(long id) {
        IncidentDetail d = new IncidentDetail();
        d.incident = dao.incident(id);
        if (d.incident == null) return d;
        d.changes = dao.changesFor(id);
        List<Finding> findings = new ArrayList<>();
        for (ChangeEntity c : d.changes) {
            findings.add(c.toFinding());
            if (c.isReverted()) d.undone++;
            d.windowStart = d.windowStart == 0 ? c.windowStart : Math.min(d.windowStart, c.windowStart);
            d.windowEnd = Math.max(d.windowEnd, c.windowEnd);
        }
        d.steps = Playbook.steps(findings);
        d.done = new LinkedHashSet<>(split(d.incident.stepsDone));
        return d;
    }

    public void setIncidentState(long id, IncidentState state) {
        IncidentEntity inc = dao.incident(id);
        if (inc == null) return;
        inc.state = state.name();
        inc.updatedAt = System.currentTimeMillis();
        inc.closedAt = state.isActive() ? 0 : inc.updatedAt;
        dao.updateIncident(inc);
        if (!state.isActive()) dao.markIncidentReviewed(id);
    }

    public void saveNotes(long id, String notes) {
        IncidentEntity inc = dao.incident(id);
        if (inc == null || notes.equals(inc.notes == null ? "" : inc.notes)) return;
        inc.notes = notes;
        if (IncidentState.OPEN.name().equals(inc.state)) inc.state = IncidentState.INVESTIGATING.name();
        dao.updateIncident(inc);
    }

    public void toggleStep(long id, String key) {
        IncidentEntity inc = dao.incident(id);
        if (inc == null) return;
        Set<String> done = new LinkedHashSet<>(split(inc.stepsDone));
        if (!done.remove(key)) done.add(key);
        inc.stepsDone = join(done);
        if (IncidentState.OPEN.name().equals(inc.state)) inc.state = IncidentState.INVESTIGATING.name();
        dao.updateIncident(inc);
    }

    public void markReviewed(long changeId) {
        dao.markReviewed(changeId);
    }

    public List<TimelineItem> timeline() {
        List<TimelineItem> items = new ArrayList<>();
        for (ChangeEntity c : dao.recentChanges()) {
            TimelineItem i = new TimelineItem();
            i.changeId = c.id;
            i.incidentId = c.incidentId;
            i.category = c.kind().category;
            i.exact = c.exactAt > 0;
            i.time = i.exact ? c.exactAt : c.windowEnd;
            i.windowStart = c.windowStart;
            i.title = c.isSetting() ? RiskRules.settingLabel(c.subject) + " " + c.oldValue + " → " + c.newValue : c.title();
            i.sub = c.source() + (c.isReverted() ? " · undone" : "");
            i.severity = c.severity();
            items.add(i);
        }
        for (CheckEntity c : dao.checks()) {
            TimelineItem i = new TimelineItem();
            i.check = true;
            i.time = c.ranAt;
            i.title = CheckEntity.BASELINE.equals(c.trigger) ? "Baseline sealed"
                    : (CheckEntity.AUTO.equals(c.trigger) ? "Background check" : "Check");
            i.sub = CheckEntity.BASELINE.equals(c.trigger) ? c.appCount + " apps recorded"
                    : c.newChanges == 0 ? "Nothing new" : c.newChanges + " new " + (c.newChanges == 1 ? "change" : "changes");
            items.add(i);
        }
        Collections.sort(items, (a, b) -> Long.compare(b.time, a.time));
        return items;
    }

    public List<ReportEntity> reports() {
        return dao.reports();
    }

    public List<AlertEntity> alerts() {
        return dao.alertsSince(System.currentTimeMillis() - 30L * 24 * 3600 * 1000);
    }

    public void markAlertsRead() {
        dao.markAlertsRead();
    }

    public DeviceInfo deviceInfo() {
        DeviceInfo d = new DeviceInfo();
        d.name = prefs.deviceName();
        d.owner = prefs.ownerName();
        d.model = Build.MANUFACTURER + " " + Build.MODEL;
        d.android = Build.VERSION.RELEASE;
        d.patch = Build.VERSION.SECURITY_PATCH;
        d.checks = dao.checkCount();
        d.changes = dao.changeCount();
        d.incidents = dao.incidents().size();
        d.reports = dao.reports().size();
        File dbFile = context.getDatabasePath("can_my_phone_be_trusted.db");
        d.bytes = dbFile.length() + folderSize(reportsDir());
        return d;
    }

    // =====================================================================
    // Stage 5: Report
    // =====================================================================

    /** Writes a PDF or JSON report about one incident, or about everything since the baseline (incidentId 0). */
    public ReportEntity createReport(long incidentId, boolean pdf) throws Exception {
        ReportWriter.Data data = reportData(incidentId);
        ReportEntity r = new ReportEntity();
        r.createdAt = data.createdAt;
        r.incidentId = incidentId;
        r.format = pdf ? ReportEntity.PDF : ReportEntity.JSON;
        r.title = data.incident != null ? data.incident.code() + " · " + shortTitle(data.incident) : "Since baseline · " + data.changes.size() + " changes";
        String name = (data.incident != null ? data.incident.code() : "baseline") + "_"
                + android.text.format.DateFormat.format("yyyyMMdd_HHmm", data.createdAt) + (pdf ? ".pdf" : ".json");
        File file = new File(reportsDir(), name);
        r.pages = pdf ? ReportWriter.writePdf(context, data, file) : ReportWriter.writeJson(data, file, false);
        r.path = file.getAbsolutePath();
        r.sizeBytes = file.length();
        r.id = dao.insertReport(r);
        return r;
    }

    /** Everything the app knows, as one JSON file, for the "Export everything" button. */
    public File exportEverything() throws Exception {
        ReportWriter.Data data = reportData(0);
        data.allChanges = dao.recentChanges();
        data.allIncidents = dao.incidents();
        data.checks = dao.checks();
        data.baselineApps = baseline.getAll();
        File file = new File(reportsDir(), "export_" + android.text.format.DateFormat.format("yyyyMMdd_HHmm", data.createdAt) + ".json");
        ReportWriter.writeJson(data, file, true);
        return file;
    }

    private ReportWriter.Data reportData(long incidentId) {
        ReportWriter.Data d = new ReportWriter.Data();
        d.createdAt = System.currentTimeMillis();
        d.deviceName = prefs.deviceName();
        d.owner = prefs.ownerName();
        d.model = Build.MANUFACTURER + " " + Build.MODEL;
        d.android = Build.VERSION.RELEASE;
        d.patch = Build.VERSION.SECURITY_PATCH;
        d.baselineAt = prefs.baselineAt();
        d.baselineAppCount = baseline.count();
        if (incidentId > 0) {
            IncidentDetail detail = incident(incidentId);
            d.incident = detail.incident;
            d.changes = detail.changes;
            d.steps = detail.steps;
            d.stepsDone = detail.done;
        } else {
            d.changes = dao.changesSince(prefs.baselineAt());
        }
        return d;
    }

    private static String shortTitle(IncidentEntity i) {
        return i.appLabel != null ? i.appLabel : "Settings";
    }

    public File reportsDir() {
        File dir = new File(context.getFilesDir(), "reports");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** "Delete all data": database, reports and choices. The app starts again from onboarding. */
    public void deleteEverything() {
        db.clearAllTables();
        File[] files = reportsDir().listFiles();
        if (files != null) for (File f : files) f.delete();
        prefs.clear();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static Set<String> split(String csv) {
        Set<String> out = new TreeSet<>();
        if (csv != null && !csv.isEmpty()) out.addAll(Arrays.asList(csv.split(",")));
        return out;
    }

    private static String join(Set<String> set) {
        StringBuilder b = new StringBuilder();
        for (String s : set) {
            if (b.length() > 0) b.append(',');
            b.append(s);
        }
        return b.toString();
    }

    private static long folderSize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) size += f.length();
        return size;
    }

    // =====================================================================
    // What the screens receive
    // =====================================================================

    public static class CheckResult {
        public int newChanges;
        public int undone;
        public final List<IncidentEntity> opened = new ArrayList<>();
    }

    public static class SealSummary {
        public long sealedAt;
        public int apps, userApps, permissions, settings;
        public String android, patch;
    }

    public static class Status {
        public boolean hasBaseline;
        public String deviceName;
        public long baselineAt, lastCheckAt;
        public IncidentEntity topIncident, lastClosed;
        public int topLinked, openIncidents, activeChanges, unreviewed, unreadAlerts;
        public int apps, permissions, settings;
        public int appsChanged, permissionsChanged, settingsChanged;
    }

    public static class ChangeDetail {
        public ChangeEntity change;
        public IncidentEntity incident;
        public String why;
        public long baselineAt;
    }

    public static class AppDetail {
        public String packageName;
        public AppState now;
        public boolean inBaseline, removed, hasUsageAccess;
        public String baselineSigner;
        public long lastOpened;
    }

    public static class IncidentDetail {
        public IncidentEntity incident;
        public List<ChangeEntity> changes = new ArrayList<>();
        public List<Playbook.Step> steps = new ArrayList<>();
        public Set<String> done = new LinkedHashSet<>();
        public int undone;
        public long windowStart, windowEnd;
    }

    public static class TimelineItem {
        public long time, windowStart, changeId, incidentId;
        public boolean exact, check;
        public String title, sub;
        public ChangeKind.Category category;
        public Severity severity;
    }

    public static class DeviceInfo {
        public String name, owner, model, android, patch;
        public int checks, changes, incidents, reports;
        public long bytes;
    }
}
