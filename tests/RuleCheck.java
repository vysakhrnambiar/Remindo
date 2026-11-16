package app.ammu.reminders;
import java.time.*;
public class RuleCheck {
    static void expect(Rule r,String after,String expected){
        long got=r.next(Instant.parse(after).toEpochMilli(),ZoneOffset.UTC);
        if(got!=Instant.parse(expected).toEpochMilli())throw new AssertionError(Instant.ofEpochMilli(got)+" expected "+expected);
    }
    public static void main(String[] args){
        Rule monthly=new Rule("months",1,"2028-01-31","09:00","18:00");
        expect(monthly,"2028-01-31T09:00:00Z","2028-02-29T09:00:00Z");
        expect(monthly,"2028-02-29T09:00:00Z","2028-03-31T09:00:00Z");
        expect(new Rule("months",2,"2026-01-31","09:00","18:00"),"2026-02-03T09:00:00Z","2026-03-31T09:00:00Z");
        expect(new Rule("weeks",2,"2026-11-16","09:00","18:00"),"2026-11-17T09:00:00Z","2026-11-30T09:00:00Z");
        Rule water=new Rule("hours",3,"2026-11-16","09:00","18:00");
        expect(water,"2026-11-16T09:00:00Z","2026-11-16T12:00:00Z");
        expect(water,"2026-11-16T18:00:00Z","2026-11-17T09:00:00Z");
        System.out.println("Recurrence checks passed");
    }
}
