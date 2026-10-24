package com.slime;
import android.content.Intent; import android.view.View;
public class More {
 // lets Termux, Shizuku and SlimeDroid keep running in the background (so Termux commands always get answered)
 static final String KEEP="for p in com.termux moe.shizuku.privileged.api com.slime; do dumpsys deviceidle whitelist +$p >/dev/null 2>&1; cmd appops set $p RUN_IN_BACKGROUND allow >/dev/null 2>&1; cmd appops set $p RUN_ANY_IN_BACKGROUND allow >/dev/null 2>&1; cmd appops set $p START_FOREGROUND allow >/dev/null 2>&1; am set-standby-bucket $p active >/dev/null 2>&1; done; echo done";
 static boolean handle(MainService s,String t,String low){
  low=low.trim();
  if(low.equals("copy")||low.equals("copy last")){ s.chatView.copyLast(); return true; }
  if(low.equals("copy all")||low.equals("copy chat")){ s.chatView.copyAll(); return true; }
  if(low.equals("guide")||low.equals("readme")||low.equals("how to use")||low.equals("instructions")){ openGuide(s); return true; }
  if(low.equals("fix termux")||low.equals("keep termux alive")){
   if(s.phone==null){ s.local("give me a second, I'm still waking up",false); return true; }
   s.phone.actShow(KEEP,"Termux, Shizuku and I are now exempt from battery limits",false); return true;
  }
  if(Keys.handle(s,t,low)) return true;
  return false;
 }
 static void openGuide(MainService s){
  try{
   Intent i=new Intent(s,GuideActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); s.startActivity(i);
   try{ s.chatView.setVisibility(View.GONE); }catch(Exception e){ L.w("More",e); }
  }catch(Exception e){ s.local("couldn't open the guide",false); }
 }
}
