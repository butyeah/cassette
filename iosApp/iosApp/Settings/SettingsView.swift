import Shared
import SwiftUI

/// Notifications, language and, while signed in, the account. Mirrors Android's SettingsScreen.
struct SettingsView: View {
    @Environment(AuthModel.self) private var auth
    @Environment(\.openURL) private var openURL

    @State private var isConfirmingSignOut = false
    @State private var isConfirmingDelete = false
    @State private var isDeleting = false
    @State private var deleteFailure: AuthFailure?

    var body: some View {
        List {
            Section {
                NavigationLink(value: ProfileRoute.notifications) {
                    Label("Notifications", systemImage: "bell")
                }
                // iOS has its own per-app language picker, on the app's page in the Settings app.
                Button {
                    if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                } label: {
                    Label {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Language")
                            Text("Follows your phone's language")
                                .font(.handjet(17))
                                .foregroundStyle(.secondary)
                        }
                    } icon: {
                        Image(systemName: "globe")
                    }
                }
                .foregroundStyle(.primary)
            }

            if auth.user != nil {
                Section {
                    Button("Sign out") { isConfirmingSignOut = true }
                    Button("Delete account", role: .destructive) { isConfirmingDelete = true }
                        .disabled(isDeleting)
                        .overlay(alignment: .trailing) {
                            if isDeleting { ProgressView() }
                        }
                }
            }
        }
        .font(.handjet(21))
        .navigationTitle("Settings")
        .confirmationDialog("Sign out?", isPresented: $isConfirmingSignOut, titleVisibility: .visible) {
            Button("Sign out", role: .destructive) { auth.signOut() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("You can sign back in any time.")
        }
        .confirmationDialog("Delete your account?", isPresented: $isConfirmingDelete, titleVisibility: .visible) {
            Button("Delete", role: .destructive) { Task { await deleteAccount() } }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This permanently deletes your account and signs you out. It can't be undone.")
        }
        .alert(
            "Couldn't delete your account",
            isPresented: Binding(get: { deleteFailure != nil }, set: { if !$0 { deleteFailure = nil } })
        ) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(deleteFailure?.message ?? "")
        }
    }

    private func deleteAccount() async {
        isDeleting = true
        do {
            try await auth.deleteAccount()
        } catch {
            deleteFailure = authFailure(of: error)
        }
        isDeleting = false
    }
}
