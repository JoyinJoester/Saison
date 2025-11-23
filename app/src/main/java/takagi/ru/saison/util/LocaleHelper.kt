package takagi.ru.saison.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.*

object LocaleHelper {
    
    /**
     * 更新应用的语言配置
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = getLocaleFromCode(languageCode)
        Locale.setDefault(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(context, locale)
        } else {
            updateResourcesLegacy(context, locale)
        }
    }
    
    /**
     * 在运行时更新 Activity 的语言配置（无需重启）
     */
    fun updateActivityLocale(activity: Activity, languageCode: String) {
        val locale = getLocaleFromCode(languageCode)
        Locale.setDefault(locale)
        
        val resources = activity.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
        
        // 重新应用系统栏颜色，防止变白
        if (activity is androidx.activity.ComponentActivity) {
            try {
                val window = activity.window
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                
                // 读取当前主题模式
                val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                val isDark = nightMode == Configuration.UI_MODE_NIGHT_YES
                
                // 重新设置系统栏图标颜色
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }
    
    /**
     * 根据语言代码获取 Locale 对象
     */
    private fun getLocaleFromCode(languageCode: String): Locale {
        return when (languageCode) {
            "system" -> {
                // 使用系统默认语言
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    android.content.res.Resources.getSystem().configuration.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    android.content.res.Resources.getSystem().configuration.locale
                }
            }
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "en" -> Locale.ENGLISH
            "ja" -> Locale.JAPANESE
            "vi" -> Locale("vi")
            else -> {
                // 默认使用系统语言
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    android.content.res.Resources.getSystem().configuration.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    android.content.res.Resources.getSystem().configuration.locale
                }
            }
        }
    }
    
    /**
     * Android N 及以上版本的语言配置更新
     */
    private fun updateResources(context: Context, locale: Locale): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        
        return context.createConfigurationContext(configuration)
    }
    
    /**
     * Android N 以下版本的语言配置更新
     */
    @Suppress("DEPRECATION")
    private fun updateResourcesLegacy(context: Context, locale: Locale): Context {
        val resources = context.resources
        val configuration = resources.configuration
        configuration.locale = locale
        configuration.setLayoutDirection(locale)
        
        resources.updateConfiguration(configuration, resources.displayMetrics)
        
        return context
    }
}
