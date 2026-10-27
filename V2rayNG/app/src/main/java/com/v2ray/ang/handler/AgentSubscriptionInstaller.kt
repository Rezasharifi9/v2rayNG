package com.v2ray.ang.handler

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.TestServiceMessage
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.util.LogUtil

/**
 * Takes the subscription link a freshly paid invoice produced and turns it into
 * a usable group in the app: stored, pulled once, and measured right away so the
 * customer sees ranked servers instead of an untested list.
 */
object AgentSubscriptionInstaller {

    /**
     * Adds (or reuses) the subscription behind [link], downloads its configs and
     * starts a relay delay test over them. Blocking network work, so it must not
     * run on the main thread.
     *
     * @param context Context used to start the test service.
     * @param link The subscribe URL returned by the invoice check.
     * @param name Name to label the subscription with; blank falls back to the
     *             remark the importer would have generated.
     * @return The subscription ID, or null when it could not be stored.
     */
    fun install(context: Context, link: String, name: String?): String? {
        val url = link.trim()
        if (url.isEmpty()) return null

        val subId = findByUrl(url) ?: create(url, name) ?: return null
        val item = MmkvManager.decodeSubscription(subId) ?: return null

        val result = AngConfigManager.updateConfigViaSub(SubscriptionCache(subId, item))
        LogUtil.i(
            AppConfig.TAG,
            "Installed purchased subscription $subId with ${result.configCount} configs"
        )

        // Land on the new group when the user gets back to the server list.
        MmkvManager.encodeSettings(AppConfig.CACHE_SUBSCRIPTION_ID, subId)
        SettingsChangeManager.makeSetupGroupTab()

        if (result.configCount > 0) {
            MessageHelper.sendMsg2TestService(
                context,
                TestServiceMessage(
                    key = AppConfig.MSG_MEASURE_CONFIG_START,
                    subscriptionId = subId
                )
            )
        }
        return subId
    }

    private fun findByUrl(url: String): String? =
        MmkvManager.decodeSubscriptions().firstOrNull { it.subscription.url == url }?.guid

    /**
     * Stores a new subscription. [MmkvManager.encodeSubscription] generates the
     * ID itself and does not hand it back, so it is looked up afterwards.
     */
    private fun create(url: String, name: String?): String? {
        val item = SubscriptionItem().apply {
            remarks = name?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_REMARKS
            this.url = url
        }
        MmkvManager.encodeSubscription("", item)
        return findByUrl(url)
    }

    /** Remark AngConfigManager assigns to a subscription imported without a fragment. */
    private const val DEFAULT_REMARKS = "import sub"
}
