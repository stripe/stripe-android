package com.stripe.android.crypto.onramp.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestion
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaire
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class KycQuestionnaireModelTest {
    @Test
    fun `required answers are validated and trimmed for submission`() {
        val model = model()
        assertThat(model.hasMissingAnswers).isTrue()
        model.updateAnswer("required", "   ")
        assertThat(model.hasMissingAnswers).isTrue()
        model.updateAnswer("required", " Savings ")
        assertThat(model.hasMissingAnswers).isFalse()
        val answers = requireNotNull(model.createSubmission()).answers
        assertThat(answers).hasSize(1)
        assertThat(answers.single().questionId).isEqualTo("required")
        assertThat(answers.single().value).isEqualTo("Savings")
        assertThat(model.state.first().answer).isEqualTo(" Savings ")
    }

    @Test
    fun `unknown questions cannot add answers`() {
        val model = model()
        assertThat(model.updateAnswer("unknown", "answer")).isFalse()
        assertThat(model.state.map { it.answer }).containsExactly("", "")
    }

    @Test
    fun `answer updates preserve character boundaries within scalar limit`() {
        val model = model()
        model.updateAnswer("required", "a".repeat(4_999) + "e\u0301")
        assertThat(model.state.first().answer).isEqualTo("a".repeat(4_999))
    }

    @Test
    fun `missing questionnaire omits submission`() {
        val model = KycQuestionnaireModel(null)
        assertThat(model.hasQuestions).isFalse()
        assertThat(model.hasMissingAnswers).isFalse()
        assertThat(model.createSubmission()).isNull()
    }

    private fun model() = KycQuestionnaireModel(
        AdditionalKycQuestionnaire(
            listOf(
                AdditionalKycQuestion("required", "Source of funds", "free_text", true),
                AdditionalKycQuestion("optional", "Details", "free_text", false),
            )
        )
    )
}
