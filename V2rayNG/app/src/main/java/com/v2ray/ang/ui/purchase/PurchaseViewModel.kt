package com.v2ray.ang.ui.purchase

import android.app.Application
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.AgentInvoice
import com.v2ray.ang.dto.entities.AgentPlan
import com.v2ray.ang.handler.AgentApiResult
import com.v2ray.ang.handler.AgentInvoiceManager
import com.v2ray.ang.handler.AgentPlanManager
import com.v2ray.ang.handler.AgentSubscriptionInstaller
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.PendingInvoice
import com.v2ray.ang.handler.PendingInvoiceStore
import com.v2ray.ang.handler.SubscriptionInfoManager
import com.v2ray.ang.ui.base.BaseViewModel
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** The four stages of the purchase wizard, in the order they are walked. */
enum class PurchaseStep {
    /** Renew one of the subscriptions on the phone, or ask for a new one. */
    SUBSCRIPTION,

    /** Pick a tariff from the agent's price list. */
    PLAN,

    /** Read everything back before anything is created on the panel. */
    REVIEW,

    /** Transfer the money and have the panel confirm it. */
    PAYMENT;

    companion object {
        val ordered: List<PurchaseStep> = entries
    }
}

/** Why the tariff list could not be shown. */
enum class PurchaseError {
    /** The build carries no agent token, so there is nothing to ask for. */
    NOT_CONFIGURED,

    /** The panel could not be reached or answered with an error. */
    REQUEST_FAILED,
}

/** One subscription already on the phone, as offered for renewal. */
data class SubscriptionChoice(
    val subId: String,
    val name: String,
    /** The `token` query parameter of the sub URL; the panel renews by this. */
    val token: String,
    val expireDate: String? = null,
    val remainingDays: Int? = null,
)

/** What the customer is buying for. */
sealed class PurchaseTarget {
    /** A subscription the panel has to create from scratch. */
    data object New : PurchaseTarget()

    /** An existing subscription to extend. */
    data class Renew(val choice: SubscriptionChoice) : PurchaseTarget()
}

data class PurchaseUiState(
    val step: PurchaseStep = PurchaseStep.SUBSCRIPTION,
    val subscriptions: List<SubscriptionChoice> = emptyList(),
    val subscriptionsLoaded: Boolean = false,
    val target: PurchaseTarget? = null,
    val plans: List<AgentPlan> = emptyList(),
    val plansLoaded: Boolean = false,
    val plansError: PurchaseError? = null,
    val selectedPlan: AgentPlan? = null,
    val invoice: AgentInvoice? = null,
    /** A request is in flight; the step's primary button waits for it. */
    val busy: Boolean = false,
    /** Panel refusal or connection problem, shown inline on the current step. */
    val errorMessage: String? = null,
    /** The payment check ran but the panel has not seen the money yet. */
    val paymentPending: Boolean = false,
    /** Set once a purchase completed; the wizard is already reset behind it. */
    val successMessage: String? = null,
) {
    val canGoNext: Boolean
        get() = when (step) {
            PurchaseStep.SUBSCRIPTION -> target != null
            PurchaseStep.PLAN -> selectedPlan != null
            PurchaseStep.REVIEW -> selectedPlan != null && !busy
            PurchaseStep.PAYMENT -> invoice != null && !busy
        }

    /**
     * Whether a step back is offered. The payment step is deliberately excluded:
     * its invoice already exists on the panel.
     */
    val canGoBack: Boolean
        get() = !busy && (step == PurchaseStep.PLAN || step == PurchaseStep.REVIEW)

    /** Name of the picked subscription, kept visible from step two onwards. */
    val targetName: String?
        get() = when (val current = target) {
            is PurchaseTarget.Renew -> current.choice.name
            else -> null
        }

    val isNewSubscription: Boolean get() = target is PurchaseTarget.New
}

class PurchaseViewModel(application: Application) : BaseViewModel(application) {

    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState.asStateFlow()

    // ---------- Step 1: which subscription ----------

