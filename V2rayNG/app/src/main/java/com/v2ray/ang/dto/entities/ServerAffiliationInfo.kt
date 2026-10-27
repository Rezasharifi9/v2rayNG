package com.v2ray.ang.dto.entities

data class ServerAffiliationInfo(var testDelayMillis: Long = 0L) {
    fun getTestDelayString(): String {
        if (testDelayMillis == 0L) {
            return ""
        }
        // A failed test carries no duration, so the "ms" suffix would be misleading.
        if (testDelayMillis < 0L) {
            return testDelayMillis.toString()
        }
        return testDelayMillis.toString() + "ms"
    }
}