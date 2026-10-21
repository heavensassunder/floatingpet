package com.slime;
import android.content.pm.ApplicationInfo; import android.content.pm.PackageManager;
import android.view.View; import android.view.WindowManager;
import java.io.File; import java.util.Locale;
// Phone control through Shizuku (rish inside Termux): taps, swipes, keys, launch apps, settings, screenshots, vision.
public class Phone {
 public static final String AI_HINT="\nPHONE CONTROL: if I clearly ask you to DO something on the phone, you may end your reply with ONE tag [[do:ACTION]] where ACTION is one of: launch APPNAME | back | home | recents | tap X Y | swipe up/down/left/right | look. I always confirm with the user first. Never use the tag otherwise.";
 static final String ENABLE_EYES="P=com.slime; cur=$(settings get secure enabled_accessibility_services); case \"$cur\" in *$P/$P.EyeService*) ;; \"\"|null) settings put secure enabled_accessibility_services $P/$P.EyeService ;; *) settings put secure enabled_accessibility_services \"$cur:$P/$P.EyeService\" ;; esac; settings put secure accessibility_enabled 1; cmd notification allow_listener $P/$P.NotificationEyesService";
 static final String GRANT_ALL="P=com.slime; for p in android.permission.POST_NOTIFICATIONS com.termux.permission.RUN_COMMAND; do pm grant $P $p >/dev/null 2>&1; done; appops set $P SYSTEM_ALERT_WINDOW allow; cmd appops set $P RUN_IN_BACKGROUND allow; cmd appops set $P RUN_ANY_IN_BACKGROUND allow; dumpsys deviceidle whitelist +$P >/dev/null 2>&1; "+ENABLE_EYES+"; echo done";
 final MainService s; final Shell sh; String pending=null; long pendingAt=0;
 public Phone(MainService s){ this.s=s; this.sh=new Shell(s); }

 static void focusable(MainService s){ try{ s.p2.flags&=~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE; }catch(Exception e){ L.w("Phone",e); } }

 // after a reboot: switch my own eyes (accessibility + notification access) back on, if Shizuku is running
 public void start(){
  s.h.postDelayed(new Runnable(){ public void run(){
   if(EyeService.instance!=null||!s.sp.getBoolean("autoheal",true)||!sh.installed()) return;
   sh.rish(ENABLE_EYES,new Shell.Result(){ public void done(int code,String out,String err,String info){
    if(info==null&&code==0&&s.slimeView!=null){ s.slimeView.say("eyes back on!"); s.slimeView.bounceAnim(); }
   }});
  }},6000);
 }

