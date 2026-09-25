import SwiftUI

/// The calendar for jumping to another day. Only the month and day are used; the year the picker
/// shows is just where that day falls next.
struct DayPickerSheet: View {
    let onSelect: (MonthDay) -> Void

    @State private var date: Date
    @Environment(\.dismiss) private var dismiss

    init(day: MonthDay, onSelect: @escaping (MonthDay) -> Void) {
        self.onSelect = onSelect
        _date = State(initialValue: day.date())
    }

    var body: some View {
        NavigationStack {
            DatePicker("Day", selection: $date, displayedComponents: .date)
                .datePickerStyle(.graphical)
                .padding(.horizontal)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { dismiss() }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Done") {
                            onSelect(MonthDay(date: date))
                            dismiss()
                        }
                    }
                }
        }
        .presentationDetents([.medium, .large])
    }
}
