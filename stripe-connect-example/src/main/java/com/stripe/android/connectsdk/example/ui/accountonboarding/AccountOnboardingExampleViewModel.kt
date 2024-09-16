
class AccountOnboardingExampleViewModel(
    private val embeddedComponentService: EmbeddedComponentService = EmbeddedComponentService(),
) : ViewModel() {

    @OptIn(PrivateBetaConnectSDK::class)
    fun fetchClientSecret(resultCallback: ClientSecretResultCallback) {
        viewModelScope.launch {
            val clientSecret = embeddedComponentService.fetchClientSecret()
            if (clientSecret != null) {
                resultCallback.onResult(clientSecret)
            } else {
                resultCallback.onError()
            }
        }
    }
}
