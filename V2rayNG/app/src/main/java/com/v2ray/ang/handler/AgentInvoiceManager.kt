package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.AgentInvoice
import com.v2ray.ang.dto.entities.AgentInvoiceStatus
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil

/**
 * Creates card-to-card invoices for the agent's customers and reads back whether
 * they were paid.
 *
 * See `Docs/API-AgentInvoice.md` in the panel repository. Both calls are POSTs
 * so neither the agent token nor a subscription token ends up in an IIS log.
 */
object AgentInvoiceManager {

    private const val CREATE_PATH = "/User/CreateAgentInvoice"
    private const val CHECK_PATH = "/User/CheckAgentInvoice"
    private const val REQUEST_TIMEOUT_MS = 20_000

    /**
     * Creates an invoice for one tariff. Network call, so it must not run on the
     * main thread.
     *
     * @param planId `tbLinkUserAndPlans.Link_PU_ID` of the chosen tariff.
     * @param subscriptionToken The token of the subscription to renew, or null
     *                          to have the panel generate a new subscription.
     * @return The invoice, or the panel's reason for refusing it.
     */
    fun createInvoice(planId: Int, subscriptionToken: String?): AgentApiResult<AgentInvoice> {
        val body = JsonUtil.toJson(
            mapOf(
                "PlanId" to planId,
                "SubscriptionToken" to subscriptionToken.orEmpty(),
                // Ties the invoice to this handset's tbMobileUsers row. Empty when
                // the device id is unreadable — the panel then files the invoice
                // without a device rather than refusing it.
                "AndroidId" to DeviceRegistrationManager.androidId().orEmpty()
            )
        )
        return call(CREATE_PATH, body, AgentInvoice::class.java)
    }

    /**
     * Asks whether an invoice has been paid. Network call, so it must not run on
     * the main thread.
     *
     * @param trackingCode The `trackingCode` [createInvoice] returned.
     * @return The payment state, or the panel's reason for refusing the lookup.
     *         An unpaid invoice is a successful answer, not a failure.
     */
    fun checkInvoice(trackingCode: String): AgentApiResult<AgentInvoiceStatus> {
        val body = JsonUtil.toJson(mapOf("TaxId" to trackingCode))
        return call(CHECK_PATH, body, AgentInvoiceStatus::class.java)
    }

    private fun <T> call(path: String, body: String, cls: Class<T>): AgentApiResult<T> {
        val token = AgentApi.token ?: return AgentApiResult.Failure(null)
        val response = HttpUtil.postJson(
            url = AgentApi.url(path),
            jsonBody = body,
            headers = AgentApi.authHeaders(token),
            timeout = REQUEST_TIMEOUT_MS
        ) ?: return AgentApiResult.Failure(null)

        if (!response.isSuccessful) {
            val message = AgentApi.parseErrorMessage(response.body)
            LogUtil.w(AppConfig.TAG, "Agent invoice call $path failed: ${response.code} $message")
            return AgentApiResult.Failure(message)
        }
        val parsed = response.body?.let { JsonUtil.fromJsonSafe(it, cls) }
            ?: return AgentApiResult.Failure(null)
        return AgentApiResult.Success(parsed)
    }
}
