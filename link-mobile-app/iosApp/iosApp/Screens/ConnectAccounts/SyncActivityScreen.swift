import SwiftUI

struct SyncActivityScreen: View {
    let accounts: [SyncActivityAccount]
    let onToggle: (String, Bool) -> Void

    var body: some View {
        ListGroupCard(items: accounts, itemContent: { account in
            SyncActivityAccountRow(account: account, onToggle: onToggle)
        }, onItemClick: { _ in nil })
    }
}

struct SyncActivityAccount: Identifiable, Equatable {
    let id: String
    let name: String
    var isEnabled: Bool
}

struct SyncActivityAccountRow: View {
    let account: SyncActivityAccount
    let onToggle: (String, Bool) -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: LinkSpacing.xSmall) {
                Text(account.name)
                    .font(.body)
            }
            Spacer()
            Toggle(
                "",
                isOn: Binding(
                    get: { account.isEnabled },
                    set: { onToggle(account.id, $0) }
                )
            )
            .labelsHidden()
        }
    }
}
