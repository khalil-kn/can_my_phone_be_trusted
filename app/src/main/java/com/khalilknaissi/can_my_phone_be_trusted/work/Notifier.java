package com.khalilknaissi.can_my_phone_be_trusted.work;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.khalilknaissi.can_my_phone_be_trusted.R;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.AlertEntity;
import com.khalilknaissi.can_my_phone_be_trusted.data.local.IncidentEntity;
import com.khalilknaissi.can_my_phone_be_trusted.ui.MainActivity;

/** Posts Android notifications for High and Critical incidents and the weekly summary. */
public final class Notifier {
    private Notifier() {}

    private static final String ALERTS = "alerts";
    private static final String SUMMARY = "summary";

    public static boolean allowed(Context context) {
        return Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    public static void incident(Context context, IncidentEntity inc) {
        Intent open = new Intent(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_INCIDENT, inc.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        post(context, ALERTS, (int) inc.id,
                inc.severity().label + ": " + inc.title,
                "Tap to see the changes behind it. " + inc.code(), open);
    }

    public static void summary(Context context, AlertEntity a) {
        Intent open = new Intent(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_CHANGES)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        post(context, SUMMARY, 0, a.title, a.body, open);
    }

    @SuppressLint("MissingPermission") // checked by allowed()
    private static void post(Context context, String channel, int id, String title, String text, Intent open) {
        if (!allowed(context)) return;
        createChannels(context);
        PendingIntent pi = PendingIntent.getActivity(context, id, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_stat_seal)
                .setColor(ContextCompat.getColor(context, R.color.seal_red))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(ALERTS.equals(channel) ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(context).notify(channel.hashCode() + id, b.build());
    }

    private static void createChannels(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        nm.createNotificationChannel(new NotificationChannel(ALERTS, "High and Critical incidents", NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(SUMMARY, "Weekly summary", NotificationManager.IMPORTANCE_DEFAULT));
    }
}
