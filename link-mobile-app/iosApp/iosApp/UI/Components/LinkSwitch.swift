import SwiftUI

/// A styled toggle switch that matches the Link design system.
struct LinkSwitch: View {
    @Binding var isOn: Bool
    var isEnabled: Bool = true

    var body: some View {
        Toggle("", isOn: $isOn)
            .labelsHidden()
            .disabled(!isEnabled)
    }
}