 // ---------- helpers ----------
 static String clip(String x){ if(x==null) return ""; x=x.trim(); return x.length()>600?x.substring(0,600)+"...":x; }
 static String q(String x){ return x.replace("'","'\\''"); }
 static Boolean onoff(String low,String prefix){
  if(!low.startsWith(prefix)) return null;
  String r=low.substring(prefix.length()).trim();
  if(r.equals("on")||r.equals("enable")||r.equals("true")) return Boolean.TRUE;
  if(r.equals("off")||r.equals("disable")||r.equals("false")) return Boolean.FALSE;
  return null;
 }
 static int[] nums(String x){
  try{ String[] p=x.trim().split("[\\s,]+"); int[] r=new int[p.length]; for(int i=0;i<p.length;i++) r[i]=Integer.parseInt(p[i]); return r; }catch(Exception e){ return null; }
 }
 static Integer keyCode(String n){
  n=n.trim();
  if(n.equals("enter")) return 66; if(n.equals("volup")) return 24; if(n.equals("voldown")) return 25; if(n.equals("mute")) return 164;
  if(n.equals("power")) return 26; if(n.equals("menu")) return 82; if(n.equals("play")) return 85; if(n.equals("next")) return 87; if(n.equals("prev")) return 88;
  return null;
 }
 static boolean allowed(String a){
  a=a.trim().toLowerCase(Locale.US);
  return a.equals("back")||a.equals("home")||a.equals("recents")||a.equals("look")||a.matches("launch [a-z0-9 ._-]{1,40}")||a.matches("swipe (up|down|left|right)")||a.matches("tap \\d{1,4} \\d{1,4}");
 }
 String findPkg(String name){
  name=name.trim().toLowerCase(Locale.US); if(name.isEmpty()) return null;
  if(name.matches("[a-z0-9_]+(\\.[a-z0-9_]+)+")) return name;
  PackageManager pm=s.getPackageManager(); String best=null;
  for(ApplicationInfo a: pm.getInstalledApplications(0)){
   if(pm.getLaunchIntentForPackage(a.packageName)==null) continue;
   String l=pm.getApplicationLabel(a).toString().toLowerCase(Locale.US);
   if(l.equals(name)) return a.packageName;
   if(best==null&&l.contains(name)) best=a.packageName;
  }
  return best;
 }
 // hide my chat so taps and keys land on the real screen, not on me
 void hideThen(Runnable r){
  try{ s.chatView.setVisibility(View.GONE); s.p2.flags|=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE; s.wm.updateViewLayout(s.chatView,s.p2); }catch(Exception e){ L.w("Phone",e); }
  s.h.postDelayed(r,350);
 }
 void act(String script,String ok,boolean hide){ go(script,ok,hide,false); }
 void actShow(String script,String ok,boolean hide){ go(script,ok,hide,true); }
 void go(final String script,final String ok,boolean hide,final boolean show){
  Runnable r=new Runnable(){ public void run(){
   sh.rish(script,new Shell.Result(){ public void done(int code,String out,String err,String info){ report(code,out,err,info,ok,show); }});
  }};
  if(hide) hideThen(r); else r.run();
 }
 void termRun(String script){
  sh.term(script,new Shell.Result(){ public void done(int code,String out,String err,String info){ report(code,out,err,info,null,true); }});
 }
 void report(int code,String out,String err,String info,String ok,boolean show){
  String o=out==null?"":out.trim(); String er=err==null?"":err.trim();
  if(info!=null){ s.showChat(); s.local(info,false); return; }
  if(code!=0){
   s.showChat();
   if(code==127||er.contains("No such file")||er.contains("not found")) s.local("Shizuku's shell (rish) isn't set up in Termux yet. Run setup_rish.sh in Termux once.",false);
   else s.local("didn't work ("+code+"): "+clip(er.isEmpty()?o:er),false);
   s.moodEvent(5,4000); return;
  }
  String msg=ok!=null?ok:(o.isEmpty()?"done":clip(o));
  if(show||(ok==null&&!o.isEmpty())) s.showChat();
  s.local(msg,true);
 }

 // ---------- AI suggested actions (always need a yes) ----------
 public String extractDo(String reply){
  try{
   int a=reply.indexOf("[[do:"); if(a<0) return reply;
   int b=reply.indexOf("]]",a); if(b<0) return reply;
   final String act=reply.substring(a+5,b).trim();
   String clean=(reply.substring(0,a)+reply.substring(b+2)).trim();
   if(allowed(act)){
    pending=act.toLowerCase(Locale.US); pendingAt=System.currentTimeMillis();
    s.h.postDelayed(new Runnable(){ public void run(){ s.addChat("Slime: want me to run \""+act+"\"? say yes or no",false); s.slimeView.say("do it? yes / no"); }},600);
   }
   return clean;
  }catch(Exception e){ return reply; }
 }

