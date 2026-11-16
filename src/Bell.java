package app.ammu.reminders;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.time.*;

public class Bell extends android.content.BroadcastReceiver {
    public void onReceive(Context c,Intent i) {
        synchronized(Bell.class) {
            if(Intent.ACTION_TIMEZONE_CHANGED.equals(i.getAction())) {
                try {
                    Store s=Store.get(c);
                    JSONArray gs=s.rows("SELECT * FROM goals WHERE active=1");
                    for(int k=0;k<gs.length();k++){
                        JSONObject g=gs.getJSONObject(k);
                        s.getWritableDatabase().execSQL("UPDATE goals SET next=? WHERE id=?",new Object[]{Store.rule(g).next(System.currentTimeMillis(),ZoneId.systemDefault()),g.getLong("id")});
                    }
                } catch(JSONException e){throw new IllegalStateException(e);}
            }
            tick(c);
        }
    }
    static boolean exact(Context c){return Build.VERSION.SDK_INT<31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms();}
    static synchronized void tick(Context c) {
        long now=System.currentTimeMillis();
        ZoneId zone=ZoneId.systemDefault();
        Store s=Store.get(c);
        SQLiteDatabase db=s.getWritableDatabase();
        long wake=Long.MAX_VALUE;
        try {
            JSONArray goals=s.rows("SELECT * FROM goals WHERE active=1");
            for(int i=0;i<goals.length();i++){
                JSONObject g=goals.getJSONObject(i);
                long due=g.getLong("next"), id=g.getLong("id");
                boolean daily=g.getString("mode").equals("daily");
                long recent=0;
                db.beginTransaction();
                try {
                    Rule rule=Store.rule(g);
                    int count=0;
                    while(due<=now && count++<500){
                        LocalDate day=Instant.ofEpochMilli(due).atZone(zone).toLocalDate();
                        long check=daily?day.atTime(LocalTime.parse(g.getString("feedback"))).atZone(zone).toInstant().toEpochMilli():due;
                        db.execSQL("INSERT OR IGNORE INTO entries(goal,title,mode,slot,day,due,retry) VALUES(?,?,?,?,?,?,?)",
                            new Object[]{id,g.getString("title"),g.getString("mode"),daily?day.toString():String.valueOf(due),day.toString(),check,check});
                        recent=due;
                        due=rule.next(due,zone);
                    }
                    db.execSQL("UPDATE goals SET next=? WHERE id=?",new Object[]{due,id});
                    db.setTransactionSuccessful();
                } finally {db.endTransaction();}
                if(daily && recent>now-5*60000 && recent!=0) post(c,"nudge"+id,g.getString("title"),"Your scheduled nudge","home");
                wake=Math.min(wake,due);
            }
            JSONArray pending=s.rows("SELECT e.* FROM entries e JOIN goals g ON g.id=e.goal WHERE outcome='' AND g.active=1 AND retry<=? ORDER BY due DESC",String.valueOf(now));
            java.util.HashSet<Long> notified=new java.util.HashSet<>();
            for(int i=0;i<pending.length();i++){
                JSONObject e=pending.getJSONObject(i);
                long goal=e.getLong("goal");
                boolean first=e.getLong("due")==e.getLong("retry");
                if((first || Store.quietEnd(now)==now) && notified.add(goal)) {
                    c.getSystemService(NotificationManager.class).cancel("nudge"+goal,1);
                    post(c,"feedback"+goal,e.getString("title"),"How did it go? Tap to check in.","entry:"+e.getLong("id"));
                }
                db.execSQL("UPDATE entries SET retry=? WHERE id=?",new Object[]{Store.quietEnd(now+3600000),e.getLong("id")});
            }
            JSONObject min=s.rows("SELECT MIN(retry) AS t FROM entries JOIN goals ON goals.id=entries.goal WHERE active=1 AND outcome=''").getJSONObject(0);
            wake=Math.min(wake,min.optLong("t",Long.MAX_VALUE));
            SharedPreferences p=c.getSharedPreferences("settings",0);
            if(p.getBoolean("weekly",true)){
                long review=p.getLong("review",0);
                if(review<=now){
                    if(review>0)post(c,"review","Your weekly review","See your check-ins and comments","history");
                    ZonedDateTime local=Instant.ofEpochMilli(now).atZone(zone);
                    ZonedDateTime sunday=local.toLocalDate().with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(18,0).atZone(zone);
                    if(!sunday.isAfter(local))sunday=sunday.plusWeeks(1);
                    review=sunday.toInstant().toEpochMilli();p.edit().putLong("review",review).apply();
                }
                wake=Math.min(wake,review);
            }
        } catch(JSONException ex){throw new IllegalStateException(ex);}
        AlarmManager am=c.getSystemService(AlarmManager.class);
        PendingIntent alarm=PendingIntent.getBroadcast(c,1,new Intent(c,Bell.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        if(wake==Long.MAX_VALUE){am.cancel(alarm);return;}
        wake=Math.max(wake,now+1000);
        if(exact(c))try{am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,wake,alarm);return;}catch(SecurityException ignored){}
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,wake,alarm);
    }
    static void post(Context c,String tag,String title,String body,String route){
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("reminders","Reminders and check-ins",NotificationManager.IMPORTANCE_HIGH));
        if(!nm.areNotificationsEnabled())return;
        Intent intent=new Intent(c,Home.class).setData(android.net.Uri.parse("ammu://open/"+route)).putExtra("route",route)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent open=PendingIntent.getActivity(c,0,intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        nm.notify(tag,1,new Notification.Builder(c,"reminders").setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title).setContentText(body).setContentIntent(open).setAutoCancel(true).build());
    }
}
