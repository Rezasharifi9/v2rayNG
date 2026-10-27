package com.v2ray.ang.handler

import com.v2ray.ang.dto.entities.AgentInvoice
import com.v2ray.ang.util.JsonUtil

/**
 * The invoice a customer is currently paying, kept across app restarts.
 *
 * Paying card-to-card means leaving the app for a banking one, and the tracking
 * code is the only way back to the invoice — losing it on the way out would
 * strand a real payment.
 */
data class PendingInvoice(
    val invoice: AgentInvoice = AgentInvoice(),
    /** True when the invoice buys a brand new subscription. */
    val isNew: Boolean = true,
    /** Subscription being renewed; null for a new one. */
    val subId: String? = null,
    val subName: String? = null,
    val subToken: String? = null,
)

object PendingInvoiceStore {

    private const val KEY = "pref_pending_agent_invoice"

    fun save(pending: PendingInvoice) {
        MmkvManager.encodeSettings(KEY, JsonUtil.toJson(pending))
    }

    /** @return The stored invoice, or null when none is waiting for payment. */
    fun load(): PendingInvoice? {
        val json = MmkvManager.decodeSettingsString(KEY)?.takeIf { it.isNotBlank() } ?: return null
        val pending = JsonUtil.fromJsonSafe(json, PendingInvoice::class.java) ?: return null
        return pending.takeIf { it.invoice.trackingCode.isNotBlank() }
    }

    fun clear() {
        MmkvManager.encodeSettings(KEY, "")
    }
}
