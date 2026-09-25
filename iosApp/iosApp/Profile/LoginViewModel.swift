import Foundation
import Observation
import Shared

/// The sign-in / create-account form. Mirrors Android's LoginViewModel and LoginValidation, minus
/// Google sign-in, which the iOS app doesn't offer.
@MainActor
@Observable
final class LoginViewModel {

    enum Mode: Hashable { case signIn, createAccount }

    var mode = Mode.signIn {
        didSet {
            // A fresh form per mode: a sign-in password shouldn't carry into a new account.
            guard mode != oldValue else { return }
            password = ""
            confirmPassword = ""
            failure = nil
            resetEmailSentTo = nil
            needsEmailForReset = false
        }
    }
    var email = "" {
        didSet { failure = nil; resetEmailSentTo = nil; needsEmailForReset = false }
    }
    var password = "" {
        didSet { failure = nil }
    }
    var confirmPassword = "" {
        didSet { failure = nil }
    }
    var isPasswordVisible = false

    private(set) var isLoading = false
    /// Why the last sign-in, sign-up or reset failed, until the form changes.
    var failure: AuthFailure?
    /// Where a password reset link was just sent, to confirm it.
    private(set) var resetEmailSentTo: String?
    /// "Forgot password?" was tapped without a usable email to send the link to.
    private(set) var needsEmailForReset = false

    private let sdk: CassetteSdk

    init(sdk: CassetteSdk) {
        self.sdk = sdk
    }

    /// Whether the form is complete enough to send: signing in only needs a password at all.
    var canSubmit: Bool {
        guard !isLoading, LoginRulesKt.isPlausibleEmail(email: email) else { return false }
        switch mode {
        case .signIn: return !password.isEmpty
        case .createAccount: return LoginRulesKt.isValidPassword(password: password) && confirmPassword == password
        }
    }

    /// A hint under the email field, once something's been typed or a reset asked for it.
    var emailHint: String? {
        if !email.trimmingCharacters(in: .whitespaces).isEmpty && !LoginRulesKt.isPlausibleEmail(email: email) {
            return "Enter a valid email address."
        }
        return needsEmailForReset ? "Enter your email and we'll send you a reset link." : nil
    }

    var passwordHint: String? {
        guard mode == .createAccount, !password.isEmpty, !LoginRulesKt.isValidPassword(password: password) else { return nil }
        return "Use at least \(LoginRulesKt.MIN_PASSWORD_LENGTH) characters."
    }

    var confirmPasswordHint: String? {
        !confirmPassword.isEmpty && confirmPassword != password ? "The passwords don't match." : nil
    }

    func submit() async {
        guard canSubmit else { return }
        let email = email.trimmingCharacters(in: .whitespaces)
        let password = password
        await run {
            switch self.mode {
            case .signIn: try await self.sdk.signIn(email: email, password: password)
            case .createAccount: try await self.sdk.signUp(email: email, password: password)
            }
            self.password = ""
            self.confirmPassword = ""
        }
    }

    func sendPasswordReset() async {
        let email = email.trimmingCharacters(in: .whitespaces)
        guard LoginRulesKt.isPlausibleEmail(email: email) else {
            needsEmailForReset = true
            return
        }
        await run {
            try await self.sdk.sendPasswordReset(email: email)
            self.resetEmailSentTo = email
        }
    }

    private func run(_ action: () async throws -> Void) async {
        isLoading = true
        failure = nil
        resetEmailSentTo = nil
        do {
            try await action()
        } catch {
            failure = authFailure(of: error)
        }
        isLoading = false
    }
}
