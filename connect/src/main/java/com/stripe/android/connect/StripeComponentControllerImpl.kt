package com.stripe.android.connect

import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider

/**
 * Controller implementation for a full screen component.
 *
 * The implementation to make this all work *well* is quite tricky and deserves some explanation.
 *
 * Goals
 * =====
 *  1. Configuration changes are handled well
 *  2. Process death is handled well
 *  3. API uses typical listener objects (no need for globals, broadcast receivers, etc.)
 *  4. API supports streaming events (not just final result)
 *  5. API enables easy programmatic dismissal
 *  6. No memory leaks (of course)
 *
 * There's a delicate dance between several classes. Here's what they are, their purpose, and their relationships:
 *
 * [EmbeddedComponentManager]
 * ==========================
 * The starting point. Users (i.e. SDK integrators) should either have a singleton or have Activity ViewModel scope
 * in order to survive configuration changes. Creates [StripeComponentController] and [StripeConnectWebViewContainer]
 * instances.
 *
 * [StripeComponentController]
 * ===========================
 * Creates and holds a single [StripeComponentDialogFragment] and provides APIs for its presentation and setting
 * the [StripeEmbeddedComponentListener]. The Controller also holds the [FragmentActivity] to display the DialogFragment
 * and therefore the Controller cannot outlive the Activity.
 *
 * The Controller also provides the [EmbeddedComponentManager] to the DialogFragment either directly (when it hasn't
 * been added to the Activity yet) or indirectly through [StripeComponentDialogFragmentViewModel]. In both cases,
 * [StripeComponentDialogFragmentViewModel] will hold onto the Manager for DialogFragment re-creation.
 *
 * [StripeComponentDialogFragment]
 * ===============================
 * Uses its provided [EmbeddedComponentManager] to create and render a [StripeConnectWebViewContainer], i.e. the
 * container view managing the [StripeConnectWebView]. The [EmbeddedComponentManager] instance is obtained in multiple
 * ways:
 *  1. From [StripeComponentController] upon creation. When attached to the Activity, the Manager is immediately
 *   stored in its [StripeComponentDialogFragmentViewModel] for persistence across config changes.
 *  2. From [StripeComponentDialogFragmentViewModel] upon re-creation after config changes.
 *  3. From [StripeComponentDialogFragmentViewModel] upon re-creation after app death. The VM will not immediately have
 *   the Manager instance, but the first thing [StripeComponentController] does after being created (it should be
 *   normally re-created if app state is restored properly) is set the Manager in the ViewModel if the DialogFragment
 *   has been added.
 *
 * [StripeComponentDialogFragmentViewModel]
 * ========================================
 * Its sole purpose is to retain an [EmbeddedComponentManager] instance to provide for [StripeComponentDialogFragment].
 *
 * [StripeEmbeddedComponentListener]
 * =================================
 * An interface for users to implement to listen to component events. Since Listeners may contain references to
 * Android app components (read: Activities), they must not be retained across config changes and must be re-set in
 * [StripeComponentDialogFragment] upon re-creation.
 *
 * [StripeConnectWebViewContainer], [StripeConnectWebViewContainerViewModel], & [StripeConnectWebView]
 * ===============================
 * Wraps and manages [StripeConnectWebView]. The WebView itself is actually created and retained by
 * [StripeConnectWebViewContainerViewModel] (see its docs for why). So upon configuration changes,
 * [StripeConnectWebViewContainer] is re-created but [StripeConnectWebView] is not -- it's simply re-added to the
 * re-created container view.
 */
@OptIn(PrivateBetaConnectSDK::class)
internal class StripeComponentControllerImpl<DF, Listener, Props> internal constructor(
    private val cls: Class<DF>,
    private val activity: FragmentActivity,
    private val embeddedComponentManager: EmbeddedComponentManager,
    private val title: String? = null,
    private val props: Props? = null,
) : StripeComponentController<Listener>
    where DF : StripeComponentDialogFragment<*, Listener, Props>,
          Listener : StripeEmbeddedComponentListener,
          Props : ComponentProps {

    private val tag: String = cls.name

    @VisibleForTesting
    internal val dialogFragment: DF

    override var listener: Listener?
        get() = dialogFragment.listener
        set(value) {
            dialogFragment.listener = value
        }

    override var onDismissListener: StripeComponentController.OnDismissListener? = null
        set(value) {
            field = value
            dialogFragment.onDismissListener = value
        }

    init {
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

    override fun show() {
        // Adding a fragment that's already been added will throw an exception.
        if (!dialogFragment.isAdded) {
            dialogFragment.show(activity.supportFragmentManager, tag)
        }
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
            title = title,
            props = props,
        )
    }
}
