package com.stripe.android.connect

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.stripe.android.connect.appearance.Appearance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    protected val cacheKey: String? get() = arguments?.getString(ARG_CACHE_KEY)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.StripeConnectFullScreenDialogStyle)

        initialEmbeddedComponentManager?.let { viewModel.embeddedComponentManager.value = it }
        initialEmbeddedComponentManager = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = StripeComponentDialogFragmentView<ComponentView>(inflater)
            .also { this._rootView = it }
        rootView.toolbar.title = title
        rootView.toolbar.setNavigationOnClickListener { dismiss() }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.embeddedComponentManager
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collectLatest { embeddedComponentManager ->
                    embeddedComponentManager?.appearanceFlow?.collectLatest(::bindAppearance)
                }
        }

        // The View scaffolding has been created, but not the component view. The Manager may not be available yet, e.g.
        // after process death, so wait for the VM to provide it.
        viewLifecycleOwner.lifecycleScope.launch {
            val embeddedComponentManager =
                viewModel.embeddedComponentManager.filterNotNull().first()
            val componentView = createComponentView(embeddedComponentManager)
                .also { it.listener = listener }
            rootView.componentView = componentView
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

    internal companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_PROPS = "props"
        private const val ARG_CACHE_KEY = "cache_key"

        fun <DF, Props> newInstance(
            cls: Class<DF>,
            title: String? = null,
            props: Props? = null,
            cacheKey: String? = null,
        ): DF
            where DF : StripeComponentDialogFragment<*, *, Props>,
                  Props : ComponentProps {
            val fragment = cls.getDeclaredConstructor().newInstance()
            return fragment.apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putParcelable(ARG_PROPS, props)
                    putString(ARG_CACHE_KEY, cacheKey)
                }
            }
        }
    }
}

@OptIn(PrivateBetaConnectSDK::class)
internal class StripeComponentDialogFragmentViewModel : ViewModel() {
    var embeddedComponentManager = MutableStateFlow<EmbeddedComponentManager?>(null)
}
