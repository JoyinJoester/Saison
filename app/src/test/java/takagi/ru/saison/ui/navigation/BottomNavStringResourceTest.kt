package takagi.ru.saison.ui.navigation

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.checkAll
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import takagi.ru.saison.R

/**
 * Property-based tests for string resource availability in bottom navigation
 * Feature: bottom-nav-i18n-layout-fix, Property 5: String Resource Availability
 * Validates: Requirements 4.1, 4.2, 4.4
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BottomNavStringResourceTest : StringSpec({
    
    lateinit var context: Context
    
    beforeTest {
        context = ApplicationProvider.getApplicationContext()
    }
    
    "Property 5: All abbreviated string resources should exist for all supported languages" {
        val labelResIds = listOf(
            R.string.nav_calendar_short,
            R.string.nav_course_short,
            R.string.nav_tasks_short,
            R.string.nav_pomodoro_short,
            R.string.nav_subscription_short,
            R.string.nav_settings_short
        )
        
        val locales = listOf("en", "zh", "ja", "vi")
        
        locales.forEach { locale ->
            val config = context.resources.configuration
            config.setLocale(java.util.Locale(locale))
            val localizedContext = context.createConfigurationContext(config)
            
            labelResIds.forEach { resId ->
                shouldNotThrowAny {
                    val label = localizedContext.getString(resId)
                    label shouldNotBe null
                    label.shouldNotBeEmpty()
                    label.shouldNotBeBlank()
                }
            }
        }
    }
    
    "Property 5: String resources should not throw exceptions when accessed" {
        checkAll(100, Arb.element(listOf("en", "zh", "ja", "vi"))) { locale ->
            val config = context.resources.configuration
            config.setLocale(java.util.Locale(locale))
            val localizedContext = context.createConfigurationContext(config)
            
            val labelResIds = listOf(
                R.string.nav_calendar_short,
                R.string.nav_course_short,
                R.string.nav_tasks_short,
                R.string.nav_pomodoro_short,
                R.string.nav_subscription_short,
                R.string.nav_settings_short
            )
            
            shouldNotThrowAny {
                labelResIds.forEach { resId ->
                    localizedContext.getString(resId)
                }
            }
        }
    }
    
    "Property 5: All full and abbreviated label pairs should exist" {
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
                shouldNotThrowAny {
                    val fullLabel = localizedContext.getString(fullResId)
                    val shortLabel = localizedContext.getString(shortResId)
                    
                    fullLabel.shouldNotBeEmpty()
                    shortLabel.shouldNotBeEmpty()
                }
            }
        }
    }
    
    "Property 5: String resources should be non-empty for any random access pattern" {
        checkAll(100, 
            Arb.element(listOf("en", "zh", "ja", "vi")),
            Arb.element(listOf(
                R.string.nav_calendar_short,
                R.string.nav_course_short,
                R.string.nav_tasks_short,
                R.string.nav_pomodoro_short,
                R.string.nav_subscription_short,
                R.string.nav_settings_short
            ))
        ) { locale, resId ->
            val config = context.resources.configuration
            config.setLocale(java.util.Locale(locale))
            val localizedContext = context.createConfigurationContext(config)
            
            val label = localizedContext.getString(resId)
            label.shouldNotBeEmpty()
            label.shouldNotBeBlank()
        }
    }
    
    "Property 5: Resource IDs should be valid and not zero" {
        val labelResIds = listOf(
            R.string.nav_calendar_short,
            R.string.nav_course_short,
            R.string.nav_tasks_short,
            R.string.nav_pomodoro_short,
            R.string.nav_subscription_short,
            R.string.nav_settings_short
        )
        
        labelResIds.forEach { resId ->
            resId shouldNotBe 0
            resId shouldNotBe Resources.ID_NULL
        }
    }
    
    "Property 5: Fallback to default language should work when locale is not found" {
        // Test with an unsupported locale - should fall back to default (English)
        val unsupportedLocale = "fr" // French is not supported
        val config = context.resources.configuration
        config.setLocale(java.util.Locale(unsupportedLocale))
        val localizedContext = context.createConfigurationContext(config)
        
        val labelResIds = listOf(
            R.string.nav_calendar_short,
            R.string.nav_course_short,
            R.string.nav_tasks_short,
            R.string.nav_pomodoro_short,
            R.string.nav_subscription_short,
            R.string.nav_settings_short
        )
        
        // Should not throw and should return English strings
        labelResIds.forEach { resId ->
            shouldNotThrowAny {
                val label = localizedContext.getString(resId)
                label.shouldNotBeEmpty()
            }
        }
    }
})
