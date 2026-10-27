package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.dto.entities.AgentPlan
import com.v2ray.ang.dto.entities.AgentPlansResponse
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil

/**
 * Reads the list of tariffs an agent sells.
 *
 * Authentication and the endpoint base live in [AgentApi]; see
 * `Docs/API-AgentPlans.md` in the panel repository for the payload.
 */
object AgentPlanManager {

    private const val PLANS_PATH = "/User/GetAgentPlans"
    private const val REQUEST_TIMEOUT_MS = 15_000

    /** Whether this build has an agent token at all. */
    fun isConfigured(): Boolean = AgentApi.isConfigured()

    /**
     * Fetches the tariffs of the configured agent. Network call, so it must not
     * run on the main thread.
     *
     * @return The plans sorted by duration and then price (an empty list when
     *         the agent has none), or null when the request failed or no agent
     *         token is configured.
     */
    fun fetchPlans(): List<AgentPlan>? {
        val token = AgentApi.token ?: return null
        val body = try {
            HttpUtil.getUrlContentWithUserAgent(
                UrlContentRequest(
                    url = AgentApi.url(PLANS_PATH),
                    timeout = REQUEST_TIMEOUT_MS,
                    requestHeaders = JsonUtil.toJson(AgentApi.authHeaders(token))
                )
            )
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to fetch agent plans", e)
            return null
        }
        if (body.isBlank()) return null

        // An error body is a bare JSON string, which parses to null here.
        val plans = JsonUtil.fromJsonSafe(body, AgentPlansResponse::class.java)?.result ?: return null
        // The panel already sorts by month, but the client groups by it, so make
        // the order explicit instead of relying on that.
        return plans.sortedWith(compareBy({ it.months }, { it.price }))
    }
}
