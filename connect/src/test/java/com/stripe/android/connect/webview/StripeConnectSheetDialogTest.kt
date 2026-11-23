package com.stripe.android.connect.webview

import android.graphics.Color
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.webkit.WebView
import androidx.activity.ComponentActivity
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class StripeConnectSheetDialogTest {

    private val activityController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    private val activity = activityController.get()
    private var dismissCount = 0
    private var closeRequestCount = 0
    private val dialog = StripeConnectSheetDialog(
        activity = activity,
        webView = WebView(activity),
        onDismissed = { dismissCount++ },
        onCloseRequested = { closeRequestCount++ },
    )

    @After
    fun cleanup() {
        dialog.release()
        activityController.destroy()
    }

    @Test
    fun `a dismissible sheet closes natively`() {
        showRevealedSheet(isDismissible = true)

        dialog.onBackPressedDispatcher.onBackPressed()
        idle()

        assertThat(dismissCount).isEqualTo(1)
        assertThat(dialog.isShowing).isFalse()
    }

    @Test
    fun `a non-dismissible sheet asks to close on back, drag, or outside tap and stays open`() {
        showRevealedSheet(isDismissible = false)

        dialog.onBackPressedDispatcher.onBackPressed()
        dragHandleDown()
        dialog.scrim.performClick()
        idle()

        assertThat(closeRequestCount).isEqualTo(3)
        assertThat(dismissCount).isEqualTo(0)
        assertThat(dialog.isShowing).isTrue()
    }

    private fun showRevealedSheet(isDismissible: Boolean) {
        dialog.show()
        dialog.bind(
            height = 300f,
            isDismissible = isDismissible,
            backgroundColor = Color.WHITE,
            textColor = Color.BLACK,
        )
        dialog.reveal()
        idle()
    }

    private fun dragHandleDown() {
        val downTime = SystemClock.uptimeMillis()
        fun event(action: Int, y: Float) = MotionEvent.obtain(downTime, downTime + 50, action, 10f, y, 0)
        dialog.handleArea.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, 100f))
        dialog.handleArea.dispatchTouchEvent(event(MotionEvent.ACTION_MOVE, 600f))
        dialog.handleArea.dispatchTouchEvent(event(MotionEvent.ACTION_UP, 600f))
    }

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    }
}
