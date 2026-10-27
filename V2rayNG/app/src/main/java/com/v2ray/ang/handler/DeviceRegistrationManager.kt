package com.v2ray.ang.handler

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.MobileDeviceRegistration
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import java.util.concurrent.TimeUnit

/**
 * Registers this installation with the panel on first launch.
 *
 * The panel files the device under the agent whose token this build carries, so
 * every invoice created from the app is traceable back to the handset. The call
 * is retried with backoff until it succeeds — a device that never reached the
 * panel would create invoices that no one can attribute.
 *
 * See `Docs/API-MobileDevice.md` in the panel repository.
 */
object DeviceRegistrationManager {

    private const val REGISTER_PATH = "/MobileDevice/Register"
    private const val REQUEST_TIMEOUT_MS = 20_000

    private const val WORK_NAME = "device_registration"

    /** Version code the panel was last told about; 0 means never registered. */
    private const val PREF_REGISTERED_VERSION = "pref_device_registered_version"

    /** Device id the panel assigned, kept so invoices can reference the handset. */
    private const val PREF_REGISTERED_DEVICE_ID = "pref_device_registered_id"

    /**
     * Schedules registration if this build has not reported itself yet.
     *
     * Safe to call on every launch: it returns immediately once the panel has
     * been told about the current version, and an already-queued attempt is kept
     * rather than restarted. Call from `MainActivity.onCreate()`.
     */
    fun sync(context: Context = AngApplication.application) {
        if (!AgentApi.isConfigured()) return
        if (isUpToDate(context)) return

        val request = OneTimeWorkRequestBuilder<RegistrationWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    /**
     * Re-registers so the panel picks up a fresh FCM token. Call from
     * `FirebaseMessagingService.onNewToken` after storing the token under
     * [DeviceInfoCollector.PREF_FIREBASE_TOKEN].
     */
    fun onFirebaseTokenChanged(context: Context = AngApplication.application, token: String) {
        MmkvManager.encodeSettings(DeviceInfoCollector.PREF_FIREBASE_TOKEN, token)

        val request = OneTimeWorkRequestBuilder<RegistrationWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /**
     * The `androidId` this device registered with, for tagging invoices. Read
     * straight from the device rather than from storage so it stays correct even
     * if registration has not completed yet.
     */
    fun androidId(context: Context = AngApplication.application): String? =
        DeviceInfoCollector.collect(context)?.androidId

    /** The panel-assigned device id, or 0 when registration has not landed yet. */
    fun registeredDeviceId(): Int = MmkvManager.decodeSettingsString(PREF_REGISTERED_DEVICE_ID)
        ?.toIntOrNull() ?: 0

    /**
     * Whether the panel already knows about this exact build. A version bump
     * re-registers so the panel's `appVersion` column does not go stale.
     */
    private fun isUpToDate(context: Context): Boolean {
        val registered = MmkvManager.decodeSettingsString(PREF_REGISTERED_VERSION)?.toIntOrNull() ?: 0
        if (registered == 0) return false

        val current = DeviceInfoCollector.collect(context)?.versionCode ?: return true
        return registered == current
    }

    /**
     * Performs the call. Network work, so it must not run on the main thread.
     *
     * @return True when the panel accepted the device. False covers both a
     *         refusal and an unreachable panel — the caller retries either way.
     */
    private fun register(context: Context): Boolean {
        val token = AgentApi.token ?: return false
        val info = DeviceInfoCollector.collect(context) ?: return false

        val response = HttpUtil.postJson(
            url = AgentApi.url(REGISTER_PATH),
            jsonBody = JsonUtil.toJson(info),
            headers = AgentApi.authHeaders(token),
            timeout = REQUEST_TIMEOUT_MS
        ) ?: return false

        if (!response.isSuccessful) {
            val message = AgentApi.parseErrorMessage(response.body)
            LogUtil.w(AppConfig.TAG, "Device registration refused: ${response.code} $message")
            return false
        }

        val parsed = response.body?.let {
            JsonUtil.fromJsonSafe(it, MobileDeviceRegistration::class.java)
        } ?: return false

        MmkvManager.encodeSettings(PREF_REGISTERED_DEVICE_ID, parsed.deviceId.toString())
        MmkvManager.encodeSettings(PREF_REGISTERED_VERSION, (info.versionCode ?: 0).toString())

        LogUtil.i(
            AppConfig.TAG,
            "Device registered as #${parsed.deviceId} under ${parsed.agentUsername} (existing=${parsed.isExisting})"
        )
        return true
    }

    /**
     * Retries with exponential backoff until the panel answers. WorkManager keeps
     * the request across restarts, so a device that installs offline still
     * registers the first time it gets a connection.
     */
    class RegistrationWorker(
        context: Context,
        params: WorkerParameters
    ) : CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            // The build lost its agent token — retrying cannot help.
            if (!AgentApi.isConfigured()) return Result.failure()

            return if (register(applicationContext)) Result.success() else Result.retry()
        }
    }
}
