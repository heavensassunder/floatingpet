package com.slime;
import android.content.SharedPreferences;
import android.os.Handler; import android.os.Looper;
import java.io.BufferedReader; import java.io.InputStreamReader;
import java.net.HttpURLConnection; import java.net.URL;
import java.util.ArrayList; import java.util.Collections;
import org.json.JSONArray; import org.json.JSONObject;
// Live list of OpenRouter's free ":free" models, cached on device and rotated per request
// so one model's daily free quota never burns out. A pinned model always wins.
final class FreeModels {
 static final long TTL=12*3600000L;
 static final String KEY_CHAT="freeChat", KEY_VISION="freeVision", KEY_AT="freeAt";
 static final String KEY_IDX="freeIdx", KEY_VIDX="freeVidx", KEY_LAST="lastModel";
 private static volatile boolean refreshing=false;
 private FreeModels(){}
 interface Done{ void done(boolean ok); }

 static ArrayList<String> chat(SharedPreferences sp){ return split(sp.getString(KEY_CHAT,"")); }
 static ArrayList<String> vision(SharedPreferences sp){ return split(sp.getString(KEY_VISION,"")); }
 private static ArrayList<String> split(String s){
  ArrayList<String> r=new ArrayList<String>();
  if(s==null) return r;
  for(String x:s.split("\n")){ x=x.trim(); if(!x.isEmpty()&&!r.contains(x)) r.add(x); }
  return r;
 }
 private static String join(ArrayList<String> a){ StringBuilder sb=new StringBuilder(); for(String x:a){ if(sb.length()>0) sb.append('\n'); sb.append(x); } return sb.toString(); }

 static void maybeRefresh(SharedPreferences sp){
  if(refreshing) return;
  long at=sp.getLong(KEY_AT,0);
  if(System.currentTimeMillis()-at<TTL&&!chat(sp).isEmpty()) return;
  refresh(sp,null);
 }
 static void refresh(final SharedPreferences sp,final Done cb){
  if(refreshing){ if(cb!=null) cb.notify(); return; }
  refreshing=true;
  new Thread(new Runnable(){ public void run(){
   boolean ok=false;
   try{
    HttpURLConnection c=(HttpURLConnection)new URL("https://openrouter.ai/api/v1/models").openConnection();
    c.setConnectTimeout(15000); c.setReadTimeout(25000);
    BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"));
    StringBuilder raw=new StringBuilder(); String l; while((l=br.readLine())!=null) raw.append(l); br.close();
    JSONArray d=new JSONObject(raw.toString()).getJSONArray("data");
    ArrayList<String> free=new ArrayList<String>(), see=new ArrayList<String>();
    for(int i=0;i<d.length();i++){
     JSONObject o=d.getJSONObject(i); String id=o.optString("id","");
     if(!id.endsWith(":free")) continue;
     free.add(id);
     if(takesImages(o)) see.add(id);
    }
    Collections.sort(free); Collections.sort(see);
    sp.edit().putString(KEY_CHAT,join(free)).putString(KEY_VISION,join(see)).putLong(KEY_AT,System.currentTimeMillis()).apply();
    ok=!free.isEmpty();
   }catch(Exception e){ L.w("FreeModels",e); }
   final boolean result=ok;
   refreshing=false;
   if(cb!=null) new Handler(Looper.getMainLooper()).post(new Runnable(){ public void run(){ cb.done(result); }});
  }}).start();
 }
 private static boolean takesImages(JSONObject o){
  try{
   JSONObject arch=o.optJSONObject("architecture");
   if(arch!=null){
    JSONArray in=arch.optJSONArray("input_modalities");
    if(in!=null){ for(int i=0;i<in.length();i++){ if("image".equals(in.optString(i))) return true; } }
    String mod=arch.optString("modality","");
    if(mod.contains("image")) return true;
   }
  }catch(Exception e){ L.w("FreeModels",e); }
  return false;
 }

 // Pinned model wins; otherwise rotate through the cached free list; alias is the last-resort fallback.
 static String nextChat(SharedPreferences sp){
  String pinned=sp.getString("model",OpenRouterClient.MODEL);
  if(pinned!=null&&!pinned.isEmpty()&&!pinned.equals(OpenRouterClient.MODEL)){ sp.edit().putString(KEY_LAST,pinned).apply(); return pinned; }
  ArrayList<String> a=chat(sp);
  if(a.isEmpty()){ sp.edit().putString(KEY_LAST,OpenRouterClient.MODEL).apply(); return OpenRouterClient.MODEL; }
  int i=sp.getInt(KEY_IDX,0); if(i<0||i>=a.size()) i=0;
  String m=a.get(i);
  sp.edit().putInt(KEY_IDX,(i+1)%a.size()).putString(KEY_LAST,m).apply();
  return m;
 }
 static String nextVision(SharedPreferences sp){
  String pinned=sp.getString("visionModel","");
  if(pinned!=null&&!pinned.isEmpty()&&!pinned.equals(Vision.DEFAULT)){ sp.edit().putString(KEY_LAST,pinned).apply(); return pinned; }
  ArrayList<String> a=vision(sp);
  if(a.isEmpty()){ sp.edit().putString(KEY_LAST,Vision.DEFAULT).apply(); return Vision.DEFAULT; }
  int i=sp.getInt(KEY_VIDX,0); if(i<0||i>=a.size()) i=0;
  String m=a.get(i);
  sp.edit().putInt(KEY_VIDX,(i+1)%a.size()).putString(KEY_LAST,m).apply();
  return m;
 }
 static String last(SharedPreferences sp){ return sp.getString(KEY_LAST,OpenRouterClient.MODEL); }
}
