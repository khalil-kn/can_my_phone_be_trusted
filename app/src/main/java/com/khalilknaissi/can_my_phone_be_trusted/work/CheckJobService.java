package com.khalilknaissi.can_my_phone_be_trusted.work;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import com.khalilknaissi.can_my_phone_be_trusted.data.local.AlertEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.CheckEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.IncidentEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.Prefs;
import com.khalilknaissi.can_my_phone_be_trusted.repository.SecurityRepository;

/** The automatic check. Android starts it; it runs the same check as the Check button. */
public class CheckJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        // JobService runs on the main thread, so the slow work goes on its own thread.
        // Inside this lambda, "this" is still the CheckJobService.
        new Thread(() -> {
            try {
                SecurityRepository repo = SecurityRepository.get(this);
                SecurityRepository.CheckResult result = repo.runCheck(CheckEntity.AUTO);
                Prefs prefs = Prefs.get(this);
                if (prefs.alertsOn()) {
                    for (IncidentEntity inc : result.opened) {
                        if (inc.severity().isHot()) Notifier.incident(this, inc);
                    }
                }
                if (prefs.weeklySummary()) {
                    AlertEntity summary = repo.weeklySummaryIfDue();
                    if (summary != null) Notifier.summary(this, summary);
                }
            } catch (Exception e) {
                Log.e("CheckJob", "Background check failed", e);
            } finally {
                jobFinished(params, false);
            }
        }).start();
        return true; // still working on the other thread
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true; // try again later
    }
}
