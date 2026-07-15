import SwiftUI

struct EnableActivityScreen: View {
    let financialConnectionsAccounts: [FinancialConnectionsAccount]
    let onToggle: (String, Bool) -> Void

    var body: some View {
        VStack(spacing: LinkSpacing.medium) {
            ListGroupCard(
                items: financialConnectionsAccounts,
                itemContent: { account in
                    FinancialConnectionsAccountRow(account: account, onToggle: onToggle)
                },
                onItemClick: { _ in nil }
            )
        }
        .padding()
    }
}

struct FinancialConnectionsAccount: Identifiable, Equatable {
    let id: String
    let institutionName: String
    let accountName: String
    var canToggle: Bool
    var isSubscribed: Bool
}

struct FinancialConnectionsAccountRow: View {
    let account: FinancialConnectionsAccount
    let onToggle: (String, Bool) -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: LinkSpacing.xSmall) {
                Text(account.institutionName)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                Text(account.accountName)
                    .font(.body)
            }
            Spacer()
            Toggle(
                "",
                isOn: Binding(
                    get: { account.isSubscribed },
                    set: { onToggle(account.id, $0) }
                )
            )
            .labelsHidden()
            .disabled(!account.canToggle)
        }
    }
}
