import Foundation

/// A day of the year with no year, the unit the Daily screen browses by.
struct MonthDay: Hashable {
    let month: Int
    let day: Int

    static func today(calendar: Calendar = .current) -> MonthDay {
        MonthDay(date: Date(), calendar: calendar)
    }

    init(month: Int, day: Int) {
        self.month = month
        self.day = day
    }

    init(date: Date, calendar: Calendar = .current) {
        let components = calendar.dateComponents([.month, .day], from: date)
        self.init(month: components.month!, day: components.day!)
    }

    /// This day in the nearest year from now that has it, so February 29 lands on a leap year
    /// instead of rolling over to March 1 (as Android's MonthDayUtcMillis.kt does).
    func date(calendar: Calendar = .current) -> Date {
        var year = calendar.component(.year, from: Date())
        while true {
            let components = DateComponents(year: year, month: month, day: day)
            if components.isValidDate(in: calendar), let date = calendar.date(from: components) {
                return date
            }
            year += 1
        }
    }

    /// "June 17", "17 de junio": the way the user's language writes a month and day.
    var formatted: String {
        let formatter = DateFormatter()
        formatter.setLocalizedDateFormatFromTemplate("MMMMd")
        return formatter.string(from: date())
    }
}
