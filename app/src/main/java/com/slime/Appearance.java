package com.slime;
import android.content.SharedPreferences;
// Pet and chat look-and-feel prefs: pet size multiplier, and (later) opacity and wandering.
class Appearance {
 static final float MIN_SCALE=0.6f, MAX_SCALE=1.8f;
 static final String PREF_SCALE="petScale";

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
 static boolean handle(MainService s,String t,String low){
  low=low.trim();
  if(low.equals("pet size")||low.equals("pet size status")){
   s.local("pet size is "+Math.round(scale(s.sp)*100)+"% (50% to 180%, normal 100%). Try 'pet bigger', 'pet size 130' or 'pet size reset'.",false); return true;
  }
  if(low.equals("pet size reset")){ s.sp.edit().putFloat(PREF_SCALE,1f).apply(); applySize(s); s.local("pet back to normal size (100%)",false); return true; }
  if(low.equals("pet bigger")||low.equals("pet larger")){ setScale(s,Math.min(MAX_SCALE,scale(s.sp)+0.15f)); return true; }
  if(low.equals("pet smaller")){ setScale(s,Math.max(MIN_SCALE,scale(s.sp)-0.15f)); return true; }
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
}
