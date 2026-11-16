package app.ammu.reminders;

import java.time.*;
import java.time.temporal.ChronoUnit;

/** A cycle starts on the task date. Nudge and feedback calendars are independent. */
final class Rule {
    final String unit, nudgeDays, checkDays;
    final int interval, weekdays, nudgeMask, count, gap, checkWeekday;
    final LocalDate anchor;
    final LocalTime start, feedback;
    Rule(String unit,int interval,String anchor,int weekdays,String nudgeDays,int nudgeMask,
         int count,int gap,String start,String checkDays,int checkWeekday,String feedback) {
        this.unit=unit; this.interval=interval; this.anchor=LocalDate.parse(anchor); this.weekdays=weekdays;
        this.nudgeDays=nudgeDays; this.nudgeMask=nudgeMask; this.count=count; this.gap=gap;
        this.start=LocalTime.parse(start); this.checkDays=checkDays; this.checkWeekday=checkWeekday;
        this.feedback=LocalTime.parse(feedback);
        if(interval<1 || interval>365 || weekdays<1 || weekdays>127 || nudgeMask<1 || nudgeMask>127 ||
           checkWeekday<1 || checkWeekday>7) throw new IllegalArgumentException("Choose valid repeat days.");
        if(count<0 || count>144 || gap<1 || gap>21900 || this.start.toSecondOfDay()/60+Math.max(0,count-1)*gap>=1440)
            throw new IllegalArgumentException("The last nudge must fit within the same day.");
        if(unit.equals("days")) {
            boolean reachable=false;
            for(int i=0;i<7;i++) reachable |= selected(this.anchor.plusDays((long)i*interval),weekdays);
            if(!reachable) throw new IllegalArgumentException("The interval never lands on a selected weekday.");
        }
    }
    private boolean selected(LocalDate day,int mask){return (mask & (1<<(day.getDayOfWeek().getValue()-1)))!=0;}
    private long cycleIndex(LocalDate day){
        if(unit.equals("months")) return ChronoUnit.MONTHS.between(YearMonth.from(anchor),YearMonth.from(day))/interval;
        return ChronoUnit.DAYS.between(anchor,day)/(unit.equals("weeks")?7L*interval:interval);
    }
    private LocalDate cycle(long i){
        switch(unit){
            case "once": return anchor;
            case "days": return anchor.plusDays(i*interval);
            case "weeks": return anchor.plusWeeks(i*interval);
            case "months": return anchor.plusMonths(i*interval);
            default: throw new IllegalArgumentException("Choose a repeat unit.");
        }
    }
    private LocalDate last(LocalDate first,long index){
        if(unit.equals("weeks")) return first.plusDays(6);
        if(unit.equals("months")) return anchor.plusMonths(index*interval+1).minusDays(1);
        return first;
    }
    private boolean checkDay(LocalDate day,LocalDate first,LocalDate last){
        if(checkDays.equals("end"))return day.equals(last);
        if(checkDays.equals("weekday")) return day.equals(first.plusDays((checkWeekday-first.getDayOfWeek().getValue()+7)%7));
        return day.equals(first);
    }
    private long at(LocalDate day,LocalTime time,ZoneId zone){return day.atTime(time).atZone(zone).toInstant().toEpochMilli();}
    boolean isFeedback(long event,ZoneId zone){
        if(checkDays.equals("legacyEach"))return true;
        LocalDate d=Instant.ofEpochMilli(event).atZone(zone).toLocalDate();
        long index=cycleIndex(d);
        if(unit.equals("months") && cycle(index).isAfter(d))index--;
        LocalDate first=cycle(index);
        return checkDay(d,first,last(first,index)) && event==at(d,feedback,zone);
    }
    long next(long after,ZoneId zone){
        LocalDate day=Instant.ofEpochMilli(after).atZone(zone).toLocalDate();
        if(day.isBefore(anchor))day=anchor;
        long index=unit.equals("once")?0:cycleIndex(day);
        // A monthly cycle may run across the first of the next calendar month.
        if(unit.equals("months") && index>0 && cycle(index).isAfter(day))index--;
        for(;;index++){
            LocalDate first=cycle(index), last=last(first,index);
            if(!unit.equals("days") || selected(first,weekdays)){
                for(LocalDate d=first.isBefore(day)?day:first;!d.isAfter(last);d=d.plusDays(1)){
                    long earliest=Long.MAX_VALUE;
                    if(checkDay(d,first,last)){
                        long time=at(d,feedback,zone);
                        if(time>after)earliest=time;
                    }
                    boolean nudge=nudgeDays.equals("selected")?selected(d,nudgeMask):d.equals(first);
                    if(nudge)for(int i=0;i<count;i++){
                        long time=at(d,start.plusMinutes(i*gap),zone);
                        if(time>after)earliest=Math.min(time,earliest);
                    }
                    if(earliest<Long.MAX_VALUE)return earliest;
                }
            }
            if(unit.equals("once"))return Long.MAX_VALUE;
        }
    }
}
