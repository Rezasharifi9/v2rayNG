package com.v2ray.ang.dto.entities

/**
 * Quota snapshot of a subscription, as returned by
 * `GET {SUB_API_BASE_URL}/api/v1/Sub/Info?token={token}`.
 *
 * Field names mirror the JSON payload so Gson can map it directly.
 */
data class SubscriptionInfo(
    val success: Boolean = false,
    val name: String? = null,
    val totalVolumeGb: Double = 0.0,
    val usedVolumeGb: Double = 0.0,
    val remainingDays: Int = REMAINING_DAYS_UNLIMITED,
    val expireDate: String? = null,
    val message: String? = null,
    /** Local timestamp of the last successful fetch; not part of the API payload. */
    val fetchedAt: Long = 0L,
) {
    /** The API returns -1 for a subscription that never expires. */
    val hasExpiry: Boolean get() = remainingDays >= 0

    /** The API omits the remaining volume, so it is derived here. */
    val remainingVolumeGb: Double get() = (totalVolumeGb - usedVolumeGb).coerceAtLeast(0.0)

    companion object {
        const val REMAINING_DAYS_UNLIMITED = -1
    }
}
