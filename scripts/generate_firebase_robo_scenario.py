#!/usr/bin/env python3
import argparse
import json
import os
import re
from pathlib import Path


CONNECT_APP = "com.stripe.android.connect.example"
CONNECTIONS_APP = "com.stripe.android.financialconnections.example"


def descriptor_id(resource_id: str) -> dict:
    return {"resourceIdRegex": rf".*{re.escape(resource_id)}"}


def descriptor_text(text: str) -> dict:
    return {"text": text}


def descriptor_text_regex(pattern: str) -> dict:
    return {"textRegex": pattern}


def adb(command: str) -> dict:
    return {"eventType": "ADB_SHELL_COMMAND", "command": command}


def wait_id(resource_id: str, timeout: int = 30_000) -> dict:
    return {
        "eventType": "WAIT_FOR_ELEMENT",
        "delayTime": timeout,
        "elementDescriptors": [descriptor_id(resource_id)],
    }


def wait_text(text: str, timeout: int = 30_000) -> dict:
    return {
        "eventType": "WAIT_FOR_ELEMENT",
        "delayTime": timeout,
        "elementDescriptors": [descriptor_text(text)],
    }


def wait_text_regex(pattern: str, timeout: int = 30_000) -> dict:
    return {
        "eventType": "WAIT_FOR_ELEMENT",
        "delayTime": timeout,
        "elementDescriptors": [descriptor_text_regex(pattern)],
    }


def click_id(resource_id: str, optional: bool = False) -> dict:
    return {
        "eventType": "VIEW_CLICKED",
        "elementDescriptors": [descriptor_id(resource_id)],
        "optional": optional,
    }


def click_text(text: str, optional: bool = False) -> dict:
    return {
        "eventType": "VIEW_CLICKED",
        "elementDescriptors": [descriptor_text(text)],
        "optional": optional,
    }


def input_id(resource_id: str, text: str) -> dict:
    return {
        "eventType": "VIEW_TEXT_CHANGED",
        "replacementText": text,
        "elementDescriptors": [descriptor_id(resource_id)],
    }


def assert_id(resource_id: str) -> dict:
    return {
        "eventType": "ASSERTION",
        "contextDescriptor": {
            "condition": "element_present",
            "elementDescriptors": [descriptor_id(resource_id)],
        },
    }


def assert_text(text: str) -> dict:
    return {
        "eventType": "ASSERTION",
        "contextDescriptor": {
            "condition": "element_present",
            "elementDescriptors": [descriptor_text(text)],
        },
    }


def assert_text_regex(pattern: str) -> dict:
    return {
        "eventType": "ASSERTION",
        "contextDescriptor": {
            "condition": "element_present",
            "elementDescriptors": [descriptor_text_regex(pattern)],
        },
    }


def swipe_up(count: int = 1) -> list[dict]:
    return [adb("input swipe 540 1800 540 500 300") for _ in range(count)]


def hide_keyboard() -> dict:
    return adb("input keyevent 4")


def input_focused(text: str) -> dict:
    return adb(f"input text {text}")


def chrome_first_run_actions() -> list[dict]:
    return [
        {"eventType": "WAIT", "delayTime": 2_000},
        {
            "eventType": "VIEW_CLICKED",
            "elementDescriptors": [
                {"resourceId": "com.android.chrome:id/signin_fre_dismiss_button"}
            ],
            "optional": True,
        },
        {
            "eventType": "VIEW_CLICKED",
            "elementDescriptors": [
                {"resourceId": "com.android.chrome:id/terms_accept"}
            ],
            "optional": True,
        },
        {
            "eventType": "VIEW_CLICKED",
            "elementDescriptors": [
                {"resourceId": "com.android.chrome:id/negative_button"}
            ],
            "optional": True,
        },
        click_text("Close app", optional=True),
    ]


def clear_and_launch(app_id: str) -> list[dict]:
    return [
        adb(f"pm clear {app_id}"),
        adb(f"monkey -p {app_id} -c android.intent.category.LAUNCHER 1"),
    ]


