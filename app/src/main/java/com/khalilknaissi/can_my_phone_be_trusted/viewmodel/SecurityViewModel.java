package com.khalilknaissi.can_my_phone_be_trusted.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.khalilknaissi.can_my_phone_be_trusted.data.local.AlertEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.ChangeEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.CheckEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.IncidentEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.ReportEntity;
import com.khalilknaissi.can_my_phone_be_trusted.model.IncidentState;
import com.khalilknaissi.can_my_phone_be_trusted.repository.SecurityRepository;
import com.khalilknaissi.can_my_phone_be_trusted.work.Scheduler;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The only door between the screens and the data. Screens call a method here,
 * the work runs on a background thread, and the result comes back through LiveData,
 * which the screen observes. It survives screen rotation.
 */
public class SecurityViewModel extends AndroidViewModel {

    private final SecurityRepository repo;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> message = new MutableLiveData<>();
    /** Bumped after anything changes, so open screens know to reload. */
    private final MutableLiveData<Long> refresh = new MutableLiveData<>(0L);

    private final MutableLiveData<SecurityRepository.Status> status = new MutableLiveData<>();
    private final MutableLiveData<int[]> sealProgress = new MutableLiveData<>();
    private final MutableLiveData<String> sealCurrent = new MutableLiveData<>();
    private final MutableLiveData<SecurityRepository.SealSummary> sealed = new MutableLiveData<>();
    private final MutableLiveData<List<ChangeEntity>> changes = new MutableLiveData<>();
    private final MutableLiveData<int[]> changeCounts = new MutableLiveData<>();
    private final MutableLiveData<SecurityRepository.ChangeDetail> change = new MutableLiveData<>();
    private final MutableLiveData<SecurityRepository.AppDetail> app = new MutableLiveData<>();
    private final MutableLiveData<List<IncidentEntity>> incidents = new MutableLiveData<>();
    private final MutableLiveData<SecurityRepository.IncidentDetail> incident = new MutableLiveData<>();
    private final MutableLiveData<List<SecurityRepository.TimelineItem>> timeline = new MutableLiveData<>();
    private final MutableLiveData<List<ReportEntity>> reports = new MutableLiveData<>();
    private final MutableLiveData<ReportEntity> createdReport = new MutableLiveData<>();
    private final MutableLiveData<List<AlertEntity>> alerts = new MutableLiveData<>();
    private final MutableLiveData<SecurityRepository.DeviceInfo> device = new MutableLiveData<>();
    private final MutableLiveData<Event<File>> exported = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> deleted = new MutableLiveData<>();

    public SecurityViewModel(@NonNull Application app) {
        super(app);
        repo = SecurityRepository.get(app);
    }

    // ---- what screens observe
    public LiveData<Boolean> busy() { return busy; }
    public LiveData<Event<String>> message() { return message; }
    public LiveData<Long> refresh() { return refresh; }
    public LiveData<SecurityRepository.Status> status() { return status; }
    public LiveData<int[]> sealProgress() { return sealProgress; }
    public LiveData<String> sealCurrent() { return sealCurrent; }
    public LiveData<SecurityRepository.SealSummary> sealed() { return sealed; }
    public LiveData<List<ChangeEntity>> changes() { return changes; }
    public LiveData<int[]> changeCounts() { return changeCounts; }
    public LiveData<SecurityRepository.ChangeDetail> change() { return change; }
    public LiveData<SecurityRepository.AppDetail> app() { return app; }
    public LiveData<List<IncidentEntity>> incidents() { return incidents; }
    public LiveData<SecurityRepository.IncidentDetail> incident() { return incident; }
    public LiveData<List<SecurityRepository.TimelineItem>> timeline() { return timeline; }
    public LiveData<List<ReportEntity>> reports() { return reports; }
    public LiveData<ReportEntity> createdReport() { return createdReport; }
    public LiveData<List<AlertEntity>> alerts() { return alerts; }
    public LiveData<SecurityRepository.DeviceInfo> device() { return device; }
    public LiveData<Event<File>> exported() { return exported; }
    public LiveData<Event<Boolean>> deleted() { return deleted; }

