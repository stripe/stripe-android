package com.stripe.android.financialconnections.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
class UriComparatorTest(
    private val uri1: String,
    private val uri2: String,
    private val result: Boolean
) {
    private val underTest = UriComparator(Logger.noop())

    companion object {
        @JvmStatic
        @Suppress("MaxLineLength")
        @ParameterizedRobolectricTestRunner.Parameters
        fun data() = listOf(
            parameters(
                uri1 = "stripe-auth://login",
                uri2 = "stripe-auth://login",
                equals = true
            ),
            parameters(
                uri1 = "stripe-auth://login#",
                uri2 = "stripe-auth://login",
                equals = true
            ),
            parameters(
                uri1 = "stripe-auth://login",
                uri2 = "stripe-auth://otherlink",
                equals = false
            ),
            parameters(
                uri1 = "stripe-auth://link-accounts/login",
                uri2 = "stripe-auth://link-accounts/login#authSessionId=1234",
                equals = true
            ),
            parameters(
                uri1 = "stripe-auth://link-accounts/com.stripe.android.paymentsheet.example/success?",
                uri2 = "stripe-auth://link-accounts/com.stripe.android.paymentsheet.example/success?linked_account=fca_1LZmURHINT0kwo6s08HupcIf",
                equals = true
            ),
            parameters(
                uri1 = "stripe-auth://link-accounts/com.stripe.android.paymentsheet.example/success",
                uri2 = "stripe-auth://link-accounts/com.stripe.android.otherapp/success",
                equals = false
            ),
            parameters(
                uri1 = "stripe-auth://link-accounts/com.stripe.android.paymentsheet.example/success",
                uri2 = "stripe-auth://link-accounts/com.stripe.android.paymentsheet.example/error",
                equals = false
            )
        )

        private fun parameters(
            uri1: String,
            uri2: String,
            equals: Boolean
        ): Array<Any> {
            return arrayOf(
                uri1,
                uri2,
                equals
            )
        }
    }

    @Test
    fun compareUrlsReturn() {
        assertThat(underTest.compareSchemeAuthorityAndPath(uri1, uri2)).isEqualTo(result)
    }
}
