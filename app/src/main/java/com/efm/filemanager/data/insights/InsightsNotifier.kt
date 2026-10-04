package com.efm.filemanager.data.insights

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.efm.filemanager.MainActivity
import com.efm.filemanager.R
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

internal const val INSIGHTS_NOTIFICATION_CHANNEL_ID = "daily_insights"
internal const val EXTRA_OPEN_DESTINATION = "open_destination"
private const val INSIGHTS_NOTIFICATION_ID = 1001

/**
 * Posts Phase 17's "N things worth a look" daily summary -- one notification per run, never
 * one per recommendation category, via its own dedicated channel so a user can mute it
 * independently of any other notification type this app might add later. The real
 * POST_NOTIFICATIONS runtime-permission *request* (with an in-app rationale) is Settings'
 * upcoming "Notify me" toggle (still open, see docs/PLAN.md Phase 17) -- until that lands,
 * this silently skips posting on API 33+ whenever the permission isn't already granted some
 * other way, same as any well-behaved notifier would.
 */
class InsightsNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun notifySummary(foundCount: Int) {
            if (foundCount <= 0) {
                Timber.i("InsightsNotifier: nothing worth a look today, skipping notification")
                return
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Timber.i("InsightsNotifier: POST_NOTIFICATIONS not granted, skipping notification for %d finding(s)", foundCount)
                return
            }
            ensureChannel()
            val notification =
                NotificationCompat.Builder(context, INSIGHTS_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(context.getString(R.string.insights_notification_title))
                    .setContentText(context.getString(R.string.insights_notification_body, foundCount))
                    .setContentIntent(openInsightsPendingIntent())
                    .setAutoCancel(true)
                    .build()
            Timber.i("InsightsNotifier: posting summary notification for %d finding(s)", foundCount)
            NotificationManagerCompat.from(context).notify(INSIGHTS_NOTIFICATION_ID, notification)
        }

        // minSdk is already 26 (O), so notification channels always exist here -- no SDK_INT
        // guard needed, unlike the version check most other apps still carry for this.
        private fun ensureChannel() {
            val channel =
                NotificationChannel(
                    INSIGHTS_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.insights_notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = context.getString(R.string.insights_notification_channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        private fun openInsightsPendingIntent(): PendingIntent {
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    // Must match EfmDestination.Insights.route (ui/nav/EfmDestination.kt) -- kept as
                    // a literal here since this data-layer class has no business importing ui.nav.
                    putExtra(EXTRA_OPEN_DESTINATION, "insights")
                }
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
    }
