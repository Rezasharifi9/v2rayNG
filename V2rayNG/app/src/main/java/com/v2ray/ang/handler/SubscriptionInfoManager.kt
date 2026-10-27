package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.dto.entities.SubscriptionInfo
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import java.net.URI
import java.net.URLEncoder

/**
 * Fetches and caches the quota summary of a subscription.
 *
 * The panel identifies a subscription purely by the `token` query parameter of
 * its subscribe link, so the token is pulled straight out of the stored
 * subscription URL. The endpoint host comes from `SUB_API_BASE_URL` in
 * `V2rayNG/.env`.
 */
object SubscriptionInfoManager {

    private const val TOKEN_QUERY_KEY = "token"
    private const val INFO_PATH = "/api/v1/Sub/Info"
    private const val REQUEST_TIMEOUT_MS = 10_000

    /** Remark AngConfigManager assigns to a subscription imported without a fragment. */
    private const val PLACEHOLDER_REMARKS = "import sub"

    /** Cached info older than this is refreshed on the next read. */
    private const val CACHE_TTL_MS = 5 * 60 * 1000L

    /**
     * Extracts the `token` query parameter from a subscription URL.
     *
     * @param url The subscription URL.
     * @return The token, or null when the URL carries none.
     */
    fun extractToken(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val query = URI(Utils.fixIllegalUrl(url.trim())).rawQuery ?: return null
            query.split('&')
                .firstOrNull { it.substringBefore('=').equals(TOKEN_QUERY_KEY, ignoreCase = true) }
                ?.substringAfter('=', "")
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { Utils.decodeURIComponent(it) }.getOrDefault(it) }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to extract subscription token", e)
            null
        }
    }

    /**
     * Calls the panel and returns the quota info for a token.
     *
     * @param token The subscription token.
     * @return The parsed info, or null on a network/parse error or an
     *         unsuccessful response.
     */
    fun fetch(token: String): SubscriptionInfo? {
        if (token.isBlank()) return null
        val url = buildString {
            append(BuildConfig.SUB_API_BASE_URL.trimEnd('/'))
            append(INFO_PATH)
            append('?')
            append(TOKEN_QUERY_KEY)
            append('=')
            append(URLEncoder.encode(token, Charsets.UTF_8.name()))
        }
        val body = HttpUtil.getUrlContent(
            UrlContentRequest(url = url, timeout = REQUEST_TIMEOUT_MS)
        )
        if (body.isNullOrBlank()) return null

        val info = JsonUtil.fromJsonSafe(body, SubscriptionInfo::class.java) ?: return null
        if (!info.success) {
            LogUtil.w(AppConfig.TAG, "Subscription info request rejected: ${info.message}")
            return null
        }
        return info.copy(fetchedAt = System.currentTimeMillis())
    }

    /**
     * Fetches the info for a subscription and caches it. Network call, so it
     * must not run on the main thread.
     *
     * @param subId The subscription ID.
     * @param url The subscription URL the token is taken from.
     * @return The fresh info, or null when it could not be fetched.
     */
    fun refresh(subId: String, url: String?): SubscriptionInfo? {
        if (subId.isBlank()) return null
        val token = extractToken(url) ?: return null
        val info = try {
            fetch(token)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to fetch subscription info", e)
            null
        } ?: return null
        MmkvManager.encodeSubscriptionInfo(subId, info)
        adoptPanelName(subId, info.name)
        return info
    }

    /**
     * Renames a subscription to the name the panel reports, but only while it
     * still carries the placeholder remark the importer generated — a name the
     * user chose themselves is never overwritten.
     */
    private fun adoptPanelName(subId: String, panelName: String?) {
        val name = panelName?.trim()?.takeIf { it.isNotBlank() } ?: return
        val item = MmkvManager.decodeSubscription(subId) ?: return
        val remarks = item.remarks.trim()
        if (remarks.isNotEmpty() && !remarks.equals(PLACEHOLDER_REMARKS, ignoreCase = true)) return
        if (remarks == name) return
        item.remarks = name
        MmkvManager.encodeSubscription(subId, item)
    }

    /**
     * Returns the cached info for a subscription, refreshing it when it is
     * missing or older than [CACHE_TTL_MS]. Network call, so it must not run on
     * the main thread.
     *
     * @param subId The subscription ID.
     * @return The cached or freshly fetched info, or null when unavailable.
     */
    fun getOrRefresh(subId: String): SubscriptionInfo? {
        if (subId.isBlank()) return null
        val cached = MmkvManager.decodeSubscriptionInfo(subId)
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL_MS) {
            return cached
        }
        val url = MmkvManager.decodeSubscription(subId)?.url
        return refresh(subId, url) ?: cached
    }

    /**
     * Returns the cached info without touching the network.
     *
     * @param subId The subscription ID.
     * @return The cached info, or null when nothing was cached yet.
     */
    fun getCached(subId: String): SubscriptionInfo? = MmkvManager.decodeSubscriptionInfo(subId)
}