 // ---------- chat commands ----------
 public boolean handle(String t,String low){
  low=low.trim();
  if(pending!=null){
   if(System.currentTimeMillis()-pendingAt>90000L) pending=null;
   else if(low.equals("yes")||low.equals("y")||low.equals("ok")||low.equals("do it")||low.equals("sure")){ String a=pending; pending=null; run(a,a,true); return true; }
   else if(low.equals("no")||low.equals("n")||low.equals("cancel")||low.equals("nope")){ pending=null; s.local("ok, cancelled",false); return true; }
   else pending=null;
  }
  return run(t,low,false);
 }
 boolean run(String t,String low,boolean ai){
  if(ai&&!allowed(low)) return true;
  if(low.equals("phone help")||low.equals("phone")){ Vibe.showHelp(s); return true; }
  if(low.equals("back")){ act("input keyevent 4","back",true); return true; }
  if(low.equals("home")){ act("input keyevent 3","home",true); return true; }
  if(low.equals("recents")){ act("input keyevent 187","recents",true); return true; }
  if(low.startsWith("key ")){
   Integer k=keyCode(low.substring(4));
   if(k==null){ s.local("keys: enter, volup, voldown, mute, power, menu, play, next, prev",false); return true; }
   act("input keyevent "+k,"pressed "+low.substring(4).trim(),true); return true;
  }
  if(low.startsWith("tap ")){
   int[] n=nums(low.substring(4));
   if(n!=null&&n.length==2){ act("input tap "+n[0]+" "+n[1],"tapped "+n[0]+","+n[1],true); return true; }
   return false;
  }
  if(low.equals("swipe")||low.startsWith("swipe ")){
   String a=low.substring(5).trim(); android.util.DisplayMetrics dm=new android.util.DisplayMetrics(); s.wm.getDefaultDisplay().getRealMetrics(dm); int W=dm.widthPixels, H=dm.heightPixels; int x1,y1,x2,y2; int ms=300;
   if(a.equals("up")){ x1=W/2; x2=W/2; y1=H*3/4; y2=H/4; }
   else if(a.equals("down")){ x1=W/2; x2=W/2; y1=H/4; y2=H*3/4; }
   else if(a.equals("left")){ y1=H/2; y2=H/2; x1=W*4/5; x2=W/5; }
   else if(a.equals("right")){ y1=H/2; y2=H/2; x1=W/5; x2=W*4/5; }
   else {
    int[] n=nums(a); if(n==null||(n.length!=4&&n.length!=5)) return false;
    x1=n[0]; y1=n[1]; x2=n[2]; y2=n[3]; if(n.length==5) ms=n[4];
   }
   act("input swipe "+x1+" "+y1+" "+x2+" "+y2+" "+ms,"swiped",true); return true;
  }
  if(low.startsWith("stype ")){ String x=t.substring(Math.min(6,t.length())).trim().replace(" ","%s"); act("input text '"+q(x)+"'","typed it",false); return true; }
  if(low.startsWith("launch ")){
   String name=low.substring(7).trim(); String pkg=findPkg(name);
   if(pkg==null){ s.local("can't find an app called \""+name+"\"",false); return true; }
   act("monkey -p "+pkg+" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; echo ok","opening "+name,true); return true;
  }
  if(low.startsWith("force stop ")||low.startsWith("kill ")){
   String name=low.startsWith("kill ")?low.substring(5).trim():low.substring(11).trim(); String pkg=findPkg(name);
   if(pkg==null){ s.local("can't find an app called \""+name+"\"",false); return true; }
   act("am force-stop "+pkg,"stopped "+name,false); return true;
  }
  if(low.startsWith("goto http://")||low.startsWith("goto https://")){
   String u=t.substring(Math.min(5,t.length())).trim();
   act("am start -a android.intent.action.VIEW -d '"+q(u)+"' >/dev/null 2>&1; echo ok","opening the link",true); return true;
  }
  if(low.equals("fg")||low.equals("what app")||low.equals("which app")){ actShow("dumpsys window | grep -E 'mCurrentFocus|mFocusedApp' | head -n 2",null,false); return true; }
  if(low.equals("shot")||low.equals("screenshot")){
   String f="/sdcard/Pictures/slime_"+(System.currentTimeMillis()/1000)+".png";
   actShow("mkdir -p /sdcard/Pictures; screencap -p "+f+" && echo saved","screenshot saved in Pictures/"+f.substring(f.lastIndexOf('/')+1),true); return true;
  }
  if(low.equals("look")||low.startsWith("look ")){
   String qn=low.length()>5?t.substring(Math.min(5,t.length())).trim():"";
   look(qn.isEmpty()?"What is on my screen? Answer briefly.":qn); return true;
  }
  if(low.startsWith("brightness ")){
   String a=low.substring(11).trim();
   if(a.equals("auto")){ act("settings put system screen_brightness_mode 1","auto brightness on",false); return true; }
   int[] n=nums(a);
   if(n!=null&&n.length==1){ int p=Math.max(0,Math.min(100,n[0])); act("settings put system screen_brightness_mode 0; settings put system screen_brightness "+(p*255/100),"brightness "+p+"%",false); return true; }
   return false;
  }
  Boolean b;
  if((b=onoff(low,"wifi "))!=null){ act("svc wifi "+(b.booleanValue()?"enable":"disable"),"wifi "+(b.booleanValue()?"on":"off"),false); return true; }
  if((b=onoff(low,"data "))!=null){ act("svc data "+(b.booleanValue()?"enable":"disable"),"mobile data "+(b.booleanValue()?"on":"off"),false); return true; }
  if((b=onoff(low,"bluetooth "))!=null){ act("svc bluetooth "+(b.booleanValue()?"enable":"disable"),"bluetooth "+(b.booleanValue()?"on":"off"),false); return true; }
  if((b=onoff(low,"airplane "))!=null){ act("cmd connectivity airplane-mode "+(b.booleanValue()?"enable":"disable"),"airplane mode "+(b.booleanValue()?"on":"off"),false); return true; }
  if((b=onoff(low,"rotate "))!=null){ act("settings put system accelerometer_rotation "+(b.booleanValue()?1:0),"auto-rotate "+(b.booleanValue()?"on":"off"),false); return true; }
  if((b=onoff(low,"dnd "))!=null){ act("cmd notification set_dnd "+(b.booleanValue()?"priority":"off"),"do not disturb "+(b.booleanValue()?"on":"off"),false); return true; }
  if((b=onoff(low,"dark mode "))!=null){ act("cmd uimode night "+(b.booleanValue()?"yes":"no"),"dark mode "+(b.booleanValue()?"on":"off"),false); return true; }
  if(low.startsWith("volume ")){
   int[] n=nums(low.substring(7));
   if(n!=null&&n.length==1){ int v=Math.max(0,Math.min(15,n[0])); act("cmd media_session volume --stream 3 --set "+v+" 2>/dev/null || media volume --stream 3 --set "+v,"volume "+v,false); return true; }
   return false;
  }
  if(low.startsWith("timeout ")){
   int[] n=nums(low.substring(8));
   if(n!=null&&n.length==1){ int sec=Math.max(5,Math.min(1800,n[0])); act("settings put system screen_off_timeout "+(sec*1000),"screen timeout "+sec+"s",false); return true; }
   return false;
  }
  if(low.equals("screen off")){ act("input keyevent 26","screen off",true); return true; }
  if(low.equals("screen on")){ act("input keyevent 224","screen on",false); return true; }
  if(low.equals("grant self")||low.equals("fix perms")||low.equals("fix permissions")){ actShow(GRANT_ALL,"permissions granted and my eyes are on",false); return true; }
  if(low.equals("shizuku")||low.equals("shizuku status")||low.equals("test shell")){ actShow("id",null,false); return true; }
  Boolean ah=onoff(low,"autoheal ");
  if(ah!=null){ s.sp.edit().putBoolean("autoheal",ah.booleanValue()).apply(); s.local("auto-heal "+(ah.booleanValue()?"on":"off"),false); return true; }
  if(low.startsWith("vision model ")){
   String m=t.substring(Math.min(13,t.length())).trim();
   if(!m.isEmpty()){ s.sp.edit().putString("visionModel",m).apply(); s.local("vision model: "+m,false); return true; }
   return false;
  }
  if(!ai&&low.startsWith("sh ")){ String c=t.substring(Math.min(3,t.length())).trim(); if(!c.isEmpty()){ actShow(c,null,false); return true; } }
  if(!ai&&low.startsWith("term ")){ String c=t.substring(Math.min(5,t.length())).trim(); if(!c.isEmpty()){ termRun(c); return true; } }
  return false;
 }

