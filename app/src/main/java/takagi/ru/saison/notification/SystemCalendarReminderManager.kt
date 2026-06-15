package takagi.ru.saison.notification

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import takagi.ru.saison.domain.model.Task
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemCalendarReminderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SystemCalendarReminder"
        private const val EVENT_DURATION_MINUTES = 30L
        private const val MARKER_PREFIX = "SAISON_TASK_REMINDER:"
    }

    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun upsertTaskReminder(task: Task) {
        if (!hasCalendarPermission()) return

        val reminderTime = task.reminderTime
        if (reminderTime == null || task.isCompleted) {
            deleteTaskReminder(task.id)
            return
        }

        runCatching {
            val calendarId = findWritableCalendarId() ?: return
            deleteTaskReminder(task.id)

            val startMillis = reminderTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val endMillis = startMillis + EVENT_DURATION_MINUTES * 60_000L
            val marker = markerForTask(task.id)

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, "Saison 提醒：${task.title}")
                put(CalendarContract.Events.DESCRIPTION, buildDescription(marker, task))
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
                put(CalendarContract.Events.HAS_ALARM, 1)
                put(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
                put(CalendarContract.Events.CUSTOM_APP_URI, "saison://task/${task.id}")
            }

            val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = eventUri?.lastPathSegment?.toLongOrNull() ?: return

            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, 0)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            Log.d(TAG, "System calendar reminder synced for task ${task.id}")
        }.onFailure { error ->
            Log.e(TAG, "Failed to sync system calendar reminder for task ${task.id}", error)
        }
    }

    fun deleteTaskReminder(taskId: Long) {
        if (!hasCalendarPermission()) return

        runCatching {
            val marker = "%${markerForTask(taskId)}%"
            val projection = arrayOf(CalendarContract.Events._ID)
            val selection = "${CalendarContract.Events.DESCRIPTION} LIKE ?"

            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                arrayOf(marker),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                while (cursor.moveToNext()) {
                    val eventId = cursor.getLong(idIndex)
                    val eventUri = ContentUris.withAppendedId(
                        CalendarContract.Events.CONTENT_URI,
                        eventId
                    )
                    context.contentResolver.delete(eventUri, null, null)
                }
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to delete system calendar reminder for task $taskId", error)
        }
    }

    private fun findWritableCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.IS_PRIMARY
        )
        val selection =
            "${CalendarContract.Calendars.VISIBLE} = 1 AND " +
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val primaryIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)

            var fallbackId: Long? = null
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                if (fallbackId == null) fallbackId = id
                if (cursor.getInt(primaryIndex) == 1) {
                    return id
                }
            }
            return fallbackId
        }

        return null
    }

    private fun buildDescription(marker: String, task: Task): String {
        return buildString {
            appendLine(marker)
            appendLine("由 Saison 创建，用于系统日历提醒。")
            task.description?.takeIf { it.isNotBlank() }?.let {
                appendLine()
                append(it)
            }
        }
    }

    private fun markerForTask(taskId: Long): String = "$MARKER_PREFIX$taskId"
}
