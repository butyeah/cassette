import Foundation
import Observation
import Shared

/// Who's signed in, app-wide, kept current from the shared auth state. Auth calls go through here
/// so every screen sees the change at once.
@MainActor
@Observable
final class AuthModel {
    private(set) var user: AuthUser?

    @ObservationIgnored let sdk: CassetteSdk
    @ObservationIgnored private var subscription: Subscription?

    init(sdk: CassetteSdk) {
        self.sdk = sdk
        user = sdk.currentUser
        subscription = sdk.observeCurrentUser { [weak self] user in
            MainActor.assumeIsolated { self?.user = user }
        }
    }

    deinit {
        subscription?.cancel()
    }

    func signOut() {
        sdk.signOut()
    }

    /// Permanently deletes the signed-in account, which also signs it out. Throws with an
    /// `AuthFailure` (see `authFailure(of:)`), e.g. when the sign-in is too old.
    func deleteAccount() async throws {
        try await sdk.deleteAccount()
    }
}

/// Why a shared auth call failed. Kotlin exceptions reach Swift as an NSError that carries the
/// original under "KotlinException"; anything else is a bug worth only a generic message.
func authFailure(of error: Error) -> AuthFailure {
    ((error as NSError).userInfo["KotlinException"] as? AuthException)?.failure ?? .unknown
}

extension AuthFailure {
    // Kotlin/Native exports the PascalCase entries fully lowercased: InvalidCredentials is
    // .invalidcredentials.

    /// The same wording as Android (app/src/main/res/values/strings.xml, login_error_*).
    var message: String {
        switch self {
        case .invalidcredentials: "Wrong email or password."
        case .emailinuse: "There's already an account with this email. Try signing in."
        case .weakpassword: "Choose a stronger password."
        case .invalidemail: "That email address isn't valid."
        case .userdisabled: "This account has been disabled."
        case .toomanyrequests: "Too many attempts. Please try again in a few minutes."
        case .requiresrecentlogin: "For your security, sign out, sign back in, and then delete your account."
        case .network: "No connection. Check your internet and try again."
        default: "Something went wrong. Please try again."
        }
    }
}
