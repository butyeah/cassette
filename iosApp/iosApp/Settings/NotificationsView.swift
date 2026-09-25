import SwiftUI
import UserNotifications

/// Whether the app may notify, and the daily reminder's time. Mirrors Android's
/// NotificationsScreen and DailyReminderSection.
struct NotificationsView: View {
    @Environment(DailyReminder.self) private var reminder
    @Environment(\.openURL) private var openURL
    @Environment(\.scenePhase) private var scenePhase

    @State private var isPickingTime = false
    @State private var pickedTime = Date()

    var body: some View {
        List {
            Section {
                switch reminder.authorization {
                case .notDetermined:
                    Button("Enable notifications") { Task { _ = await reminder.requestPermission() } }
                case .denied:
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Notifications are off")
                        manageInSettings
                    }
                default:
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Notifications enabled")
                        manageInSettings
                    }
                }
            }

            Section {
                Button {
                    pickedTime = reminderDate
                    withAnimation { isPickingTime.toggle() }
                } label: {
                    Text(reminderText)
                }
                .foregroundStyle(.primary)

                if isPickingTime {
                    DatePicker("Time", selection: $pickedTime, displayedComponents: .hourAndMinute)
                        .datePickerStyle(.wheel)
                        .labelsHidden()
                        .frame(maxWidth: .infinity)
                    HStack {
                        if reminder.time != nil {
                            Button("Turn off", role: .destructive) {
                                Task { await reminder.cancel() }
                                isPickingTime = false
                            }
                            .buttonStyle(.borderless)
                        }
                        Spacer()
                        Button("Save") {
                            let components = Calendar.current.dateComponents([.hour, .minute], from: pickedTime)
                            Task { await reminder.schedule(hour: components.hour ?? 9, minute: components.minute ?? 0) }
                            isPickingTime = false
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
            }
        }
        .font(.handjet(21))
        .navigationTitle("Notifications")
        .task { await reminder.refresh() }
        // Coming back from the Settings app, the permission may have changed.
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { Task { await reminder.refresh() } }
        }
    }

    private var manageInSettings: some View {
        Button("Manage in Settings") {
            if let url = URL(string: UIApplication.openNotificationSettingsURLString) { openURL(url) }
        }
        .font(.handjet(19))
        .buttonStyle(.borderless)
    }

    private var reminderText: String {
        guard let time = reminder.time else { return String(localized: "No daily reminder set") }
        return String(format: String(localized: "Daily reminder at %02d:%02d"), time.hour, time.minute)
    }

    /// The picker starts on the current reminder time, or 9:00 when there's none.
    private var reminderDate: Date {
        let time = reminder.time ?? (9, 0)
        return Calendar.current.date(bySettingHour: time.hour, minute: time.minute, second: 0, of: Date()) ?? Date()
    }
}
