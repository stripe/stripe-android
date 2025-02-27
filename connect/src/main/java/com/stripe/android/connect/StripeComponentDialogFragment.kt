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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.databinding.StripeFullScreenComponentBinding
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

    protected val props: Props
        @Suppress("DEPRECATION")
        get() = arguments?.getParcelable(ARG_PROPS)!!

    protected val cacheKey: String? get() = arguments?.getString(ARG_CACHE_KEY)

    private var _binding: StripeFullScreenComponentBinding? = null
    private val binding get() = _binding!!

    private var componentView: ComponentView? = null
        set(value) {
            field = value
            value?.listener = listener
        }

    internal var initialEmbeddedComponentManager: EmbeddedComponentManager? = null

    var listener: Listener? = null
        set(value) {
            field = value
            componentView?.listener = value
        }

    var onDismissListener: StripeComponentController.OnDismissListener? = null

    protected abstract fun createComponentView(
        embeddedComponentManager: EmbeddedComponentManager
    ): ComponentView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.StripeConnectFullScreenDialogStyle)

        initialEmbeddedComponentManager?.let { viewModel.embeddedComponentManager.value = it }
        initialEmbeddedComponentManager = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = StripeFullScreenComponentBinding.inflate(inflater, container, false)
            .also { this._binding = it }
        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.embeddedComponentManager
                        .collectLatest { embeddedComponentManager ->
                            embeddedComponentManager?.appearanceFlow?.collectLatest(::bindAppearance)
                        }
                }
                launch {
                    val embeddedComponentManager =
                        viewModel.embeddedComponentManager.filterNotNull().first()
                    val componentView = createComponentView(embeddedComponentManager)
                        .also { this@StripeComponentDialogFragment.componentView = it }
                    componentView.layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    binding.root.addView(componentView)
                }
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
        componentView = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        onDismissListener?.onDismiss()
        super.onDismiss(dialog)
    }

    private fun bindAppearance(appearance: Appearance) {
        appearance.colors.background?.let {
            binding.toolbar.setBackgroundColor(it)
        }
        appearance.colors.text?.let {
            binding.toolbar.setTitleTextColor(it)
            binding.toolbar.navigationIcon?.setTint(it)
        }
        appearance.colors.border?.let {
            binding.divider.setBackgroundColor(it)
        }
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
