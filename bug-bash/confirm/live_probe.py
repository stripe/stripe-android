"""Live test-mode Checkout SFU/PMO SFU probes; no native UI is exercised.

Run `python3 bug-bash/confirm/live_probe.py PHASE`.
PHASE: boundaries, sfu, global, pmo, sepa, precedence, save-none, or mixed.
Each phase creates fresh synthetic customers/sessions. Keys remain in memory.
"""
import importlib.util
import json
from pathlib import Path
import sys
import urllib.parse

sys.dont_write_bytecode = True
OUT = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("email_probe", OUT.parent / "email-collection/live_probe.py")
api = importlib.util.module_from_spec(spec)
spec.loader.exec_module(api)
api.OUT = OUT
request, backend, client = api.request, api.backend, api.client


def save(name, value):
    def clean(item):
        if isinstance(item, dict):
            return {k: "https://example.com/redacted" if k in ("url", "return_url")
                    and isinstance(v, str) and v.startswith("https://") else clean(v)
                    for k, v in item.items()}
        if isinstance(item, list):
            return [clean(v) for v in item]
        return item
    api.save(name, clean(value))


def create(name, key, customer, types, **extra):
    params = {"ui_mode": "mobile_elements", "currency": "eur", "customer": customer,
              "allowed_payment_method_types": types,
              "automatic_tax": {"enabled": False}, "adaptive_pricing": {"enabled": False},
              "items": [{"type": "one_time_price", "one_time_price": {"items": [{
                  "price_data": {"currency": "eur", "unit_amount": 12000,
                                 "product_data": {"name": "Confirm SFU Test"}}, "quantity": 1}]}}], **extra}
    save(name + "-creation", params)
    secret = backend("create_checkout_session", params)["client_secret"]
    response = client(secret, "init", key)
    save(name + "-init", response)
    assert response["livemode"] is False and response["status"] == "open"
    return secret, response


def inventory(response):
    return [{k: p.get(k) for k in ("id", "type", "allow_redisplay")}
            for p in (response.get("customer") or {}).get("payment_methods", [])]


def run_case(key, name, sfu=None, pmo=None, choice=None, enabled=False, method="card", types=None):
    customer = backend("create_customer", {"email": "confirm-sfu@example.com"})["id"]
    extra = {"saved_payment_method_options": {"payment_method_save": "enabled" if enabled else "disabled"}}
    if sfu is not None:
        extra["payment_settings"] = {"setup_future_usage": sfu}
    if pmo is not None:
        extra["payment_method_options"] = {method: {"setup_future_usage": pmo}}
    try:
        secret, initial = create(name, key, customer, types or [method], **extra)
        effective = initial.get("setup_future_usage_for_payment_method_type", {}).get(method)
        # PaymentMethodMetadata + PaymentMethodSaveConsentBehavior derive these values.
        redisplay = "always" if enabled and choice else "limited" if effective in ("on_session", "off_session") else "unspecified"
        params = {"payment_method_data[type]": method,
                  "payment_method_data[allow_redisplay]": redisplay,
                  "payment_method_data[billing_details][name]": "Confirm SFU Test",
                  "payment_method_data[billing_details][email]": "confirm-sfu@example.com",
                  "expected_payment_method_type": method, "expected_amount": 12000,
                  "return_url": "stripesdk://payment_return_url/" + api.PACKAGE}
        if method == "card":
            params["payment_method_data[card][token]"] = "tok_visa"
        elif method == "sepa_debit":
            params["payment_method_data[sepa_debit][iban]"] = "DE89370400440532013000"
        if choice is not None:
            params["collected_information[save_payment_method]"] = str(choice).lower()
        save(name + "-confirm-request", params)
        confirmed = client(secret, "confirm", key, params)
        save(name + "-confirm", confirmed)
        ref = confirmed["payment_response"]["payment_intent"]
        pi = request("https://api.stripe.com/v1/payment_intents/" + ref["id"] + "?" +
                     urllib.parse.urlencode({"client_secret": ref["client_secret"], "expand[]": "payment_method"}), key=key)
        save(name + "-payment-intent", pi)
        pm = pi["payment_method"]
        _, default = create(name + "-returning-default", key, customer, [method])
        _, all_methods = create(name + "-returning-all", key, customer, [method],
                                saved_payment_method_options={"allow_redisplay_filters": ["always", "limited", "unspecified"]})
        result = {"case": name, "method": method, "merchant_sfu": sfu, "merchant_pmo_sfu": pmo,
                  "save_enabled": enabled, "save_choice": choice, "effective_initial_sfu": effective,
                  "initial_save_options": initial.get("saved_payment_method_options"),
                  "initial_offer_save": initial.get("customer_managed_saved_payment_methods_offer_save"),
                  "sent_allow_redisplay": redisplay, "checkout_status": confirmed["status"],
                  "payment_status": confirmed["payment_status"], "intent_status": pi["status"],
                  "intent_sfu": pi.get("setup_future_usage"), "intent_method_options": pi.get("payment_method_options"),
                  "payment_method_id": pm["id"], "payment_method_customer": pm.get("customer"),
                  "payment_method_redisplay": pm.get("allow_redisplay"),
                  "customer_id": customer, "returning_default": inventory(default), "returning_all": inventory(all_methods)}
        assert confirmed["status"] == "complete"
        assert pi["status"] == ("succeeded" if method == "card" else "processing")
    except api.ApiError as error:
        save(name + "-error", error.body)
        result = {"case": name, "error": error.body}
    print(json.dumps(api.redact(result)), flush=True)
    return result


