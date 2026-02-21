package com.theveloper.pixelplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theveloper.pixelplay.data.ai.AiThemeGenerator
import com.theveloper.pixelplay.data.model.CustomTheme
import com.theveloper.pixelplay.data.model.ThemeColors
import com.theveloper.pixelplay.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ThemeStudioUiState(
    val currentColors: ThemeColors? = null,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val themeName: String = "",
    val isDark: Boolean = true
)

@HiltViewModel
class ThemeStudioViewModel @Inject constructor(
    private val aiThemeGenerator: AiThemeGenerator,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemeStudioUiState())
    val uiState = _uiState.asStateFlow()

    fun onThemeNameChange(name: String) {
        _uiState.value = _uiState.value.copy(themeName = name)
    }

    fun onIsDarkChange(isDark: Boolean) {
        _uiState.value = _uiState.value.copy(isDark = isDark)
    }

    fun generateTheme(prompt: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null)
            val result = aiThemeGenerator.generate(prompt, _uiState.value.isDark)
            result.onSuccess { colors ->
                _uiState.value = _uiState.value.copy(currentColors = colors, isGenerating = false)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message, isGenerating = false)
            }
        }
    }

    fun saveTheme() {
        val state = _uiState.value
        val colors = state.currentColors ?: return
        val name = state.themeName.ifBlank { "New AI Theme" }

        viewModelScope.launch {
            val theme = CustomTheme(
                id = UUID.randomUUID().toString(),
                name = name,
                isDark = state.isDark,
                colors = colors
            )
            userPreferencesRepository.addCustomTheme(theme)
            userPreferencesRepository.setActiveCustomTheme(theme.id)
        }
    }
}
