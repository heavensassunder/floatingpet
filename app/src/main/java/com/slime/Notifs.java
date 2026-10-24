package com.slime;
import android.view.View;
// Notification reactions: unread counting, urgency bubbles and AI catch-up summaries.
class Notifs {
 final MainService s;
 int unseen; long lastBubble;
 Notifs(MainService s){ this.s=s; }

 boolean looksUrgent(String text){
  String l=text.toLowerCase();
  String[] k={"urgent","asap","emergency","call me","immediately","important","where are you","are you there","!!!"};
  for(String w:k){ if(l.contains(w)) return true; }
  return false;
 }
 // Called from NotificationEyesService. The unread counter always advances, even when no bubble is shown.
 void onNotif(String pkg,String title,String text){
  try{
   unseen++;
   long now=System.currentTimeMillis();
   if(!s.sp.getBoolean("pings",true)||!s.screenOn()||Modes.quiet(s)) return;
   if(s.chatView.getVisibility()==View.VISIBLE||s.asleep()||s.privateNow()) return;
   if(now<s.sp.getLong("muteUntil",0)||now-lastBubble<20000) return;
   lastBubble=now;
   if(looksUrgent(title+" "+text)) s.react(title+" sounds urgent!",6,5000);
   else { s.slimeView.say("ping from "+title); s.slimeView.bounceAnim(); }
  }catch(Exception e){ L.w("Notifs",e); }
 }
 String list(int n){
  try{
   org.json.JSONArray a=s.mem.loadArr(s.mem.notifFile); StringBuilder sb=new StringBuilder(); long now=System.currentTimeMillis();
   for(int i=Math.max(0,a.length()-n);i<a.length();i++){ org.json.JSONObject o=a.getJSONObject(i); long age=(now-o.optLong("t",now))/60000; sb.append("- ").append(o.optString("title")).append(": ").append(o.optString("text")).append(" (").append(age).append(" min ago)\n"); }
   return sb.toString();
  }catch(Exception e){ L.w("Notifs",e); return ""; }
 }
 void catchUp(String original){
  String list=list(15); unseen=0;
  if(list.isEmpty()){ s.local("nothing new, your inbox is quiet"); return; }
  s.askTask(original,"Catch me up on my notifications. Say how many there are, who matters most, and flag anything that sounds urgent.\n\nNOTIFICATIONS (oldest first):\n"+list);
 }
}
