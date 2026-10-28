package com.slime;
import android.content.SharedPreferences;
// Pet and chat look-and-feel prefs: pet size multiplier, and (later) opacity and wandering.
class Appearance {
 static final float MIN_SCALE=0.6f, MAX_SCALE=1.8f;
 static final int MIN_ALPHA=10, MAX_ALPHA=100;
 static final String PREF_SCALE="petScale", PREF_PET_ALPHA="petAlpha", PREF_CHAT_ALPHA="chatAlpha";

 static float scale(SharedPreferences sp){
  float k=sp.getFloat(PREF_SCALE,1f);
  if(k<MIN_SCALE) return MIN_SCALE; if(k>MAX_SCALE) return MAX_SCALE; return k;
 }
 // resize the overlay window and the canvas together so touch area, drawing and bounds all match
 static void applySize(MainService s){
  float k=scale(s.sp);
  Walls.petScale=k;
  s.p1.width=Math.round(MainService.SW*k);
  s.p1.height=Math.round(MainService.SH*k);
  if(s.slimeView!=null) s.slimeView.setPetScale(k);
  s.clampPos(); s.updateViews();
 }
 static void applyAlpha(MainService s){
  if(s.slimeView!=null) s.slimeView.setAlpha(s.sp.getInt(PREF_PET_ALPHA,100)/100f);
  if(s.chatView!=null) s.chatView.setAlpha(s.sp.getInt(PREF_CHAT_ALPHA,100)/100f);
 }
 static boolean handle(MainService s,String t,String low){
  low=low.trim();
  if(handleOpacity(s,low)) return true;
  if(low.equals("pet size")||low.equals("pet size status")){
   s.local("pet size is "+Math.round(scale(s.sp)*100)+"% (50% to 180%, normal 100%). Try 'pet bigger', 'pet size 130' or 'pet size reset'.",false); return true;
  }
  if(low.equals("pet size reset")){ s.sp.edit().putFloat(PREF_SCALE,1f).apply(); applySize(s); s.local("pet back to normal size (100%)",false); return true; }
  if(low.equals("pet bigger")||low.equals("pet larger")){ setScale(s,Math.min(MAX_SCALE,scale(s.sp)+0.15f)); return true; }
  if(low.equals("pet smaller")){ setScale(s,Math.max(MIN_SCALE,scale(s.sp)-0.15f)); return true; }
  if(handleWander(s,low)) return true;
  if(low.startsWith("pet size ")){
   String arg=low.substring(9).trim();
   if(arg.equals("small")){ setScale(s,0.7f); return true; }
   if(arg.equals("normal")||arg.equals("medium")){ setScale(s,1f); return true; }
   if(arg.equals("big")||arg.equals("large")){ setScale(s,1.3f); return true; }
   if(arg.equals("huge")){ setScale(s,1.6f); return true; }
   try{
    int pct=Integer.parseInt(arg.replaceAll("[^0-9]",""));
    if(pct<50||pct>180){ s.local("pick a size between 50% and 180%",false); return true; }
    setScale(s,pct/100f); return true;
   }catch(Exception e){ s.local("say e.g. 'pet size 130', 'pet size small', 'pet bigger' or 'pet size reset'",false); return true; }
  }
  return false;
 }
 static void setScale(MainService s,float k){
  k=Math.max(MIN_SCALE,Math.min(MAX_SCALE,k));
  s.sp.edit().putFloat(PREF_SCALE,k).apply(); applySize(s);
  s.local("pet size now "+Math.round(k*100)+"%",false);
 }

 static boolean handleWander(MainService s,String low){
  low=low.trim();
  if(low.equals("wander status")){ s.local("wandering is "+(s.sp.getBoolean("wander",true)?"on (I roam by myself)":"off (I stay put until you drag me)"),false); return true; }
  if(low.equals("wander on")||low.equals("walk around")||low.equals("pet walk")){ setWander(s,true); return true; }
  if(low.equals("wander off")||low.equals("stay")||low.equals("stay still")||low.equals("pet static")){ setWander(s,false); return true; }
  return false;
 }
 static boolean handleOpacity(MainService s,String low){
  boolean pet=low.startsWith("pet opacity"), chat=low.startsWith("chat opacity")||low.startsWith("text box opacity")||low.startsWith("textbox opacity");
  if(!pet&&!chat) return false;
  String arg=low.substring(low.indexOf("opacity")+7).trim();
  String key=pet?PREF_PET_ALPHA:PREF_CHAT_ALPHA;
  String who=pet?"pet":"chat box";
  if(arg.equals("reset")||arg.equals("normal")){ s.sp.edit().putInt(key,100).apply(); applyAlpha(s); s.local(who+" opacity back to 100%",false); return true; }
  if(arg.isEmpty()){ s.local(who+" opacity is "+s.sp.getInt(key,100)+"%. Say e.g. '"+(pet?"pet":"chat")+" opacity 70' (10 to 100)",false); return true; }
  try{
   int v=Integer.parseInt(arg.replaceAll("[^0-9]",""));
   if(v<MIN_ALPHA||v>MAX_ALPHA){ s.local("pick a number from 10 to 100",false); return true; }
   s.sp.edit().putInt(key,v).apply(); applyAlpha(s); s.local(who+" opacity now "+v+"%",false);
  }catch(Exception e){ s.local("say e.g. '"+(pet?"pet":"chat")+" opacity 70' or '"+(pet?"pet":"chat")+" opacity reset'",false); }
  return true;
 }
 static void setWander(MainService s,boolean on){
  s.sp.edit().putBoolean("wander",on).apply();
  if(!on&&s.wander!=null) s.wander.cancel();
  s.local(on?"ok, I'll roam around the screen again":"ok, I'll stay right here until you drag me (pokes and 'play' still work)",false);
 }
}
