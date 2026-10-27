package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.SubscriptionInfo
import com.v2ray.ang.ui.compose.AppDivider
import com.v2ray.ang.ui.compose.colorPing
import com.v2ray.ang.ui.compose.colorPingRed
import com.v2ray.ang.ui.compose.verticalScrollbar
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun MainDrawerContent(
    subscriptionInfo: SubscriptionInfo?,
    onNavigate: (String) -> Unit
) {
    val drawerScrollState = rememberScrollState()

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .navigationBarsPadding(),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(drawerScrollState)
                .verticalScrollbar(drawerScrollState)
                .padding(bottom = 80.dp)
        ) {
            SubscriptionInfoHeader(subscriptionInfo)
            DrawerMenuGroup(
                items = listOf(
                    DrawerMenuItemData(R.drawable.ic_cart_24dp, R.string.title_purchase, "purchase"),
                    DrawerMenuItemData(R.drawable.ic_subscriptions_24dp, R.string.title_sub_setting, "sub_setting"),
                    DrawerMenuItemData(R.drawable.ic_per_apps_24dp, R.string.per_app_proxy_settings, "per_app_proxy"),
                    DrawerMenuItemData(R.drawable.ic_routing_24dp, R.string.routing_settings_title, "routing_setting"),
                    DrawerMenuItemData(R.drawable.ic_file_24dp, R.string.title_user_asset_setting, "user_asset"),
                    DrawerMenuItemData(R.drawable.ic_settings_24dp, R.string.title_settings, "settings")
                ),
                onNavigate = onNavigate
            )
            AppDivider()
            DrawerMenuGroup(
                items = listOf(
                    DrawerMenuItemData(R.drawable.ic_promotion_24dp, R.string.title_pref_promotion, "promotion"),
                    DrawerMenuItemData(R.drawable.ic_logcat_24dp, R.string.title_logcat, "logcat"),
                    DrawerMenuItemData(R.drawable.ic_check_update_24dp, R.string.update_check_for_update, "check_update"),
                    DrawerMenuItemData(R.drawable.ic_restore_24dp, R.string.title_configuration_backup_restore, "backup_restore"),
                    DrawerMenuItemData(R.drawable.ic_about_24dp, R.string.title_about, "about")
                ),
                onNavigate = onNavigate
            )
        }
    }
}

/**
 * Drawer header showing the quota of the active subscription. Falls back to the
 * app name while no info has been fetched yet (no token in the sub URL, offline,
 * or no subscription at all).
 */
@Composable
private fun SubscriptionInfoHeader(info: SubscriptionInfo?) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        if (info == null) {
            SubscriptionInfoPlaceholder()
        } else {
            // The panel content is Persian, so lay it out right-to-left
            // regardless of the app locale. Scoped to this header only.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SubscriptionInfoBody(info)
            }
        }
    }
}

@Composable
private fun SubscriptionInfoPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = FontFamily(Font(R.font.montserrat_thin)),
                fontWeight = FontWeight.Thin
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.sub_info_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SubscriptionInfoBody(info: SubscriptionInfo) {
    val name = info.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.app_name)
    val hasVolumeLimit = info.totalVolumeGb > 0.0
    val usedFraction = if (hasVolumeLimit) {
        (info.usedVolumeGb / info.totalVolumeGb).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }
    // Flag the two states the user actually needs to act on: quota used up, or expired.
    val quotaExhausted = hasVolumeLimit && info.usedVolumeGb >= info.totalVolumeGb
    val expired = info.hasExpiry && info.remainingDays == 0
    val accent = if (quotaExhausted || expired) colorPingRed else colorPing

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        // The quota rows below stay right-to-left, but the avatar and the
        // subscription name are pinned to the left edge of the drawer.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        InfoRow(
            label = stringResource(R.string.sub_info_volume_label),
            value = if (hasVolumeLimit) {
                stringResource(
                    R.string.sub_info_volume_format,
                    formatGb(info.usedVolumeGb),
                    formatGb(info.totalVolumeGb)
                )
            } else {
                stringResource(R.string.sub_info_unlimited)
            },
            valueColor = if (quotaExhausted) colorPingRed else MaterialTheme.colorScheme.onSurface
        )

        if (hasVolumeLimit) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { usedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }

        Spacer(Modifier.height(12.dp))

        InfoRow(
            label = stringResource(R.string.sub_info_expire_label),
            value = info.expireDate?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.sub_info_unlimited),
            valueColor = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(6.dp))

        InfoRow(
            label = stringResource(R.string.sub_info_remaining_label),
            value = when {
                !info.hasExpiry -> stringResource(R.string.sub_info_unlimited)
                info.remainingDays == 0 -> stringResource(R.string.sub_info_expired)
                else -> stringResource(R.string.sub_info_remaining_days_format, info.remainingDays)
            },
            valueColor = if (expired) colorPingRed else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Renders a gigabyte value with at most two decimals and no trailing zeros. */
internal fun formatGb(value: Double): String {
    val rounded = (value * 100).roundToLong() / 100.0
    if (rounded == rounded.toLong().toDouble()) return rounded.toLong().toString()
    return String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
}

data class DrawerMenuItemData(
    val iconRes: Int,
    val labelRes: Int,
    val route: String
)

@Composable
private fun DrawerMenuGroup(
    items: List<DrawerMenuItemData>,
    onNavigate: (String) -> Unit
) {
    items.forEach { item ->
        DrawerMenuItem(
            icon = painterResource(item.iconRes),
            label = stringResource(item.labelRes),
            onClick = { onNavigate(item.route) }
        )
    }
}

@Composable
fun DrawerMenuItem(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
