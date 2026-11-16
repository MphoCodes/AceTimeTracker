package com.mphocodes.acetimetracker.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.mphocodes.acetimetracker.data.sync.OfflineData
import com.mphocodes.acetimetracker.service.TimerForegroundService
import java.io.File

/** A committed acceptance marker makes interrupted device cleanup retryable on launch. */
object AccountLocalCleanup {
    fun recover(context: Context) {
        context.getSharedPreferences("account_deletion", Context.MODE_PRIVATE)
            .getString("accepted_uid", null)?.let { erase(context, it) {} }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun erase(context: Context, uid: String, callback: (Boolean) -> Unit) {
        val marker = context.getSharedPreferences("account_deletion", Context.MODE_PRIVATE)
        if (!marker.edit().putString("accepted_uid", uid).commit()) { callback(false); return }
        TimerForegroundService.stop(context)
        CleanupTransaction.run(
            steps = listOf(
                { context.getSharedPreferences("timer_drafts", Context.MODE_PRIVATE).edit()
                    .remove(uid).remove("writer:$uid").commit() },
                { context.getSharedPreferences("favorite_categories", Context.MODE_PRIVATE).edit()
                    .remove("ids_$uid").commit() },
                { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit()
                    .putBoolean("reminder_enabled", false).commit().also {
                        com.mphocodes.acetimetracker.reminder.ReminderScheduler.reschedule(context)
                    } },
                { File(context.cacheDir, "reports").let { !it.exists() || it.deleteRecursively() } },
                { coil.Coil.imageLoader(context).memoryCache?.clear()
                    coil.Coil.imageLoader(context).diskCache?.clear()
                    File(context.cacheDir, "image_cache").let { !it.exists() || it.deleteRecursively() } }
            ),
            purgeRecords = { done -> OfflineData.get(FirebaseFirestore.getInstance()).eraseAccount(uid, done) },
            acknowledge = {
                if (AuthRepository().currentUserId == uid) AuthRepository().logout()
                marker.edit().remove("accepted_uid").commit()
            }, callback = callback
        )
    }
}
