package com.v2ray.ang.ui.purchase

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.AgentInvoice
import com.v2ray.ang.dto.entities.AgentPlan
import com.v2ray.ang.dto.entities.SubscriptionInfo
import kotlin.math.roundToInt

private val ContentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

// ---------------------------------------------------------------- step 1

@Composable
fun SubscriptionStep(
    state: PurchaseUiState,
    onSelect: (SubscriptionChoice) -> Unit,
    onSelectNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.secondary
    val newAccent = MaterialTheme.colorScheme.tertiary
    val selectedId = (state.target as? PurchaseTarget.Renew)?.choice?.subId

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = ContentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            StepHint(
                text = if (state.subscriptions.isEmpty() && state.subscriptionsLoaded) {
                    stringResource(R.string.purchase_no_subscriptions)
                } else {
                    stringResource(R.string.purchase_subscription_hint)
                }
            )
        }

        items(state.subscriptions, key = { it.subId }) { choice ->
            SubscriptionCard(
                choice = choice,
                selected = choice.subId == selectedId,
                accent = accent,
                onClick = { onSelect(choice) }
            )
        }

        item {
            Spacer(Modifier.height(2.dp))
            NewSubscriptionCard(
                selected = state.target is PurchaseTarget.New,
                accent = newAccent,
                onClick = onSelectNew
            )
        }
    }
}

@Composable
private fun SubscriptionCard(
    choice: SubscriptionChoice,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    SelectableCard(selected = selected, accent = accent, onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = choice.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = choice.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subscriptionStatusLabel(choice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            SelectionMark(selected = selected, accent = accent)
        }
    }
}

@Composable
private fun subscriptionStatusLabel(choice: SubscriptionChoice): String {
    val days = choice.remainingDays
    return when {
        days == null -> choice.expireDate.orEmpty()
        days == SubscriptionInfo.REMAINING_DAYS_UNLIMITED ->
            stringResource(R.string.purchase_sub_no_expiry)

        days == 0 -> stringResource(R.string.purchase_sub_expired)
        else -> {
            val remaining = stringResource(R.string.purchase_sub_remaining_format, days)
            val expire = choice.expireDate
            if (expire.isNullOrBlank()) {
                remaining
            } else {
                "$remaining • ${stringResource(R.string.purchase_sub_expire_format, expire)}"
            }
        }
    }
}

@Composable
private fun NewSubscriptionCard(
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    SelectableCard(selected = selected, accent = accent, onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accent
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.purchase_new_subscription_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.purchase_new_subscription_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            SelectionMark(selected = selected, accent = accent)
        }
    }
}

// ---------------------------------------------------------------- step 2

