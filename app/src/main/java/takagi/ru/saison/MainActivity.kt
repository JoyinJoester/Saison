package takagi.ru.saison

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Configure edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            SaisonAppWithTheme()
        }
    }
    
    override fun attachBaseContext(newBase: Context) {
        // 从 SharedPreferences 读取语言设置以便初始化
        val prefs = newBase.getSharedPreferences("app_language", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language_code", "zh") ?: "zh"
        
        val context = LocaleHelper.setLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }
}

@Composable
fun SaisonAppWithTheme() {
    // 使用 hiltViewModel 获取 ThemeViewModel
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
        SaisonApp()
    }
}

@Composable
fun SaisonApp() {
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
    
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp
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
        labelRes = R.string.nav_course,
        route = Screen.Course.route
    )
    BottomNavTab.CALENDAR -> NavItem(
        icon = Icons.Default.DateRange,
        labelRes = R.string.nav_calendar,
        route = Screen.Calendar.route
    )
    BottomNavTab.TASKS -> NavItem(
        icon = Icons.Default.CheckCircle,
        labelRes = R.string.nav_tasks,
        route = Screen.Tasks.route
    )
    BottomNavTab.POMODORO -> NavItem(
        icon = Icons.Default.Timer,
        labelRes = R.string.nav_pomodoro,
        route = Screen.Pomodoro.route
    )
    BottomNavTab.SUBSCRIPTION -> NavItem(
        icon = Icons.Default.Loyalty, // Or CardMembership, or Subscriptions if available
        labelRes = R.string.nav_subscription,
        route = Screen.Subscription.route
    )
    BottomNavTab.SETTINGS -> NavItem(
        icon = Icons.Default.Settings,
        labelRes = R.string.nav_settings,
        route = Screen.Settings.route
    )
}