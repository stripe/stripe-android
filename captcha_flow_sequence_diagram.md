# Passive CAPTCHA Challenge Flow Sequence Diagram

This sequence diagram shows the flow of how payment confirmation works with the passive CAPTCHA challenge integration.

```mermaid
sequenceDiagram
    participant User
    participant PaymentLauncher
    participant ConfirmationHandler
    participant ChallengeConfirmationDefinition
    participant PassiveChallengeActivity
    participant HCaptchaService
    participant IntentConfirmationDefinition
    participant StripeAPI

    User->>PaymentLauncher: confirm(params)
    PaymentLauncher->>ConfirmationHandler: handleConfirmation(option)
    Note over ConfirmationHandler: Check ElementsSession.enablePassiveCaptcha
    
    alt enablePassiveCaptcha == true
        ConfirmationHandler->>ChallengeConfirmationDefinition: action(option, parameters)
        ChallengeConfirmationDefinition->>ChallengeConfirmationDefinition: ConfirmationDefinition.Action.Launch
        ChallengeConfirmationDefinition->>PassiveChallengeActivity: launch(args)
        PassiveChallengeActivity->>PassiveChallengeViewModel: startPassiveChallenge(activity)
        PassiveChallengeViewModel->>HCaptchaService: performPassiveHCaptcha(activity)
        HCaptchaService->>HCaptchaService: hcaptcha.setup(config).verifyWithHCaptcha()
        Note over HCaptchaService: Invisible CAPTCHA verification
        HCaptchaService-->>PassiveChallengeViewModel: Result.Success(token)
        PassiveChallengeViewModel->>PassiveChallengeViewModel: Update confirmation option with RadarOptions
        PassiveChallengeViewModel-->>PassiveChallengeActivity: Updated confirmation option
        PassiveChallengeActivity-->>ChallengeConfirmationDefinition: Result.NextStep
        ChallengeConfirmationDefinition-->>ConfirmationHandler: Updated confirmation option
    end

    ConfirmationHandler->>IntentConfirmationDefinition: action(option, parameters)
    IntentConfirmationDefinition->>PaymentLauncherConfirmationActivity: launch(args)
    PaymentLauncherConfirmationActivity->>PaymentLauncherViewModel: confirmStripeIntent(params)
    Note over PaymentLauncherViewModel: Params include RadarOptions with hcaptcha_token
    PaymentLauncherViewModel->>StripeAPI: confirm intent with hcaptcha_token
    StripeAPI-->>PaymentLauncherViewModel: Intent response
    PaymentLauncherViewModel-->>PaymentLauncherConfirmationActivity: Result
    PaymentLauncherConfirmationActivity-->>PaymentLauncher: Result
    PaymentLauncher-->>User: Payment result
```

## Flow Description

1. The flow starts with the user initiating payment confirmation
2. When passive CAPTCHA is enabled (via `ElementsSession.enablePassiveCaptcha`), the `ChallengeConfirmationDefinition` is hit first
3. HCaptcha verification is performed invisibly in `PassiveChallengeActivity`
4. The confirmation option is updated with the CAPTCHA token in `RadarOptions`
5. The flow then continues to the regular `IntentConfirmationDefinition` with the updated params
6. The API request includes the CAPTCHA token for fraud prevention