@Composable
fun PlanStep(
    state: PurchaseUiState,
    onSelect: (AgentPlan) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.secondary

    val error = state.plansError
    if (error != null) {
        PurchaseMessage(
            message = stringResource(
                when (error) {
                    PurchaseError.NOT_CONFIGURED -> R.string.purchase_not_configured
                    PurchaseError.REQUEST_FAILED -> R.string.purchase_load_failed
                }
            ),
            onRetry = if (error == PurchaseError.REQUEST_FAILED) onRetry else null,
            modifier = modifier
        )
        return
    }
    if (state.plans.isEmpty()) {
        if (state.plansLoaded) {
            PurchaseMessage(
                message = stringResource(R.string.purchase_empty),
                modifier = modifier
            )
        }
        return
    }

    // The panel sorts by duration; group on the same key so a month is one block.
    val groups = state.plans.groupBy { it.months }.toSortedMap()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = ContentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { StepHint(text = stringResource(R.string.purchase_plan_hint)) }

        groups.forEach { (months, plans) ->
            item(key = "header-$months") {
                SectionHeader(
                    text = stringResource(R.string.purchase_plan_month_format, months),
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }
            items(plans, key = { it.planId }) { plan ->
                PlanCard(
                    plan = plan,
                    selected = state.selectedPlan?.planId == plan.planId,
                    accent = accent,
                    onClick = { onSelect(plan) }
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: AgentPlan,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    SelectableCard(selected = selected, accent = accent, onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionMark(selected = selected, accent = accent)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = planDetailLabel(plan),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(
                        R.string.purchase_price_format,
                        formatAmount(plan.price)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
        }
    }
}

/** Label of a tariff without its duration, which the section header carries. */
@Composable
fun planDetailLabel(plan: AgentPlan): String {
    plan.rawDetail?.let { return it }
    val deviceLimit = plan.deviceLimit
    return when {
        plan.isUnlimited && deviceLimit != null ->
            stringResource(R.string.purchase_plan_unlimited_devices_format, deviceLimit)

        plan.isUnlimited -> stringResource(R.string.purchase_plan_unlimited)
        else -> stringResource(
            R.string.purchase_plan_volume_format,
            formatVolume(plan.volumeGb ?: 0.0)
        )
    }
}

// ---------------------------------------------------------------- step 3

@Composable
fun ReviewStep(
    state: PurchaseUiState,
    modifier: Modifier = Modifier
) {
    val plan = state.selectedPlan ?: return
    val target = state.target ?: return
    val accent = MaterialTheme.colorScheme.secondary

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
    ) {
        StepHint(text = stringResource(R.string.purchase_review_hint))
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                DetailRow(
                    label = stringResource(R.string.purchase_review_sub_label),
                    value = when (target) {
                        is PurchaseTarget.New -> stringResource(R.string.purchase_selected_new)
                        is PurchaseTarget.Renew -> target.choice.name
                    }
                )
                DetailRow(
                    label = stringResource(R.string.purchase_review_token_label),
                    value = when (target) {
                        is PurchaseTarget.New ->
                            stringResource(R.string.purchase_review_token_new)

                        is PurchaseTarget.Renew -> target.choice.token
                    },
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DetailRow(
                    label = stringResource(R.string.purchase_review_plan_label),
                    value = "${stringResource(R.string.purchase_plan_month_format, plan.months)} • ${planDetailLabel(plan)}"
                )
                DetailRow(
                    label = stringResource(R.string.purchase_review_price_label),
                    value = stringResource(
                        R.string.purchase_price_format,
                        formatAmount(plan.price)
                    ),
                    valueColor = accent,
                    emphasized = true
                )
            }
        }
    }
}

// ---------------------------------------------------------------- step 4

@Composable
fun PaymentStep(
    invoice: AgentInvoice,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.secondary

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = ContentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.purchase_invoice_registered),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.height(10.dp))
                    DetailRow(
                        label = stringResource(R.string.purchase_invoice_tracking_label),
                        value = invoice.trackingCode
                    )
                    if (invoice.subscriptionName.isNotBlank()) {
                        DetailRow(
                            label = stringResource(R.string.purchase_review_sub_label),
                            value = invoice.subscriptionName
                        )
                    }
                    invoicePlanLabel(invoice)?.let { label ->
                        DetailRow(
                            label = stringResource(R.string.purchase_review_plan_label),
                            value = label
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.purchase_invoice_amount_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            CopyableValue(
                value = stringResource(
                    R.string.purchase_amount_rial_format,
                    formatAmount(invoice.amount)
                ),
                actionLabel = stringResource(R.string.purchase_copy_amount),
                accent = accent,
                emphasized = true,
                onCopy = { onCopy(invoice.amount.toString()) }
            )
        }

        item {
            CopyableValue(
                value = invoice.cardNumber,
                caption = stringResource(
                    R.string.purchase_invoice_card_holder_format,
                    invoice.cardHolderName
                ),
                actionLabel = stringResource(R.string.purchase_copy_card),
                accent = accent,
                emphasized = true,
                onCopy = { onCopy(invoice.cardNumber) }
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InvoiceNote(stringResource(R.string.purchase_invoice_tip_copy))
                InvoiceNote(stringResource(R.string.purchase_invoice_tip_exact))
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.purchase_invoice_note_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    InvoiceNote(
                        text = stringResource(R.string.purchase_invoice_note_expiry),
                        bold = true
                    )
                    InvoiceNote(
                        text = stringResource(R.string.purchase_invoice_note_wrong_amount),
                        bold = true
                    )
                    InvoiceNote(
                        text = stringResource(R.string.purchase_invoice_note_apps),
                        bold = true
                    )
                }
            }
        }
    }
}

/** Builds `3 ماهه • 50 گیگ` out of what the invoice reports about the tariff. */
@Composable
private fun invoicePlanLabel(invoice: AgentInvoice): String? {
    val months = invoice.planMonth.roundToInt()
    if (months <= 0) return null
    val duration = stringResource(R.string.purchase_plan_month_format, months)
    val deviceLimit = invoice.deviceLimit
    val detail = if (deviceLimit != null) {
        stringResource(R.string.purchase_plan_unlimited_devices_format, deviceLimit)
    } else {
        stringResource(R.string.purchase_plan_volume_format, formatVolume(invoice.planVolume))
    }
    return "$duration • $detail"
}

@Composable
private fun CopyableValue(
    value: String,
    actionLabel: String,
    accent: Color,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    emphasized: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onCopy),
        shape = shape,
        color = accent.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = if (emphasized) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (caption != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy),
                    contentDescription = actionLabel,
                    modifier = Modifier.size(18.dp),
                    tint = accent
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun InvoiceNote(text: String, bold: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.6f
    )
}

// ---------------------------------------------------------------- shared

@Composable
private fun StepHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    )
}

@Composable
fun PurchaseMessage(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.purchase_retry))
            }
        }
    }
}
