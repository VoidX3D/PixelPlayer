package com.theveloper.pixelplay.presentation.netease.auth

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import com.theveloper.pixelplay.ui.theme.PixelPlayTheme
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject

/**
 * WebView-based login for Netease Cloud Music.
 * User logs in at music.163.com in the WebView, then taps "Done" to
 * capture the MUSIC_U session cookie. Cookies are saved directly via
 * the ViewModel/Repository — no activity result needed.
 *
 * Same approach as NeriPlayer.
 */
@AndroidEntryPoint
class NeteaseLoginActivity : ComponentActivity() {

    companion object {
        const val TARGET_URL = "https://music.163.com/"
        const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/124.0.0.0 Safari/537.36"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixelPlayTheme {
                NeteaseWebLoginScreen(onClose = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NeteaseWebLoginScreen(
    viewModel: NeteaseLoginViewModel = hiltViewModel(),
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    val loginState by viewModel.state.collectAsState()
    val titleStyle = rememberNeteaseLoginTitleStyle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle login state changes
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is NeteaseLoginState.Success -> {
                Toast.makeText(context, "Welcome, ${state.nickname}!", Toast.LENGTH_SHORT).show()
                onClose()
            }
            is NeteaseLoginState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            else -> {}
        }
    }

    fun onDoneClick() {
        if (loginState is NeteaseLoginState.Loading) return
        readAndProcessCookies(
            onSuccess = { cookieJson -> viewModel.processCookies(cookieJson) },
            onError = { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        //modifier = Modifier.padding(start = 4.dp),
                        text = "Login to Netease",
                        style = titleStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 6.dp),
                        onClick = {
                            if (webView?.canGoBack() == true) {
                                webView?.goBack()
                            } else {
                                onClose()
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 4.dp,
                actions = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 10.dp),
                        onClick = { webView?.reload() },
                        enabled = loginState !is NeteaseLoginState.Loading,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = ::onDoneClick,
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        if (loginState is NeteaseLoginState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                //modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (loginState is NeteaseLoginState.Loading) "Saving…" else "Done",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        NeteaseWebView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            onWebViewCreated = { webView = it }
        )
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberNeteaseLoginTitleStyle(): TextStyle {
    return remember {
        TextStyle(
            fontFamily = FontFamily(
                Font(
                    resId = R.font.gflex_variable,
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(620),
                        FontVariation.width(128f),
                        FontVariation.Setting("ROND", 88f),
                        FontVariation.Setting("XTRA", 520f),
                        FontVariation.Setting("YOPQ", 90f),
                        FontVariation.Setting("YTLC", 505f)
                    )
                )
            ),
            fontWeight = FontWeight(700),
            fontSize = 18.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.2).sp
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun NeteaseWebView(
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = NeteaseLoginActivity.DESKTOP_UA
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                loadUrl(NeteaseLoginActivity.TARGET_URL)
                onWebViewCreated(this)
            }
        }
    )
}

/**
 * Read cookies from the WebView's CookieManager and pass them as JSON.
 */
private fun readAndProcessCookies(
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val cm = CookieManager.getInstance()
        val main = cm.getCookie("https://music.163.com") ?: ""
        val api = cm.getCookie("https://interface.music.163.com") ?: ""
        val merged = listOf(main, api).filter { it.isNotBlank() }.joinToString("; ")

        if (merged.isBlank()) {
            onError("No cookies found. Please log in first.")
            return
        }

        val map = cookieStringToMap(merged)
        if (!map.containsKey("os")) map["os"] = "pc"
        if (!map.containsKey("appver")) map["appver"] = "8.10.35"

        if (!map.containsKey("MUSIC_U")) {
            onError("Login not detected yet. Complete login and try again.")
            return
        }

        val json = JSONObject(map as Map<*, *>).toString()
        onSuccess(json)
    } catch (e: Throwable) {
        onError("Failed: ${e.message}")
    }
}

private fun cookieStringToMap(raw: String): MutableMap<String, String> {
    val map = linkedMapOf<String, String>()
    raw.split(';')
        .map { it.trim() }
        .filter { it.isNotBlank() && it.contains('=') }
        .forEach { part ->
            val idx = part.indexOf('=')
            val key = part.substring(0, idx).trim()
            val value = part.substring(idx + 1).trim()
            if (key.isNotEmpty()) map[key] = value
        }
    return map
}
