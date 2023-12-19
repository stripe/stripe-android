package com.stripe.hcaptcha

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.BadParcelableException
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.InflateException
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.annotation.RestrictTo
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaInternalConfig
import com.stripe.hcaptcha.config.HCaptchaSize
import com.stripe.hcaptcha.webview.HCaptchaWebView
import com.stripe.hcaptcha.webview.HCaptchaWebViewHelper
import kotlin.AssertionError
import kotlin.ClassCastException
import kotlin.IllegalStateException

/**
 * HCaptcha Dialog Fragment Class.
 *
 * Must have `public` modifier, so it can be properly recreated from instance state!
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class HCaptchaDialogFragment : DialogFragment(), IHCaptchaVerifier {
    private var webViewHelper: HCaptchaWebViewHelper? = null
    private var loadingContainer: LinearLayout? = null
    private var defaultDimAmount = 0.6f
    private var readyForInteraction = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.StripeHCaptchaDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var listener: HCaptchaStateListener? = null

        try {
            val args = arguments
            if (args == null) {
                dismiss()
                return null
            }

            listener = HCaptchaCompat.getStateListener(args)
            if (listener == null) {
                dismiss()
                return null
            }

            val config = HCaptchaCompat.getConfig(args)
            if (config == null) {
                dismiss()
                listener.onFailure(HCaptchaException(HCaptchaError.ERROR))
                return null
            }

            val internalConfig = HCaptchaCompat.getInternalConfig(args)
            if (internalConfig == null) {
                dismiss()
                listener.onFailure(HCaptchaException(HCaptchaError.ERROR))
                return null
            }

            val rootView = prepareRootView(inflater, container, config)
            val webView = prepareWebView(rootView, config)

            loadingContainer = rootView.findViewById<LinearLayout>(R.id.loadingContainer).apply {
                visibility = if (config.loading) View.VISIBLE else View.GONE
            }

            webViewHelper = HCaptchaWebViewHelper(
                Handler(Looper.getMainLooper()),
                requireContext(),
                config,
                internalConfig,
                this,
                listener,
                webView
            )

            readyForInteraction = false
            return rootView
        } catch (e: AssertionError) {
            // Happens when fragment tries to reconstruct because the activity was killed
            // And thus there is no way of communicating back
            dismiss()
            listener?.onFailure?.let { it(HCaptchaException(HCaptchaError.ERROR)) }
        } catch (e: BadParcelableException) {
            dismiss()
            listener?.onFailure?.let { it(HCaptchaException(HCaptchaError.ERROR)) }
        } catch (e: InflateException) {
            dismiss()
            listener?.onFailure?.let { it(HCaptchaException(HCaptchaError.ERROR)) }
        } catch (e: ClassCastException) {
            dismiss()
            listener?.onFailure?.let { it(HCaptchaException(HCaptchaError.ERROR)) }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        webViewHelper?.destroy()
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog
        val window = dialog?.window
        val webViewHelper = webViewHelper
        if (dialog != null && window != null && webViewHelper != null) {
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            defaultDimAmount = window.attributes.dimAmount
            if (!webViewHelper.config.loading) {
                // Remove dialog shadow to appear completely invisible
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(0f)
            }
        }
    }

    override fun onCancel(dialogInterface: DialogInterface) {
        // User canceled the dialog through either `back` button or an outside touch
        super.onCancel(dialogInterface)
        this.onFailure(HCaptchaException(HCaptchaError.CHALLENGE_CLOSED))
    }

    private fun hideLoadingContainer() {
        if (webViewHelper?.config?.loading == true) {
            loadingContainer?.apply {
                animate().alpha(0.0f).setDuration(200).setListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            visibility = View.GONE
                        }
                    }
                )
            }
        } else {
            // Add back dialog shadow in case the checkbox or challenge is shown
            dialog?.window?.apply {
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setDimAmount(defaultDimAmount)
            }
        }
    }

    override fun onLoaded() {
        if (webViewHelper?.config?.size !== HCaptchaSize.INVISIBLE) {
            // checkbox will be shown
            readyForInteraction = true
            hideLoadingContainer()
        }
    }

    override fun onOpen() {
        if (webViewHelper?.config?.size === HCaptchaSize.INVISIBLE) {
            hideLoadingContainer()
        }
        readyForInteraction = true
        webViewHelper?.listener?.onOpen
    }

    override fun onFailure(exception: HCaptchaException) {
        val silentRetry = webViewHelper?.shouldRetry(exception) == true
        if (isAdded && !silentRetry) {
            dismissAllowingStateLoss()
        }

        webViewHelper?.apply {
            if (silentRetry) {
                resetAndExecute()
            } else {
                listener.onFailure(exception)
            }
        }
    }

    override fun onSuccess(result: String) {
        if (isAdded) {
            dismissAllowingStateLoss()
        }
        webViewHelper?.listener?.onSuccess?.let { it(result) }
    }

    override fun startVerification(activity: FragmentActivity) {
        val fragmentManager = activity.supportFragmentManager
        val oldFragment = fragmentManager.findFragmentByTag(TAG)

        if (oldFragment?.isAdded == true) {
            return
        }

        try {
            show(fragmentManager, TAG)
        } catch (e: IllegalStateException) {
            // https://stackoverflow.com/q/14262312/902217
            // Happens if Fragment is stopped i.e. activity is about to destroy on show call
            webViewHelper?.listener?.onFailure?.let { it(HCaptchaException(HCaptchaError.ERROR)) }
        }
    }

    override fun reset() {
        if (webViewHelper != null) {
            webViewHelper!!.reset()
        }
        if (isAdded) {
            dismissAllowingStateLoss()
        }
    }

    private fun prepareRootView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        config: HCaptchaConfig
    ): View {
        val rootView: View = inflater.inflate(R.layout.stripe_hcaptcha_fragment, container, false)
        rootView.isFocusableInTouchMode = true
        rootView.requestFocus()

        rootView.setOnKeyListener { _, keyCode, event ->
            val backDown = keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN
            if (!backDown) {
                return@setOnKeyListener false
            }

            val withoutLoadingUI = !readyForInteraction && !config.loading
            if (withoutLoadingUI) {
                return@setOnKeyListener true
            }

            webViewHelper?.shouldRetry(HCaptchaException(HCaptchaError.CHALLENGE_CLOSED)) ?: false
        }

        return rootView
    }

    private fun prepareWebView(rootView: View, config: HCaptchaConfig): HCaptchaWebView {
        val webView = rootView.findViewById<HCaptchaWebView>(R.id.webView)

        if (!config.loading) {
            webView.setOnTouchListener { view: View, event: MotionEvent? ->
                if (!readyForInteraction && isAdded) {
                    activity?.apply {
                        dispatchTouchEvent(event)
                        return@setOnTouchListener true
                    }
                }

                view.performClick()
            }
        }

        return webView
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * Static TAG String
         */
        private val TAG = HCaptchaDialogFragment::class.java.simpleName

        /**
         * Creates a new instance
         *
         * @param config   the config
         * @param listener the listener
         * @return a new instance
         */
        @JvmStatic
        fun newInstance(
            config: HCaptchaConfig,
            internalConfig: HCaptchaInternalConfig,
            listener: HCaptchaStateListener
        ): HCaptchaDialogFragment {
            val hCaptchaDialogFragment = HCaptchaDialogFragment()
            hCaptchaDialogFragment.arguments = HCaptchaCompat.storeValues(config, internalConfig, listener)
            return hCaptchaDialogFragment
        }
    }
}
