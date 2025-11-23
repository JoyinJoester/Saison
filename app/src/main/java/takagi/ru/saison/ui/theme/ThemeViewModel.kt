package takagi.ru.saison.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import takagi.ru.saison.data.local.datastore.SeasonalTheme
import takagi.ru.saison.data.local.datastore.ThemeMode
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeManager: ThemeManager
) : ViewModel() {
    
    val currentTheme: StateFlow<SeasonalTheme> = themeManager.currentTheme
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SeasonalTheme.DYNAMIC
        )
    
    val themeMode: StateFlow<ThemeMode> = themeManager.themeMode
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ThemeMode.FOLLOW_SYSTEM
        )
    
    val useDynamicColor: StateFlow<Boolean> = themeManager.useDynamicColor
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )
}
