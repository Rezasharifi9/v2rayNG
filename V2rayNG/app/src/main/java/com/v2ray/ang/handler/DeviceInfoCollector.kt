package com.v2ray.ang.handler

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.MobileDeviceInfo
import com.v2ray.ang.util.LogUtil
import java.io.File
import java.util.Locale
import java.util.TimeZone

/**
 * Reads the facts the panel keeps about an installation.
 *
 * Nothing here is personally identifying beyond `ANDROID_ID`, which the panel
 * needs as the uniqueness key so a reinstall updates the existing row instead of
 * creating a second one.
 */
object DeviceInfoCollector {

    /**
     * MMKV key holding the current FCM token. This build has no Firebase SDK, so
     * the key stays empty; wire a `FirebaseMessagingService.onNewToken` into
     * [DeviceRegistrationManager.onFirebaseTokenChanged] and the value flows from
     * here to the panel with no further changes.
     */
    const val PREF_FIREBASE_TOKEN = "pref_firebase_token"

    /**
     * Collects everything the register endpoint takes.
     *
     * @param context Any context; the application context is used internally.
     * @return The payload, or null when `ANDROID_ID` could not be read — without
     *         it the panel has no key to store the row under.
     */
    fun collect(context: Context): MobileDeviceInfo? {
        val app = context.applicationContext

        val androidId = readAndroidId(app)
        if (androidId.isNullOrEmpty()) {
            LogUtil.w(AppConfig.TAG, "Device registration skipped: ANDROID_ID unavailable")
            return null
        }

        val metrics = app.resources.displayMetrics
        val locale = currentLocale(app)

        return MobileDeviceInfo(
            androidId = androidId,
            firebaseToken = MmkvManager.decodeSettingsString(PREF_FIREBASE_TOKEN)?.takeIf { it.isNotBlank() },
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            hardware = Build.HARDWARE,
            androidVersion = Build.VERSION.RELEASE,
            sdk = Build.VERSION.SDK_INT,
            appVersion = readVersionName(app),
            versionCode = readVersionCode(app),
            packageName = app.packageName,
            language = locale?.language,
            country = locale?.country,
            timezone = TimeZone.getDefault().id,
            screenWidth = metrics.widthPixels,
            screenHeight = metrics.heightPixels,
            density = metrics.densityDpi,
            notificationEnabled = notificationsEnabled(app),
            rooted = isRooted()
        )
    }

    /**
     * @return `Settings.Secure.ANDROID_ID`, or null when the platform refuses it.
     */
    @SuppressLint("HardwareIds")
    private fun readAndroidId(context: Context): String? = runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }.getOrNull()

    private fun readVersionName(context: Context): String? = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull()

    private fun readVersionCode(context: Context): Int? = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode
        }
    }.getOrNull()

    private fun currentLocale(context: Context): Locale? = runCatching {
        val locales = context.resources.configuration.locales
        if (locales.isEmpty) null else locales[0]
    }.getOrNull()

    /**
     * Whether the app may post notifications at all. A device with push disabled
     * still gets registered — the panel just knows not to target it.
     */
    private fun notificationsEnabled(context: Context): Boolean = runCatching {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }.getOrDefault(false)

    /**
     * Best-effort root check by looking for the usual `su` locations and a
     * test-keys build tag. Advisory only — it is trivially defeated and is
     * reported to the panel as a hint, never used to gate anything.
     */
    private fun isRooted(): Boolean = runCatching {
        if (Build.TAGS?.contains("test-keys") == true) return@runCatching true

        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        paths.any { File(it).exists() }
    }.getOrDefault(false)
}
