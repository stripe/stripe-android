package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class SaveForFutureUseControllerTest {
    private val mandateIdentifier = IdentifierSpec("mandate")
    private val nameSectionIdentifier = IdentifierSpec("name")
    private val optionalIdentifiers = listOf(nameSectionIdentifier, mandateIdentifier)
    private val saveForFutureUseController = SaveForFutureUseController(optionalIdentifiers)

    @Test
    fun `Save for future use is initialized as true and no optional items`() =
        runBlocking {
            assertThat(saveForFutureUseController.saveForFutureUse.first()).isTrue()
            assertThat(saveForFutureUseController.optionalIdentifiers.first()).isEmpty()
        }

    @Test
    fun `Save for future use when set to false shows the optional items specified`() =
        runBlocking {
            saveForFutureUseController.onValueChange(false)
            assertThat(saveForFutureUseController.saveForFutureUse.first()).isFalse()
            assertThat(saveForFutureUseController.optionalIdentifiers.first()).isEqualTo(
                optionalIdentifiers
            )
        }
}
