package app.ammu.reminders;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;
import java.time.*;

final class Store extends SQLiteOpenHelper {
    private static Store instance;
    static synchronized Store get(Context context) {
        if (instance == null) instance = new Store(context.getApplicationContext());
        return instance;
    }
    private Store(Context c) { super(c,"ammu.db",null,2); }
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) { addCalendar(db); }
    private void addCalendar(SQLiteDatabase db) {
        String[] columns={"weekdays INTEGER DEFAULT 127","nudgeDays TEXT DEFAULT 'first'","nudgeMask INTEGER DEFAULT 127",
            "nudgeCount INTEGER DEFAULT 1","gap INTEGER DEFAULT 60","checkDays TEXT DEFAULT 'first'","checkWeekday INTEGER DEFAULT 7"};
        for(String column:columns)db.execSQL("ALTER TABLE goals ADD COLUMN "+column);
        db.execSQL("UPDATE goals SET feedback=start,checkDays='legacyEach' WHERE mode='each'");
        db.execSQL("UPDATE goals SET nudgeCount=1+(strftime('%s','2000-01-01 '||end)-strftime('%s','2000-01-01 '||start))/(3600*step), gap=60*step,step=1,unit='days' WHERE unit='hours'");
        
        // Include any feedback due before the next old nudge, preserving overdue alarms.
        try(Cursor cursor=db.rawQuery("SELECT * FROM goals WHERE active=1",null)){
            while(cursor.moveToNext()){
                JSONObject row=new JSONObject();
                try {
                    for(int i=0;i<cursor.getColumnCount();i++)row.put(cursor.getColumnName(i),cursor.getString(i));
                    long next=rule(row).next(System.currentTimeMillis()-1,ZoneId.systemDefault());
                    db.execSQL("UPDATE goals SET next=MIN(next,?) WHERE id=?",new Object[]{next,row.getLong("id")});
                }catch(JSONException ex){throw new IllegalStateException(ex);}
            }
        }
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE goals(id INTEGER PRIMARY KEY,title TEXT,unit TEXT,step INTEGER,anchor TEXT,start TEXT,end TEXT,mode TEXT,feedback TEXT,next INTEGER,active INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE entries(id INTEGER PRIMARY KEY,goal INTEGER,title TEXT,mode TEXT,slot TEXT,day TEXT,due INTEGER,retry INTEGER,outcome TEXT DEFAULT '',note TEXT DEFAULT '',UNIQUE(goal,slot))");
        db.execSQL("CREATE INDEX awaiting ON entries(outcome,retry)");
        addCalendar(db);
    }
    JSONArray rows(String sql,String... args) throws JSONException {
        JSONArray rows = new JSONArray();
        try(Cursor c=getReadableDatabase().rawQuery(sql,args)) {
            while(c.moveToNext()) {
                JSONObject row=new JSONObject();
                for(int i=0;i<c.getColumnCount();i++) row.put(c.getColumnName(i),c.getType(i)==Cursor.FIELD_TYPE_INTEGER?(Object)c.getLong(i):c.getString(i));
                rows.put(row);
            }
        }
        return rows;
    }
    static Rule rule(JSONObject g) throws JSONException {
        return new Rule(g.getString("unit"),g.getInt("step"),g.getString("anchor"),g.getInt("weekdays"),
            g.getString("nudgeDays"),g.getInt("nudgeMask"),g.getInt("nudgeCount"),g.getInt("gap"),
            g.getString("start"),g.getString("checkDays"),g.getInt("checkWeekday"),g.getString("feedback"));
    }
    void save(JSONObject g) throws JSONException {
        Rule r=rule(g);
        String name=g.getString("title").trim();
        if(name.isEmpty() || name.length()>120) throw new IllegalArgumentException("Enter a goal of 1 to 120 characters.");
        LocalTime feedback=LocalTime.parse(g.getString("feedback"));
        long now=System.currentTimeMillis();
        long next=r.next(now-1,ZoneId.systemDefault());
        if(r.unit.equals("once") && r.anchor.atTime(feedback).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()<now)
            throw new IllegalArgumentException("Choose a one-off check-in in the future.");
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values=new ContentValues();
            values.put("title",name); values.put("unit",r.unit); values.put("step",r.interval);
            values.put("anchor",r.anchor.toString()); values.put("start",r.start.toString()); values.put("end",r.start.toString());
            values.put("mode","daily"); values.put("feedback",feedback.toString()); values.put("next",next);
            for(String key:new String[]{"weekdays","nudgeMask","nudgeCount","gap","checkWeekday"})values.put(key,g.getInt(key));
            for(String key:new String[]{"nudgeDays","checkDays"})values.put(key,g.getString(key));
            long id=g.optLong("id");
            if(id>0) db.update("goals",values,"id=?",new String[]{String.valueOf(id)});
            else id=db.insertOrThrow("goals",null,values);
            db.delete("entries","goal=? AND due>? AND outcome=''",new String[]{String.valueOf(id),String.valueOf(System.currentTimeMillis())});
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    void archive(long id) {
        getWritableDatabase().execSQL("UPDATE goals SET active=0 WHERE id=?",new Object[]{id});
        getWritableDatabase().execSQL("DELETE FROM entries WHERE goal=? AND due>? AND outcome=''",new Object[]{id,System.currentTimeMillis()});
    }
    static long quietEnd(long time) {
        ZonedDateTime when=Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault());
        int h=when.getHour();
        if(h>=21 || h<8) return when.toLocalDate().plusDays(h>=21?1:0).atTime(8,0).atZone(when.getZone()).toInstant().toEpochMilli();
        return time;
    }
}