def boundaries(key):
    customer = backend("create_customer", {"email": "confirm-sfu@example.com"})["id"]
    results = []
    cases = [("global-none", {"payment_intent_data": {"setup_future_usage": "none"}}),
             ("modeless-global-none", {"payment_settings": {"setup_future_usage": "none"}}),
             ("top-level-sfu", {"setup_future_usage": "off_session"}),
             ("sepa-on", {"payment_method_options": {"sepa_debit": {"setup_future_usage": "on_session"}}})]
    for name, extra in cases:
        name = "boundary-" + name
        try:
            _, initial = create(name, key, customer, ["card", "sepa_debit"], **extra)
            result = {"case": name, "accepted": True,
                      "effective_sfu": initial.get("setup_future_usage_for_payment_method_type")}
        except api.ApiError as error:
            save(name + "-error", error.body)
            result = {"case": name, "error": error.body}
        results.append(result)
        print(json.dumps(api.redact(result)), flush=True)
    save("boundaries-results", results)


PHASES = {
    "sfu": [
        ("sfu-absent-disabled", {}),
        ("sfu-absent-unchecked", {"enabled": True, "choice": False}),
        ("sfu-absent-checked", {"enabled": True, "choice": True}),
    ],
    "global": [
        ("global-on-disabled", {"sfu": "on_session"}),
        ("global-off-unchecked", {"sfu": "off_session", "enabled": True, "choice": False}),
        ("global-on-checked", {"sfu": "on_session", "enabled": True, "choice": True}),
        ("global-on-pmo-off", {"sfu": "on_session", "pmo": "off_session"}),
        ("global-off-pmo-none", {"sfu": "off_session", "pmo": "none"}),
    ],
    "pmo": [
        ("pmo-off-disabled", {"pmo": "off_session"}),
        ("pmo-on-disabled", {"pmo": "on_session"}),
        ("pmo-none-disabled", {"pmo": "none"}),
        ("pmo-off-unchecked", {"pmo": "off_session", "enabled": True, "choice": False}),
        ("pmo-on-checked", {"pmo": "on_session", "enabled": True, "choice": True}),
        ("pmo-none-checked", {"pmo": "none", "enabled": True, "choice": True}),
    ],
    "sepa": [
        ("sepa-pmo-off", {"method": "sepa_debit", "pmo": "off_session"}),
        ("sepa-pmo-none", {"method": "sepa_debit", "pmo": "none"}),
        ("sepa-pmo-on", {"method": "sepa_debit", "pmo": "on_session"}),
    ],
    "precedence": [
        ("sepa-global-off-pmo-none", {"method": "sepa_debit", "sfu": "off_session", "pmo": "none"}),
        ("sepa-global-off-pmo-none-checked", {"method": "sepa_debit", "sfu": "off_session", "pmo": "none", "enabled": True, "choice": True}),
    ],
    "save-none": [
        ("sepa-save-control", {"method": "sepa_debit", "enabled": True, "choice": True}),
        ("sepa-none-checked", {"method": "sepa_debit", "pmo": "none", "enabled": True, "choice": True}),
    ],
    "mixed": [
        ("mixed-sepa-none-checked", {"method": "sepa_debit", "types": ["card", "sepa_debit"], "pmo": "none", "enabled": True, "choice": True}),
    ],
}


if __name__ == "__main__":
    key = request(api.BASE + "/publishable_key?merchant=US")["publishable_key"]
    assert key.startswith("pk_test_"), "Only test mode is authorized"
    phase = sys.argv[1]
    if phase == "boundaries":
        boundaries(key)
    else:
        results = []
        for name, options in PHASES[phase]:
            results.append(run_case(key, name, **options))
            save(phase + "-results", results)
