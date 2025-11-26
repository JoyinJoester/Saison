package takagi.ru.saison

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import takagi.ru.saison.data.repository.DefaultSemesterInitializer
import takagi.ru.saison.ui.widget.CourseWidgetScheduler
import javax.inject.Inject

@HiltAndroidApp
class SaisonApplication : Application() {
    
    @Inject
    lateinit var widgetScheduler: CourseWidgetScheduler
    
    @Inject
    lateinit var defaultSemesterInitializer: DefaultSemesterInitializer
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val TAG = "SaisonApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 在后台线程确保默认学期存在
        // Requirements: 2.1, 2.2, 2.3, 2.4
        applicationScope.launch {
            try {
                Log.d(TAG, "Initializing default semester")
                defaultSemesterInitializer.ensureDefaultSemester()
                Log.d(TAG, "Default semester initialization completed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize default semester", e)
                // 不阻塞应用启动，错误已在初始化器中处理
            }
        }
        
        // 启动小组件定期更新
        widgetScheduler.schedulePeriodicUpdate()
    }
}