def clear_and_open_link(uri: str) -> list[dict]:
    return [
        adb(f"pm clear {CONNECTIONS_APP}"),
        adb(
            "am start -W -a android.intent.action.VIEW "
            f"-d '{uri}' {CONNECTIONS_APP}"
        ),
    ]


def finish(name: str, assertion: dict) -> list[dict]:
    return [
        assertion,
        {"eventType": "TAKE_SCREENSHOT", "screenshotName": name},
        {"eventType": "TERMINATE_CRAWL"},
    ]


def connect_account_onboarding() -> list[dict]:
    return [
        *clear_and_launch(CONNECT_APP),
        wait_id("settings_button", 60_000),
        click_id("settings_button"),
        *swipe_up(2),
        wait_id("other_account_input", 60_000),
        input_id("other_account_input", "acct_1RKLk9PwPtoT2bUJ"),
        hide_keyboard(),
        click_id("save_button"),
        wait_text("Account Onboarding", 60_000),
        click_text("Account Onboarding"),
        wait_text("Review and confirm", 60_000),
        *finish("connect-account-onboarding", assert_text("Review and confirm")),
    ]


def open_connections(uri: str) -> list[dict]:
    return [
        *clear_and_open_link(uri),
        *swipe_up(2),
        wait_id("connect_accounts", 30_000),
        click_id("connect_accounts"),
        wait_id("consent_cta", 30_000),
    ]


def oauth_data_flow(connected_account: bool) -> list[dict]:
    account_suffix = "&stripe_account_id=acct_1PnnD9CY58qxxwvr" if connected_account else ""
    merchant = "networking" if connected_account else "testmode"
    uri = (
        "stripeconnectionsexample://playground?integration_type=Standalone"
        "&experience=FinancialConnections&flow=Data"
        "&financial_connections_override_native=native"
        f"&merchant={merchant}&financial_connections_test_mode=true{account_suffix}"
    )
    actions = [
        *open_connections(uri),
        click_text("Agree and continue"),
        wait_id("bcinst_LLQZzmKZMjl0j0", 30_000),
        click_id("bcinst_LLQZzmKZMjl0j0"),
        wait_id("prepane_cta", 30_000),
        click_id("prepane_cta"),
        *chrome_first_run_actions(),
        wait_text("Connect accounts", 60_000),
        click_text("Connect accounts"),
        wait_id("skip_cta", 30_000),
        click_id("skip_cta"),
        wait_text("Your accounts were connected", 30_000),
        click_id("done_button"),
        *swipe_up(2),
        wait_text_regex(".*Completed!.*", 60_000),
    ]
    if not connected_account:
        actions.append(wait_text_regex(".*StripeBank.*", 30_000))
    return [
        *actions,
        *finish(
            "connections-data-oauth-connected" if connected_account else "connections-data-oauth",
            assert_text_regex(".*Completed!.*"),
        ),
    ]


def instant_debits(email: str) -> list[dict]:
    uri = (
        "stripeconnectionsexample://playground?integration_type=Standalone"
        "&experience=InstantDebits&flow=PaymentIntent"
        "&financial_connections_override_native=native&merchant=networking"
        "&financial_connections_test_mode=true&permissions=transactions,payment_method"
        "&financial_connections_confirm_intent=false"
    )
    return [
        *clear_and_open_link(uri),
        *swipe_up(2),
        wait_id("Customer email setting", 30_000),
        click_id("Customer email setting"),
        input_focused(email),
        hide_keyboard(),
        *swipe_up(2),
        wait_id("connect_accounts", 30_000),
        click_id("connect_accounts"),
        wait_id("consent_cta", 30_000),
        click_id("consent_cta"),
        wait_text_regex(".*555.*", 30_000),
        input_focused("6223115555"),
        click_text("Continue with Link"),
        wait_id("bcinst_QsDedeogZ5PA7V", 30_000),
        click_id("bcinst_QsDedeogZ5PA7V"),
        *chrome_first_run_actions(),
        wait_text("Success", 60_000),
        click_text("Success"),
        click_text("Connect account"),
        wait_text("Your account was connected", 30_000),
        click_id("done_button"),
        *swipe_up(2),
        wait_text_regex("Session Completed!.*", 30_000),
        click_id("connect_accounts"),
        wait_id("consent_cta", 30_000),
        click_id("consent_cta"),
        wait_id("existing_email-button", 30_000),
        click_id("existing_email-button"),
        wait_id("OTP-0", 30_000),
        click_id("OTP-0"),
        input_focused("111111"),
        wait_text("Success", 30_000),
        click_text("Success"),
        click_text("Connect account"),
        wait_text("Your account was connected", 30_000),
        click_id("done_button"),
        *swipe_up(2),
        wait_text_regex("Session Completed!.*", 30_000),
        *finish("connections-instant-debits", assert_text_regex("Session Completed!.*")),
    ]


