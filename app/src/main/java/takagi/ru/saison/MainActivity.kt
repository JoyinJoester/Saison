package takagi.ru.saison

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import takagi.ru.saison.data.local.datastore.BottomNavTab
import takagi.ru.saison.data.local.datastore.PreferencesManager
import takagi.ru.saison.ui.navigation.Screen
import takagi.ru.saison.ui.navigation.SaisonNavHost
import takagi.ru.saison.ui.theme.SaisonTheme
import takagi.ru.saison.util.LocaleHelper
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    // 用于从小组件传递的导航参数
    private var widgetTaskId: Long? = null
    private var widgetNavigateTo: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // 安装 Splash Screen（必须在 super.onCreate 之前）
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // 处理从小组件传递的Intent
        handleWidgetIntent(intent)
        
        // 启用沉浸式状态栏（参考 Monica 的实现）
        enableEdgeToEdge()
        
        // 立即设置状态栏为透明，避免启动时闪白
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // 根据系统夜间模式设置状态栏图标颜色
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isNightMode
            isAppearanceLightNavigationBars = !isNightMode
        }
        
        setContent {
            SaisonAppWithTheme(
                widgetTaskId = widgetTaskId,
                widgetNavigateTo = widgetNavigateTo,
                onWidgetNavigationHandled = {
                    // 清除导航参数，避免重复导航
                    widgetTaskId = null
                    widgetNavigateTo = null
                }
            )
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 处理从小组件传递的新Intent（当Activity已存在时）
        handleWidgetIntent(intent)
    }
    
    private fun handleWidgetIntent(intent: Intent?) {
        intent?.let {
            val taskId = it.getLongExtra("widget_task_id", -1L)
            val navigateTo = it.getStringExtra("widget_navigate_to")
            
            if (taskId != -1L && navigateTo != null) {
                android.util.Log.d("MainActivity", "Widget intent: taskId=$taskId, navigateTo=$navigateTo")
                widgetTaskId = taskId
                widgetNavigateTo = navigateTo
            }
        }
    }
    
    /**
     * 启用沉浸式边到边显示
     * 参考 Monica 的实现，让系统栏完全透明，由 Compose 主题自动处理颜色
     */
    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
    
    override fun attachBaseContext(newBase: Context) {
        // 从 SharedPreferences 读取语言设置以便初始化
        val prefs = newBase.getSharedPreferences("app_language", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language_code", "zh") ?: "zh"
        
        android.util.Log.d("MainActivity", "attachBaseContext: languageCode = $languageCode")
        
        val context = LocaleHelper.setLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }
}

@Composable
fun SaisonAppWithTheme(
    widgetTaskId: Long? = null,
    widgetNavigateTo: String? = null,
    onWidgetNavigationHandled: () -> Unit = {}
) {
    // 使用 hiltViewModel 获取 ThemeViewModel
    // 注意：这里使用的是 Activity Context，因为 attachBaseContext 已经设置了正确的 locale
    val themeViewModel = androidx.hilt.navigation.compose.hiltViewModel<takagi.ru.saison.ui.theme.ThemeViewModel>()
    val currentTheme by themeViewModel.currentTheme.collectAsState()
    val themeMode by themeViewModel.themeMode.collectAsState()
    val useDynamicColor by themeViewModel.useDynamicColor.collectAsState()
    
    // 根据 themeMode 计算实际的 darkTheme 值和主题
    val systemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    // 如果是动态时间模式，使用时间段配置
    val timeOfDayConfig = if (themeMode == takagi.ru.saison.data.local.datastore.ThemeMode.AUTO_TIME) {
        takagi.ru.saison.util.TimeOfDayHelper.getCurrentConfig()
    } else {
        null
    }
    
    val actualTheme = timeOfDayConfig?.theme ?: currentTheme
    val darkTheme = when (themeMode) {
        takagi.ru.saison.data.local.datastore.ThemeMode.FOLLOW_SYSTEM -> systemInDarkTheme
        takagi.ru.saison.data.local.datastore.ThemeMode.LIGHT -> false
        takagi.ru.saison.data.local.datastore.ThemeMode.DARK -> true
        takagi.ru.saison.data.local.datastore.ThemeMode.AUTO_TIME -> timeOfDayConfig?.isDark ?: false
    }
    
    SaisonTheme(
        seasonalTheme = actualTheme,
        darkTheme = darkTheme,
        dynamicColor = useDynamicColor
    ) {
        SaisonApp(
            widgetTaskId = widgetTaskId,
            widgetNavigateTo = widgetNavigateTo,
            onWidgetNavigationHandled = onWidgetNavigationHandled
        )
    }
}

