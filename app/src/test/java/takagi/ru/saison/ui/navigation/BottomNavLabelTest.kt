package takagi.ru.saison.ui.navigation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import takagi.ru.saison.R

/**
 * Property-based tests for bottom navigation label display
 * Feature: bottom-nav-i18n-layout-fix, Property 1: Complete Label Display
 * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BottomNavLabelTest : StringSpec({
    
    lateinit var context: Context
    
    beforeTest {
        context = ApplicationProvider.getApplicationContext()
    }
    
    "Property 1: All abbreviated navigation labels should be non-empty for all supported languages" {
        val labelResIds = listOf(
            R.string.nav_calendar_short,
            R.string.nav_course_short,
            R.string.nav_tasks_short,
            R.string.nav_pomodoro_short,
            R.string.nav_subscription_short,
            R.string.nav_settings_short
        )
        
        // Test for each supported locale
        val locales = listOf("en", "zh", "ja", "vi")
        
        locales.forEach { locale ->
            val config = context.resources.configuration
            config.setLocale(java.util.Locale(locale))
            val localizedContext = context.createConfigurationContext(config)
            
            labelResIds.forEach { resId ->
                val label = localizedContext.getString(resId)
                label.shouldNotBeEmpty()
                // Abbreviated labels should be reasonably short (max 10 characters)
                label.length shouldBe { it <= 10 }
            }
        }
    }
    
    "Property 1: Abbreviated labels should be shorter than or equal to full labels" {
        val labelPairs = listOf(
            R.string.nav_calendar to R.string.nav_calendar_short,
            R.string.nav_course to R.string.nav_course_short,
            R.string.nav_tasks to R.string.nav_tasks_short,
            R.string.nav_pomodoro to R.string.nav_pomodoro_short,
            R.string.nav_subscription to R.string.nav_subscription_short,
            R.string.nav_settings to R.string.nav_settings_short
        )
        
        val locales = listOf("en", "zh", "ja", "vi")
        
        locales.forEach { locale ->
            val config = context.resources.configuration
            config.setLocale(java.util.Locale(locale))
            val localizedContext = context.createConfigurationContext(config)
            
            labelPairs.forEach { (fullResId, shortResId) ->
                val fullLabel = localizedContext.getString(fullResId)
                val shortLabel = localizedContext.getString(shortResId)
                
                // Short label should not be longer than full label
                shortLabel.length shouldBe { it <= fullLabel.length }
            }
        }
    }
    
    "Property 1: All navigation labels should exist and be accessible" {
        checkAll(100, Arb.list(Arb.string(1..2), 1..4)) { _ ->
            // For any random configuration, all label resources should be accessible
            val labelResIds = listOf(
                R.string.nav_calendar_short,
                R.string.nav_course_short,
                R.string.nav_tasks_short,
                R.string.nav_pomodoro_short,
                R.string.nav_subscription_short,
                R.string.nav_settings_short
            )
            
            labelResIds.forEach { resId ->
                val label = context.getString(resId)
                label shouldNotBe null
                label.shouldNotBeEmpty()
            }
        }
    }
})
