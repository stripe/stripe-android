import SwiftUI

struct PaymentMethodScreen: View {
    let state: PaymentMethodState

    var body: some View {
        VStack {
            // Payment method content
            PaymentMethodContentFooter(
                actions: state.footerActions,
                isActionEnabled: state.isActionEnabled
            )
        }
    }
}

struct PaymentMethodState {
    let footerActions: [PaymentMethodAction]
    let isActionEnabled: (PaymentMethodAction) -> Bool
}

struct PaymentMethodAction: Identifiable, Equatable {
    let id: String
    let title: String
    let switchState: SwitchState?
    let onClick: () -> Void

    static func == (lhs: PaymentMethodAction, rhs: PaymentMethodAction) -> Bool {
        lhs.id == rhs.id
    }
}

struct SwitchState: Equatable {
    var isOn: Bool
    var isEnabled: Bool

    init(isOn: Bool, isEnabled: Bool = true) {
        self.isOn = isOn
        self.isEnabled = isEnabled
    }
}

private func resolveIsActionEnabled(_ action: PaymentMethodAction, using isEnabled: (PaymentMethodAction) -> Bool) -> Bool {
    isEnabled(action)
}

struct PaymentMethodContentFooter: View {
    let actions: [PaymentMethodAction]
    let isActionEnabled: (PaymentMethodAction) -> Bool

    private func resolveIsActionEnabled(_ action: PaymentMethodAction) -> Bool {
        isActionEnabled(action)
    }

    var body: some View {
        ListGroupCard(
            items: actions,
            itemContent: { action in
                PaymentMethodActionRow(action: action)
            },
            onItemClick: { action in
                guard action.switchState == nil else { return nil }
                return {
                    guard resolveIsActionEnabled(action) else { return }
                    action.onClick()
                }
            }
        )
    }
}

struct PaymentMethodActionRow: View {
    let action: PaymentMethodAction

    var body: some View {
        HStack {
            Text(action.title)
                .font(.body)
            Spacer()
            if let switchState = action.switchState {
                LinkSwitch(
                    isOn: .constant(switchState.isOn),
                    isEnabled: switchState.isEnabled
                )
            } else {
                Image(systemName: "chevron.right")
                    .foregroundColor(.secondary)
            }
        }
    }
}
