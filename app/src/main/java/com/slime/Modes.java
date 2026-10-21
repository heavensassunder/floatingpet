package com.slime;
import android.content.pm.ApplicationInfo; import android.graphics.Color; import android.os.Build; import android.view.View; import android.view.accessibility.AccessibilityNodeInfo;
public class Modes {
 static String mode="chill"; static boolean ticking=false;
 static final String MATHP="\nMATH: write math in LaTeX, inline like $x^2$ and display like $$\\int_0^1 x\\,dx$$. Never write fake text like frac(a,b).";
 static void load(MainService s){ mode=s.sp.getString("mode","chill"); startTick(s); }
 static boolean is(String m){ return mode.equals(m); }
 static boolean quiet(MainService s){ return mode.equals("focus")||mode.equals("game"); }
 static int tint(int mood){
  if(mood==8) return 0;
  if(mode.equals("focus")) return Color.parseColor("#E74C3C");
  if(mode.equals("hacker")) return Color.parseColor("#2C3E50");
  if(mode.equals("study")) return Color.parseColor("#2E86C1");
  if(mode.equals("game")) return Color.parseColor("#F4D03F");
  return 0;
 }
 static String prompt(MainService s){
  String p=MATHP;
  if(mode.equals("focus")) p+="\nMODE FOCUS: answer in at most 12 words, no chit-chat, no jokes, only the useful point.";
  else if(mode.equals("hacker")) p+="\nMODE HACKER: be technical and terse like a terminal buddy. Prefer exact commands in code blocks.";
  else if(mode.equals("study")) p+="\nMODE STUDY: teach step by step, explain why, give a small worked example.";
  else if(mode.equals("game")) p+="\nMODE GAME: one short line only.";
  return p;
 }
 static boolean handle(MainService s,String t,String low){
  low=low.trim();
  if(low.equals("mode")||low.equals("modes")){ s.local("mode: "+mode+"   (focus, hacker, study, game, chill)",false); return true; }
  String m=null;
  if(low.endsWith(" mode off")){ set(s,"chill"); return true; }
  if(low.endsWith(" mode on")) m=low.substring(0,low.length()-8).trim();
  else if(low.endsWith(" mode")) m=low.substring(0,low.length()-5).trim();
  else if(low.startsWith("mode ")) m=low.substring(5).trim();
  if(m!=null&&(m.equals("focus")||m.equals("hacker")||m.equals("study")||m.equals("game")||m.equals("chill"))){ set(s,m); return true; }
  return false;
 }
 static void set(MainService s,String m){
  String old=mode; mode=m;
  s.sp.edit().putString("mode",m).apply();
  boolean q=m.equals("focus")||m.equals("game"), wasQ=old.equals("focus")||old.equals("game");
  if(q&&!wasQ) s.sp.edit().putBoolean("modePings",s.sp.getBoolean("pings",true)).putBoolean("pings",false).apply();
  if(!q&&wasQ) s.sp.edit().putBoolean("pings",s.sp.getBoolean("modePings",true)).apply();
  if(m.equals("focus")&&!old.equals("focus")) dnd(s,true);
  else if(old.equals("focus")&&!m.equals("focus")) dnd(s,false);
  try{ s.slimeView.setVisibility(View.VISIBLE); s.slimeView.invalidate(); }catch(Exception e){}
  startTick(s);
  String line="chill mode. back to being my goofy self.";
  if(m.equals("focus")) line="focus mode. I'll keep quiet and out of your way.";
  else if(m.equals("hacker")) line="hacker mode. terminal brain online.";
  else if(m.equals("study")) line="study mode. what are we learning?";
  else if(m.equals("game")) line="game mode. I'll hide while you game.";
  s.local(line,true);
 }
 static void dnd(MainService s,boolean on){
  try{
   if(s.phone!=null&&s.phone.sh.installed()) s.phone.sh.rish("cmd notification set_dnd "+(on?"priority":"off"),new Shell.Result(){ public void done(int a,String b,String c,String d){} });
  }catch(Exception e){}
 }
 static boolean gameFg(MainService s){
  try{
   EyeService e=EyeService.instance; if(e==null) return false;
   AccessibilityNodeInfo r=e.getRootInActiveWindow(); if(r==null) return false;
   CharSequence pk=r.getPackageName(); if(pk==null) return false;
   if(Build.VERSION.SDK_INT<26) return false;
   ApplicationInfo ai=s.getPackageManager().getApplicationInfo(pk.toString(),0);
   return ai.category==ApplicationInfo.CATEGORY_GAME;
  }catch(Exception e){ return false; }
 }
 static void startTick(final MainService s){
  if(ticking||!mode.equals("game")) return;
  ticking=true;
  s.h.postDelayed(new Runnable(){ public void run(){
   if(!mode.equals("game")||MainService.instance!=s){ ticking=false; try{ s.slimeView.setVisibility(View.VISIBLE); }catch(Exception e){} return; }
   try{ s.slimeView.setVisibility(gameFg(s)?View.INVISIBLE:View.VISIBLE); }catch(Exception e){}
   s.h.postDelayed(this,4000);
  }},4000);
 }
}