def unplanned_downtime() -> list[dict]:
    uri = (
        "stripeconnectionsexample://playground?integration_type=Standalone"
        "&experience=FinancialConnections&flow=PaymentIntent"
        "&financial_connections_override_native=native&financial_connections_test_mode=true"
        "&merchant=testmode&permissions=payment_method"
    )
    return [
        *open_connections(uri),
        click_id("consent_cta"),
        wait_text("Search", 30_000),
        *swipe_up(4),
        wait_text("Down (Unscheduled)", 30_000),
        click_text("Down (Unscheduled)"),
        wait_text("Select another bank", 30_000),
        click_text("Select another bank"),
        wait_text("Search", 30_000),
        *swipe_up(4),
        wait_text("Down (Unscheduled)", 30_000),
        click_text("Down (Unscheduled)"),
        wait_text("Close icon", 30_000),
        click_text("Close icon"),
        *swipe_up(2),
        wait_text_regex("Failed! Request-id: .*", 30_000),
        *finish("connections-unplanned-downtime", assert_text_regex("Failed! Request-id: .*")),
    ]


def payment_intent() -> list[dict]:
    uri = (
        "stripeconnectionsexample://playground?integration_type=Standalone"
        "&experience=FinancialConnections&flow=PaymentIntent"
        "&financial_connections_override_native=native&merchant=testmode"
        "&permissions=payment_method&financial_connections_test_mode=true"
        "&financial_connections_confirm_intent=true"
    )
    return [
        *open_connections(uri),
        click_id("consent_cta"),
        wait_text("Test (Non-OAuth)", 30_000),
        click_text("Test (Non-OAuth)"),
        *chrome_first_run_actions(),
        wait_text("Success", 60_000),
        click_text("Success"),
        click_text("Connect account"),
        wait_id("skip_cta", 30_000),
        click_id("skip_cta", optional=True),
        wait_id("done_button", 30_000),
        click_id("done_button"),
        *swipe_up(2),
        wait_text_regex(".*Intent Confirmed!.*", 30_000),
        *finish("connections-payment-intent", assert_text_regex(".*Intent Confirmed!.*")),
    ]


def manual_entry() -> list[dict]:
    uri = (
        "stripeconnectionsexample://playground?integration_type=Standalone"
        "&experience=FinancialConnections&flow=Token"
        "&financial_connections_override_native=native&financial_connections_test_mode=true"
        "&merchant=testmode&permissions=balances,payment_method"
    )
    return [
        *open_connections(uri),
        click_text("Manually verify instead"),
        wait_text("Enter bank details", 30_000),
        input_id("RoutingInput", "110000000"),
        hide_keyboard(),
        *swipe_up(),
        input_id("AccountInput", "000123456789"),
        hide_keyboard(),
        *swipe_up(),
        input_id("ConfirmAccountInput", "000123456789"),
        click_text("Submit"),
        wait_id("skip_cta", 30_000),
        click_id("skip_cta", optional=True),
        wait_id("done_button", 30_000),
        click_id("done_button"),
        *swipe_up(2),
        wait_text_regex(".*Completed!.*", 30_000),
        *finish("connections-manual-entry", assert_text_regex(".*Completed!.*")),
    ]


