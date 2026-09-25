import Shared
import SwiftUI

/// Who's signed in, or the form to sign in or create an account. Mirrors Android's Profile tab.
struct ProfileView: View {
    @Environment(AuthModel.self) private var auth

    var body: some View {
        NavigationStack {
            ScrollView {
                if let user = auth.user {
                    SignedInContent(email: user.email ?? "")
                        .padding(16)
                } else {
                    LoginForm(sdk: auth.sdk)
                        .padding(16)
                }
            }
            .scrollDismissesKeyboard(.interactively)
        }
    }
}

/// An avatar with the email's initial, the email, and "Signed in". Mirrors SignedInContent.kt.
private struct SignedInContent: View {
    let email: String

    var body: some View {
        HStack(spacing: 16) {
            Text(email.first.map { String($0).uppercased() } ?? "")
                .font(.pixel(26, weight: .semibold))
                .frame(width: 56, height: 56)
                .background(.tint.opacity(0.2), in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(email).font(.pixel(18, weight: .medium)).lineLimit(1).truncationMode(.middle)
                Text("Signed in").font(.handjet(19)).foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
        .accessibilityElement(children: .combine)
    }
}

private struct LoginForm: View {
    @State private var model: LoginViewModel
    @FocusState private var focusedField: Field?

    private enum Field { case email, password, confirmPassword }

    init(sdk: CassetteSdk) {
        _model = State(initialValue: LoginViewModel(sdk: sdk))
    }

    var body: some View {
        @Bindable var model = model
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 8) {
                Text("Welcome to Cassette").font(.pixel(32, weight: .semibold))
                Text("Sign in to keep your reminders and preferences with you.")
                    .font(.handjet(21))
                    .foregroundStyle(.secondary)
            }

            VStack(spacing: 16) {
                Picker("Mode", selection: $model.mode) {
                    Text("Sign in").tag(LoginViewModel.Mode.signIn)
                    Text("Create account").tag(LoginViewModel.Mode.createAccount)
                }
                .pickerStyle(.segmented)

                LabeledField(hint: model.emailHint) {
                    TextField("Email", text: $model.email)
                        .textContentType(.username)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .focused($focusedField, equals: .email)
                        .submitLabel(.next)
                        .onSubmit { focusedField = .password }
                }

                LabeledField(hint: model.passwordHint) {
                    HStack {
                        PasswordField("Password", text: $model.password, isVisible: model.isPasswordVisible)
                            .textContentType(model.mode == .signIn ? .password : .newPassword)
                            .focused($focusedField, equals: .password)
                            .submitLabel(model.mode == .signIn ? .go : .next)
                            .onSubmit {
                                if model.mode == .signIn { Task { await model.submit() } } else { focusedField = .confirmPassword }
                            }
                        Button { model.isPasswordVisible.toggle() } label: {
                            Image(systemName: model.isPasswordVisible ? "eye.slash" : "eye")
                        }
                        .buttonStyle(.borderless)
                        .accessibilityLabel(model.isPasswordVisible ? "Hide password" : "Show password")
                    }
                }

                if model.mode == .createAccount {
                    LabeledField(hint: model.confirmPasswordHint) {
                        PasswordField("Confirm password", text: $model.confirmPassword, isVisible: model.isPasswordVisible)
                            .textContentType(.newPassword)
                            .focused($focusedField, equals: .confirmPassword)
                            .submitLabel(.go)
                            .onSubmit { Task { await model.submit() } }
                    }
                }

                Button {
                    focusedField = nil
                    Task { await model.submit() }
                } label: {
                    Group {
                        if model.isLoading {
                            ProgressView()
                        } else {
                            Text(model.mode == .signIn ? "Sign in" : "Create account")
                        }
                    }
                    .font(.pixel(17, weight: .medium))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
                }
                .buttonStyle(.borderedProminent)
                .buttonBorderShape(.capsule)
                .disabled(!model.canSubmit)

                if model.mode == .signIn {
                    Button("Forgot password?") {
                        focusedField = nil
                        Task { await model.sendPasswordReset() }
                    }
                    .font(.pixel(15, weight: .medium))
                    .disabled(model.isLoading)
                }

                if let sentTo = model.resetEmailSentTo {
                    Text("We sent a reset link to \(sentTo).")
                        .font(.handjet(19))
                        .multilineTextAlignment(.center)
                }
            }
            .padding(16)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(.secondary.opacity(0.4)))
        }
        .alert(
            model.failure?.message ?? "",
            isPresented: Binding(get: { model.failure != nil }, set: { if !$0 { model.failure = nil } })
        ) {
            Button("OK", role: .cancel) {}
        }
    }
}

/// A text field in a rounded box, with an optional hint under it.
private struct LabeledField<Field: View>: View {
    let hint: String?
    @ViewBuilder let field: Field

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            field
                .font(.handjet(22))
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(.secondary.opacity(0.5)))
            if let hint {
                Text(hint).font(.handjet(17)).foregroundStyle(.red)
            }
        }
    }
}

/// A password field that can show what's typed. Switching between SecureField and TextField
/// keeps the text.
private struct PasswordField: View {
    let title: String
    @Binding var text: String
    let isVisible: Bool

    init(_ title: String, text: Binding<String>, isVisible: Bool) {
        self.title = title
        _text = text
        self.isVisible = isVisible
    }

    var body: some View {
        Group {
            if isVisible {
                TextField(title, text: $text)
            } else {
                SecureField(title, text: $text)
            }
        }
        .textInputAutocapitalization(.never)
        .autocorrectionDisabled()
    }
}
