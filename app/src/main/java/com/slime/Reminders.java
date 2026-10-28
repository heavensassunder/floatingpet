package com.slime;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
// Time-based reminders: "remind me in 20 minutes to call mum", "remind me tomorrow 7am charge power bank".
// Stored as JSON in prefs so they re-arm after a reboot (MainService starts on boot and calls armAll).
class Reminders {
 static final String ACTION="com.slime.REMINDER";
 static final String STORE="reminders", SEQ="reminderSeq";
 static final Pattern REL=Pattern.compile("(\\d+)\\s*(seconds?|secs?|minutes?|mins?|m|hours?|hrs?|h|days?|d)\\b");
 static final Pattern CLOCK=Pattern.compile("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b");

 static JSONArray all(SharedPreferences sp){
  try{ return new JSONArray(sp.getString(STORE,"[]")); }catch(Exception e){ return new JSONArray(); }
 }
 static void save(SharedPreferences sp,JSONArray a){ sp.edit().putString(STORE,a.toString()).apply(); }

 static boolean handle(MainService s,String t,String low){
  low=low.trim();
  if(low.equals("reminders")||low.equals("list reminders")||low.equals("reminder list")){ list(s); return true; }
  if(low.startsWith("forget reminder ")||low.startsWith("remove reminder ")||low.startsWith("reminder ")){
   String n=low.replaceAll(".*reminder\\s*","").replaceAll("[^0-9]","");
   if(n.isEmpty()){ s.local("say e.g. 'forget reminder 2' - see 'reminders' for the numbers",false); return true; }
   remove(s,Integer.parseInt(n)); return true;
  }
  if(!low.startsWith("remind")) return false;
  String body=low.replaceFirst("^remind( me)?( to)?\\s*","").trim();
  long at=parseAt(body);
  String text=extractText(t);
  if(at<=0){ s.local("when? e.g. 'remind me in 20 minutes to call mum' or 'remind me tomorrow 7am to charge my power bank'",false); return true; }
  if(text.isEmpty()){ s.local("what should I remind you about? e.g. 'remind me in 20 minutes to stretch'",false); return true; }
  int id=s.sp.getInt(SEQ,0)+1; s.sp.edit().putInt(SEQ,id).apply();
  JSONObject o=new JSONObject();
  try{ o.put("id",id); o.put("at",at); o.put("text",text); }catch(Exception e){ L.w("Reminders",e); }
  JSONArray a=all(s.sp); a.put(o); save(s.sp,a);
  schedule(s,id,at);
  s.local("ok, reminder "+id+" set for "+when(at)+": "+text,false);
  return true;
 }

 static void list(MainService s){
  JSONArray a=all(s.sp);
  if(a.length()==0){ s.local("no reminders set. Try 'remind me in 20 minutes to call mum'",false); return; }
  StringBuilder sb=new StringBuilder("Reminders:\n");
  long now=System.currentTimeMillis();
  for(int i=0;i<a.length();i++){
   JSONObject o=a.optJSONObject(i); if(o==null) continue;
   sb.append(o.optInt("id")).append(". ").append(o.optString("text")).append(" - ").append(when(o.optLong("at")));
   if(o.optLong("at")<now) sb.append(" (due!)");
   sb.append("\n");
  }
  sb.append("remove one with 'forget reminder N'");
  s.showChat(); s.local(sb.toString().trim(),false);
 }
 static void remove(MainService s,int id){
  JSONArray a=all(s.sp), kept=new JSONArray(); boolean found=false;
  for(int i=0;i<a.length();i++){ JSONObject o=a.optJSONObject(i); if(o!=null&&o.optInt("id")==id){ found=true; cancel(s,id); } else kept.put(o); }
  save(s.sp,kept);
  s.local(found?"reminder "+id+" removed":"there's no reminder "+id+" - say 'reminders' to see them",false);
 }

