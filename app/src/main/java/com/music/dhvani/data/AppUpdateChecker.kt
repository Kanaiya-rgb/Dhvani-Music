package com.music.dhvani.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.music.dhvani.BuildConfig
import com.music.dhvani.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * BitChord ships as a sideloaded APK off GitHub Releases rather than through
 * a store, so there's nothing to push an update notice on its own — this
 * polls the repo's "latest release" once per launch and compares its tag
 * against the running build.
 *
 * The update itself is also handled here: the release's `.apk` asset is
 * downloaded into the app's cache and handed to the system package installer,
 * so the whole round trip stays inside the app instead of bouncing out to a
 * browser.
 */
object AppUpdateChecker {

    data class UpdateInfo(
        val version: String,
        val releaseUrl: String,
        val apkUrl: String?,
        /** The release's own Markdown body, shown as this update's "what's new". */
        val notes: String?,
    )

    private const val CACHE_SUBDIR = "updates"

    private const val CDN_VERSION_URL =
        "https://raw.githubusercontent.com/Kanaiya-rgb/Dhvani-Music/main/version.json"

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/Kanaiya-rgb/Dhvani-Music/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    private val _available = MutableStateFlow<UpdateInfo?>(null)
    val available = _available.asStateFlow()

    private val _promptDialog = MutableStateFlow(false)
    val promptDialog = _promptDialog.asStateFlow()

    fun triggerDialog() {
        _promptDialog.value = true
    }

    fun consumeDialog() {
        _promptDialog.value = false
    }

    /** Where this update's APK download currently stands, for the dialog's progress row. */
    sealed interface DownloadState {
        data object Idle : DownloadState
        data class Downloading(val fraction: Float) : DownloadState
        data class Ready(val file: File) : DownloadState
        data class Failed(val message: String) : DownloadState
    }

    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download = _download.asStateFlow()

    /** Set from the UI thread when the user cancels; polled between network reads. */
    @Volatile
    private var downloadCancelled = false

    sealed interface CheckResult {
        data class UpdateAvailable(val info: UpdateInfo) : CheckResult
        data class UpToDate(val currentVersion: String, val latestVersion: String) : CheckResult
        data class Error(val message: String) : CheckResult
    }

    /**
     * Checks for updates using a multi-tiered strategy:
     * 1. Raw GitHub CDN (version.json): Never rate-limited, ultra-fast.
     * 2. GitHub REST API: Official releases API.
     * 3. GitHub Web redirect: Direct fallback without rate limits.
     *
     * If an update is found and [context] is provided, also posts a system notification.
     */
    suspend fun check(context: Context? = null): CheckResult = withContext(Dispatchers.IO) {
        val latestInfo = fetchFromCdn() ?: fetchFromGitHubApi() ?: fetchFromGitHubRedirect()
        if (latestInfo == null) {
            return@withContext CheckResult.Error("Could not reach update server")
        }

        if (isNewer(latestInfo.version, BuildConfig.VERSION_NAME)) {
            _available.value = latestInfo
            context?.let { postUpdateNotification(it, latestInfo) }
            CheckResult.UpdateAvailable(latestInfo)
        } else {
            CheckResult.UpToDate(BuildConfig.VERSION_NAME, latestInfo.version)
        }
    }