 // ---------- screenshot + vision ----------
 void look(final String question){
  final String key=s.sp.getString("key","");
  if(key.isEmpty()){ s.local("add API key first bestie",false); return; }
  if(s.privateNow()){ s.local("that looks like a private app, I'll look away",true); return; }
  File dir=s.getExternalFilesDir(null);
  if(dir==null){ s.local("I have no storage to keep a screenshot in",false); return; }
  dir.mkdirs();
  final File img=new File(dir,"look.png");
  s.slimeView.say("looking...");
  hideThen(new Runnable(){ public void run(){
   String p=img.getAbsolutePath();
   sh.rish("screencap -p '"+q(p)+"' && chmod 666 '"+q(p)+"'",new Shell.Result(){ public void done(int code,String out,String err,String info){
    if(info!=null||code!=0){ s.showChat(); s.local(info!=null?info:"screencap failed: "+clip(err),false); return; }
    Vision.ask(key,FreeModels.nextVision(s.sp),question,img,new Vision.Callback(){ public void onResult(String reply){
     try{ img.delete(); }catch(Exception e){ L.w("Phone",e); }
     s.showChat(); s.speak(reply);
     try{ s.mem.saveChat("look: "+question,reply); }catch(Exception e){ L.w("Phone",e); }
    }});
   }});
  }});
 }
}
