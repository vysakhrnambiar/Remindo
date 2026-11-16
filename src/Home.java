package app.ammu.reminders;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.webkit.*;
import org.json.*;

public class Home extends Activity {
    WebView web;
    String route="";
    boolean ready;
    public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(255,245,237));
        getWindow().setNavigationBarColor(Color.rgb(255,245,237));
        getWindow().getDecorView().setSystemUiVisibility(8192 | 16);
        web=new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setBlockNetworkLoads(true);
        web.setWebViewClient(new WebViewClient(){
            public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}
            public void onPageFinished(WebView v,String url){ready=true;refresh();}
        });
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Api(),"Phone");
        setContentView(web);route=getIntent().getStringExtra("route");
        web.loadUrl("file:///android_asset/index.html");
    }
    public void onNewIntent(Intent i){super.onNewIntent(i);route=i.getStringExtra("route");refresh();}
    public void onResume(){super.onResume();Bell.tick(this);refresh();}
    void refresh(){
        if(ready){web.evaluateJavascript("resume("+JSONObject.quote(route==null?"":route)+")",null);route="";}
    }
    public void onBackPressed(){web.evaluateJavascript("back()",result->{if("false".equals(result))super.onBackPressed();});}
    class Api {
        @JavascriptInterface public String call(String action,String payload){
            synchronized(Bell.class){
                try {
                    JSONObject p=new JSONObject(payload);
                    Store s=Store.get(Home.this);
                    switch(action){
                        case "state":
                            JSONObject state=new JSONObject();
                            state.put("goals",s.rows("SELECT * FROM goals WHERE active=1 ORDER BY next"));
                            state.put("entries",s.rows("SELECT * FROM entries WHERE due<=? ORDER BY due DESC",String.valueOf(System.currentTimeMillis())));
                            state.put("alerts",getSystemService(NotificationManager.class).areNotificationsEnabled());
                            state.put("exact",Bell.exact(Home.this));
                            state.put("weekly",getSharedPreferences("settings",0).getBoolean("weekly",true));
                            return state.toString();
                        case "save": s.save(p);break;
                        case "archive":s.archive(p.getLong("id"));break;
                        case "answer":
                            String result=p.getString("outcome");
                            if(result.isEmpty())throw new IllegalArgumentException("Choose a response.");
                            s.getWritableDatabase().execSQL("UPDATE entries SET outcome=?,note=? WHERE id=?",new Object[]{result,p.getString("note"),p.getLong("id")});
                            dismiss(p);break;
                        case "snooze":
                            s.getWritableDatabase().execSQL("UPDATE entries SET retry=? WHERE id=? AND outcome=''",new Object[]{Store.quietEnd(System.currentTimeMillis()+1800000),p.getLong("id")});
                            dismiss(p);break;
                        case "weekly":getSharedPreferences("settings",0).edit().putBoolean("weekly",p.getBoolean("enabled")).remove("review").apply();break;
                        case "notifications":
                            runOnUiThread(()->{
                                if(Build.VERSION.SDK_INT>=33 && !getSharedPreferences("settings",0).getBoolean("asked",false)){
                                    getSharedPreferences("settings",0).edit().putBoolean("asked",true).apply();
                                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},1);
                                }else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
                            }); return "{}";
                        case "exact": if(Build.VERSION.SDK_INT>=31)runOnUiThread(()->startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName()))));return "{}";
                        default:throw new IllegalArgumentException("Unknown action.");
                    }
                    Bell.tick(Home.this);
                    return "{}";
                }catch(Exception e){return "{\"error\":"+JSONObject.quote(e.getMessage())+"}";}
            }
        }
        void dismiss(JSONObject p) throws JSONException{
            getSystemService(NotificationManager.class).cancel("feedback"+p.getLong("goal"),1);
        }
    }
    public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);refresh();}
}
