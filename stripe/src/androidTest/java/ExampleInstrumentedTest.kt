import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.stripe.android.networking.ConnectionFactory
import com.stripe.android.networking.RequestHeadersFactory
import com.stripe.android.networking.StripeRequest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun testMakeRequest() {
        // Context of the app under test.

        ConnectionFactory.Default().create(FakeStripeRequest())
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        Assert.assertEquals("com.stripe.android.test", appContext.packageName)
    }

    private class FakeStripeRequest : StripeRequest() {
        override val method: Method = Method.POST
        override val baseUrl: String = "https://api.stripe.com"
        override val params: Map<String, *>? = null
        override val mimeType: MimeType = MimeType.Form
        override val headersFactory = RequestHeadersFactory.Fingerprint(
            UUID.randomUUID().toString()
        )

        override val body: String = ""
    }
}
