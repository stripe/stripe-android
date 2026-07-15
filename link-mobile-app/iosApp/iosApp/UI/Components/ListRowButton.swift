import SwiftUI

struct ListRowButton<Content: View>: View {
    private let action: () -> Void
    private let content: () -> Content
    private let verticalPadding: CGFloat

    init(
        verticalPadding: CGFloat = LinkSpacing.small,
        action: @escaping () -> Void,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.verticalPadding = verticalPadding
        self.action = action
        self.content = content
    }

    var body: some View {
        Button(action: action) {
            content()
                .frame(maxWidth: .infinity, alignment: .leading)
                .frame(maxHeight: .infinity)
                .scenePadding(.horizontal)
                .padding(.vertical, verticalPadding)
        }
        .buttonStyle(ListRowButtonStyle())
    }
}

private struct ListRowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? Color(.systemGray5) : Color.clear)
    }
}

// MARK: - ListRowContent

/// A non-interactive container that provides the same layout as `ListRowButton`
/// but without a `Button` wrapper or pressed highlight. Use this for rows that
/// contain their own interactive controls (e.g. Toggle / LinkSwitch) where the
/// row itself should not be tappable.
struct ListRowContent<Content: View>: View {
    private let content: () -> Content
    private let verticalPadding: CGFloat

    init(
        verticalPadding: CGFloat = LinkSpacing.small,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.verticalPadding = verticalPadding
        self.content = content
    }

    var body: some View {
        content()
            .frame(maxWidth: .infinity, alignment: .leading)
            .frame(maxHeight: .infinity)
            .scenePadding(.horizontal)
            .padding(.vertical, verticalPadding)
    }
}
