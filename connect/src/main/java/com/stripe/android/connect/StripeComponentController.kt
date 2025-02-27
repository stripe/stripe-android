package com.stripe.android.connect

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider

@PrivateBetaConnectSDK
interface StripeComponentController<Listener : StripeEmbeddedComponentListener> {
    var listener: Listener?

    fun show()

    fun dismiss()
}

@OptIn(PrivateBetaConnectSDK::class)
internal class StripeComponentControllerImpl<DF, Listener, Props> internal constructor(
    private val cls: Class<DF>,
    private val activity: FragmentActivity,
    private val embeddedComponentManager: EmbeddedComponentManager,
    private val props: Props? = null,
    private val cacheKey: String? = null,
) : StripeComponentController<Listener>
    where DF : StripeComponentDialogFragment<*, Listener, Props>,
          Listener : StripeEmbeddedComponentListener,
          Props : ComponentProps {

    private val tag: String = cls.name
    private val dialogFragment: DF

    override var listener: Listener? = null
        set(value) {
            field = value
            dialogFragment.listener = value
        }

    init {
        val existingDialogFragment = getExistingDialogFragment()
        if (existingDialogFragment?.isAdded == true) {
            val viewModelProvider = ViewModelProvider.create(existingDialogFragment)
            val viewModel = viewModelProvider[StripeComponentDialogFragmentViewModel::class]
            viewModel.embeddedComponentManager.value = embeddedComponentManager
        }
        dialogFragment = (existingDialogFragment ?: createDialogFragment())
            .apply { initialEmbeddedComponentManager = embeddedComponentManager }
        dialogFragment.listener = this.listener
    }

    override fun show() {
        dialogFragment.show(activity.supportFragmentManager, tag)
    }

    override fun dismiss() {
        dialogFragment.dismiss()
    }

    private fun getExistingDialogFragment(): DF? {
        return activity.supportFragmentManager.findFragmentByTag(tag)
            ?.takeIf { cls.isInstance(it) }
            ?.let { cls.cast(it) }
    }

    private fun createDialogFragment(): DF {
        return StripeComponentDialogFragment.newInstance(
            cls = cls,
            props = props,
            cacheKey = cacheKey,
        )
    }
}
