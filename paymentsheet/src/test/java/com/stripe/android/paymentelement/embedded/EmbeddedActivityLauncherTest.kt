import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedActivityLauncher
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.FormContract
import com.stripe.android.paymentelement.embedded.FormResult
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedActivityLauncherTest {

    private lateinit var activityResultCaller: ActivityResultCaller
    private lateinit var selectionHolder: EmbeddedSelectionHolder
    private lateinit var lifecycleOwner: TestLifecycleOwner
    private lateinit var launcher: DefaultEmbeddedActivityLauncher
    private lateinit var formActivityLauncher: ActivityResultLauncher<FormContract.Args>

    @Captor
    private lateinit var contractCallbackCaptor: ArgumentCaptor<ActivityResultCallback<FormResult>>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        activityResultCaller = mock()
        lifecycleOwner = TestLifecycleOwner()
        selectionHolder = mock()

        formActivityLauncher = mock()

        whenever(
            activityResultCaller.registerForActivityResult(
                any<FormContract>(),
                capture(contractCallbackCaptor)
            )
        ).thenReturn(formActivityLauncher)

        launcher = DefaultEmbeddedActivityLauncher(activityResultCaller, lifecycleOwner, selectionHolder)
    }

    @Test
    fun `formLauncher launches activity with correct parameters`() {
        val code = "test_code"
        val expectedArgs = FormContract.Args(code, null)
        launcher.formLauncher.invoke(code, null)
        verify(formActivityLauncher).launch(expectedArgs)
    }

    @Test
    fun `formActivityLauncher callback updates selection holder on complete result`() {
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        val result = FormResult.Complete(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        contractCallbackCaptor.value.onActivityResult(result)
        verify(selectionHolder).set(selection)
    }

    @Test
    fun `formActivityLauncher callback does not update selection holder on non-complete result`() {
        val result = FormResult.Cancelled
        contractCallbackCaptor.value.onActivityResult(result)
        verify(selectionHolder, never()).set(any())
    }

    @Test
    fun `cleanup happens on lifecycle destroy`() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        verify(formActivityLauncher).unregister()
    }
}
