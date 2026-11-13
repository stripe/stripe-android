# Email and lifecycle documentation contracts

Parent read authoritative Markdoc via Sourcegraph on 2026-11-12. Email revision `bc0cb468f4869ecbd66ef6f996ff3851a80b48ce`; curated extract.

Source: https://stripe.sourcegraphcloud.com/stripe-internal/mint@bc0cb468f4869ecbd66ef6f996ff3851a80b48ce/-/blob/pay-server/docs/content/payments/mobile/checkout-sessions/partials/collect-customer-email.md

- Lines 13–15: valid checkout email required before confirmation; used for fraud, returning Link customers, enabled receipts; separate from PM billing email.
- Lines 21–23: server customer_email or Customer with email means merchant need not render email field. Immutable on client; do not combine with defaults.email/updateEmail. Docs promise overriding returns error—already disproved/reported in ISSUE_docs_foundation_fixed_email_error_contract.md; do not duplicate.
- Lines 27–29: editable email requires own UI; don't set server customer_email or pass Customer with email. Optional initial value in configuration defaults before initialization.
- Android 36–43: `CheckoutController.Configuration().defaults(CheckoutController.Configuration.Defaults().email(customerEmail)).paymentElement(PaymentElement.Configuration())`. Class-name drift already reported.
- Line 52: populate field with default, call updateEmail on entry/change; handle validation errors; valid email before confirm; omitted email yields confirmation error.

Build guide (previously fetched by parent/Foundation) instructs controller in ViewModel with SavedStateHandle, destroy in onCleared, create presenter during Activity creation, retain element instances. Express guide at revision7f9d2f1a0f1349f4d6f0d617009b7f8882337eae includes explicit rotation testing and result StateFlow cleared after handling to avoid repeat UI effects after recreation (accept-a-payment.md lines421–437,546).

Unconfirmed Foundation lead (see its RUN_NOTES): selected 3155 card first failed confirmation for missing checkout email; valid updateEmail followed by Confirm reported no payment method selected. Reopening Select retained card fields; Continue restored selection and authentication worked. Close timing left cause unresolved. Isolate before any failed confirm, after failure, and after awaiting update completion. Do not presume SDK bug.

Lifecycle coverage must distinguish SDK behavior from playground integration and from intentional security handling of card data. For process recreation, verify reuse of same Session/client secret and valid documented merchant wiring before claiming lost state; a new Session isn't evidence of broken restoration. Never infer cancellation from timeout; use explicit observed cancellation. Logout/clearLocalCustomerData can be checked against public source and the guide's own logout instructions (request exact source if required).

Parent additionally read build-a-checkout-page.md lines1083–1131 at revision32675c217641012e5770c477e93b16ed82560ff9: Mobile Checkout Elements store local information remembering customer Link use; app logout should clear this authentication state. Android explicitly calls `CheckoutElementsController.clearLocalCustomerData(context.applicationContext)` under CheckoutSessionPreview opt-in (lines1115–1127). The actual class name in this logout snippet is correct.
