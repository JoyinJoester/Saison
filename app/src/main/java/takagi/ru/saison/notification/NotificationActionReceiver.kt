package takagi.ru.saison.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import takagi.ru.saison.domain.model.notification.NotificationType
import javax.inject.Inject

/**
 * 通知操作广播接收器
 * 接收并处理通知中的用户操作
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var actionHandler: NotificationActionHandler
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        const val ACTION_MARK_COMPLETE = "takagi.ru.saison.ACTION_MARK_COMPLETE"
        const val ACTION_SNOOZE = "takagi.ru.saison.ACTION_SNOOZE"
        const val ACTION_VIEW_DETAILS = "takagi.ru.saison.ACTION_VIEW_DETAILS"
        
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_COURSE_ID = "extra_course_id"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_MARK_COMPLETE -> {
                        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
                        if (taskId != -1L) {
                            actionHandler.handleMarkAsComplete(taskId)
                        }
                    }

                    ACTION_SNOOZE -> {
                        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
                        if (taskId != -1L) {
                            actionHandler.handleSnooze(taskId)
                        }
                    }

                    ACTION_VIEW_DETAILS -> {
                        val itemId = intent.getLongExtra(
                            EXTRA_ITEM_ID,
                            intent.getLongExtra(EXTRA_TASK_ID, intent.getLongExtra(EXTRA_COURSE_ID, -1))
                        )
                        val typeOrdinal = intent.getIntExtra(EXTRA_NOTIFICATION_TYPE, -1)
                        val type = NotificationType.values().getOrNull(typeOrdinal)
                        if (itemId != -1L && type != null) {
                            actionHandler.handleViewDetails(itemId, type)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
