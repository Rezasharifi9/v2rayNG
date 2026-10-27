package com.v2ray.ang.ui.purchase

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Step-by-step indicator in the wizard header: every stage is a numbered dot,
 * finished stages carry a check and the connectors fill up as the customer
 * moves forward.
 */
@Composable
fun PurchaseStepper(
    current: PurchaseStep,
    modifier: Modifier = Modifier
) {
    val steps = PurchaseStep.ordered
    val currentIndex = steps.indexOf(current)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, step ->
                if (index > 0) {
                    StepConnector(filled = index <= currentIndex, modifier = Modifier.weight(1f))
                }
                StepDot(
                    number = index + 1,
                    done = index < currentIndex,
                    active = index == currentIndex
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(stepTitleRes(current)),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(
                R.string.purchase_step_counter_format,
                currentIndex + 1,
                steps.size
            ),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StepDot(number: Int, done: Boolean, active: Boolean) {
    val accent = MaterialTheme.colorScheme.secondary
    val idle = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val background by animateColorAsState(
        targetValue = if (done || active) accent else idle,
        animationSpec = tween(250),
        label = "StepDotBackground"
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(250),
        label = "StepDotRing"
    )
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        if (ringAlpha > 0f) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f * ringAlpha))
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_done),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            } else {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (active) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun StepConnector(filled: Boolean, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        targetValue = if (filled) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        },
        animationSpec = tween(250),
        label = "StepConnector"
    )
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}

fun stepTitleRes(step: PurchaseStep): Int = when (step) {
    PurchaseStep.SUBSCRIPTION -> R.string.purchase_step_subscription
    PurchaseStep.PLAN -> R.string.purchase_step_plan
    PurchaseStep.REVIEW -> R.string.purchase_step_review
    PurchaseStep.PAYMENT -> R.string.purchase_step_payment
}

/**
 * Reminder of what the customer picked in step one. Stays on screen from step
 * two onwards so the target of the purchase is never in doubt.
 */
@Composable
fun SelectedTargetChip(
    target: PurchaseTarget,
    modifier: Modifier = Modifier
) {
    val isNew = target is PurchaseTarget.New
    val accent = if (isNew) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
    val label = when (target) {
        is PurchaseTarget.New -> stringResource(R.string.purchase_selected_new)
        is PurchaseTarget.Renew -> stringResource(
            R.string.purchase_selected_renew_format,
            target.choice.name
        )
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Card used for every pickable row in the wizard, with the selection state drawn
 * as an accent border plus a tinted background.
 */
@Composable
fun SelectableCard(
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
        animationSpec = tween(200),
        label = "SelectableCardBorder"
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .border(width = if (selected) 1.5.dp else 1.dp, color = borderColor, shape = shape),
        shape = shape,
        color = if (selected) accent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    ) {
        content()
    }
}

/** Small round check badge shown on the selected card. */
@Composable
fun SelectionMark(selected: Boolean, accent: Color) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) accent else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_action_done),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

/** Section heading used above the tariff groups. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        )
    }
}

/** Label/value line used by the review and invoice cards. */
@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    emphasized: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = if (emphasized) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

/** Renders a gigabyte value with at most two decimals and no trailing zeros. */
fun formatVolume(value: Double): String {
    val rounded = (value * 100).roundToLong() / 100.0
    if (rounded == rounded.toLong().toDouble()) return rounded.toLong().toString()
    return String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
}

/** Renders an amount with thousands separators and no decimals. */
fun formatAmount(value: Double): String = String.format(Locale.US, "%,d", value.toLong())

/** Renders an amount with thousands separators and no decimals. */
fun formatAmount(value: Long): String = String.format(Locale.US, "%,d", value)
