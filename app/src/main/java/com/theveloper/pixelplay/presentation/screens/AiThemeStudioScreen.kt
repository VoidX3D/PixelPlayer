package com.theveloper.pixelplay.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.theveloper.pixelplay.presentation.components.CollapsibleCommonTopBar
import com.theveloper.pixelplay.presentation.viewmodel.ThemeStudioViewModel
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiThemeStudioScreen(
    navController: NavController,
    onBackClick: () -> Unit,
    viewModel: ThemeStudioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var prompt by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CollapsibleCommonTopBar(
                title = "AI Theme Studio",
                onBackClick = onBackClick,
                collapseFraction = 0f,
                headerHeight = 64.dp
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Describe your perfect theme, and Gemini will generate a custom color palette for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Theme Prompt (e.g., 'Sunset in Tokyo', 'Deep Ocean')") },
                    placeholder = { Text("A retro synthwave vibe with neon purples") },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Mode:", style = MaterialTheme.typography.titleSmall)
                    FilterChip(
                        selected = uiState.isDark,
                        onClick = { viewModel.onIsDarkChange(true) },
                        label = { Text("Dark") }
                    )
                    FilterChip(
                        selected = !uiState.isDark,
                        onClick = { viewModel.onIsDarkChange(false) },
                        label = { Text("Light") }
                    )
                }
            }

            item {
                Button(
                    onClick = { viewModel.generateTheme(prompt) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = prompt.isNotBlank() && !uiState.isGenerating,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate Theme")
                    }
                }
            }

            if (uiState.currentColors != null) {
                item {
                    ThemePreviewCard(uiState.currentColors!!, uiState.isDark)
                }

                item {
                    OutlinedTextField(
                        value = uiState.themeName,
                        onValueChange = { viewModel.onThemeNameChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Theme Name") },
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                item {
                    Button(
                        onClick = {
                            viewModel.saveTheme()
                            onBackClick()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save & Apply")
                    }
                }
            }

            if (uiState.error != null) {
                item {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun ThemePreviewCard(colors: com.theveloper.pixelplay.data.model.ThemeColors, isDark: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(if (isDark) colors.surface else colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(colors.outline))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Preview", fontWeight = FontWeight.Bold, color = Color(colors.onSurface))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(colors.primary)))
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(colors.secondary)))
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(colors.tertiary)))
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(colors.primaryContainer)))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(colors.primaryContainer)
            ) {
                Text(
                    "Sample Content Text",
                    modifier = Modifier.padding(12.dp),
                    color = Color(colors.onPrimaryContainer)
                )
            }
        }
    }
}