 // ---------- time parsing ----------
 static long parseAt(String low){
  long now=System.currentTimeMillis(); long rel=0;
  Matcher m=REL.matcher(low);
  while(m.find()){
   int n=Integer.parseInt(m.group(1)); String u=m.group(2);
   if(u.startsWith("sec")) rel+=n*1000L;
   else if(u.startsWith("m")) rel+=n*60000L;
   else if(u.startsWith("h")) rel+=n*3600000L;
   else rel+=n*86400000L;
  }
  if(rel>0) return now+rel;
  Matcher c=CLOCK.matcher(low);
  while(c.find()){
   int hr=Integer.parseInt(c.group(1)); if(hr>23) continue;
   int min=c.group(2)!=null?Integer.parseInt(c.group(2)):0; if(min>59) continue;
   String ap=c.group(3);
   if(ap!=null){ if(hr<1||hr>12) continue; if(hr==12) hr=0; if(ap.equals("pm")) hr+=12; }
   Calendar cal=Calendar.getInstance(); cal.set(Calendar.SECOND,0); cal.set(Calendar.MILLISECOND,0);
   cal.set(Calendar.HOUR_OF_DAY,hr); cal.set(Calendar.MINUTE,min);
   if(low.contains("tomorrow")||cal.getTimeInMillis()<=now) cal.add(Calendar.DAY_OF_MONTH,1);
   return cal.getTimeInMillis();
  }
  if(low.contains("tomorrow")){ Calendar cal=Calendar.getInstance(); cal.add(Calendar.DAY_OF_MONTH,1); cal.set(Calendar.HOUR_OF_DAY,8); cal.set(Calendar.MINUTE,0); cal.set(Calendar.SECOND,0); cal.set(Calendar.MILLISECOND,0); return cal.getTimeInMillis(); }
  return 0;
 }
 static String extractText(String original){
  String txt=original.toLowerCase();
  txt=txt.replaceFirst("^remind( me)?( to)?\\s*","");
  boolean relative=REL.matcher(txt).find();
  txt=txt.replaceAll("\\bin\\s+(\\d+)\\s*(seconds?|secs?|minutes?|mins?|m|hours?|hrs?|h|days?|d)\\b","");
  txt=txt.replaceAll("\\btomorrow\\b","");
  if(!relative){
   // only treat bare numbers as clock times when there was no "in N minutes" style phrase
   Matcher c=CLOCK.matcher(txt);
   while(c.find()){ if(Integer.parseInt(c.group(1))<=23) txt=txt.replace(c.group()," "); }
  }
  txt=txt.replaceAll("\\b(at|am|pm)\\b"," ").replaceAll("\\s+"," ").replaceAll("^[\\s.,'-]+|[\\s.,'-]+$","");
  return txt.trim();
 }
 static String when(long at){
  long mins=(at-System.currentTimeMillis())/60000;
  if(mins<1) return "any moment now";
  if(mins<60) return "in "+mins+" min";
  if(mins<1440) return "in "+(mins/60)+"h "+(mins%60)+"m";
  Calendar c=Calendar.getInstance(); c.setTimeInMillis(at);
  return String.format(java.util.Locale.US,"%ta %02d:%02d",c,c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE));
 }

 // ---------- alarms ----------
 static PendingIntent pi(Context c,int id){
  Intent i=new Intent(c,AlarmReceiver.class); i.setAction(ACTION); i.putExtra("id",id);
  int flags=PendingIntent.FLAG_UPDATE_CURRENT;
  if(Build.VERSION.SDK_INT>=23) flags|=PendingIntent.FLAG_IMMUTABLE;
  return PendingIntent.getBroadcast(c,id+7000,i,flags);
 }
 static void schedule(Context c,int id,long at){
  AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  PendingIntent p=pi(c,id);
  boolean exact=Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms();
  if(exact){
   if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p);
   else am.setExact(AlarmManager.RTC_WAKEUP,at,p);
  } else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p);
 }
 static void cancel(Context c,int id){ AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE); am.cancel(pi(c,id)); }
 static void armAll(final MainService s){
  JSONArray a=all(s.sp); long now=System.currentTimeMillis();
  for(int i=0;i<a.length();i++){
   final JSONObject o=a.optJSONObject(i); if(o==null) continue;
   final int id=o.optInt("id"); long at=o.optLong("at");
   if(at<=now){ s.h.postDelayed(new Runnable(){ public void run(){ fire(s,id); }},2500); }
   else schedule(s,id,at);
  }
 }
 static void fire(MainService s,int id){
  JSONArray a=all(s.sp); String text=null; JSONArray kept=new JSONArray();
  for(int i=0;i<a.length();i++){ JSONObject o=a.optJSONObject(i); if(o!=null&&o.optInt("id")==id) text=o.optString("text"); else kept.put(o); }
  if(text==null) return;
  save(s.sp,kept);
  s.speak("\u23F0 reminder: "+text);
  try{ s.mem.saveChat("reminder "+id,text); }catch(Exception e){ L.w("Reminders",e); }
 }
}
