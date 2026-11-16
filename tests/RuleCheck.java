package app.ammu.reminders;
import java.time.*;
public class RuleCheck {
    static void expect(Rule r,String after,String expected){
        long got=r.next(Instant.parse(after).toEpochMilli(),ZoneOffset.UTC);
        if(got!=Instant.parse(expected).toEpochMilli())throw new AssertionError(Instant.ofEpochMilli(got)+" expected "+expected);
    }
    static Rule simple(String unit,int step,String date){
        return new Rule(unit,step,date,127,"first",127,0,60,"08:00","first",7,"09:00");
    }
    public static void main(String[] args){
        Rule monthly=simple("months",1,"2028-01-31");
        expect(monthly,"2028-01-31T09:00:00Z","2028-02-29T09:00:00Z");
        expect(monthly,"2028-02-29T09:00:00Z","2028-03-31T09:00:00Z");
        expect(simple("months",2,"2026-01-31"),"2026-02-03T09:00:00Z","2026-03-31T09:00:00Z");
        expect(simple("weeks",2,"2026-11-16"),"2026-11-17T09:00:00Z","2026-11-30T09:00:00Z");
        Rule water=new Rule("days",1,"2026-11-16",31,"first",127,4,180,"09:00","first",7,"20:00");
        expect(water,"2026-11-16T09:00:00Z","2026-11-16T12:00:00Z");
        expect(water,"2026-11-20T20:00:00Z","2026-11-23T09:00:00Z");
        Rule weekly=new Rule("weeks",1,"2026-11-16",127,"selected",31,4,60,"09:00","weekday",7,"20:00");
        expect(weekly,"2026-11-16T09:00:00Z","2026-11-16T10:00:00Z");
        expect(weekly,"2026-11-20T12:00:00Z","2026-11-22T20:00:00Z");
        if(!weekly.isFeedback(Instant.parse("2026-11-22T20:00:00Z").toEpochMilli(),ZoneOffset.UTC))throw new AssertionError();
        Rule once=simple("once",1,"2026-12-15");
        expect(once,"2026-12-14T12:00:00Z","2026-12-15T09:00:00Z");
        if(once.next(Instant.parse("2026-12-15T09:00:00Z").toEpochMilli(),ZoneOffset.UTC)!=Long.MAX_VALUE)throw new AssertionError();
        System.out.println("Recurrence checks passed");
    }
}
