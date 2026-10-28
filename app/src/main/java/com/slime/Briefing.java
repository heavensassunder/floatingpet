package com.slime;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
// Daily morning briefing at a chosen time: greeting, weather (Open-Meteo, no key), unread pings,
// pet level/streak and needs. Re-arms itself every day and re-arms on reboot via MainService.
class Briefing {
 static final String ACTION="com.slime.BRIEFING";
 static final int REQ=8100;
 static final Pattern CLOCK=Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?");

 static boolean handle(MainService s,String t,String low){
  low=low.trim();
  if(low.startsWith("weather city")){ Weather.setCity(s,t.substring(low.indexOf("city")+4).trim()); return true; }
  if(low.equals("weather")||low.equals("forecast")){ Weather.showNow(s); return true; }
  if(low.equals("stop briefing")) low="briefing off";
  if(!low.startsWith("briefing")) return false;
  String rest=low.substring(8).trim();
  if(rest.equals("off")||rest.equals("stop")||rest.equals("cancel")){
   s.sp.edit().putInt("briefingMin",-1).apply(); cancel(s);
   s.local("morning briefing cancelled. Say 'briefing 7am' to bring it back.",false); return true;
  }
  if(rest.isEmpty()||rest.equals("now")){ fire(s); return true; }
  Matcher m=CLOCK.matcher(rest);
  if(!m.find()){ s.local("when? e.g. 'briefing 7am', 'briefing 6:30' or 'briefing off'",false); return true; }
  int hr=Integer.parseInt(m.group(1)), min=m.group(2)!=null?Integer.parseInt(m.group(2)):0;
  String ap=m.group(3);
  if(ap!=null){ if(hr<1||hr>12||min>59){ bad(s); return true; } if(hr==12) hr=0; if(ap.equals("pm")) hr+=12; }
  if(hr>23||min>59){ bad(s); return true; }
  int mins=hr*60+min;
  s.sp.edit().putInt("briefingMin",mins).apply(); schedule(s,mins);
  s.local("morning briefing set for "+String.format(java.util.Locale.US,"%02d:%02d",hr,min)+" every day. Say 'briefing now' for a preview, or 'briefing off' to stop it.",false);
  return true;
 }
 static void bad(MainService s){ s.local("that time doesn't make sense - e.g. 'briefing 7am' or 'briefing 18:45'",false); }

 static PendingIntent pi(Context c){
  Intent i=new Intent(c,AlarmReceiver.class); i.setAction(ACTION);
  int flags=PendingIntent.FLAG_UPDATE_CURRENT;
  if(Build.VERSION.SDK_INT>=23) flags|=PendingIntent.FLAG_IMMUTABLE;
  return PendingIntent.getBroadcast(c,REQ,i,flags);
 }
 static void schedule(Context c,int mins){
  Calendar cal=Calendar.getInstance(); cal.set(Calendar.SECOND,0); cal.set(Calendar.MILLISECOND,0);
  cal.set(Calendar.HOUR_OF_DAY,mins/60); cal.set(Calendar.MINUTE,mins%60);
  if(cal.getTimeInMillis()<=System.currentTimeMillis()) cal.add(Calendar.DAY_OF_MONTH,1);
  AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE); PendingIntent p=pi(c);
  boolean exact=Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms();
  if(exact){
   if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),p);
   else am.setExact(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),p);
  } else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),p);
 }
 static void cancel(Context c){ ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(pi(c)); }
 static void arm(MainService s){ int mins=s.sp.getInt("briefingMin",-1); if(mins>=0) schedule(s,mins); }

 static void fire(final MainService s){
  int mins=s.sp.getInt("briefingMin",-1);
  if(mins>=0) schedule(s,mins); // tomorrow's slot
  new Thread(new Runnable(){ public void run(){
   final StringBuilder b=new StringBuilder(greeting());
   String w=Weather.now(s.sp);
   if(!w.isEmpty()) b.append(' ').append(w).append('.');
   if(s.notifs.unseen>0){
    b.append(" You have ").append(s.notifs.unseen).append(s.notifs.unseen==1?" unread ping":" unread pings")
     .append(" - say 'catch me up' for the summary.");
   } else b.append(" No pings overnight, all quiet.");
   int level=Evo.lvl(s.sp.getInt("xp",0));
   b.append(" You're a level ").append(level).append(" slime on a ").append(s.sp.getInt("evo_streak",0)).append("-day streak.");
   if(s.needs.food<25f) b.append(" I'm a bit hungry though - a snack soon?");
   if(s.needs.energy<25f) b.append(" I'm still sleepy...");
   final String text=b.toString();
   s.h.post(new Runnable(){ public void run(){ s.speak(text); try{ s.mem.saveChat("morning briefing",text); }catch(Exception e){ L.w("Briefing",e); } }});
  }}).start();
 }
 static String greeting(){
  int hr=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
  if(hr<12) return "Good morning!";
  if(hr<17) return "Good afternoon!";
  return "Good evening!";
 }
}
