package com.stripe.android.financialconnections.domain

import com.stripe.android.core.model.serializers.EnumIgnoreUnknownSerializer
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.serializer.AuthSessionChallengeSerializer
import com.stripe.android.financialconnections.repository.FinancialConnectionsCredentialsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.repository.TokenResponse
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

internal class ChallengeFlowController @Inject constructor(
    private val challengeRepository: FinancialConnectionsManifestRepository,
    private val credentialsRepository: FinancialConnectionsCredentialsRepository,
    private val configuration: FinancialConnectionsSheet.Configuration,
) {

    suspend fun getChallenge(
        authSessionId: String,
    ): AuthSessionChallengeResponse {
        return challengeRepository.postChallengeState(
            sessionId = authSessionId,
            clientSecret = configuration.financialConnectionsSessionClientSecret,
        )
    }

    suspend fun submitChallenge(
        authSessionId: String,
        challengeId: String,
        type: String,
        username: String,
        password: String
    ) {
        val token: TokenResponse = tokenize(
            authSessionId = authSessionId,
            username = username,
            password = password
        )
        val answer = challengeRepository.answerChallenge(
            sessionId = authSessionId,
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            challenge = challengeId,
            type = type,
            token = token.id
        )

        var status = answer.status
        while (status == AuthSessionChallengeStatus.AwaitingChallenge) {
            delay(2000)
            val challenge = challengeRepository.postChallengeState(
                clientSecret = configuration.financialConnectionsSessionClientSecret,
                sessionId = authSessionId
            )
            status = challenge.status
        }
        if (status != AuthSessionChallengeStatus.Complete) {
            throw Exception("Failed to submit challenge: status $status")
        }
    }

    private suspend fun tokenize(
        authSessionId: String,
        username: String,
        password: String,
    ): TokenResponse = credentialsRepository.tokenize(
        authSessionId = authSessionId,
        username = username,
        password = password
    )

}


@Serializable
data class AuthSessionChallengeResponse(
    val challenge: AuthSessionChallenge? = null,
    val status: AuthSessionChallengeStatus,
//    val failure: AuthSessionChallengeFailure?
)

@Serializable(with = AuthSessionChallengeSerializer::class)
data class AuthSessionChallenge(
    val id: String,
    val type: String,
    val challengeType: AuthSessionChallengeType,
)

@Serializable
sealed class AuthSessionChallengeType {
    @Serializable
    data class UsernamePassword(
        @SerialName("username_label")
        val usernameLabel: String,
        @SerialName("password_label")
        val passwordLabel: String,
        @SerialName("forgot_password_url")
        val forgotPasswordUrl: String? = null,
    ) : AuthSessionChallengeType()

    @Serializable
    data class Text(
        val text: Text,
    ) : AuthSessionChallengeType() {

        @Serializable
        data class Text(val label: String)
    }

    @Serializable
    data class TokenizedText(
        val tokenizedText: TokenizedText,
    ) : AuthSessionChallengeType() {

        @Serializable
        data class TokenizedText(val label: String)
    }

    @Serializable
    data class Options(
        val options: AuthOptions,
    ) : AuthSessionChallengeType() {
        @Serializable
        data class AuthOptions(val prompt: String, val options: List<AuthOption>)

        @Serializable
        data class AuthOption(val label: String, val value: String)
    }
}

data class AuthSessionChallengeFailure(
    val challenge: String?,
    val message: String,
    val invalidKeys: List<String>?
)

@Serializable(with = AuthSessionChallengeStatus.Serializer::class)
enum class AuthSessionChallengeStatus(val value: String) {
    @SerialName("complete")
    Complete("complete"),

    @SerialName("awaiting_challenge")
    AwaitingChallenge("awaiting_challenge"),

    @SerialName("awaiting_answer")
    AwaitingAnswer("awaiting_answer"),

    @SerialName("failed")
    Failed("failed"),

    @SerialName("unknown")
    Unknown("unknown");

    internal object Serializer :
        EnumIgnoreUnknownSerializer<AuthSessionChallengeStatus>(values(), Unknown)
}


data class AuthenticationParams(val map: Map<String, String>)

data class AuthSessionKeyId(
    val authenticationParams: AuthenticationParams,
    val clientSecret: String,
    val authSessionId: String
)