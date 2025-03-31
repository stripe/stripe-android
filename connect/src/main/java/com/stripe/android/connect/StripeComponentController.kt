package com.stripe.android.connect

import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider

/**
 * Controller for a full screen component.
 */
@PrivateBetaConnectSDK
abstract class StripeComponentController<Listener, Props> internal constructor(
    private val activity: FragmentActivity,
    private val embeddedComponentManager: EmbeddedComponentManager,
    private val title: String? = null,
    private val props: Props? = null,
    private val dfClass: Class<out StripeComponentDialogFragment<*, Listener, Props>>
)
    where Listener : StripeEmbeddedComponentListener,
          Props : Parcelable {

    private val tag: String = dfClass.name

    @VisibleForTesting
    internal val dialogFragment: StripeComponentDialogFragment<*, Listener, Props>

    /**
     * Listener of component events.
     */
    var listener: Listener?
        get() = dialogFragment.listener
        set(value) {
            dialogFragment.listener = value
        }

    /**
     * Optional listener of component dismissal.
     */
    var onDismissListener: OnDismissListener? = null
        set(value) {
            field = value
            dialogFragment.onDismissListener = value
        }

    init {
        embeddedComponentManager.coordinator.checkContextDuringCreate(activity)

        val existingDialogFragment = getExistingDialogFragment()
        if (existingDialogFragment?.isAdded == true) {
            // The DF may already be added during re-creation after process death.
            // Pass the manager to the DF through the VM, which it already listens to.
            // We cannot do this if the DF has not yet been added, because we can't scope a VM to an un-added DF.
            val viewModelProvider = ViewModelProvider.create(existingDialogFragment)
            val viewModel = viewModelProvider[StripeComponentDialogFragmentViewModel::class]
            viewModel.embeddedComponentManager.value = embeddedComponentManager
        }
        dialogFragment = (existingDialogFragment ?: createDialogFragment())
            // Setting the manager here is merely to forward it along to the VM.
            .apply { initialEmbeddedComponentManager = embeddedComponentManager }
        dialogFragment.onDismissListener = this.onDismissListener
    }

    /**
     * Shows the component.
     */
    fun show() {
        // Adding a fragment that's already been added will throw an exception.
        if (!dialogFragment.isAdded) {
            dialogFragment.show(activity.supportFragmentManager, tag)
        }
    }

    /**
     * Dismisses the component.
     */
    fun dismiss() {
        dialogFragment.dismiss()
    }

    private fun getExistingDialogFragment(): StripeComponentDialogFragment<*, Listener, Props>? {
        return activity.supportFragmentManager.findFragmentByTag(tag)
            ?.takeIf { dfClass.isInstance(it) }
            ?.let { dfClass.cast(it) }
    }

    private fun createDialogFragment(): StripeComponentDialogFragment<*, Listener, Props> {
        return StripeComponentDialogFragment.newInstance(
            cls = dfClass,
            title = title,
            props = props,
        )
    }

    /**
     * Listener of component dismissal.
     */
    fun interface OnDismissListener {
        /**
         * Called when the full screen component is dismissed.
         */
        fun onDismiss()
    }
}
