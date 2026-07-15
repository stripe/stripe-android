import SwiftUI
import XCTest
@testable import iosApp

final class ListGroupCardSnapshotTests: XCTestCase {

    func testListGroupCardWithRegularRows() {
        struct TestItem: Identifiable, Equatable {
            let id: String
            let title: String
        }

        let items = [
            TestItem(id: "1", title: "Row One"),
            TestItem(id: "2", title: "Row Two"),
            TestItem(id: "3", title: "Row Three"),
        ]

        let view = ListGroupCard(
            items: items,
            itemContent: { item in
                Text(item.title)
            },
            onItemClick: { _ in }
        )
        .frame(width: 375)

        assertComponentSnapshots(of: view, named: "ListGroupCard_RegularRows")
    }

    func testListGroupCardWithToggleRow() {
        struct ToggleItem: Identifiable, Equatable {
            let id: String
            let title: String
            var isOn: Bool
        }

        let items = [
            ToggleItem(id: "a", title: "Notifications", isOn: true),
            ToggleItem(id: "b", title: "Dark Mode", isOn: false),
        ]

        let view = ListGroupCard(
            items: items,
            itemContent: { item in
                HStack {
                    Text(item.title)
                    Spacer()
                    Toggle("", isOn: .constant(item.isOn))
                        .labelsHidden()
                }
            },
            onItemClick: { _ in nil }
        )
        .frame(width: 375)

        assertComponentSnapshots(of: view, named: "ListGroupCard_WithToggleRow")
    }
}