    /**
     * Lists the subscriptions that carry a token, refreshing their quota in the
     * background so the expiry dates shown are the panel's.
     */
    fun loadSubscriptions() {
        val stored = readSubscriptions()
        _uiState.update { it.copy(subscriptions = stored, subscriptionsLoaded = true) }
        restorePendingInvoice()

        launchLoading {
            val refreshed = withContext(Dispatchers.IO) {
                stored.map { choice ->
                    val info = try {
                        SubscriptionInfoManager.getOrRefresh(choice.subId)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (e: Exception) {
                        LogUtil.e(AppConfig.TAG, "Failed to refresh subscription info", e)
                        null
                    } ?: return@map choice
                    choice.copy(
                        name = info.name?.takeIf { it.isNotBlank() } ?: choice.name,
                        expireDate = info.expireDate,
                        remainingDays = info.remainingDays
                    )
                }
            }
            _uiState.update { state ->
                // Keep the picked subscription in sync with its refreshed name.
                val target = state.target
                val newTarget = if (target is PurchaseTarget.Renew) {
                    refreshed.firstOrNull { it.subId == target.choice.subId }
                        ?.let { PurchaseTarget.Renew(it) } ?: target
                } else {
                    target
                }
                state.copy(subscriptions = refreshed, target = newTarget)
            }
        }
    }

    private fun readSubscriptions(): List<SubscriptionChoice> =
        MmkvManager.decodeSubscriptions().mapNotNull { cache ->
            val token = SubscriptionInfoManager.extractToken(cache.subscription.url) ?: return@mapNotNull null
            val cached = SubscriptionInfoManager.getCached(cache.guid)
            SubscriptionChoice(
                subId = cache.guid,
                name = cached?.name?.takeIf { it.isNotBlank() }
                    ?: cache.subscription.remarks.takeIf { it.isNotBlank() }
                    ?: token,
                token = token,
                expireDate = cached?.expireDate,
                remainingDays = cached?.remainingDays
            )
        }

    /**
     * Puts the wizard back on the payment step when an invoice from an earlier
     * visit is still unpaid, so the customer does not lose its tracking code by
     * switching to a banking app.
     */
    private fun restorePendingInvoice() {
        if (_uiState.value.invoice != null) return
        val pending = PendingInvoiceStore.load() ?: return
        val target = if (pending.isNew) {
            PurchaseTarget.New
        } else {
            PurchaseTarget.Renew(
                SubscriptionChoice(
                    subId = pending.subId.orEmpty(),
                    name = pending.subName.orEmpty(),
                    token = pending.subToken.orEmpty()
                )
            )
        }
        _uiState.update {
            it.copy(step = PurchaseStep.PAYMENT, target = target, invoice = pending.invoice)
        }
    }

    fun selectSubscription(choice: SubscriptionChoice) {
        _uiState.update {
            it.copy(target = PurchaseTarget.Renew(choice), errorMessage = null)
        }
    }

    fun selectNewSubscription() {
        _uiState.update { it.copy(target = PurchaseTarget.New, errorMessage = null) }
    }

    // ---------- Step 2: which tariff ----------

    fun loadPlans(force: Boolean = false) {
        if (!force && _uiState.value.plansLoaded && _uiState.value.plansError == null) return
        if (!AgentPlanManager.isConfigured()) {
            _uiState.update {
                it.copy(plansError = PurchaseError.NOT_CONFIGURED, plansLoaded = true)
            }
            return
        }
        launchLoading {
            val plans = try {
                withContext(Dispatchers.IO) { AgentPlanManager.fetchPlans() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to load agent plans", e)
                null
            }
            _uiState.update {
                if (plans == null) {
                    it.copy(plansError = PurchaseError.REQUEST_FAILED, plansLoaded = true)
                } else {
                    it.copy(plans = plans, plansError = null, plansLoaded = true)
                }
            }
        }
    }

    fun selectPlan(plan: AgentPlan) {
        _uiState.update { it.copy(selectedPlan = plan, errorMessage = null) }
    }

    // ---------- Navigation ----------

    fun goNext() {
        val state = _uiState.value
        if (!state.canGoNext) return
        when (state.step) {
            PurchaseStep.SUBSCRIPTION -> {
                _uiState.update { it.copy(step = PurchaseStep.PLAN, errorMessage = null) }
                loadPlans()
            }

            PurchaseStep.PLAN ->
                _uiState.update { it.copy(step = PurchaseStep.REVIEW, errorMessage = null) }

            PurchaseStep.REVIEW -> createInvoice()
            PurchaseStep.PAYMENT -> confirmPayment()
        }
    }

    /**
     * Steps back one stage. The payment step is deliberately excluded: its
     * invoice already exists on the panel, and walking back would silently
     * create a second one.
     */
    fun goBack() {
        val state = _uiState.value
        if (state.busy) return
        val previous = when (state.step) {
            PurchaseStep.PLAN -> PurchaseStep.SUBSCRIPTION
            PurchaseStep.REVIEW -> PurchaseStep.PLAN
            else -> return
        }
        _uiState.update { it.copy(step = previous, errorMessage = null) }
    }

    // ---------- Step 3: create the invoice ----------

    private fun createInvoice() {
        val state = _uiState.value
        val plan = state.selectedPlan ?: return
        val token = (state.target as? PurchaseTarget.Renew)?.choice?.token

        _uiState.update { it.copy(busy = true, errorMessage = null) }
        launchLoading {
            val result = withContext(Dispatchers.IO) {
                AgentInvoiceManager.createInvoice(plan.planId, token)
            }
            when (result) {
                is AgentApiResult.Success -> {
                    val renewed = (state.target as? PurchaseTarget.Renew)?.choice
                    PendingInvoiceStore.save(
                        PendingInvoice(
                            invoice = result.data,
                            isNew = renewed == null,
                            subId = renewed?.subId,
                            subName = renewed?.name,
                            subToken = renewed?.token
                        )
                    )
                    _uiState.update {
                        it.copy(
                            busy = false,
                            invoice = result.data,
                            step = PurchaseStep.PAYMENT,
                            paymentPending = false,
                            errorMessage = null
                        )
                    }
                }

                is AgentApiResult.Failure -> _uiState.update {
                    it.copy(busy = false, errorMessage = failureMessage(result))
                }
            }
        }
    }

    // ---------- Step 4: confirm the payment ----------

    private fun confirmPayment() {
        val state = _uiState.value
        val invoice = state.invoice ?: return
        val wasNew = state.isNewSubscription
        val renewedSubId = (state.target as? PurchaseTarget.Renew)?.choice?.subId

        _uiState.update { it.copy(busy = true, errorMessage = null, paymentPending = false) }
        launchLoading {
            val result = withContext(Dispatchers.IO) {
                AgentInvoiceManager.checkInvoice(invoice.trackingCode)
            }
            if (result is AgentApiResult.Failure) {
                _uiState.update { it.copy(busy = false, errorMessage = failureMessage(result)) }
                return@launchLoading
            }
            val status = (result as AgentApiResult.Success).data
            if (!status.isConfirmed) {
                _uiState.update { it.copy(busy = false, paymentPending = true) }
                return@launchLoading
            }

            if (wasNew) {
                val link = status.subscriptionLink?.trim().orEmpty()
                if (link.isEmpty()) {
                    // Payment accepted but the subscription is not built yet; the
                    // panel says so in its own words, so pass that through.
                    _uiState.update {
                        it.copy(
                            busy = false,
                            errorMessage = status.message
                                ?: getString(R.string.purchase_error_link_missing)
                        )
                    }
                    return@launchLoading
                }
                withContext(Dispatchers.IO) {
                    AgentSubscriptionInstaller.install(app, link, status.subscriptionName)
                }
                finishWizard(getString(R.string.purchase_success_new))
            } else {
                if (renewedSubId != null) {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            SubscriptionInfoManager.refresh(
                                renewedSubId,
                                MmkvManager.decodeSubscription(renewedSubId)?.url
                            )
                        }
                    }
                }
                finishWizard(getString(R.string.purchase_success_renew))
            }
        }
    }

    /** Drops every choice and starts over at step one, as a purchase should. */
    private fun finishWizard(message: String) {
        PendingInvoiceStore.clear()
        toastSuccess(message)
        _uiState.update {
            PurchaseUiState(
                subscriptions = it.subscriptions,
                subscriptionsLoaded = it.subscriptionsLoaded,
                plans = it.plans,
                plansLoaded = it.plansLoaded,
                successMessage = message
            )
        }
        loadSubscriptions()
    }

    fun restartWizard() {
        PendingInvoiceStore.clear()
        _uiState.update {
            PurchaseUiState(
                subscriptions = it.subscriptions,
                subscriptionsLoaded = it.subscriptionsLoaded,
                plans = it.plans,
                plansLoaded = it.plansLoaded
            )
        }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null, paymentPending = false) }
    }

    private fun failureMessage(failure: AgentApiResult.Failure): String =
        failure.message ?: getString(R.string.purchase_error_network)
}