    private fun fetchFromCdn(): UpdateInfo? = runCatching {
        val req = Request.Builder()
            .url(CDN_VERSION_URL)
            .header("User-Agent", "Dhvani-Music/${BuildConfig.VERSION_NAME}")
            .build()
        Http.client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            val text = res.body?.string() ?: return null
            val obj = json.parseToJsonElement(text) as? JsonObject ?: return null
            val ver = obj["version"]?.jsonPrimitive?.contentOrNull?.trim()?.removePrefix("v") ?: return null
            val relUrl = obj["releaseUrl"]?.jsonPrimitive?.contentOrNull
                ?: "https://github.com/Kanaiya-rgb/Dhvani-Music/releases"
            val apk = obj["apkUrl"]?.jsonPrimitive?.contentOrNull
            val notes = obj["notes"]?.jsonPrimitive?.contentOrNull
            UpdateInfo(ver, relUrl, apk, notes)
        }
    }.getOrNull()

    private fun fetchFromGitHubApi(): UpdateInfo? = runCatching {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("User-Agent", "Dhvani-Music/${BuildConfig.VERSION_NAME}")
            .header("Accept", "application/vnd.github+json")
            .build()
        Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val release = json.parseToJsonElement(body) as? JsonObject ?: return null
            val tag = release["tag_name"]?.jsonPrimitive?.contentOrNull ?: return null
            val url = release["html_url"]?.jsonPrimitive?.contentOrNull ?: return null
            val apkUrl = apkAssetUrl(release)
            val notes = release["body"]?.jsonPrimitive?.contentOrNull
            val latest = tag.removePrefix("v").trim()
            UpdateInfo(latest, url, apkUrl, notes)
        }
    }.getOrNull()

    private fun fetchFromGitHubRedirect(): UpdateInfo? = runCatching {
        val req = Request.Builder()
            .url("https://github.com/Kanaiya-rgb/Dhvani-Music/releases/latest")
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            .build()
        Http.client.newCall(req).execute().use { res ->
            val finalUrl = res.request.url.toString()
            if (finalUrl.contains("/tag/")) {
                val tag = finalUrl.substringAfterLast("/tag/").removePrefix("v").trim()
                if (tag.isNotEmpty()) {
                    val apk = "https://github.com/Kanaiya-rgb/Dhvani-Music/releases/download/v$tag/DhvaniMusic-v$tag.apk"
                    return UpdateInfo(tag, finalUrl, apk, null)
                }
            }
            null
        }
    }.getOrNull()

    /**
     * Wipes any APK left over from a previous run. Called once at cold start
     * so a downloaded update is only ever "Install Now" for the session that
     * downloaded it — the next launch starts clean rather than trying to work
     * out whether a leftover file is still good.
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        File(context.cacheDir, CACHE_SUBDIR).listFiles()?.forEach { it.delete() }
    }

    /**
     * The release usually carries exactly one `.apk`; take its direct download
     * URL. A release without one (source-only draft, renamed asset) leaves
     * [UpdateInfo.apkUrl] null and the UI falls back to opening the releases
     * page as before.
     */
    private fun apkAssetUrl(release: JsonObject): String? = runCatching {
        release["assets"]?.jsonArray
            ?.mapNotNull { it as? JsonObject }
            ?.firstOrNull { asset ->
                asset["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk", ignoreCase = true) == true &&
                    asset["state"]?.jsonPrimitive?.contentOrNull == "uploaded"
            }
            ?.get("browser_download_url")
            ?.jsonPrimitive
            ?.contentOrNull
    }.getOrNull()

    /**
     * Streams the current update's APK into the app cache, reporting progress
     * through [download]. A finished file survives a cancelled dialog: until
     * the state is reset, "Install Now" comes straight back without a second
     * download.
     */
    suspend fun downloadApk(context: Context): Unit = withContext(Dispatchers.IO) {
        val info = _available.value ?: return@withContext
        val url = info.apkUrl ?: return@withContext
        downloadCancelled = false
        _download.value = DownloadState.Downloading(0f)

        runCatching {
            val dir = File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
            // Drop anything left over from an earlier attempt.
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, "dhvani-${info.version}.apk")

            val request = Request.Builder().url(url).build()
            Http.client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Download failed: HTTP ${response.code}" }
                val body = response.body ?: error("Empty download body")
                val total = body.contentLength().takeIf { it > 0 }

                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var readTotal = 0L
                        while (true) {
                            if (downloadCancelled) {
                                _download.value = DownloadState.Idle
                                return@withContext
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            readTotal += read
                            total?.let {
                                _download.value =
                                    DownloadState.Downloading((readTotal.toFloat() / it).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }
            _download.value = DownloadState.Ready(target)
        }.onFailure { error ->
            _download.value = if (downloadCancelled) {
                DownloadState.Idle
            } else {
                DownloadState.Failed(error.message ?: "Download failed")
            }
        }
    }

    /** Stops an in-flight download; the next read loop sees this and bails. */
    fun cancelDownload() {
        downloadCancelled = true
    }

    /** Back to square one after a failure, so the dialog offers Download again. */
    fun resetDownload() {
        _download.value = DownloadState.Idle
    }

    /**
     * Hands a downloaded APK to the system installer.
     *
     * Sideloaded apps need the user's blessing per app ("install unknown apps");
     * without it the installer intent silently does nothing on most ROMs, so
     * the user is sent to that one switch first and taps Install again after.
     */
    fun installApk(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK,
            )
        }
        context.startActivity(installIntent)
    }

    /** Robust, numeric, dot-separated comparison — handles "2.0.5", "v2.0.5", "2.0.5-release". */
    private fun isNewer(latest: String, current: String): Boolean {
        val l = parseVersionParts(latest)
        val c = parseVersionParts(current)
        for (i in 0 until maxOf(l.size, c.size)) {
            val a = l.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun parseVersionParts(version: String): List<Int> {
        val clean = version.trim().removePrefix("v")
        val base = clean.substringBefore("-").substringBefore("+")
        return base.split(".").map { part ->
            part.filter { it.isDigit() }.toIntOrNull() ?: 0
        }
    }

    private const val UPDATE_CHANNEL_ID = "dhvani_app_updates"
    private const val UPDATE_NOTIFICATION_ID = 2001
    private const val PREF_LAST_NOTIFIED_VERSION = "last_notified_update_version"
    private const val PREF_LAST_NOTIFIED_TIME = "last_notified_update_time"
    private const val REMINDER_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours

    /**
     * Schedules periodic and immediate background checks via WorkManager
     * to check for updates whenever device has active internet connectivity.
     */
    fun schedulePeriodicCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<AppUpdateWorker>(4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DhvaniAppUpdatePeriodicWorker",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest,
        )

        val oneTimeRequest = OneTimeWorkRequestBuilder<AppUpdateWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "DhvaniAppUpdateInitialWorker",
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest,
        )
    }

    /**
     * Posts a high-priority system notification when a new update is found.
     * Includes rate-limiting (maximum once per 24 hours per version) so it doesn't spam.
     */
    fun postUpdateNotification(context: Context, info: UpdateInfo) {
        val prefs = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
        val lastNotifiedVersion = prefs.getString(PREF_LAST_NOTIFIED_VERSION, null)
        val lastNotifiedTime = prefs.getLong(PREF_LAST_NOTIFIED_TIME, 0L)
        val now = System.currentTimeMillis()

        if (lastNotifiedVersion == info.version && (now - lastNotifiedTime) < REMINDER_INTERVAL_MS) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (manager != null) {
                val channel = NotificationChannel(
                    UPDATE_CHANNEL_ID,
                    "App Updates",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Notifies when a new Dhvani Music update is available on GitHub"
                    enableLights(true)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra("open_update_dialog", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle("🎵 New Update: Dhvani Music v${info.version}")
            .setContentText("Version ${info.version} is now available! Tap to download & install.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Dhvani Music v${info.version} is now available!\n" +
                            (info.notes?.take(180)?.let { "\n$it\n\n" } ?: "") +
                            "Tap to open and install the new update.",
                    ),
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification)
            prefs.edit()
                .putString(PREF_LAST_NOTIFIED_VERSION, info.version)
                .putLong(PREF_LAST_NOTIFIED_TIME, now)
                .apply()
        }
    }
}
