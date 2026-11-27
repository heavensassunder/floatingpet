package com.slime;
// Keeps a Termux task alive: checks every N seconds, restarts if Android killed it.
// Commands: guard task <cmd>, guard on/off/status, guard every N, guard now, guard watch <pattern>
public class TaskGuard implements Runnable {
 final MainService s;
 TaskGuard(MainService s){ this.s=s; }
 public void run(){
  if(MainService.instance!=s||!isOn()) return;
  check();
  s.h.postDelayed(this,intervalMs());
 }
 boolean isOn(){ return s.sp.getBoolean("guard_on",false); }
 long intervalMs(){ return s.sp.getLong("guard_every",30)*1000; }
 String task(){ return s.sp.getString("guard_task",""); }
 String watch(){ return s.sp.getString("guard_watch",""); }
 // ---------- commands ----------
 boolean handle(String t,String low){
  low=low.trim();
  if(low.equals("guard on")){ s.sp.edit().putBoolean("guard_on",true).apply(); s.h.postDelayed(this,1200); s.local("guard on - I'll keep your task alive every "+intervalMs()/1000+"s",false); return true; }
  if(low.equals("guard off")){ s.sp.edit().putBoolean("guard_on",false).apply(); s.local("guard off - I won't restart your task anymore",false); return true; }
  if(low.equals("guard status")){ status(); return true; }
  if(low.equals("guard now")){ check(); s.local("guard checked just now",false); return true; }
  if(low.startsWith("guard task ")){ String c=t.substring(Math.min(11,t.length())).trim(); if(c.isEmpty()){ s.local("say: guard task <command> to keep alive"); return true; } s.sp.edit().putString("guard_task",c).apply(); s.local("guard task set: "+c,false); return true; }
  if(low.startsWith("guard watch ")){ String c=t.substring(Math.min(12,t.length())).trim(); if(c.isEmpty()){ s.local("say: guard watch <pattern>"); return true; } s.sp.edit().putString("guard_watch",c).apply(); s.local("guard watch pattern: "+c,false); return true; }
  if(low.startsWith("guard every ")){ try{ int n=Integer.parseInt(t.substring(Math.min(12,t.length())).trim()); if(n<5||n>3600){ s.local("guard every must be 5 to 3600 seconds"); return true; } s.sp.edit().putLong("guard_every",n).apply(); s.local("guard checks every "+n+"s",false); }catch(Exception e){ s.local("say: guard every 30"); } return true; }
  return false;
 }
 void status(){
  String t=task();
  if(t.isEmpty()){ s.local("no guard task set. say: guard task <command>"); return; }
  String w=watch(); if(w.isEmpty()) w=defaultWatch(t);
  s.local("guard "+(isOn()?"ON":"off")+", every "+intervalMs()/1000+"s\ntask: "+t+"\nwatch: "+w,false);
 }
 // ---------- check + restart ----------
 void check(){
  if(!isOn()) return;
  String t=task(); if(t.isEmpty()) return;
  String w=watch(); if(w.isEmpty()) w=defaultWatch(t);
  isRunning(w,new Shell.Result(){
   public void done(int code,String out,String err,String info){
    boolean running=(code==0);
    if(!running){
     L.w("TaskGuard","task not running ("+w+"), restarting");
     s.phone.sh.term(t,new Shell.Result(){ public void done(int c2,String o2,String e2,String i2){
      if(c2==0) s.local("guard restarted your task: "+t,false);
      else s.local("guard tried to restart but Termux said: "+Phone.clip(e2+i2),false);
     }});
    }
   }
  });
 }
 // pgrep -f <pattern>; echo $?   -> code 0 means running
 void isRunning(String pattern,Shell.Result cb){
  s.phone.sh.term("pgrep -f '"+pattern+"' >/dev/null 2>&1; echo $?",cb);
 }
 String defaultWatch(String t){
  // take the first real command token (skip termux-wake-lock; nohup etc.)
  String x=t.trim();
  if(x.startsWith("termux-wake-lock")){ int i=x.indexOf(';'); if(i>=0) x=x.substring(i+1).trim(); }
  if(x.startsWith("nohup ")){ x=x.substring(6).trim(); int i=x.indexOf(' '); if(i>=0) x=x.substring(0,i); else x=""; }
  if(x.startsWith("&")) x=x.substring(1).trim();
  if(x.isEmpty()) return t;
  return x;
 }
}
