package com.stripe.android.connect

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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

    protected var props: Props? = null
    protected var cacheKey: String? = null

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

        val arguments = this.arguments!!

        @Suppress("DEPRECATION")
        props = arguments.getParcelable(ARG_PROPS)
        cacheKey = arguments.getString(ARG_CACHE_KEY)

        initialEmbeddedComponentManager?.let { viewModel.embeddedComponentManager.value = it }
        initialEmbeddedComponentManager = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FrameLayout(inflater.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.embeddedComponentManager
                        .collectLatest { embeddedComponentManager ->
                            embeddedComponentManager?.appearanceFlow?.collectLatest { appearance ->
                                dialog?.window?.setBackgroundDrawable(
                                    ColorDrawable(appearance.colors.background ?: Color.WHITE)
                                )
                            }
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
                    (view as ViewGroup).addView(componentView)
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

    override fun onDismiss(dialog: DialogInterface) {
        onDismissListener?.onDismiss()
        super.onDismiss(dialog)
    }

    internal companion object {
        private const val ARG_PROPS = "props"
        private const val ARG_CACHE_KEY = "cache_key"

        fun <DF, Props> newInstance(
            cls: Class<DF>,
            props: Props? = null,
            cacheKey: String? = null,
        ): DF
            where DF : StripeComponentDialogFragment<*, *, Props>,
                  Props : ComponentProps {
            val fragment = cls.getDeclaredConstructor().newInstance()
            return fragment.apply {
                arguments = Bundle().apply {
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
