package com.stripe.android.identity.states

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_MB_STATUS
import com.stripe.android.identity.networking.IdentityRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MBDetectorTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val analyticsRequestFactory = IdentityAnalyticsRequestFactory(
        context = context,
        args = mock()
    )

    private val mockIdentityRepository = mock<IdentityRepository>()

    @Test
    fun `Returns null and send analysis when MBSettings is null`() = runBlocking {
        val createdResult = MBDetector.maybeCreateMBInstance(
            context = context,
            settings = null,
            identityAnalyticsRequestFactory = analyticsRequestFactory,
            identityRepository = mockIdentityRepository
        )
        assertThat(createdResult).isNull()
        verify(mockIdentityRepository).sendAnalyticsRequest(
            argWhere {
                it.eventName == EVENT_MB_STATUS &&
                    (
                    it.params[IdentityAnalyticsRequestFactory.PARAM_EVENT_META_DATA]
                        as Map<*, *>
                    )[IdentityAnalyticsRequestFactory.PARAM_REQUIRED] == false
            }
        )
    }
}
