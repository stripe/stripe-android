package com.stripe.android.link

import android.content.Context
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.link.LinkController.LookupConsumerCallback
import com.stripe.android.link.LinkController.PresentPaymentMethodsCallback
import com.stripe.android.link.ui.wallet.displayName
import com.stripe.android.paymentsheet.R
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

interface LinkController {

    fun getPaymentMethodPreview(): PaymentMethodPreview?

    fun presentPaymentMethods(email: String?)

    fun lookupConsumer(email: String)

    sealed interface PresentPaymentMethodsResult {
        class Selected(val preview: PaymentMethodPreview) : PresentPaymentMethodsResult
        data object Canceled : PresentPaymentMethodsResult
        class Failed(val error: Throwable) : PresentPaymentMethodsResult
    }

    sealed interface LookupConsumerResult {
        class Success(val email: String, val isConsumer: Boolean) : LookupConsumerResult
        class Failed(val email: String, val error: Throwable) : LookupConsumerResult
    }

    fun interface PresentPaymentMethodsCallback {
        fun onResult(result: PresentPaymentMethodsResult)
    }

    fun interface LookupConsumerCallback {
        fun onResult(result: LookupConsumerResult)
    }

    @Parcelize
    @Poko
    class PaymentMethodPreview(
        @DrawableRes val iconRes: Int,
        val label: String,
        val sublabel: String?
    ) : Parcelable

    companion object {
        fun create(
            activity: ComponentActivity,
            presentPaymentMethodsCallback: PresentPaymentMethodsCallback,
            lookupConsumerCallback: LookupConsumerCallback,
        ): LinkController {
            val viewModelProvider = ViewModelProvider(
                owner = activity,
                factory = LinkPaymentMethodLauncherViewModel.Factory()
            )
            val viewModel = viewModelProvider[LinkPaymentMethodLauncherViewModel::class.java]
            return RealLinkController(
                activity = activity,
                viewModel = viewModel,
                presentPaymentMethodsCallback = presentPaymentMethodsCallback,
                lookupConsumerCallback = lookupConsumerCallback,
            )
        }
    }
}

internal class RealLinkController(
    private val activity: ComponentActivity,
    private val viewModel: LinkPaymentMethodLauncherViewModel,
    private val presentPaymentMethodsCallback: PresentPaymentMethodsCallback,
    private val lookupConsumerCallback: LookupConsumerCallback,
) : LinkController {

    private var linkActivityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args> =
        activity.registerForActivityResult(viewModel.linkActivityContract) { result ->
            viewModel.onResult(activity, result)
        }

    init {
        activity.lifecycleScope.launch {
            activity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state
                        .map { it.presentPaymentMethodsResult }
                        .filterNotNull()
                        .collect(presentPaymentMethodsCallback::onResult)
                }

                launch {
                    viewModel.state
                        .map { it.lookupConsumerResult }
                        .filterNotNull()
                        .collect(lookupConsumerCallback::onResult)
                }
            }
        }
    }

    override fun getPaymentMethodPreview(): LinkController.PaymentMethodPreview? {
        return viewModel.state.value.paymentMethodPreview
    }

    override fun presentPaymentMethods(email: String?) {
        viewModel.onPresent(linkActivityResultLauncher, email)
    }

    override fun lookupConsumer(email: String) {
        viewModel.onLookupConsumer(email)
    }
}

internal fun LinkPaymentMethod.toPreview(context: Context): LinkController.PaymentMethodPreview {
    val sublabel = buildString {
        append(details.displayName.resolve(context))
        append(" •••• ")
        append(details.last4)
    }
    return LinkController.PaymentMethodPreview(
        iconRes = R.drawable.stripe_ic_paymentsheet_link_arrow,
        label = context.getString(com.stripe.android.R.string.stripe_link),
        sublabel = sublabel
    )
}
