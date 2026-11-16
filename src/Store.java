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
    private Store(Context c) { super(c,"ammu.db",null,1); }
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {}
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE goals(id INTEGER PRIMARY KEY,title TEXT,unit TEXT,step INTEGER,anchor TEXT,start TEXT,end TEXT,mode TEXT,feedback TEXT,next INTEGER,active INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE entries(id INTEGER PRIMARY KEY,goal INTEGER,title TEXT,mode TEXT,slot TEXT,day TEXT,due INTEGER,retry INTEGER,outcome TEXT DEFAULT '',note TEXT DEFAULT '',UNIQUE(goal,slot))");
        db.execSQL("CREATE INDEX awaiting ON entries(outcome,retry)");
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
        return new Rule(g.getString("unit"),g.getInt("step"),g.getString("anchor"),g.getString("start"),g.getString("end"));
    }
    void save(JSONObject g) throws JSONException {
        Rule r=rule(g);
        String name=g.getString("title").trim();
        if(name.isEmpty() || name.length()>120) throw new IllegalArgumentException("Enter a goal of 1 to 120 characters.");
        LocalTime feedback=LocalTime.parse(g.getString("feedback"));
        if(g.getString("mode").equals("daily") && feedback.isBefore(r.unit.equals("hours")?r.end:r.start))
            throw new IllegalArgumentException("Choose a check-in at or after the last nudge.");
        long next=r.next(System.currentTimeMillis()-1,ZoneId.systemDefault());
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values=new ContentValues();
            values.put("title",name); values.put("unit",r.unit); values.put("step",r.interval);
            values.put("anchor",r.anchor.toString()); values.put("start",r.start.toString()); values.put("end",r.end.toString());
            values.put("mode",g.getString("mode")); values.put("feedback",feedback.toString()); values.put("next",next);
            long id=g.optLong("id");
            if(id>0) db.update("goals",values,"id=?",new String[]{String.valueOf(id)});
            else id=db.insertOrThrow("goals",null,values);
            db.delete("entries","goal=? AND due>? AND outcome=''",new String[]{String.valueOf(id),String.valueOf(System.currentTimeMillis())});
            ZoneId zone=ZoneId.systemDefault();
            LocalDate today=LocalDate.now(zone);
            long first=r.next(today.atStartOfDay(zone).toInstant().toEpochMilli()-1,zone);
            long check=today.atTime(feedback).atZone(zone).toInstant().toEpochMilli();
            if(g.getString("mode").equals("daily") && first<=System.currentTimeMillis() && check>=System.currentTimeMillis())
                db.execSQL("INSERT OR IGNORE INTO entries(goal,title,mode,slot,day,due,retry) VALUES(?,?,?,?,?,?,?)",
                    new Object[]{id,name,"daily",today.toString(),today.toString(),check,check});
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
