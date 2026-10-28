package com.slime;
import android.content.SharedPreferences;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
// User-defined if-this-then-that rules. Triggers come from EyeService (foreground app changes)
// and DeviceEvents (charging, headphones, unlock); actions run through the existing command
// handlers silently, exactly as if the owner had typed them.
class Routines {
 static final String STORE="routines", SEQ="routineSeq";
 static final HashMap<Integer,Long> LAST_FIRED=new HashMap<>();
 static String lastPkg=""; static long lastPkgAt=0;

 static JSONArray all(SharedPreferences sp){
  try{ return new JSONArray(sp.getString(STORE,"[]")); }catch(Exception e){ return new JSONArray(); }
 }
 static void save(SharedPreferences sp,JSONArray a){ sp.edit().putString(STORE,a.toString()).apply(); }

 static boolean handle(MainService s,String t,String low){
  low=low.trim();
  if(low.equals("routines")||low.equals("list routines")){ list(s); return true; }
  if(low.startsWith("forget routine ")||low.startsWith("remove routine ")||low.startsWith("routine ")){
   String n=low.replaceAll(".*routine\\s*","").replaceAll("[^0-9]","");
   if(n.isEmpty()){ s.local("say e.g. 'forget routine 2' - see 'routines' for the numbers",false); return true; }
   remove(s,Integer.parseInt(n)); return true;
  }
  if(!low.startsWith("when ")) return false;
  int then=low.indexOf(" then ");
  if(then<0){ usage(s); return true; }
  String trig=low.substring(5,then).trim();
  String action=t.substring(low.indexOf(" then ")+6).trim();
  if(action.isEmpty()){ usage(s); return true; }
  String trigger=null, pkg=null, label=null;
  if(trig.contains("open")||trig.contains("launch")){
   String name=trig.replaceFirst("^i\\s+","").replaceAll("\\b(i|open|launch|app)\\b","").trim();
   pkg=s.phone==null?null:s.phone.findPkg(name);
   if(pkg==null){ s.local("I can't find an app called \""+name+"\" - check the name from the launcher, e.g. 'when I open tiktok then shh'",false); return true; }
   trigger="app"; label=name;
  } else if(trig.contains("unplug")) trigger="unplug";
  else if(trig.contains("charg")||trig.contains("plug in")||trig.contains("plugged in")) trigger="charge";
  else if(trig.contains("headphone")||trig.contains("headset")) trigger=trig.contains("disconnect")||trig.contains("unplug")?"headoff":"headon";
  else if(trig.contains("unlock")) trigger="unlock";
  if(trigger==null){ usage(s); return true; }
  int id=s.sp.getInt(SEQ,0)+1; s.sp.edit().putInt(SEQ,id).apply();
  JSONObject o=new JSONObject();
  try{ o.put("id",id); o.put("trigger",trigger); o.put("action",action); if(pkg!=null){ o.put("pkg",pkg); o.put("name",label); } }catch(Exception e){ L.w("Routines",e); }
  JSONArray a=all(s.sp); a.put(o); save(s.sp,a);
  s.local("routine "+id+" saved: "+describe(o),false);
  return true;
 }
 static void usage(MainService s){
  s.local("routine examples:\n- when I open tiktok then shh\n- when charging then brightness 20\n- when headphones connect then tts off\n- when I unlock then say good morning\nsee them with 'routines', remove with 'forget routine N'",false);
 }
 static void list(MainService s){
  JSONArray a=all(s.sp);
  if(a.length()==0){ s.local("no routines yet. Try 'when I open tiktok then shh' - say 'routines' anytime to see them.",false); return; }
  StringBuilder sb=new StringBuilder("Routines:\n");
  for(int i=0;i<a.length();i++){ JSONObject o=a.optJSONObject(i); if(o!=null) sb.append(o.optInt("id")).append(". ").append(describe(o)).append('\n'); }
  sb.append("remove one with 'forget routine N'");
  s.showChat(); s.local(sb.toString().trim(),false);
 }
 static void remove(MainService s,int id){
  JSONArray a=all(s.sp), kept=new JSONArray(); boolean found=false;
  for(int i=0;i<a.length();i++){ JSONObject o=a.optJSONObject(i); if(o!=null&&o.optInt("id")==id) found=true; else kept.put(o); }
  save(s.sp,kept); LAST_FIRED.remove(id);
  s.local(found?"routine "+id+" removed":"there's no routine "+id+" - say 'routines' to see them",false);
 }
 static String describe(JSONObject o){
  String tr=o.optString("trigger");
  if("app".equals(tr)) tr="I open "+o.optString("name",o.optString("pkg"));
  else if("charge".equals(tr)) tr="charging starts";
  else if("unplug".equals(tr)) tr="phone is unplugged";
  else if("headon".equals(tr)) tr="headphones connect";
  else if("headoff".equals(tr)) tr="headphones disconnect";
  else if("unlock".equals(tr)) tr="I unlock the phone";
  return "when "+tr+" -> "+o.optString("action");
 }

 // ---------- trigger entry points ----------
 static void onApp(MainService s,String pkg){
  if(pkg==null||pkg.startsWith("com.slime")) return;
  long now=System.currentTimeMillis();
  if(pkg.equals(lastPkg)&&now-lastPkgAt<5000) return; // window events fire repeatedly per app
  lastPkg=pkg; lastPkgAt=now;
  fire(s,"app",pkg);
 }
 static void fire(MainService s,String trigger,String pkg){
  try{
   JSONArray a=all(s.sp); long now=System.currentTimeMillis();
   for(int i=0;i<a.length();i++){
    JSONObject o=a.optJSONObject(i); if(o==null||!trigger.equals(o.optString("trigger"))) continue;
    if("app".equals(trigger)&&!o.optString("pkg").equals(pkg)) continue;
    int id=o.optInt("id"); Long lf=LAST_FIRED.get(id);
    if(lf!=null&&now-lf<30000) continue; // one run per rule per 30 s
    LAST_FIRED.put(id,now);
    String action=o.optString("action");
    if(runAction(s,action)) Evo.award(s,"task",2,2);
    else s.local("routine "+id+" couldn't run: '"+action+"' isn't a command I know",false);
   }
  }catch(Exception e){ L.w("Routines",e); }
 }
 // run a stored action through the same handlers as typed chat, without adding a user bubble
 static boolean runAction(MainService s,String action){
  String low=action.toLowerCase().trim();
  if(low.startsWith("say ")||low.startsWith("speak ")){
   String txt=action.substring(low.startsWith("say ")?4:6).trim();
   if(!txt.isEmpty()){ s.speak(txt); return true; }
  }
  if(s.localCommand(low)) return true;
  if(s.phone!=null&&s.phone.run(action,low,false)) return true;
  if(Modes.handle(s,action,low)) return true;
  if(s.voice!=null&&s.voice.handle(action,low)) return true;
  if(s.vibe!=null&&s.vibe.handle(action,low)) return true;
  return false;
 }
}
