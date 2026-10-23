package com.example

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RoyalGold
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        PrayerCornerApp()
      }
    }
  }
}

class WebAppInterface(
  private val context: Context,
  private val onShowSnackbar: (String) -> Unit
) {

  private val mainHandler = Handler(Looper.getMainLooper())

  @JavascriptInterface
  fun openWhatsApp(url: String) {
    mainHandler.post {
      try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
      } catch (e: Exception) {
        try {
          val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(browserIntent)
        } catch (ex: Exception) {
          Toast.makeText(
            context,
            "تم تسجيل طلبك بنجاح! شكراً لاختيارك ركن الصلاة.",
            Toast.LENGTH_LONG
          ).show()
        }
      }
    }
  }

  @JavascriptInterface
  fun onOrderPlaced(jsonDetails: String) {
    onShowSnackbar("تم تسجيل طلبك بنجاح! سيتم التواصل معك هاتفياً لتأكيد الشحن.")
  }

  @JavascriptInterface
  fun copyHtmlCode() {
    mainHandler.post {
      try {
        val htmlCode = context.assets.open("index.html").bufferedReader().use { it.readText() }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("PrayerCornerHTML", htmlCode)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ كود صفحة الهبوط بالكامل إلى الحافظة!", Toast.LENGTH_SHORT).show()
        onShowSnackbar("تم نسخ كود صفحة الهبوط بالكامل (HTML + Tailwind + JS) إلى الحافظة بنجاح!")
      } catch (e: Exception) {
        onShowSnackbar("تعذر نسخ الكود: ${e.message}")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrayerCornerApp() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  var webViewInstance by remember { mutableStateOf<WebView?>(null) }
  var canGoBack by remember { mutableStateOf(false) }

  BackHandler(enabled = canGoBack) {
    webViewInstance?.let {
      if (it.canGoBack()) {
        it.goBack()
      }
    }
  }

  fun copyHtmlSource() {
    try {
      val htmlCode = context.assets.open("index.html").bufferedReader().use { it.readText() }
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText("PrayerCornerHTML", htmlCode)
      clipboard.setPrimaryClip(clip)
      scope.launch {
        snackbarHostState.showSnackbar("تم نسخ كود صفحة الهبوط بالكامل (HTML + Tailwind + JS) إلى الحافظة بنجاح!")
      }
    } catch (e: Exception) {
      scope.launch {
        snackbarHostState.showSnackbar("تعذر نسخ الكود: ${e.message}")
      }
    }
  }

  fun shareProduct() {
    val shareIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(
        Intent.EXTRA_TEXT,
        "ركن الصلاة من مادة الفوراكس عالية الجودة المقاومة للماء بنقوش إسلامية راقية - بسعر 4500 دج فقط مع توصيل لكافة الولايات عبر Swift Express والدفع عند الاستلام!"
      )
      type = "text/plain"
    }
    context.startActivity(Intent.createChooser(shareIntent, "مشاركة ركن الصلاة"))
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color(0xFF1E2824),
          titleContentColor = RoyalGold,
          actionIconContentColor = RoyalGold
        ),
        title = {
          Text(
            text = "ركن الصلاة",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
          )
        },
        actions = {
          IconButton(
            onClick = { copyHtmlSource() },
            modifier = Modifier.testTag("copy_html_button")
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "نسخ كود الـ HTML",
              tint = RoyalGold
            )
          }
          IconButton(
            onClick = { shareProduct() },
            modifier = Modifier.testTag("share_product_button")
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "مشاركة",
              tint = RoyalGold
            )
          }
          IconButton(
            onClick = { webViewInstance?.reload() },
            modifier = Modifier.testTag("reload_page_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "إعادة تحميل",
              tint = RoyalGold
            )
          }
        }
      )
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(Color(0xFFFAF8F5))
    ) {
      AndroidView(
        modifier = Modifier.fillMaxSize().testTag("prayer_corner_webview"),
        factory = { ctx ->
          WebView(ctx).apply {
            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              allowFileAccess = true
              allowContentAccess = true
              allowFileAccessFromFileURLs = true
              allowUniversalAccessFromFileURLs = true
              useWideViewPort = true
              loadWithOverviewMode = true
              cacheMode = WebSettings.LOAD_DEFAULT
              mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
              displayZoomControls = false
              builtInZoomControls = false
            }
            webChromeClient = object : WebChromeClient() {
              override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
              ): Boolean {
                Toast.makeText(ctx, message ?: "", Toast.LENGTH_SHORT).show()
                result?.confirm()
                return true
              }

              override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d("WebViewConsole", "${consoleMessage?.message()} [line ${consoleMessage?.lineNumber()}]")
                return true
              }
            }
            webViewClient = object : WebViewClient() {
              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                canGoBack = view?.canGoBack() == true
              }

              override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
              ) {
                super.onReceivedError(view, request, error)
                Log.e("WebViewError", "Error: ${error?.description} for ${request?.url}")
              }
            }
            addJavascriptInterface(
              WebAppInterface(ctx) { message ->
                scope.launch {
                  snackbarHostState.showSnackbar(message)
                }
              },
              "AndroidBridge"
            )
            loadUrl("file:///android_asset/index.html")
            webViewInstance = this
          }
        },
        update = { webView ->
          webViewInstance = webView
        }
      )
    }
  }
}
