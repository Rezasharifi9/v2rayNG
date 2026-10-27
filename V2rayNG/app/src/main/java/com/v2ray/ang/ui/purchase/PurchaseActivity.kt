package com.v2ray.ang.ui.purchase

import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.extension.toast
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.util.Utils

/**
 * Four-step purchase wizard: pick the subscription, pick the tariff, review, and
 * pay the card-to-card invoice the panel issues. A confirmed payment installs
 * (or renews) the subscription and drops the wizard back to step one.
 */
class PurchaseActivity : BaseComponentActivity() {

    private val viewModel: PurchaseViewModel by viewModels()

    @Composable
    override fun ScreenContent() {
        PurchaseWizardScreen(viewModel = viewModel, onBackClick = { finish() })
    }
}

@Composable
fun PurchaseWizardScreen(
    viewModel: PurchaseViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadSubscriptions()
    }

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.title_purchase),
                onBackClick = onBackClick,
                isLoading = isLoading
            )
        }
    ) { innerPadding ->
        // Everything on this screen comes from the panel and is Persian, so lay
        // it out right-to-left regardless of the app locale.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Spacer(Modifier.height(14.dp))
                PurchaseStepper(current = uiState.step)
                Spacer(Modifier.height(10.dp))

                uiState.target?.takeIf { uiState.step != PurchaseStep.SUBSCRIPTION }?.let { target ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SelectedTargetChip(target = target)
                    }
                    Spacer(Modifier.height(6.dp))
                }

                Box(modifier = Modifier.weight(1f)) {
                    StepContent(
                        state = uiState,
                        viewModel = viewModel,
                        onCopy = { value ->
                            Utils.setClipboard(context, value)
                            context.toast(R.string.purchase_copied)
                        }
                    )
                }

                uiState.errorMessage?.let { message ->
                    PurchaseBanner(text = message, isError = true)
                }
                if (uiState.paymentPending) {
                    PurchaseBanner(
                        text = stringResource(R.string.purchase_payment_pending),
                        isError = false
                    )
                }

                WizardFooter(
                    state = uiState,
                    onBack = viewModel::goBack,
                    onNext = viewModel::goNext,
                    onRestart = viewModel::restartWizard
                )
            }
        }
    }

    uiState.successMessage?.let { message ->
        SuccessDialog(message = message, onDismiss = viewModel::dismissSuccess)
    }
}

@Composable
private fun StepContent(
    state: PurchaseUiState,
    viewModel: PurchaseViewModel,
    onCopy: (String) -> Unit
) {
    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            // Right-to-left layout: moving forward slides content in from the left.
            val offset = if (forward) 1 else -1
            (slideInHorizontally(tween(260)) { width -> -offset * width } + fadeIn(tween(200)))
                .togetherWith(
                    slideOutHorizontally(tween(260)) { width -> offset * width } +
                            fadeOut(tween(200))
                )
                .using(SizeTransform(clip = false))
        },
        label = "PurchaseStepContent"
    ) { step ->
        when (step) {
            PurchaseStep.SUBSCRIPTION -> SubscriptionStep(
                state = state,
                onSelect = viewModel::selectSubscription,
                onSelectNew = viewModel::selectNewSubscription
            )

            PurchaseStep.PLAN -> PlanStep(
                state = state,
                onSelect = viewModel::selectPlan,
                onRetry = { viewModel.loadPlans(force = true) }
            )

            PurchaseStep.REVIEW -> ReviewStep(state = state)

            PurchaseStep.PAYMENT -> state.invoice?.let { invoice ->
                PaymentStep(invoice = invoice, onCopy = onCopy)
            }
        }
    }
}

@Composable
private fun WizardFooter(
    state: PurchaseUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.canGoBack) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.purchase_back),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(if (state.canGoBack) 1.4f else 1f)
                        .height(48.dp),
                    enabled = state.canGoNext,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(primaryActionRes(state.step)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (state.step == PurchaseStep.PAYMENT) {
                TextButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy
                ) {
                    Text(
                        text = stringResource(R.string.purchase_restart),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun primaryActionRes(step: PurchaseStep): Int = when (step) {
    PurchaseStep.SUBSCRIPTION, PurchaseStep.PLAN -> R.string.purchase_next
    PurchaseStep.REVIEW -> R.string.purchase_create_invoice
    PurchaseStep.PAYMENT -> R.string.purchase_paid
}

@Composable
private fun PurchaseBanner(text: String, isError: Boolean) {
    val color = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.secondary
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.10f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
private fun SuccessDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.purchase_success_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.purchase_success_confirm))
            }
        }
    )
}
