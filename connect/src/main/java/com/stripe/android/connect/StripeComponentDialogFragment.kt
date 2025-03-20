package com.stripe.android.connect

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.webview.MobileInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * A full-screen DialogFragment that displays a full-screen component.
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
@PrivateBetaConnectSDK
internal abstract class StripeComponentDialogFragment<ComponentView, Listener, Props> : DialogFragment()
    where ComponentView : StripeComponentView<Listener, Props>,
          Listener : StripeEmbeddedComponentListener,
          Props : ComponentProps {

    private val viewModel: StripeComponentDialogFragmentViewModel by viewModels()

    protected val title: String? get() = arguments?.getString(ARG_TITLE)

    protected val props: Props?
        @Suppress("DEPRECATION")
        get() = arguments?.getParcelable(ARG_PROPS)

    protected abstract fun createComponentView(
        embeddedComponentManager: EmbeddedComponentManager
    ): ComponentView

    private var _rootView: StripeComponentDialogFragmentView<ComponentView>? = null
    private val rootView get() = _rootView!!

    internal var initialEmbeddedComponentManager: EmbeddedComponentManager? = null

    var listener: Listener? = null
        set(value) {
            field = value
            _rootView?.componentView?.listener = value
        }

    var onDismissListener: StripeComponentController.OnDismissListener? = null

    private val onBackPressedCallback = OnBackPressedCallbackImpl(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.StripeConnectFullScreenDialogStyle)

        initialEmbeddedComponentManager?.let { viewModel.embeddedComponentManager.value = it }
        initialEmbeddedComponentManager = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return ComponentDialog(requireContext(), theme).apply {
            onBackPressedDispatcher.addCallback(onBackPressedCallback)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // `setDecorFitsSystemWindows()` must be called here in onCreateView. If called too early, app crashes on
        // older Android versions; if too late, it doesn't do anything on newer versions.
        dialog?.window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }

        return StripeComponentDialogFragmentView<ComponentView>(inflater).also {
            this._rootView = it
            it.title = title
            it.listener = DialogFragmentViewListener()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Update root view padding to keep content visible with soft keyboard.
        // This works in conjunction with `setDecorFitsSystemWindows()`.
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val paddings = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.ime() or
                    WindowInsetsCompat.Type.navigationBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(
                left = paddings.left,
                top = paddings.top,
                right = paddings.right,
                bottom = paddings.bottom,
            )
            insets.inset(0, paddings.top, 0, paddings.bottom)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.embeddedComponentManager
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collectLatest { embeddedComponentManager ->
                    embeddedComponentManager?.coordinator?.appearanceFlow?.collectLatest(::bindAppearance)
                }
        }

        // The View scaffolding has been created, but not the component view. The Manager may not be available yet, e.g.
        // after process death, so wait for the VM to provide it.
        viewLifecycleOwner.lifecycleScope.launch {
            // Setup the component view.
            val embeddedComponentManager =
                viewModel.embeddedComponentManager.filterNotNull().first()
            val componentView = createComponentView(embeddedComponentManager)
                .also { it.listener = listener }
            componentView.layoutParams =
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            rootView.componentView = componentView

            // With the component view set up, we can allow it to control back behavior.
            onBackPressedCallback.isEnabled = true

            // Dismiss on command.
            launch {
                componentView.receivedCloseWebView
                    .filter { it }
                    .collectLatest { dismiss() }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        _rootView = null
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        onDismissListener?.onDismiss()
        super.onDismiss(dialog)
    }

    private fun bindAppearance(appearance: Appearance) {
        rootView.bindAppearance(appearance)
        dialog?.window?.setBackgroundDrawable(rootView.background)
    }

    private inner class DialogFragmentViewListener : StripeComponentDialogFragmentView.Listener {
        override fun onCloseButtonClickError() {
            dismiss()
        }
    }

    private inner class OnBackPressedCallbackImpl(enabled: Boolean) : OnBackPressedCallback(enabled) {
        override fun handleOnBackPressed() {
            // Defer to the component view if it's available; if not, dismiss.
            val componentView = rootView.componentView
            if (componentView == null) {
                dismiss()
                return
            }
            componentView.mobileInputReceived(
                input = MobileInput.BACK_BUTTON_PRESSED,
                resultCallback = { result ->
                    if (result.isFailure) {
                        dismiss()
                    }
                }
            )
        }
    }

    internal companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_PROPS = "props"

        fun <DF, Props> newInstance(
            cls: Class<DF>,
            title: String? = null,
            props: Props? = null,
        ): DF
            where DF : StripeComponentDialogFragment<*, *, Props>,
                  Props : ComponentProps {
            val fragment = cls.getDeclaredConstructor().newInstance()
            return fragment.apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putParcelable(ARG_PROPS, props)
                }
            }
        }
    }
}

@OptIn(PrivateBetaConnectSDK::class)
internal class StripeComponentDialogFragmentViewModel : ViewModel() {
    var embeddedComponentManager = MutableStateFlow<EmbeddedComponentManager?>(null)
}
