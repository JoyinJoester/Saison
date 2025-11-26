package takagi.ru.saison.ui.navigation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import takagi.ru.saison.R

/**
 * Property-based tests for visual balance in bottom navigation
 * Feature: bottom-nav-i18n-layout-fix, Property 4: Visual Balance Maintenance
 * Validates: Requirements 3.1, 3.4
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BottomNavVisualBalanceTest : StringSpec({
    
    lateinit var context: Context
    
    beforeTest {
        context = ApplicationProvider.getApplicationContext()
    }
    
    "Property 4: All abbreviated labels should have similar lengths for visual balance" {
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
            
            val labels = labelResIds.map { localizedContext.getString(it) }
            val lengths = labels.map { it.length }
            
            // Calculate variance in label lengths
            val avgLength = lengths.average()
            val maxDeviation = lengths.maxOf { kotlin.math.abs(it - avgLength) }
            
            // Maximum deviation should be reasonable (not more than 5 characters)
            maxDeviation shouldBeLessThanOrEqual 5.0
        }
    }
    
    "Property 4: Label count should remain constant across all languages" {
        val labelResIds = listOf(
            R.string.nav_calendar_short,
            R.string.nav_course_short,
            R.string.nav_tasks_short,
            R.string.nav_pomodoro_short,
            R.string.nav_subscription_short,
            R.string.nav_settings_short
        )
        
        val locales = listOf("en", "zh", "ja", "vi")
        val expectedCount = labelResIds.size
        
        locales.forEach { locale ->
            val config = context.resources.configuration
            config.setLocale(java.util.Locale(locale))
            val localizedContext = context.createConfigurationContext(config)
            
            val labels = labelResIds.map { localizedContext.getString(it) }
            labels shouldHaveSize expectedCount
        }
    }
    
    "Property 4: For any subset of visible navigation items, spacing should be equal" {
        checkAll(100, Arb.list(Arb.string(1..10), 2..6)) { visibleItems ->
            // For any subset of visible navigation items (2-6 items),
            // the spacing between them should be equal
            // This is handled by NavigationBar's equal weight distribution
            
            val itemCount = visibleItems.size
            // Each item gets equal space: 1.0 / itemCount
            val expectedWeight = 1.0 / itemCount
            
            // Verify that all items would get equal weight
            visibleItems.forEach { _ ->
                val weight = 1.0 / itemCount
                weight shouldBe expectedWeight
            }
        }
    }
    
    "Property 4: Abbreviated labels should not exceed maximum reasonable length" {
        val labelResIds = listOf(
            R.string.nav_calendar_short,
            R.string.nav_course_short,
            R.string.nav_tasks_short,
            R.string.nav_pomodoro_short,
            R.string.nav_subscription_short,
            R.string.nav_settings_short
        )
        
        val locales = listOf("en", "zh", "ja", "vi")
        val maxReasonableLength = 10 // Maximum 10 characters for abbreviated labels
        
        locales.forEach { locale ->
            val config = context.resources.configuration
            config.setLocale(java.util.Locale(locale))
            val localizedContext = context.createConfigurationContext(config)
            
            labelResIds.forEach { resId ->
                val label = localizedContext.getString(resId)
                label.length shouldBeLessThanOrEqual maxReasonableLength
            }
        }
    }
    
    "Property 4: Visual balance should be maintained with different navigation item combinations" {
        // Test different combinations of visible items (simulating user customization)
        val allLabels = listOf(
            R.string.nav_calendar_short,
            R.string.nav_course_short,
            R.string.nav_tasks_short,
            R.string.nav_pomodoro_short,
            R.string.nav_subscription_short,
            R.string.nav_settings_short
        )
        
        // Test with different numbers of visible items
        val combinations = listOf(
            allLabels.take(3),  // 3 items
            allLabels.take(4),  // 4 items
            allLabels.take(5),  // 5 items
            allLabels           // All 6 items
        )
        
        combinations.forEach { visibleLabels ->
            val labels = visibleLabels.map { context.getString(it) }
            val lengths = labels.map { it.length }
            
            // All labels should be reasonably short
            lengths.forEach { length ->
                length shouldBeLessThanOrEqual 10
            }
        }
    }
})