@Composable
fun SaisonApp(
    widgetTaskId: Long? = null,
    widgetNavigateTo: String? = null,
    onWidgetNavigationHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // 获取底栏设置
    val settingsViewModel = androidx.hilt.navigation.compose.hiltViewModel<takagi.ru.saison.ui.screens.settings.SettingsViewModel>()
    val bottomNavVisibility by settingsViewModel.bottomNavVisibility.collectAsState()
    val bottomNavOrder by settingsViewModel.bottomNavOrder.collectAsState()
    
    // 过滤出可见的导航项
    val visibleNavItems = remember(bottomNavOrder, bottomNavVisibility) {
        bottomNavOrder.filter { tab -> bottomNavVisibility.isVisible(tab) }
    }
    
    // 确定起始页面：使用第一个可见的导航项
    val startDestination = remember(visibleNavItems) {
        visibleNavItems.firstOrNull()?.toNavItem()?.route ?: Screen.Tasks.route
    }
    
    // 处理从小组件传递的导航请求
    LaunchedEffect(widgetTaskId, widgetNavigateTo) {
        if (widgetTaskId != null && widgetNavigateTo != null) {
            android.util.Log.d("SaisonApp", "Handling widget navigation: taskId=$widgetTaskId, navigateTo=$widgetNavigateTo")
            when (widgetNavigateTo) {
                "task_preview" -> {
                    navController.navigate(Screen.TaskPreview.createRoute(widgetTaskId)) {
                        // 确保不会创建多个相同的目的地
                        launchSingleTop = true
                    }
                }
                "task_edit" -> {
                    navController.navigate(Screen.TaskEdit.createRoute(widgetTaskId)) {
                        launchSingleTop = true
                    }
                }
            }
            onWidgetNavigationHandled()
        }
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.systemBars
                    .only(WindowInsetsSides.Bottom)
                    .add(WindowInsets(bottom = 8.dp))
            ) {
                visibleNavItems.forEach { tab ->
                    val navItem = tab.toNavItem()
                    // 任务按钮在任务、事件、日程页面都高亮显示
                    val isSelected = if (tab == BottomNavTab.TASKS) {
                        currentRoute == Screen.Tasks.route || 
                        currentRoute == Screen.Events.route || 
                        currentRoute == Screen.Routine.route
                    } else {
                        currentRoute == navItem.route
                    }
                    
                    NavigationBarItem(
                        icon = { Icon(navItem.icon, contentDescription = null) },
                        label = { Text(stringResource(navItem.labelRes)) },
                        selected = isSelected,
                        onClick = {
                            // 如果点击任务按钮，且当前在事件或日程页面，则不导航
                            if (tab == BottomNavTab.TASKS && 
                                (currentRoute == Screen.Events.route || currentRoute == Screen.Routine.route)) {
                                // 不做任何操作，保持在当前页面
                            } else {
                                navController.navigate(navItem.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        SaisonNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// 导航项数据类
private data class NavItem(
    val icon: ImageVector,
    val labelRes: Int,
    val route: String
)

// 扩展函数：将 BottomNavTab 转换为 NavItem
private fun BottomNavTab.toNavItem(): NavItem = when (this) {
    BottomNavTab.COURSE -> NavItem(
        icon = Icons.Default.School,
        labelRes = R.string.nav_course_short,
        route = Screen.Course.route
    )
    BottomNavTab.CALENDAR -> NavItem(
        icon = Icons.Default.DateRange,
        labelRes = R.string.nav_calendar_short,
        route = Screen.Calendar.route
    )
    BottomNavTab.TASKS -> NavItem(
        icon = Icons.Default.CheckCircle,
        labelRes = R.string.nav_tasks_short,
        route = Screen.Tasks.route
    )
    BottomNavTab.POMODORO -> NavItem(
        icon = Icons.Default.Timer,
        labelRes = R.string.nav_pomodoro_short,
        route = Screen.Pomodoro.route
    )
    BottomNavTab.SUBSCRIPTION -> NavItem(
        icon = Icons.Default.Loyalty, // Or CardMembership, or Subscriptions if available
        labelRes = R.string.nav_subscription_short,
        route = Screen.Subscription.route
    )
    BottomNavTab.SETTINGS -> NavItem(
        icon = Icons.Default.Settings,
        labelRes = R.string.nav_settings_short,
        route = Screen.Settings.route
    )
}