package com.slime;
import android.os.Handler; import android.os.Looper;
import java.io.BufferedReader; import java.io.InputStreamReader; import java.net.HttpURLConnection; import java.net.URL; import java.util.ArrayList; import java.util.Collections;
import org.json.JSONArray; import org.json.JSONObject;
public class Models {
 static void list(final MainService s){
  s.local("looking up free models...",false);
  new Thread(new Runnable(){ public void run(){
   String res;
   try{
    HttpURLConnection c=(HttpURLConnection)new URL("https://openrouter.ai/api/v1/models").openConnection();
    c.setConnectTimeout(15000); c.setReadTimeout(25000);
    BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"));
    StringBuilder raw=new StringBuilder(); String l; while((l=br.readLine())!=null) raw.append(l); br.close();
    JSONArray d=new JSONObject(raw.toString()).getJSONArray("data");
    ArrayList<String> free=new ArrayList<String>();
    for(int i=0;i<d.length();i++){ String id=d.getJSONObject(i).optString("id",""); if(id.endsWith(":free")) free.add(id); }
    Collections.sort(free);
    StringBuilder sb=new StringBuilder("free models right now ("+free.size()+"):\n");
    for(int i=0;i<Math.min(25,free.size());i++) sb.append(free.get(i)).append("\n");
    sb.append("\npin one with: model NAME\nthe default (openrouter/free) picks a free model for you each time.");
    res=sb.toString();
   }catch(Exception e){ res="couldn't fetch the list: "+e.getMessage(); }
   final String out=res;
   new Handler(Looper.getMainLooper()).post(new Runnable(){ public void run(){ s.showChat(); s.local(out,false); }});
  }}).start();
 }
}
