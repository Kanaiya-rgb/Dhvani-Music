package com.music.dhvani.ui.components

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.dhvani.data.sources.SourceConfig
import com.music.dhvani.data.sources.SourceKind
import com.music.dhvani.data.sources.SourceRegistry
import com.music.dhvani.data.sources.zarz.ZarzSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * In-app Cloudflare Turnstile verification dialog for Zarz / TIDAL HiFi lossless streaming.
 * Also provides a 1-tap fallback to direct TIDAL addon to avoid captcha barriers.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TurnstileVerificationDialog(
    onDismiss: () -> Unit,
    onVerified: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var challengeId by remember { mutableStateOf(ZarzSessionManager.getPendingChallengeId()) }
    val siteKey = remember { ZarzSessionManager.getTurnstileSiteKey() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ZarzSessionManager.bootstrap()
        }
        challengeId = ZarzSessionManager.getPendingChallengeId()
    }

    val html = remember(siteKey, challengeId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
          <script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>
          <style>
            * { box-sizing: border-box; }
            body {
              background: #0F1118;
              color: #E2E8F0;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              margin: 0;
              padding: 16px;
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              min-height: 100vh;
              text-align: center;
            }
            .card {
              background: #161A24;
              border: 1px solid rgba(255, 255, 255, 0.08);
              border-radius: 18px;
              padding: 20px 16px;
              width: 100%;
              max-width: 340px;
              display: flex;
              flex-direction: column;
              align-items: center;
            }
            .hint {
              font-size: 13px;
              color: #94A3B8;
              margin-bottom: 16px;
              line-height: 1.4;
            }
            .widget {
              margin-top: 4px;
            }
          </style>
        </head>
        <body>
          <div class="card">
            <div class="hint">Tap below to verify Cloudflare security for 7-day lossless access:</div>
            <div class="cf-turnstile widget" data-sitekey="$siteKey" data-cdata="$challengeId" data-callback="onTurnstileSuccess" data-theme="dark"></div>
          </div>
          <script>
            function onTurnstileSuccess(token) {
              window.location.href = "dhvani://session-grant?grant=" + encodeURIComponent(token);
            }
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F1118),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Waves,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "TIDAL HiFi Session",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                            ),
                            color = Color.White,
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "A quick security check is required to authorize bit-exact 16-bit FLAC streaming. Session stays active for 7 days.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                    ),
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))

                if (isVerifying) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 2.5.dp,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Activating Lossless Session...",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.userAgentString =
                                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                        ): Boolean {
                                            val uri = request?.url ?: return false
                                            if ((uri.scheme == "dhvani" || uri.scheme == "mornye") && uri.host == "session-grant") {
                                                val grant = uri.getQueryParameter("grant")
                                                if (!grant.isNullOrBlank()) {
                                                    isVerifying = true
                                                    scope.launch(Dispatchers.IO) {
                                                        val ok = ZarzSessionManager.exchangeGrant(grant)
                                                        withContext(Dispatchers.Main) {
                                                            isVerifying = false
                                                            if (ok) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "TIDAL Lossless Session Active!",
                                                                    Toast.LENGTH_SHORT,
                                                                ).show()
                                                                onVerified()
                                                                onDismiss()
                                                            } else {
                                                                errorMessage = "Session exchange failed. Use Direct TIDAL Addon below."
                                                            }
                                                        }
                                                    }
                                                    return true
                                                }
                                            }
                                            return false
                                        }
                                    }
                                    loadDataWithBaseURL("https://api.zarz.moe", html, "text/html", "utf-8", null)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color(0xFFF87171),
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Direct TIDAL Addon 1-Tap Solution (No Captcha)
                OutlinedButton(
                    onClick = {
                        val directTidalConfig = SourceConfig(
                            id = "builtin_addon_aurix_tidal",
                            kind = SourceKind.CUSTOM_MODULE,
                            label = "Aurix TIDAL [Fesi]",
                            baseUrl = "https://addon-2.aurix.dpdns.org/free_rLnRPnC6oZF5gvAx/manifest.json",
                            enabled = true,
                            isManagedRemote = true,
                        )
                        SourceRegistry.add(directTidalConfig)
                        Toast.makeText(context, "Added Direct TIDAL Addon! (No Captcha Needed)", Toast.LENGTH_LONG).show()
                        onVerified()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FlashOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF00E5FF),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Or Use Direct TIDAL Addon (No Captcha)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
