package com.v2ray.ang.dto.entities

import com.google.gson.annotations.SerializedName
import kotlin.math.roundToInt

/**
 * One tariff the agent offers, as returned by
 * `GET {SUB_API_BASE_URL}/User/GetAgentPlans`.
 *
 * See `Docs/API-AgentPlans.md` in the panel repository. Two payload shapes are
 * in the wild: the newer one describes the tariff with separate fields
 * (`planMonth` / `planVolume` / `isUnlimited`), the older one only ships a ready
 * made Persian `PlanName` such as `1 ماهه | 30 گیگ`. Both are accepted so the
 * app keeps working against panels that have not been updated yet.
 */
data class AgentPlan(
    /**
     * `tbLinkUserAndPlans.Link_PU_ID` — the ID of the agent-to-plan link, not
     * the plan itself. It is what the invoice call has to send back.
     */
    @SerializedName("planId", alternate = ["PlanId"])
    val planId: Int = 0,
    /** Display name, present only in the older payload shape. */
    @SerializedName("planName", alternate = ["PlanName"])
    val rawName: String? = null,
    /** Agent's selling price in Toman, raw and without digit grouping. */
    @SerializedName("planPrice", alternate = ["PlanPrice", "Price", "price"])
    val price: Double = 0.0,
    /** Duration in months; absent in the older payload shape. */
    @SerializedName("planMonth", alternate = ["PlanMonth"])
    val rawMonth: Double? = null,
    /** Volume in gigabytes; meaningless when [isUnlimited] is true. */
    @SerializedName("planVolume", alternate = ["PlanVolume"])
    val rawVolumeGb: Double? = null,
    /** Allowed devices; null means the panel defines no limit. */
    @SerializedName("deviceLimit", alternate = ["DeviceLimit"])
    val deviceLimit: Int? = null,
    /** True for device-based tariffs, where the volume carries no meaning. */
    @SerializedName("isUnlimited", alternate = ["IsUnlimited"])
    val isUnlimited: Boolean = false,
) {

    /**
     * Duration in whole months, taken from the field when the panel sends one
     * and otherwise read off the leading number of the display name. Zero means
     * neither source produced a usable value.
     */
    val months: Int
        get() = rawMonth?.roundToInt()?.takeIf { it > 0 }
            ?: rawName?.substringBefore(NAME_SEPARATOR)
                ?.let { NUMBER.find(it)?.groupValues?.get(1)?.toIntOrNull() }
            ?: 0

    /**
     * The part of the panel's display name after the month, e.g. `30 گیگ`. Only
     * available in the older payload shape; null means the caller has to build
     * the label from the individual fields.
     */
    val rawDetail: String?
        get() {
            if (rawMonth != null) return null
            val name = rawName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val separator = name.indexOf(NAME_SEPARATOR)
            val detail = if (separator >= 0) name.substring(separator + 1) else name
            return detail.replace(NAME_SEPARATOR, DETAIL_SEPARATOR)
                .split(WHITESPACE)
                .filter { it.isNotEmpty() }
                .joinToString(" ")
                .takeIf { it.isNotEmpty() }
        }

    /** Volume in gigabytes, or null when the tariff is not volume based. */
    val volumeGb: Double?
        get() = if (isUnlimited) null else rawVolumeGb

    private companion object {
        const val NAME_SEPARATOR = '|'
        const val DETAIL_SEPARATOR = '•'
        val NUMBER = Regex("(\\d+)")
        val WHITESPACE = Regex("\\s+")
    }
}

/** Envelope of the plan list endpoint: `{ "result": [ ... ] }`. */
data class AgentPlansResponse(
    @SerializedName("result", alternate = ["Result"])
    val result: List<AgentPlan>? = null,
)
