package takagi.ru.saison.data.local.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "saison_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    // Keys
    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")  // 保留用于数据迁移
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val LANGUAGE = stringPreferencesKey("language")
        val LAST_SYNC_ETAG = stringPreferencesKey("last_sync_etag")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val WEBDAV_URL = stringPreferencesKey("webdav_url")
        val WEBDAV_USERNAME = stringPreferencesKey("webdav_username")
        val WEBDAV_PASSWORD = stringPreferencesKey("webdav_password")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val SYNC_ON_WIFI_ONLY = booleanPreferencesKey("sync_on_wifi_only")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val TASK_REMINDERS_ENABLED = booleanPreferencesKey("task_reminders_enabled")
        val COURSE_REMINDERS_ENABLED = booleanPreferencesKey("course_reminders_enabled")
        val POMODORO_REMINDERS_ENABLED = booleanPreferencesKey("pomodoro_reminders_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_CODE = stringPreferencesKey("pin_code")
        val AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        val POMODORO_WORK_DURATION = intPreferencesKey("pomodoro_work_duration")
        val POMODORO_BREAK_DURATION = intPreferencesKey("pomodoro_break_duration")
        val POMODORO_LONG_BREAK_DURATION = intPreferencesKey("pomodoro_long_break_duration")
        val POMODORO_SOUND_ENABLED = booleanPreferencesKey("pomodoro_sound_enabled")
        val POMODORO_VIBRATION_ENABLED = booleanPreferencesKey("pomodoro_vibration_enabled")
        
        // Bottom Navigation Settings
        val BOTTOM_NAV_CALENDAR = booleanPreferencesKey("bottom_nav_calendar")
        val BOTTOM_NAV_COURSE = booleanPreferencesKey("bottom_nav_course")
        val BOTTOM_NAV_TASKS = booleanPreferencesKey("bottom_nav_tasks")
        val BOTTOM_NAV_EVENTS = booleanPreferencesKey("bottom_nav_events")
        val BOTTOM_NAV_ROUTINE = booleanPreferencesKey("bottom_nav_routine")
        val BOTTOM_NAV_POMODORO = booleanPreferencesKey("bottom_nav_pomodoro")
        val BOTTOM_NAV_SETTINGS = booleanPreferencesKey("bottom_nav_settings")
        val BOTTOM_NAV_ORDER = stringPreferencesKey("bottom_nav_order")
        
        // Item Type Selection
        val SELECTED_ITEM_TYPE = intPreferencesKey("selected_item_type")
        
        // Course Settings
        val COURSE_PERIODS_PER_DAY = intPreferencesKey("course_periods_per_day")  // 保留用于数据迁移
        val COURSE_MORNING_PERIODS = intPreferencesKey("course_morning_periods")
        val COURSE_AFTERNOON_PERIODS = intPreferencesKey("course_afternoon_periods")
        val COURSE_EVENING_PERIODS = intPreferencesKey("course_evening_periods")
        val COURSE_PERIOD_DURATION = intPreferencesKey("course_period_duration")
        val COURSE_BREAK_DURATION = intPreferencesKey("course_break_duration")
        val COURSE_FIRST_PERIOD_START = stringPreferencesKey("course_first_period_start")
        val COURSE_LUNCH_BREAK_AFTER = intPreferencesKey("course_lunch_break_after")
        val COURSE_LUNCH_BREAK_DURATION = intPreferencesKey("course_lunch_break_duration")
        val COURSE_DINNER_BREAK_DURATION = intPreferencesKey("course_dinner_break_duration")
        val COURSE_SEMESTER_START_DATE = stringPreferencesKey("course_semester_start_date")
        val COURSE_TOTAL_WEEKS = intPreferencesKey("course_total_weeks")
        val COURSE_TIMELINE_COMPACTNESS = floatPreferencesKey("course_timeline_compactness")  // 保留用于数据迁移
        val COURSE_GRID_CELL_HEIGHT = intPreferencesKey("course_grid_cell_height")
        val COURSE_SHOW_WEEKENDS = booleanPreferencesKey("course_show_weekends")
        val COURSE_AUTO_SCROLL_TO_CURRENT_TIME = booleanPreferencesKey("course_auto_scroll_to_current_time")
        val COURSE_HIGHLIGHT_CURRENT_PERIOD = booleanPreferencesKey("course_highlight_current_period")
        
        // Semester Settings
        val CURRENT_SEMESTER_ID = longPreferencesKey("current_semester_id")
        val SEMESTER_HISTORY = stringPreferencesKey("semester_history")
    }
    
    // Theme Preferences
    val themePreferences: Flow<ThemePreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // 数据迁移逻辑：从旧的 IS_DARK_MODE 迁移到新的 THEME_MODE
            val themeModeString = preferences[PreferencesKeys.THEME_MODE]
            val themeMode = if (themeModeString != null) {
                // 如果存在新的 THEME_MODE，直接使用
                ThemeMode.fromString(themeModeString)
            } else {
                // 否则从旧的 IS_DARK_MODE 迁移
                val oldIsDarkMode = preferences[PreferencesKeys.IS_DARK_MODE]
                when (oldIsDarkMode) {
                    true -> ThemeMode.DARK
                    false -> ThemeMode.LIGHT
                    null -> ThemeMode.FOLLOW_SYSTEM  // 默认值
                }
            }
            
            // 主题迁移逻辑：处理已删除的主题
            val themeString = preferences[PreferencesKeys.THEME] ?: SeasonalTheme.DYNAMIC.name
            val theme = try {
                SeasonalTheme.valueOf(themeString)
            } catch (e: IllegalArgumentException) {
                // 如果主题不存在（已被删除），回退到 DYNAMIC
                SeasonalTheme.DYNAMIC
            }
            
            ThemePreferences(
                theme = theme,
                themeMode = themeMode,
                useDynamicColor = preferences[PreferencesKeys.USE_DYNAMIC_COLOR] ?: true
            )
        }
    
    suspend fun setTheme(theme: SeasonalTheme) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }
    
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
            // 清理旧数据
            preferences.remove(PreferencesKeys.IS_DARK_MODE)
        }
    }
    
    suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLOR] = enabled
        }
    }
    
    val themeMode: Flow<ThemeMode> = themePreferences.map { it.themeMode }
    
    // Language
    val language: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LANGUAGE] ?: "en"
        }
    
    suspend fun setLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = languageCode
        }
        // 同时保存到 SharedPreferences 以便在 attachBaseContext 中同步读取
        context.getSharedPreferences("app_language", Context.MODE_PRIVATE)
            .edit()
            .putString("language_code", languageCode)
            .apply()
    }
    
    // WebDAV Sync
    data class WebDavConfig(
        val url: String,
        val username: String,
        val password: String
    )
    
    suspend fun getWebDavConfig(): WebDavConfig? {
        val preferences = dataStore.data.map { it }.catch { emit(emptyPreferences()) }
        var config: WebDavConfig? = null
        preferences.collect { prefs ->
            val url = prefs[PreferencesKeys.WEBDAV_URL]
            val username = prefs[PreferencesKeys.WEBDAV_USERNAME]
            val password = prefs[PreferencesKeys.WEBDAV_PASSWORD]
            
            if (url != null && username != null && password != null) {
                config = WebDavConfig(url, username, password)
            }
        }
        return config
    }
    
    suspend fun setWebDavConfig(url: String, username: String, password: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEBDAV_URL] = url
            preferences[PreferencesKeys.WEBDAV_USERNAME] = username
            preferences[PreferencesKeys.WEBDAV_PASSWORD] = password
        }
    }
    
    suspend fun clearWebDavConfig() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.WEBDAV_URL)
            preferences.remove(PreferencesKeys.WEBDAV_USERNAME)
            preferences.remove(PreferencesKeys.WEBDAV_PASSWORD)
        }
    }
    
    val autoSyncEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AUTO_SYNC_ENABLED] ?: true
        }
    
    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SYNC_ENABLED] = enabled
        }
    }
    
    val syncOnWifiOnly: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SYNC_ON_WIFI_ONLY] ?: false
        }
    
    suspend fun setSyncOnWifiOnly(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_ON_WIFI_ONLY] = enabled
        }
    }
    
    suspend fun getLastSyncETag(): String? {
        var etag: String? = null
        dataStore.data.collect { preferences ->
            etag = preferences[PreferencesKeys.LAST_SYNC_ETAG]
        }
        return etag
    }
    
    suspend fun setLastSyncETag(etag: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_ETAG] = etag
            preferences[PreferencesKeys.LAST_SYNC_TIME] = System.currentTimeMillis()
        }
    }
    
    // Notification Settings
    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
        }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    val taskRemindersEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TASK_REMINDERS_ENABLED] ?: true
        }
    
    suspend fun setTaskRemindersEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TASK_REMINDERS_ENABLED] = enabled
        }
    }
    
    val courseRemindersEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.COURSE_REMINDERS_ENABLED] ?: true
        }
    
    suspend fun setCourseRemindersEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.COURSE_REMINDERS_ENABLED] = enabled
        }
    }
    
    val pomodoroRemindersEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.POMODORO_REMINDERS_ENABLED] ?: true
        }
    
    suspend fun setPomodoroRemindersEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.POMODORO_REMINDERS_ENABLED] = enabled
        }
    }
    
    // Security
    val biometricEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false
        }
    
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }
    
    suspend fun setPinCode(pinCode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PIN_CODE] = pinCode
        }
    }
    
    suspend fun getPinCode(): String? {
        var pin: String? = null
        dataStore.data.collect { preferences ->
            pin = preferences[PreferencesKeys.PIN_CODE]
        }
        return pin
    }
    
    val autoLockMinutes: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AUTO_LOCK_MINUTES] ?: 3
        }
    
    suspend fun setAutoLockMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_LOCK_MINUTES] = minutes
        }
    }
    
    // Pomodoro Settings
    val pomodoroWorkDuration: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.POMODORO_WORK_DURATION] ?: 25
        }
    
    val pomodoroBreakDuration: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.POMODORO_BREAK_DURATION] ?: 5
        }
    
    val pomodoroLongBreakDuration: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.POMODORO_LONG_BREAK_DURATION] ?: 15
        }
    
    suspend fun setPomodoroWorkDuration(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.POMODORO_WORK_DURATION] = minutes
        }
    }
    
    suspend fun setPomodoroBreakDuration(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.POMODORO_BREAK_DURATION] = minutes
        }
    }
    
    suspend fun setPomodoroLongBreakDuration(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.POMODORO_LONG_BREAK_DURATION] = minutes
        }
    }
    
    val pomodoroSoundEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.POMODORO_SOUND_ENABLED] ?: true
        }
    
    suspend fun setPomodoroSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.POMODORO_SOUND_ENABLED] = enabled
        }
    }
    
    val pomodoroVibrationEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.POMODORO_VIBRATION_ENABLED] ?: true
        }
    
    suspend fun setPomodoroVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.POMODORO_VIBRATION_ENABLED] = enabled
        }
    }
    
    // Bottom Navigation Settings
    val bottomNavVisibility: Flow<BottomNavVisibility> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            BottomNavVisibility(
                course = preferences[PreferencesKeys.BOTTOM_NAV_COURSE] ?: true,
                tasks = preferences[PreferencesKeys.BOTTOM_NAV_TASKS] ?: true,
                pomodoro = preferences[PreferencesKeys.BOTTOM_NAV_POMODORO] ?: true,
                settings = preferences[PreferencesKeys.BOTTOM_NAV_SETTINGS] ?: true
            )
        }
    
    val bottomNavOrder: Flow<List<BottomNavTab>> = dataStore.data
        .map { preferences ->
            val orderString = preferences[PreferencesKeys.BOTTOM_NAV_ORDER] ?: ""
            BottomNavTab.parseOrder(orderString)
        }
    
    suspend fun updateBottomNavVisibility(tab: BottomNavTab, visible: Boolean) {
        dataStore.edit { preferences ->
            when (tab) {
                BottomNavTab.COURSE -> preferences[PreferencesKeys.BOTTOM_NAV_COURSE] = visible
                BottomNavTab.CALENDAR -> {
                    // CALENDAR tab visibility is always true, cannot be changed
                }
                BottomNavTab.TASKS -> preferences[PreferencesKeys.BOTTOM_NAV_TASKS] = visible
                BottomNavTab.POMODORO -> preferences[PreferencesKeys.BOTTOM_NAV_POMODORO] = visible
                BottomNavTab.SUBSCRIPTION -> {
                    // SUBSCRIPTION tab visibility is not stored in preferences
                    // It's always visible or controlled elsewhere
                }
                BottomNavTab.SETTINGS -> preferences[PreferencesKeys.BOTTOM_NAV_SETTINGS] = visible
            }
        }
    }
    
    suspend fun updateBottomNavOrder(order: List<BottomNavTab>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BOTTOM_NAV_ORDER] = BottomNavTab.orderToString(order)
        }
    }
    
    // Item Type Selection
    val selectedItemType: Flow<takagi.ru.saison.domain.model.ItemType> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading selected item type", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val value = preferences[PreferencesKeys.SELECTED_ITEM_TYPE] 
                ?: takagi.ru.saison.domain.model.ItemType.TASK.value
            takagi.ru.saison.domain.model.ItemType.fromValue(value)
        }
    
    suspend fun setSelectedItemType(type: takagi.ru.saison.domain.model.ItemType) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.SELECTED_ITEM_TYPE] = type.value
            }
            Log.d(TAG, "Selected item type saved: $type")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save selected item type", e)
            throw e
        }
    }
    
    // Course Settings
    val courseSettings: Flow<takagi.ru.saison.domain.model.CourseSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading course settings", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // 数据迁移逻辑：从旧的分时段配置迁移到新的统一配置
            val totalPeriods: Int = if (preferences[PreferencesKeys.COURSE_MORNING_PERIODS] != null) {
                // 从旧的分时段配置迁移
                val morning = preferences[PreferencesKeys.COURSE_MORNING_PERIODS] ?: 4
                val afternoon = preferences[PreferencesKeys.COURSE_AFTERNOON_PERIODS] ?: 4
                val evening = preferences[PreferencesKeys.COURSE_EVENING_PERIODS] ?: 0
                Log.d(TAG, "Migrating from time-based periods: morning=$morning, afternoon=$afternoon, evening=$evening")
                morning + afternoon + evening
            } else {
                // 使用旧的 periodsPerDay 或默认值
                preferences[PreferencesKeys.COURSE_PERIODS_PER_DAY] ?: 8
            }
            
            takagi.ru.saison.domain.model.CourseSettings(
                totalPeriods = totalPeriods,
                periodDuration = preferences[PreferencesKeys.COURSE_PERIOD_DURATION] ?: 45,
                breakDuration = preferences[PreferencesKeys.COURSE_BREAK_DURATION] ?: 10,
                firstPeriodStartTime = preferences[PreferencesKeys.COURSE_FIRST_PERIOD_START]
                    ?.let { java.time.LocalTime.parse(it) } ?: java.time.LocalTime.of(8, 0),
                lunchBreakAfterPeriod = preferences[PreferencesKeys.COURSE_LUNCH_BREAK_AFTER],
                lunchBreakDuration = preferences[PreferencesKeys.COURSE_LUNCH_BREAK_DURATION] ?: 90,
                dinnerBreakDuration = preferences[PreferencesKeys.COURSE_DINNER_BREAK_DURATION] ?: 60,
                semesterStartDate = preferences[PreferencesKeys.COURSE_SEMESTER_START_DATE]
                    ?.let { java.time.LocalDate.parse(it) },
                totalWeeks = preferences[PreferencesKeys.COURSE_TOTAL_WEEKS] ?: 18,
                gridCellHeight = preferences[PreferencesKeys.COURSE_GRID_CELL_HEIGHT] ?: 80,
                showWeekends = preferences[PreferencesKeys.COURSE_SHOW_WEEKENDS] ?: true,
                autoScrollToCurrentTime = preferences[PreferencesKeys.COURSE_AUTO_SCROLL_TO_CURRENT_TIME] ?: true,
                highlightCurrentPeriod = preferences[PreferencesKeys.COURSE_HIGHLIGHT_CURRENT_PERIOD] ?: true
            )
        }
    
    suspend fun setCourseSettings(settings: takagi.ru.saison.domain.model.CourseSettings) {
        try {
            dataStore.edit { preferences ->
                // 保存统一的节次配置
                preferences[PreferencesKeys.COURSE_PERIODS_PER_DAY] = settings.totalPeriods
                
                // 保存其他设置
                preferences[PreferencesKeys.COURSE_PERIOD_DURATION] = settings.periodDuration
                preferences[PreferencesKeys.COURSE_BREAK_DURATION] = settings.breakDuration
                preferences[PreferencesKeys.COURSE_FIRST_PERIOD_START] = settings.firstPeriodStartTime.toString()
                settings.lunchBreakAfterPeriod?.let {
                    preferences[PreferencesKeys.COURSE_LUNCH_BREAK_AFTER] = it
                } ?: preferences.remove(PreferencesKeys.COURSE_LUNCH_BREAK_AFTER)
                preferences[PreferencesKeys.COURSE_LUNCH_BREAK_DURATION] = settings.lunchBreakDuration
                preferences[PreferencesKeys.COURSE_DINNER_BREAK_DURATION] = settings.dinnerBreakDuration
                settings.semesterStartDate?.let {
                    preferences[PreferencesKeys.COURSE_SEMESTER_START_DATE] = it.toString()
                } ?: preferences.remove(PreferencesKeys.COURSE_SEMESTER_START_DATE)
                preferences[PreferencesKeys.COURSE_TOTAL_WEEKS] = settings.totalWeeks
                preferences[PreferencesKeys.COURSE_GRID_CELL_HEIGHT] = settings.gridCellHeight
                preferences[PreferencesKeys.COURSE_SHOW_WEEKENDS] = settings.showWeekends
                preferences[PreferencesKeys.COURSE_AUTO_SCROLL_TO_CURRENT_TIME] = settings.autoScrollToCurrentTime
                preferences[PreferencesKeys.COURSE_HIGHLIGHT_CURRENT_PERIOD] = settings.highlightCurrentPeriod
                
                // 清理旧的分时段数据
                preferences.remove(PreferencesKeys.COURSE_MORNING_PERIODS)
                preferences.remove(PreferencesKeys.COURSE_AFTERNOON_PERIODS)
                preferences.remove(PreferencesKeys.COURSE_EVENING_PERIODS)
                preferences.remove(PreferencesKeys.COURSE_TIMELINE_COMPACTNESS)
            }
            Log.d(TAG, "Course settings saved: $settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save course settings", e)
            throw e
        }
    }
    
    // Semester Settings
    suspend fun getCurrentSemesterId(): Long? {
        return try {
            dataStore.data.map { preferences ->
                preferences[PreferencesKeys.CURRENT_SEMESTER_ID]
            }.first()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current semester ID", e)
            null
        }
    }
    
    suspend fun setCurrentSemesterId(semesterId: Long) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.CURRENT_SEMESTER_ID] = semesterId
            }
            Log.d(TAG, "Current semester ID saved: $semesterId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save current semester ID", e)
            throw e
        }
    }
    
    fun getSemesterHistory(): Flow<List<Long>> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e(TAG, "Error reading semester history", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val historyString = preferences[PreferencesKeys.SEMESTER_HISTORY] ?: ""
                if (historyString.isEmpty()) {
                    emptyList()
                } else {
                    historyString.split(",").mapNotNull { it.toLongOrNull() }
                }
            }
    }
    
    suspend fun addToSemesterHistory(semesterId: Long) {
        try {
            dataStore.edit { preferences ->
                val currentHistory = preferences[PreferencesKeys.SEMESTER_HISTORY] ?: ""
                val historyList = if (currentHistory.isEmpty()) {
                    emptyList()
                } else {
                    currentHistory.split(",").mapNotNull { it.toLongOrNull() }
                }.toMutableList()
                
                // 移除已存在的相同ID
                historyList.remove(semesterId)
                // 添加到开头
                historyList.add(0, semesterId)
                // 只保留最近10个
                val limitedHistory = historyList.take(10)
                
                preferences[PreferencesKeys.SEMESTER_HISTORY] = limitedHistory.joinToString(",")
            }
            Log.d(TAG, "Semester added to history: $semesterId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add semester to history", e)
        }
    }
    
    companion object {
        private const val TAG = "PreferencesManager"
    }
}

