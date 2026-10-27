package com.v2ray.ang.dto.entities

import com.google.gson.annotations.SerializedName

/**
 * Everything the panel stores about one installation, as posted to
 * `POST {SUB_API_BASE_URL}/MobileDevice/Register` on first launch.
 *
 * The panel writes it to `tbMobileUsers` and files the row under the agent whose
 * token this build carries, so every invoice the device later creates is
 * traceable back to it.
 *
 * See `Docs/API-MobileDevice.md` in the panel repository.
 */
data class MobileDeviceInfo(
    /** `Settings.Secure.ANDROID_ID` — the panel's uniqueness key for a device. */
    @SerializedName("androidId")
    val androidId: String,
    /**
     * FCM registration token. Null when the build ships without Firebase, in
     * which case the panel records the device as "not reachable by push".
     */
    @SerializedName("firebaseToken")
    val firebaseToken: String? = null,
    @SerializedName("manufacturer")
    val manufacturer: String? = null,
    @SerializedName("model")
    val model: String? = null,
    @SerializedName("device")
    val device: String? = null,
    @SerializedName("product")
    val product: String? = null,
    @SerializedName("hardware")
    val hardware: String? = null,
    /** Android release string, e.g. `15`. */
    @SerializedName("androidVersion")
    val androidVersion: String? = null,
    /** Android API level, e.g. `35`. */
    @SerializedName("sdk")
    val sdk: Int? = null,
    @SerializedName("appVersion")
    val appVersion: String? = null,
    @SerializedName("versionCode")
    val versionCode: Int? = null,
    @SerializedName("packageName")
    val packageName: String? = null,
    @SerializedName("language")
    val language: String? = null,
    @SerializedName("country")
    val country: String? = null,
    /** IANA zone id, e.g. `Asia/Tehran` — lets the panel send push at a sane hour. */
    @SerializedName("timezone")
    val timezone: String? = null,
    @SerializedName("screenWidth")
    val screenWidth: Int? = null,
    @SerializedName("screenHeight")
    val screenHeight: Int? = null,
    @SerializedName("density")
    val density: Int? = null,
    /** Whether the user has granted the notification permission. */
    @SerializedName("notificationEnabled")
    val notificationEnabled: Boolean = false,
    @SerializedName("rooted")
    val rooted: Boolean = false,
)

/** The panel's answer to a successful device registration. */
data class MobileDeviceRegistration(
    @SerializedName("deviceId", alternate = ["DeviceId"])
    val deviceId: Int = 0,
    @SerializedName("agentUsername", alternate = ["AgentUsername"])
    val agentUsername: String? = null,
    @SerializedName("businessName", alternate = ["BusinessName"])
    val businessName: String? = null,
    /** True when this androidId was already on file and the row was refreshed. */
    @SerializedName("isExisting", alternate = ["IsExisting"])
    val isExisting: Boolean = false,
    @SerializedName("message", alternate = ["Message"])
    val message: String? = null,
)