def networked_manual_entry(email: str) -> list[dict]:
    uri = (
        "stripeconnectionsexample://playground?experience=FinancialConnections&flow=Token"
        "&financial_connections_override_native=native&merchant=networking"
        "&financial_connections_test_mode=true&permissions=payment_method"
        "&financial_connections_confirm_intent=true"
    )
    return [
        *clear_and_open_link(uri),
        *swipe_up(2),
        wait_id("Customer email setting", 30_000),
        click_id("Customer email setting"),
        input_focused(email),
        hide_keyboard(),
        *swipe_up(2),
        wait_id("connect_accounts", 30_000),
        click_id("connect_accounts"),
        wait_id("consent_cta", 30_000),
        {"eventType": "POINT_TAP", "pointTapXPercent": 50, "pointTapYPercent": 93},
        wait_text("Use test account", 30_000),
        click_text("Use test account"),
        wait_text_regex(".*555.*", 30_000),
        input_focused("6223115555"),
        click_text("Save with Link"),
        wait_id("done_button", 30_000),
        click_id("done_button"),
        *swipe_up(2),
        wait_text_regex(".*Completed.*", 30_000),
        click_id("connect_accounts"),
        wait_id("consent_cta", 30_000),
        click_id("consent_cta"),
        wait_id("existing_email-button", 30_000),
        click_id("existing_email-button"),
        wait_id("OTP-0", 30_000),
        click_id("OTP-0"),
        input_focused("111111"),
        wait_text("Connect account", 30_000),
        click_text("Connect account"),
        wait_id("done_button", 30_000),
        click_id("done_button"),
        *swipe_up(2),
        wait_text_regex(".*Completed.*", 30_000),
        *finish("connections-networked-manual-entry", assert_text_regex(".*Completed.*")),
    ]


FINANCIAL_CONNECTIONS_SCENARIOS = (
    "data-oauth-connected",
    "data-oauth",
    "instant-debits",
    "unplanned-downtime",
    "payment-intent",
    "manual-entry",
    "networked-manual-entry",
)


def scenario_actions(name: str, email: str) -> list[dict]:
    scenarios = {
        "connect-account-onboarding": connect_account_onboarding,
        "data-oauth-connected": lambda: oauth_data_flow(connected_account=True),
        "data-oauth": lambda: oauth_data_flow(connected_account=False),
        "instant-debits": lambda: instant_debits(email),
        "unplanned-downtime": unplanned_downtime,
        "payment-intent": payment_intent,
        "manual-entry": manual_entry,
        "networked-manual-entry": lambda: networked_manual_entry(email),
    }
    try:
        return scenarios[name]()
    except KeyError as error:
        raise ValueError(f"Unknown scenario: {name}") from error


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    selector = parser.add_mutually_exclusive_group(required=True)
    selector.add_argument("--scenario")
    selector.add_argument("--financial-connections-index", type=int)
    parser.add_argument("--output", type=Path, required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    scenario = args.scenario
    if args.financial_connections_index is not None:
        try:
            scenario = FINANCIAL_CONNECTIONS_SCENARIOS[args.financial_connections_index]
        except IndexError as error:
            raise ValueError(
                f"Financial Connections scenario index must be between 0 and "
                f"{len(FINANCIAL_CONNECTIONS_SCENARIOS) - 1}"
            ) from error

    build_number = re.sub(r"[^a-zA-Z0-9]", "", os.environ.get("BITRISE_BUILD_NUMBER", "local"))
    shard_index = re.sub(r"[^a-zA-Z0-9]", "", os.environ.get("BITRISE_IO_PARALLEL_INDEX", "0"))
    email = f"ftl{build_number}{shard_index}@example.com"

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8") as output_file:
        json.dump(scenario_actions(scenario, email), output_file, indent=2)
        output_file.write("\n")

    print(f"Generated {scenario}: {args.output}")


if __name__ == "__main__":
    main()
