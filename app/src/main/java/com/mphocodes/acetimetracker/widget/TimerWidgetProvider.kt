package com.mphocodes.acetimetracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mphocodes.acetimetracker.R
import com.mphocodes.acetimetracker.ui.activity.MainActivity

/** A launcher shortcut, never a second timer or an exported timer mutation endpoint. */
class TimerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val openTimer = PendingIntent.getActivity(
            context, 2201,
            Intent(context, MainActivity::class.java)
                .putExtra("open_tab", "Timer")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_timer)
            views.setOnClickPendingIntent(R.id.widget_open_timer, openTimer)
            manager.updateAppWidget(id, views)
        }
    }
}
