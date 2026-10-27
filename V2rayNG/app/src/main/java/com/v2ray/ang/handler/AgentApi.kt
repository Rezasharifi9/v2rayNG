package com.v2ray.ang.handler

import com.v2ray.ang.BuildConfig
import com.v2ray.ang.util.JsonUtil

/** Outcome of a call to one of the agent endpoints. */
sealed class AgentApiResult<out T> {
    data class Success<T>(val data: T) : AgentApiResult<T>()

    /**
     * @param message The panel's own Persian explanation, or null when the call
     *                never got an answer at all.
     */
    data class Failure(val message: String?) : AgentApiResult<Nothing>()
}

/**
 * Shared plumbing of the `/User` endpoints that authenticate with the agent
 * token (`tbUsers.Token`), which has nothing to do with the subscription token
 * in a sub URL. A build sells for exactly one agent, so the token comes from
 * `AGENT_API_TOKEN` in `V2rayNG/.env` through BuildConfig.
 */
object AgentApi {

    /** The agent token this build was compiled with, or null when unset. */
    val token: String?
        get() = BuildConfig.AGENT_API_TOKEN.trim().takeIf { it.isNotEmpty() }

    /** Whether this build has an agent token at all. */
    fun isConfigured(): Boolean = token != null

    /** Absolute URL of an endpoint path such as `/User/GetAgentPlans`. */
    fun url(path: String): String = BuildConfig.SUB_API_BASE_URL.trimEnd('/') + path

    fun authHeaders(agentToken: String): Map<String, String> = mapOf(
        "Authorization" to agentToken,
        "Accept" to "application/json"
    )

    /**
     * Reads the message out of an error body. The panel answers errors with a
     * bare JSON string, e.g. `"تعرفه ای با این شناسه یافت نشد"`.
     *
     * @param body The raw response body.
     * @return The message, or null when the body carried none.
     */
    fun parseErrorMessage(body: String?): String? {
        val raw = body?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val parsed = runCatching { JsonUtil.fromJson(raw, String::class.java) }.getOrNull()
        return (parsed ?: raw).trim().takeIf { it.isNotEmpty() }
    }
}