    // ---- loading
    public void loadStatus() { load(status, repo::status); }
    public void loadChanges(boolean unreviewedOnly) {
        load(changes, () -> repo.changes(unreviewedOnly));
        load(changeCounts, repo::changeCounts);
    }
    public void loadChange(long id) { load(change, () -> repo.change(id)); }
    public void loadApp(String packageName) { load(app, () -> repo.app(packageName)); }
    public void loadIncidents() { load(incidents, repo::incidents); }
    public void loadIncident(long id) { load(incident, () -> repo.incident(id)); }
    public void loadTimeline() { load(timeline, repo::timeline); }
    public void loadReports() { load(reports, repo::reports); }
    public void loadAlerts() { load(alerts, repo::alerts); }
    public void loadDevice() { load(device, repo::deviceInfo); }

    // ---- actions
    public void check() {
        busy.setValue(true);
        io.execute(() -> {
            try {
                SecurityRepository.CheckResult r = repo.runCheck(CheckEntity.MANUAL);
                String text = r.newChanges == 0 ? "Check done. Nothing new." :
                        "Check done. " + r.newChanges + " new " + (r.newChanges == 1 ? "change" : "changes") + ".";
                if (r.undone > 0) text += " " + r.undone + " undone.";
                if (!r.opened.isEmpty()) text += " New incident: " + r.opened.get(0).code() + ".";
                message.postValue(new Event<>(text));
                changed();
            } catch (Exception e) {
                message.postValue(new Event<>("The check failed: " + e.getMessage()));
            } finally {
                busy.postValue(false);
            }
        });
    }

    /** "Check again" inside an incident: checks, then reloads that incident to show what was undone. */
    public void recheck(long incidentId) {
        busy.setValue(true);
        io.execute(() -> {
            repo.runCheck(CheckEntity.MANUAL);
            SecurityRepository.IncidentDetail d = repo.incident(incidentId);
            incident.postValue(d);
            int total = d.changes.size();
            message.postValue(new Event<>(d.undone == total ? "All " + total + " changes undone."
                    : d.undone + " of " + total + " changes undone. " + (total - d.undone) + " still there."));
            busy.postValue(false);
            changed();
        });
    }

    public void seal() {
        busy.setValue(true);
        io.execute(() -> {
            SecurityRepository.SealSummary s = repo.sealBaseline((done, total, pkg) -> {
                sealProgress.postValue(new int[]{done, total});
                sealCurrent.postValue(pkg);
            });
            Scheduler.apply(getApplication());
            sealed.postValue(s);
            busy.postValue(false);
            changed();
        });
    }

    public void markReviewed(long changeId) {
        run(() -> repo.markReviewed(changeId), () -> loadChange(changeId));
    }

    public void setIncidentState(long id, IncidentState state) {
        run(() -> repo.setIncidentState(id, state), () -> loadIncident(id));
    }

    public void toggleStep(long id, String key) {
        run(() -> repo.toggleStep(id, key), () -> loadIncident(id));
    }

    public void saveNotes(long id, String notes) {
        run(() -> repo.saveNotes(id, notes), null);
    }

    public void createReport(long incidentId, boolean pdf) {
        busy.setValue(true);
        io.execute(() -> {
            try {
                createdReport.postValue(repo.createReport(incidentId, pdf));
                changed();
            } catch (Exception e) {
                message.postValue(new Event<>("Couldn't create the report: " + e.getMessage()));
            } finally {
                busy.postValue(false);
            }
        });
    }

    public void markAlertsRead() {
        run(repo::markAlertsRead, this::loadAlerts);
    }

    public void exportEverything() {
        io.execute(() -> {
            try {
                exported.postValue(new Event<>(repo.exportEverything()));
            } catch (Exception e) {
                message.postValue(new Event<>("Export failed: " + e.getMessage()));
            }
        });
    }

    public void deleteEverything() {
        io.execute(() -> {
            repo.deleteEverything();
            Scheduler.apply(getApplication());
            deleted.postValue(new Event<>(true));
        });
    }

    // ---- helpers

    /** Runs work in the background and posts its result into a LiveData. */
    private <T> void load(MutableLiveData<T> target, Callable<T> work) {
        io.execute(() -> {
            try {
                target.postValue(work.call());
            } catch (Exception e) {
                message.postValue(new Event<>("Something went wrong: " + e.getMessage()));
            }
        });
    }

    private void run(Runnable work, Runnable then) {
        io.execute(() -> {
            work.run();
            changed();
            if (then != null) then.run();
        });
    }

    private void changed() {
        Long v = refresh.getValue();
        refresh.postValue((v == null ? 0 : v) + 1);
    }

    @Override
    protected void onCleared() {
        io.shutdown();
    }
}
