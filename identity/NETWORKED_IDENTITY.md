# Networked Identity foundation

This internal implementation follows the September 10, 2026 iOS handoff at
`aabdbc091de9`. It is deliberately absent from `IdentityNavGraph`. Existing merchant
Identity flows continue to use their current capture and disclosure screens.

The standalone flow supports typed email, Link lookup, fresh SMS start, OTP
confirmation, filtering saved documents against the current VerificationPage, and
changeable document selection. Selecting a document is not verification or submission.
There is no completion callback at selection and no clone, consent, or save button.

## Ownership and network behavior

- Construct `DefaultNetworkedIdentityRepository` through `create` with the merchant
  publishable key in `ApiRequest.Options`. Consumer operations use the consumer
  publishable key and omit merchant account scope. Identity ephemeral keys are not
  interchangeable with these keys.
- All eight Link operations use ordinary Android Stripe headers and form encoding,
  with `request_surface=web_identity_product`. No custom Identity client-version value
  is invented. Secret-list cookies are nested request-body values, not HTTP cookies.
- Only listing documents retries: one retry after 250 ms for HTTP 500–599. The
  production client has logging and inherited retries disabled. Transport and decode
  errors are sanitized before leaving the repository.
- Credentials and auth-session secrets remain in per-flow memory. The coordinator
  requires a new, unambiguous STARTED SMS ID and confirmation of that exact ID as
  VERIFIED, regardless of historical authenticated sessions.
- Scope `NetworkedIdentityViewModel` to the eventual host flow's ViewModelStore.
  Configuration changes retain it. Explicit cancellation/manual capture clears
  state immediately; external dismissal calls `abandon`. Permanent owner removal
  also abandons the flow through `onCleared`. Backgrounding and recomposition do
  not abandon it. Cleanup requests and late credential responses can drain after
  the owner is cleared; host callbacks are released at exit.
- Signup, association-token creation, and extension are API primitives only. Lookup
  not found uses ordinary capture without automatic signup. Extension has no
  automatic policy and does not overwrite auth-session secrets.

## Remaining contracts

1. Merchant publishable-key delivery and the host entry point after Identity's
   required disclosure. Eligibility alone must not open an unfinished flow.
2. Clone/attach endpoint, authentication, association-token linkage/lifetime,
   request/response, errors, and retry/idempotency behavior.
3. The post-capture save-consent/account-reference mutation. The backend copies
   images after successful checks; mobile does not upload them again or report
   saving complete merely because consent was recorded.
4. The dedicated skip/manual endpoint and timing for other fallback reasons.
   Cancelling saving before consent means not calling the consent API. Link logout
   is separate cleanup; it does not delete saved documents or revoke consent.
5. SMS verification-session ID behavior for first start, expiry restart, and resend.
   Android Link already sends `is_resend_sms_code=true` for explicit resend, but
   that precedent does not establish NI's fresh-ID contract.
6. Session-extension triggers, secret replacement/expiry, and shared Link storage
   ownership. No pre-capture credentials are retained through manual capture to
   support a later save prompt.
7. The SDK merchant-email response field and email-screen design. The public API's
   `provided_details.email` has not been assumed to be the SDK response shape.
8. NI test-account provisioning, reusable documents and OTP access, seed/reset
   instructions, example-backend session access, and final copy/multi-document design.

No additional per-attribute sharing screen is assumed. Preserve the existing
welcome disclosure and explicit reuse choice when integration becomes possible.

## Validation scope

Validated September 10, 2026 with Java 21: all 400 Identity test cases passed,
with zero skipped tests, failures, or errors. The focused NI set contains 124 test
cases (including five screenshot methods producing 20 visual variants).
`verifyPaparazziDebug`, `detekt`, and `lintDebug` all passed.

Tests use synthetic sessions, credentials, documents, and network responses.
Paparazzi cases cover blank/valid email, awaiting/invalid OTP, and selected documents
with light/dark and standard/large-font variants. These checks do not establish
live backend availability. No NI-enabled account or real backend session was used.

The checkout requires Java 21 for its Paparazzi plugin. Focused commands:

```sh
./gradlew :identity:testDebugUnitTest --tests '*NetworkedIdentity*' --tests '*VerificationPageNetworkingDataTest'
./gradlew :identity:recordPaparazziDebug --tests '*NetworkedIdentityScreenshotTest'
./gradlew :identity:verifyPaparazziDebug
./gradlew :identity:detekt
./gradlew :identity:lintDebug
```

Recording may stage baselines under the repository's screenshot workflow. This
handoff requests uncommitted work; keep source and reference images unstaged.

## Visual review

All 20 Pixel 6 reference images were inspected: the five cases below in both
appearances and both font sizes. They were compared against the iOS reference PNGs
in `StripeIdentityTests.NetworkedIdentityFlowViewControllerSnapshotTest` at the
handoff commit, not against an independently supplied final Figma export.

| Android case | iOS reference | Review |
| --- | --- | --- |
| `blankEmail` | `testEmailEntry` | Link/close header, neutral email input, visibly disabled primary action, filled manual action. |
| `validEmail` | `testEmailEntryWithValidEmail` | Enabled black/white primary action, readable email at both font sizes. |
| `awaitingOtp` | `testAwaitingOTP` | Six cells, redacted phone, plain manual action; Android input behavior retained. |
| `invalidOtp` | `testInvalidOTP` | Error wraps at larger text sizes without covering controls; error colors adapted for light/dark readability. |
| `selectedSavedDocument` | `testSelectedSavedDocument` | Redacted labels and changeable checkmark selection, visible row boundaries, no unfinished progression button. |

Snapshots live in `src/test/snapshots/images/`. The local comparison gallery is in
`build/reports/networked-identity/`; Paparazzi's report is in
`build/reports/paparazzi/debug/`. The viewport is 800 dp high and the suite uses the
repository's standard Pixel 6 runtime and 1.0/1.5 font scales. Platform typography,
spacing, email label treatment, and dark colors intentionally use Android controls.
NI copy and multi-document styling remain provisional. Robolectric covers semantics,
focus, IME actions and back handling; physical-device keyboard and TalkBack behavior
have not been exercised.
