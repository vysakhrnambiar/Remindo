package app.ammu.reminders;

import java.time.*;
import java.time.temporal.ChronoUnit;

final class Rule {
    final String unit;
    final int interval;
    final LocalDate anchor;
    final LocalTime start, end;

    Rule(String unit, int interval, String anchor, String start, String end) {
        this.unit = unit;
        this.interval = interval;
        this.anchor = LocalDate.parse(anchor);
        this.start = LocalTime.parse(start);
        this.end = LocalTime.parse(end);
        if (interval < 1 || interval > 365 || (unit.equals("hours") && this.end.isBefore(this.start)))
            throw new IllegalArgumentException("Check the interval and end time.");
    }

    long next(long after, ZoneId zone) {
        LocalDate day = Instant.ofEpochMilli(after).atZone(zone).toLocalDate();
        if (day.isBefore(anchor)) day = anchor;
        if (unit.equals("hours")) {
            for (int offset = 0; offset < 2; offset++) {
                for (int minute = start.toSecondOfDay()/60; minute <= end.toSecondOfDay()/60; minute += 60*interval) {
                    long time = day.plusDays(offset).atTime(LocalTime.of(minute/60, minute%60)).atZone(zone).toInstant().toEpochMilli();
                    if (time > after) return time;
                }
            }
        }
        long index;
        if (unit.equals("months")) index = ChronoUnit.MONTHS.between(YearMonth.from(anchor), YearMonth.from(day))/interval;
        else index = ChronoUnit.DAYS.between(anchor, day)/(unit.equals("weeks") ? 7L*interval : interval);
        for (;;index++) {
            LocalDate date;
            switch (unit) {
                case "days": date = anchor.plusDays(index*interval); break;
                case "weeks": date = anchor.plusWeeks(index*interval); break;
                case "months": date = anchor.plusMonths(index*interval); break;
                default: throw new IllegalArgumentException("Choose a repeat unit.");
            }
            long time = date.atTime(start).atZone(zone).toInstant().toEpochMilli();
            if (time > after) return time;
        }
    }
}
