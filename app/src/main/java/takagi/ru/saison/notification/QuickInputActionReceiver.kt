package takagi.ru.saison.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.RemoteInput
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import takagi.ru.saison.R
import takagi.ru.saison.domain.usecase.CreateQuickTaskUseCase
import javax.inject.Inject

/**
 * 快捷输入操作接收器
 * 处理用户从通知提交的输入内容
 */
@AndroidEntryPoint
class QuickInputActionReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var createQuickTaskUseCase: CreateQuickTaskUseCase
    
    @Inject
    lateinit var quickInputManager: QuickInputNotificationManager
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            QuickInputNotificationManager.ACTION_QUICK_INPUT -> {
                val pendingResult = goAsync()
                handleQuickInput(context, intent, pendingResult)
            }
        }
    }
    
    /**
     * 处理快捷输入
     */
    private fun handleQuickInput(
        context: Context,
        intent: Intent,
        pendingResult: BroadcastReceiver.PendingResult
    ) {
        val text = getTextFromIntent(intent)
        
        if (text.isNullOrBlank()) {
            showErrorToast(context, context.getString(R.string.quick_input_error_empty))
            quickInputManager.updateQuickInputNotification()
            pendingResult.finish()
            return
        }
        
        // 使用协程处理异步任务创建
        scope.launch {
            try {
                val result = createQuickTaskUseCase(text)

                result.onSuccess {
                    showSuccessToast(context, text)
                    quickInputManager.updateQuickInputNotification()
                }.onFailure { error ->
                    val errorMessage = when {
                        error.message?.contains("empty") == true ->
                            context.getString(R.string.quick_input_error_empty)
                        error.message?.contains("too long") == true ->
                            context.getString(R.string.quick_input_error_too_long)
                        else ->
                            context.getString(R.string.quick_input_error_generic)
                    }
                    showErrorToast(context, errorMessage)
                    quickInputManager.updateQuickInputNotification()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    /**
     * 从 RemoteInput 获取文本
     */
    private fun getTextFromIntent(intent: Intent): String? {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        return remoteInput?.getCharSequence(QuickInputNotificationManager.KEY_TEXT_REPLY)?.toString()
    }
    
    /**
     * 显示成功提示
     */
    private fun showSuccessToast(context: Context, taskTitle: String) {
        val message = context.getString(R.string.quick_input_success, taskTitle)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 显示错误提示
     */
    private fun showErrorToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
