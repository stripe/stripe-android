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
import com.stripe.android.link.ui.wallet.displayName
import com.stripe.android.paymentsheet.R
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

interface LinkPaymentMethodLauncher {
    val state: State

    var listener: Listener?

    fun present(email: String?)

    interface Listener {
        fun onStateChange(state: State)
    }

    @Parcelize
    @Poko
    class State(
        val preview: PaymentMethodPreview? = null,
    ) : Parcelable

    @Parcelize
    @Poko
    class PaymentMethodPreview(
        @DrawableRes val iconRes: Int,
        val label: String,
        val sublabel: String?
    ) : Parcelable

    companion object {
        fun create(activity: ComponentActivity): LinkPaymentMethodLauncher {
            val viewModelProvider = ViewModelProvider(
                owner = activity,
                factory = LinkPaymentMethodLauncherViewModel.Factory()
            )
            val viewModel = viewModelProvider[LinkPaymentMethodLauncherViewModel::class.java]
            return RealLinkPaymentMethodLauncher(activity, viewModel)
        }
    }
}

internal class RealLinkPaymentMethodLauncher(
    private val activity: ComponentActivity,
    private val viewModel: LinkPaymentMethodLauncherViewModel
) : LinkPaymentMethodLauncher {

    override val state: LinkPaymentMethodLauncher.State get() = viewModel.state.value.toPublicState(activity)

    override var listener: LinkPaymentMethodLauncher.Listener? = null

    private var linkActivityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args> =
        activity.registerForActivityResult(viewModel.linkActivityContract) { result ->
            viewModel.onResult(result)
        }

    init {
        activity.lifecycleScope.launch {
            activity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state
                        .map { it.toPublicState(activity) }
                        .collect { listener?.onStateChange(it) }
                }
            }
        }
    }

    override fun present(email: String?) {
        viewModel.onPresent(linkActivityResultLauncher, email)
    }
}

private fun LinkPaymentMethodLauncherState.toPublicState(context: Context) : LinkPaymentMethodLauncher.State {
    val preview = selectedPaymentMethod?.let { pm ->
        val sublabel = buildString {
            append(pm.details.displayName.resolve(context))
            append(" •••• ")
            append(pm.details.last4)
        }
        LinkPaymentMethodLauncher.PaymentMethodPreview(
            iconRes = R.drawable.stripe_ic_paymentsheet_link_arrow,
            label = context.getString(com.stripe.android.R.string.stripe_link),
            sublabel = sublabel
        )
    }
    return LinkPaymentMethodLauncher.State(
        preview = preview,
    )
}
