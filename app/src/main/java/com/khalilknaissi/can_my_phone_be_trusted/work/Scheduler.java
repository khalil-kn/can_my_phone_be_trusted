package com.khalilknaissi.can_my_phone_be_trusted.work;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

import com.khalilknaissi.can_my_phone_be_trusted.data.local.Prefs;

/**
 * Plans the automatic check every 12 hours with Android's own JobScheduler.
 * Android decides the exact moment (battery saving), so "every 12 hours" is approximate.
 */
public final class Scheduler {
    private Scheduler() {}

    private static final int JOB_ID = 12;
    private static final long EVERY_12_HOURS = 12L * 60 * 60 * 1000;

    /** Call after the baseline is sealed and whenever check settings change. */
    public static void apply(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js == null) return;
        Prefs prefs = Prefs.get(context);
        if (!prefs.autoCheck() || !prefs.onboardingDone()) {
            js.cancel(JOB_ID);
            return;
        }
        JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(context, CheckJobService.class))
                .setPeriodic(EVERY_12_HOURS)
                .setRequiresCharging(prefs.chargingOnly())
                .setPersisted(true) // survives a reboot (needs RECEIVE_BOOT_COMPLETED)
                .build();
        js.schedule(job);
    }
}
