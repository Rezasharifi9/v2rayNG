package com.v2ray.ang.dto.entities

import com.google.gson.annotations.SerializedName

/**
 * A card-to-card invoice created for the agent's customer, as returned by
 * `POST {SUB_API_BASE_URL}/User/CreateAgentInvoice`.
 *
 * See `Docs/API-AgentInvoice.md` in the panel repository. Both the camelCase and
 * the PascalCase spelling are accepted, because the deployed panel is not always
 * the one the docs were written against.
 */
data class AgentInvoice(
    /** `{guid8}#{agent user id}` — the handle for checking the payment later. */
    @SerializedName("trackingCode", alternate = ["TrackingCode"])
    val trackingCode: String = "",
    /**
     * The exact amount in Rial the customer has to transfer, last three digits
     * included: the panel matches payments by amount, so it must not be rounded.
     */
    @SerializedName("amount", alternate = ["Amount"])
    val amount: Long = 0L,
    @SerializedName("cardNumber", alternate = ["CardNumber"])
    val cardNumber: String = "",
    @SerializedName("cardHolderName", alternate = ["CardHolderName"])
    val cardHolderName: String = "",
    /** Subscription name without its `$` and `@` parts; display only. */
    @SerializedName("subscriptionName", alternate = ["SubscriptionName"])
    val subscriptionName: String = "",
    /** Tariff price in Toman, before any agent discount. */
    @SerializedName("planPrice", alternate = ["PlanPrice"])
    val planPrice: Double = 0.0,
    @SerializedName("planVolume", alternate = ["PlanVolume"])
    val planVolume: Double = 0.0,
    @SerializedName("planMonth", alternate = ["PlanMonth"])
    val planMonth: Double = 0.0,
    @SerializedName("deviceLimit", alternate = ["DeviceLimit"])
    val deviceLimit: Int? = null,
)

/**
 * Payment state of an invoice, as returned by
 * `POST {SUB_API_BASE_URL}/User/CheckAgentInvoice`.
 */
data class AgentInvoiceStatus(
    @SerializedName("trackingCode", alternate = ["TrackingCode"])
    val trackingCode: String = "",
    /** The only field worth branching on: true once the payment was accepted. */
    @SerializedName("isConfirmed", alternate = ["IsConfirmed"])
    val isConfirmed: Boolean = false,
    /** Raw state: `FOR_PAY` / `FINISH` / `FOR_RESERVE`. */
    @SerializedName("status", alternate = ["Status"])
    val status: String? = null,
    /** `جدید` or `تمدید`. */
    @SerializedName("orderType", alternate = ["OrderType"])
    val orderType: String? = null,
    @SerializedName("subscriptionName", alternate = ["SubscriptionName"])
    val subscriptionName: String? = null,
    @SerializedName("amount", alternate = ["Amount"])
    val amount: Long = 0L,
    /** Set only once a *new* subscription was confirmed and created. */
    @SerializedName("subscriptionLink", alternate = ["SubscriptionLink"])
    val subscriptionLink: String? = null,
    @SerializedName("backupSubscriptionLink", alternate = ["BackupSubscriptionLink"])
    val backupSubscriptionLink: String? = null,
    /** Persian sentence that can be shown to the customer as is. */
    @SerializedName("message", alternate = ["Message"])
    val message: String? = null,
)
