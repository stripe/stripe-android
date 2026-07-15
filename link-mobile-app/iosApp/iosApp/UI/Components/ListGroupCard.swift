import SwiftUI

struct ListGroupCard<Item: Identifiable & Equatable, Content: View>: View {
    private let items: [Item]
    private let itemContent: (Item) -> Content
    private let onItemClick: (Item) -> (() -> Void)?

    @State private var animatedItems: [Item]

    // MARK: - Canonical init

    init(
        items: [Item],
        @ViewBuilder itemContent: @escaping (Item) -> Content,
        onItemClick: @escaping (Item) -> (() -> Void)?
    ) {
        self.items = items
        self.itemContent = itemContent
        self.onItemClick = onItemClick
        self._animatedItems = State(initialValue: items)
    }

    // MARK: - Convenience init (backward-compatible)

    /// Convenience initialiser that accepts the classic `(Item) -> Void` callback
    /// so existing callers compile unchanged.
    init(
        items: [Item],
        @ViewBuilder itemContent: @escaping (Item) -> Content,
        onItemClick: @escaping (Item) -> Void
    ) {
        self.init(items: items, itemContent: itemContent) { item in
            { onItemClick(item) }
        }
    }

    // MARK: - Body

    var body: some View {
        VStack(spacing: 0) {
            ForEach(animatedItems) { item in
                let clickAction = onItemClick(item)
                if let clickAction {
                    ListRowButton(action: clickAction) {
                        itemContent(item)
                    }
                } else {
                    ListRowContent {
                        itemContent(item)
                    }
                }

                if item.id != animatedItems.last?.id {
                    Divider()
                        .padding(.leading, LinkSpacing.medium)
                }
            }
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: LinkCornerRadius.medium))
        .onChange(of: items) { newItems in
            withAnimation {
                animatedItems = newItems
            }
        }
    }
}
