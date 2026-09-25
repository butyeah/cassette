import Foundation
import Observation
import UserNotifications

/// The one reminder request, replaced whenever its time changes.
private let reminderIdentifier = "daily-reminder"

/// The daily reminder notification: when it's set for, whether the app may notify at all, and
/// what happens when it's tapped. The iOS side of Android's DailyReminderWorker and
/// NotificationScheduleRepositoryImpl.
///
/// The scheduled time is read back from the pending notification itself, so there's no separate
/// setting that could disagree with what's actually scheduled.
@MainActor
@Observable
final class DailyReminder: NSObject {
    /// Hour and minute the reminder fires at, or `nil` when none is set.
    private(set) var time: (hour: Int, minute: Int)?
    private(set) var authorization = UNAuthorizationStatus.notDetermined
    /// Goes up by one each time the reminder is tapped, so the Daily screen can jump to today.
    private(set) var openTodayRequests = 0

    @ObservationIgnored private let center = UNUserNotificationCenter.current()

    override init() {
        super.init()
        center.delegate = self
    }

    /// Re-reads the permission and the scheduled reminder, which can change outside the app
    /// (the Settings app, or iOS clearing notifications).
    func refresh() async {
        authorization = await center.notificationSettings().authorizationStatus
        let pending = await center.pendingNotificationRequests()
        let trigger = pending.first { $0.identifier == reminderIdentifier }?.trigger as? UNCalendarNotificationTrigger
        if let hour = trigger?.dateComponents.hour, let minute = trigger?.dateComponents.minute {
            time = (hour, minute)
        } else {
            time = nil
        }
    }

    /// Asks for permission if it hasn't been asked yet; `false` when notifications aren't allowed.
    func requestPermission() async -> Bool {
        if authorization == .notDetermined {
            _ = try? await center.requestAuthorization(options: [.alert, .sound, .badge])
        }
        await refresh()
        return authorization == .authorized || authorization == .provisional
    }

    /// Fires every day at `hour`:`minute`, replacing any earlier time. Asks for permission first;
    /// without it nothing is scheduled.
    func schedule(hour: Int, minute: Int) async {
        guard await requestPermission() else { return }
        let content = UNMutableNotificationContent()
        content.title = String(localized: "One day like today")
        content.body = String(localized: "Today's albums released in history are ready — take a look!")
        content.sound = .default
        let trigger = UNCalendarNotificationTrigger(
            dateMatching: DateComponents(hour: hour, minute: minute),
            repeats: true
        )
        try? await center.add(UNNotificationRequest(identifier: reminderIdentifier, content: content, trigger: trigger))
        await refresh()
    }

    func cancel() async {
        center.removePendingNotificationRequests(withIdentifiers: [reminderIdentifier])
        await refresh()
    }
}

extension DailyReminder: UNUserNotificationCenterDelegate {
    /// Shown as a banner even while the app is open.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }

    /// Tapping the reminder opens today's albums.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        guard response.notification.request.identifier == reminderIdentifier else { return }
        await MainActor.run { openTodayRequests += 1 }
    }
}
