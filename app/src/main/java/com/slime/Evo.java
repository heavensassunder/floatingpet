package com.slime;
import android.graphics.Canvas; import android.graphics.Color; import android.graphics.Paint; import android.graphics.Path; import android.graphics.RectF; import android.graphics.Typeface;
import android.os.SystemClock;
import java.io.ByteArrayOutputStream; import java.io.File; import java.io.FileInputStream; import java.io.FileOutputStream;
import java.text.SimpleDateFormat; import java.util.Date; import java.util.HashSet; import java.util.Locale;
import org.json.JSONArray; import org.json.JSONObject;
// Growth: real behaviour earns XP (chats, voice, phone tasks, screenshots, daily care),
// with per-day anti-spam caps. Topic chatter still unlocks accessories. Tiers change the look.
public class Evo {
 static boolean glasses,cap,phones,sparkle,formulas,crown; static int level=1;
 static final String[] NAMES={"baby slime","slimelet","slime cadet","slime knight","slime king","slime sage","slime archon","celestial slime"};
 static final int[] NEED={0,60,160,340,640,1050,1600,2300};
 static final String[] CODE={"code","script","bug","error","compile","java","python","termux","function","patch","apk","build"};
 static final String[] MATH={"integral","derivative","equation","solve","math","circuit","voltage","current","resistor","matrix","calculus","formula","laplace","fourier"};
 static final String[] STUDY={"exam","test","assignment","homework","lecture","notes","revision","study","eet"};
 static final String[] MUSIC={"song","music","hum","sing","playlist"};
 static final String[] FIX={"fixed","it works","works now","solved"};
 static final String[] GAME={"game","gaming","pubg","fifa","minecraft"};
 // id, title, how to earn
 static final String[][] ACH={
  {"first_chat","First Words","your first real chat"},
  {"chat_50","Chatty","50 conversations"},
  {"chat_200","Constant Companion","200 conversations"},
  {"voice_10","Out Loud","10 voice messages heard"},
  {"voice_100","Voice of the Pet","100 voice messages heard"},
  {"task_10","Helping Hand","10 phone tasks completed"},
  {"shot_10","Through Your Eyes","10 screenshots taken"},
  {"code_25","Coder's Sidekick","25 coding chats"},
  {"math_15","Math Whisperer","15 math chats"},
  {"fix_8","Bug Slayer","8 bugs fixed together"},
  {"streak_7","Week One","a 7-day streak"},
  {"streak_30","Devoted","a 30-day streak"},
  {"lvl_5","Crowned","reach slime king"},
  {"lvl_max","Ascended","reach the top tier"}
 };
 static final String[] DC={"dc_chat","dc_code","dc_math","dc_study","dc_music","dc_fix","dc_game","dc_voice","dc_task","dc_shot","dc_care"};
 static boolean any(String l,String[] k){ for(String w:k){ if(l.contains(w)) return true; } return false; }
 static int lvl(int xp){ int l=1; for(int i=0;i<NEED.length;i++){ if(xp>=NEED[i]) l=i+1; } return l; }
 static int dayKey(){ return (int)(System.currentTimeMillis()/86400000L); }
 static void init(MainService s){ if(s.sp.getLong("evo_born",0)==0) s.sp.edit().putLong("evo_born",System.currentTimeMillis()).apply(); refresh(s); }
 static void refresh(MainService s){
  int xp=s.sp.getInt("xp",0); level=lvl(xp);
  glasses=s.sp.getInt("c_code",0)>=25;
  cap=s.sp.getInt("c_math",0)>=15||s.sp.getInt("c_study",0)>=20;
  phones=s.sp.getInt("c_music",0)>=12;
  sparkle=s.sp.getInt("c_fix",0)>=8;
  formulas=s.sp.getInt("c_math",0)>=15;
  crown=level>=5;
 }
 static void bump(MainService s,String key,int by){ s.sp.edit().putInt(key,s.sp.getInt(key,0)+by).apply(); }
 // ---------- day, streak and care ----------
 static void touchDay(MainService s){
  int today=dayKey(), last=s.sp.getInt("evo_day",-1);
  if(last==today) return;
  if(last==-1){ s.sp.edit().putInt("evo_day",today).putInt("evo_streak",1).putInt("evo_best",1).apply(); return; }
  int streak= today==last+1 ? s.sp.getInt("evo_streak",1)+1 : 1;
  android.content.SharedPreferences.Editor e=s.sp.edit();
  e.putInt("evo_day",today).putInt("evo_streak",streak).putInt("evo_best",Math.max(streak,s.sp.getInt("evo_best",1))).putInt("evo_today_xp",0);
  for(String k:DC) e.putInt(k,0);
  e.apply();
  if(streak>=2) addXp(s,3);
  float avg=(s.needs.food+s.needs.energy+s.needs.joy)/3f;
  if(avg>50f){ e.putInt("dc_care",1).apply(); addXp(s,4); s.react("good to see you again - I'm well looked after",0,5000); }
  checkAch(s);
 }
 // ---------- behaviour XP (daily caps, lifetime stats always counted) ----------
 static void award(MainService s,String key,int xp,int dailyCap){
  touchDay(s);
  String stat="stat_"+key, dc="dc_"+key;
  bump(s,stat,1);
  int used=s.sp.getInt(dc,0);
  if(used>=dailyCap) return;
  s.sp.edit().putInt(dc,used+1).apply();
  addXp(s,xp);
 }
 static void addXp(MainService s,int by){
  if(by<=0) return;
  int old=level;
  bump(s,"xp",by);
  s.sp.edit().putInt("evo_today_xp",s.sp.getInt("evo_today_xp",0)+by).apply();
  refresh(s);
  if(level>old){ writeHistory(s,level); s.react("level up! I'm a "+NAMES[level-1]+" now",3,6000); }
  checkAch(s);
 }
 // topic chatter: accessories plus small bonus XP, capped per topic per day, duplicates ignored
 static void count(MainService s,String low){
  touchDay(s);
  String norm=low.trim().toLowerCase(Locale.US);
  if(norm.length()<2) return;
  boolean duplicate=norm.equals(s.sp.getString("evo_last_text",""));
  s.sp.edit().putString("evo_last_text",norm.length()>180?norm.substring(0,180):norm).apply();
  if(duplicate) return;
  bump(s,"stat_chats",1);
  if(s.sp.getInt("dc_chat",0)<40) addXp(s,1);
  int add=0;
  if(any(norm,CODE)&&s.sp.getInt("dc_code",0)<5){ bump(s,"c_code",1); bump(s,"dc_code",1); add+=2; }
  if(any(norm,MATH)&&s.sp.getInt("dc_math",0)<5){ bump(s,"c_math",1); bump(s,"dc_math",1); add+=2; }
  if(any(norm,STUDY)&&s.sp.getInt("dc_study",0)<5){ bump(s,"c_study",1); bump(s,"dc_study",1); add+=1; }
  if(any(norm,MUSIC)&&s.sp.getInt("dc_music",0)<5){ bump(s,"c_music",1); bump(s,"dc_music",1); }
  if(any(norm,FIX)&&s.sp.getInt("dc_fix",0)<5){ bump(s,"c_fix",1); bump(s,"dc_fix",1); add+=3; }
  if(any(norm,GAME)&&s.sp.getInt("dc_game",0)<5){ bump(s,"c_game",1); bump(s,"dc_game",1); }
  boolean og=glasses,oc=cap,op=phones,os=sparkle;
  refresh(s);
  if((glasses&&!og)||(cap&&!oc)||(phones&&!op)||(sparkle&&!os)) s.react("ooh, I got a new accessory!",7,5000);
  if(add>0) addXp(s,add);
 }
 // ---------- achievements ----------
 static int target(String id){
  if(id.endsWith("_50")||id.equals("task_10")||id.equals("shot_10")||id.equals("voice_10")||id.equals("code_25")) return id.equals("voice_10")||id.equals("task_10")||id.equals("shot_10")?10:id.equals("code_25")?25:50;
  if(id.equals("first_chat")) return 1;
  if(id.equals("chat_200")||id.equals("voice_100")) return 200;
  if(id.equals("math_15")) return 15;
  if(id.equals("fix_8")) return 8;
  if(id.equals("streak_7")) return 7;
  if(id.equals("streak_30")) return 30;
  if(id.equals("lvl_5")) return 5;
  if(id.equals("lvl_max")) return NAMES.length;
  return 1;
 }
 static int progress(MainService s,String id){
  if(id.startsWith("chat_")) return s.sp.getInt("stat_chats",0);
  if(id.startsWith("voice_")) return s.sp.getInt("stat_voice",0);
  if(id.startsWith("task_")) return s.sp.getInt("stat_tasks",0);
  if(id.startsWith("shot_")) return s.sp.getInt("stat_shots",0);
  if(id.startsWith("code_")) return s.sp.getInt("c_code",0);
  if(id.startsWith("math_")) return s.sp.getInt("c_math",0);
  if(id.startsWith("fix_")) return s.sp.getInt("c_fix",0);
  if(id.startsWith("streak_")) return s.sp.getInt("evo_best",0);
  if(id.startsWith("lvl_")) return level;
  return 0;
 }
 static HashSet<String> unlocked(MainService s){
  HashSet<String> set=new HashSet<String>();
  for(String x:s.sp.getString("evo_ach","").split(",")){ if(!x.isEmpty()) set.add(x); }
  return set;
 }
 static void checkAch(MainService s){
  try{
   HashSet<String> have=unlocked(s); boolean changed=false;
   for(String[] a:ACH){
    if(!have.contains(a[0])&&progress(s,a[0])>=target(a[0])){
     have.add(a[0]); changed=true;
     s.react("achievement unlocked: "+a[1]+"! ("+a[2]+")",7,6000);
    }
   }
   if(changed){ StringBuilder sb=new StringBuilder(); for(String x:have) sb.append(x).append(','); s.sp.edit().putString("evo_ach",sb.toString()).apply(); }
  }catch(Exception e){ L.w("Evo",e); }
 }
 // ---------- level-up history ----------
 static File historyFile(MainService s){ return new File(s.getFilesDir(),"evo_history.json"); }
 static void writeHistory(MainService s,int lvl){
  try{
   File f=historyFile(s); JSONArray a;
   if(f.exists()){ FileInputStream in=new FileInputStream(f); ByteArrayOutputStream bo=new ByteArrayOutputStream(); byte[] b=new byte[2048]; int n; while((n=in.read(b))>0) bo.write(b,0,n); in.close(); a=new JSONArray(bo.toString("UTF-8")); }
   else a=new JSONArray();
   a.put(new JSONObject().put("l",lvl).put("ts",System.currentTimeMillis()));
   FileOutputStream out=new FileOutputStream(f); out.write(a.toString().getBytes("UTF-8")); out.close();
  }catch(Exception e){ L.w("Evo",e); }
 }
 static String historyText(MainService s){
  try{
   File f=historyFile(s); if(!f.exists()) return "no level-ups yet";
   FileInputStream in=new FileInputStream(f); ByteArrayOutputStream bo=new ByteArrayOutputStream(); byte[] b=new byte[2048]; int n; while((n=in.read(b))>0) bo.write(b,0,n); in.close();
   JSONArray a=new JSONArray(bo.toString("UTF-8")); SimpleDateFormat df=new SimpleDateFormat("dd MMM",Locale.US);
   int start=Math.max(0,a.length()-5); StringBuilder sb=new StringBuilder();
   for(int i=start;i<a.length();i++){ JSONObject o=a.getJSONObject(i); int l=o.optInt("l");
    if(sb.length()>0) sb.append(", ");
    sb.append(NAMES[Math.min(l,NAMES.length)-1]).append(" ").append(df.format(new Date(o.optLong("ts"))));
   }
   return sb.toString();
  }catch(Exception e){ L.w("Evo",e); return "history unavailable"; }
 }
 static String line(MainService s){
  int xp=s.sp.getInt("xp",0); refresh(s);
  String nx=level>=NAMES.length?"max":(xp+"/"+NEED[level]);
  return "lvl "+level+" "+NAMES[level-1]+" (xp "+nx+") | ai "+OpenRouterClient.budget()+" today";
 }
 static boolean handle(MainService s,String low){
  low=low.trim();
  if(low.equals("evolution")||low.equals("evo")||low.equals("level")){
   refresh(s);
   long age=(System.currentTimeMillis()-s.sp.getLong("evo_born",System.currentTimeMillis()))/86400000L;
   StringBuilder sb=new StringBuilder(line(s)).append("\n");
   sb.append("age: ").append(age).append(" days | streak: ").append(s.sp.getInt("evo_streak",0)).append(" (best ").append(s.sp.getInt("evo_best",0)).append(")");
   sb.append("\ntoday's XP: ").append(s.sp.getInt("evo_today_xp",0));
   sb.append("\nchats ").append(s.sp.getInt("stat_chats",0)).append(" | voice ").append(s.sp.getInt("stat_voice",0)).append(" | tasks ").append(s.sp.getInt("stat_tasks",0)).append(" | shots ").append(s.sp.getInt("stat_shots",0));
   sb.append("\nlevel-ups: ").append(historyText(s)).append("\n");
   sb.append("code chats: ").append(s.sp.getInt("c_code",0)).append(glasses?"  (glasses unlocked)":"  (25 for glasses)").append("\n");
   sb.append("math/study chats: ").append(s.sp.getInt("c_math",0)).append("/").append(s.sp.getInt("c_study",0)).append(cap?"  (grad cap unlocked)":"  (15 math or 20 study for the cap)").append("\n");
   sb.append("music chats: ").append(s.sp.getInt("c_music",0)).append(phones?"  (headphones unlocked)":"  (12 for headphones)").append("\n");
   sb.append("bugs fixed: ").append(s.sp.getInt("c_fix",0)).append(sparkle?"  (sparkles unlocked)":"  (8 for sparkles)").append("\n");
   sb.append(crown?"crown unlocked!":"level 5 gets a crown");
   if(level>=6) sb.append("\nsage aura, archon shield and celestial form come at levels 6 to 8");
   sb.append("\n\nsay 'achievements' to see them all.");
   s.showChat(); s.local(sb.toString(),false); return true;
  }
  if(low.equals("achievements")||low.equals("awards")){
   refresh(s); HashSet<String> have=unlocked(s);
   StringBuilder sb=new StringBuilder("achievements ("+have.size()+"/"+ACH.length+"):\n");
   for(String[] a:ACH){
    int p=Math.min(progress(s,a[0]),target(a[0]));
    sb.append(have.contains(a[0])?"\u2713 ":"- ").append(a[1]).append(" - ").append(a[2]).append(" (").append(p).append("/").append(target(a[0])).append(")\n");
   }
   s.showChat(); s.local(sb.toString(),false); return true;
  }
  return false;
 }
 // ---------- drawing ----------
 static final Paint dk=new Paint(Paint.ANTI_ALIAS_FLAG), rim=new Paint(Paint.ANTI_ALIAS_FLAG), lens=new Paint(Paint.ANTI_ALIAS_FLAG), gold=new Paint(Paint.ANTI_ALIAS_FLAG), goldLine=new Paint(Paint.ANTI_ALIAS_FLAG);
 static final Paint band=new Paint(Paint.ANTI_ALIAS_FLAG), cup=new Paint(Paint.ANTI_ALIAS_FLAG), green=new Paint(Paint.ANTI_ALIAS_FLAG), star=new Paint(Paint.ANTI_ALIAS_FLAG), sym=new Paint(Paint.ANTI_ALIAS_FLAG), doc=new Paint(Paint.ANTI_ALIAS_FLAG), docLine=new Paint(Paint.ANTI_ALIAS_FLAG);
 static final Paint aura=new Paint(Paint.ANTI_ALIAS_FLAG), shield=new Paint(Paint.ANTI_ALIAS_FLAG), orbitStar=new Paint(Paint.ANTI_ALIAS_FLAG);
 static final RectF rr=new RectF(); static final Path pth=new Path();
 static final String[] SYM={"\u03c0","\u222b","\u03a3","\u221a"};
 static{
  dk.setColor(Color.parseColor("#2C3E50"));
  rim.setColor(Color.parseColor("#1B2631")); rim.setStyle(Paint.Style.STROKE); rim.setStrokeWidth(2.4f); rim.setStrokeCap(Paint.Cap.ROUND);
  lens.setColor(Color.WHITE); lens.setAlpha(45);
  gold.setColor(Color.parseColor("#F4D03F")); goldLine.setColor(Color.parseColor("#F4D03F")); goldLine.setStyle(Paint.Style.STROKE); goldLine.setStrokeWidth(2f); goldLine.setStrokeCap(Paint.Cap.ROUND);
  band.setColor(Color.parseColor("#566573")); band.setStyle(Paint.Style.STROKE); band.setStrokeWidth(5f); band.setStrokeCap(Paint.Cap.ROUND);
  cup.setColor(Color.parseColor("#E74C3C"));
  green.setColor(Color.parseColor("#2ECC71")); green.setStyle(Paint.Style.STROKE); green.setStrokeWidth(3f); green.setTypeface(Typeface.MONOSPACE);
  star.setColor(Color.parseColor("#FDFEFE")); star.setStyle(Paint.Style.STROKE); star.setStrokeWidth(1.8f); star.setStrokeCap(Paint.Cap.ROUND);
  sym.setColor(Color.parseColor("#D6EAF8")); sym.setTextAlign(Paint.Align.CENTER); sym.setTypeface(Typeface.DEFAULT_BOLD); sym.setTextSize(13f);
  doc.setColor(Color.WHITE); docLine.setColor(Color.parseColor("#85929E")); docLine.setStyle(Paint.Style.STROKE); docLine.setStrokeWidth(1.2f);
  aura.setStyle(Paint.Style.STROKE); aura.setStrokeWidth(3.2f); aura.setStrokeCap(Paint.Cap.ROUND);
  shield.setStyle(Paint.Style.STROKE); shield.setStrokeWidth(2f); shield.setColor(Color.parseColor("#5DADE2")); shield.setAlpha(70);
  orbitStar.setColor(Color.parseColor("#F7DC6F")); orbitStar.setStyle(Paint.Style.STROKE); orbitStar.setStrokeWidth(2f); orbitStar.setStrokeCap(Paint.Cap.ROUND);
 }
 static void draw(SlimeView v,Canvas cv,float cx,float cy,float rx,float ry,boolean run){
  float t=SystemClock.uptimeMillis()/1000f;
  float sh=run?v.face*4f:0f;
  float ex1=cx-17+sh, ex2=cx+17+sh, ey=cy-4;
  boolean study=Modes.is("study"), focus=Modes.is("focus"), hacker=Modes.is("hacker"), game=Modes.is("game");
  if(phones){
   rr.set(cx-rx-1,cy-ry-4,cx+rx+1,cy+ry*0.6f); cv.drawArc(rr,180f,180f,false,band);
   rr.set(cx-rx-7,cy-8,cx-rx+2,cy+10); cv.drawRoundRect(rr,4f,4f,cup);
   rr.set(cx+rx-2,cy-8,cx+rx+7,cy+10); cv.drawRoundRect(rr,4f,4f,cup);
  }
  if(crown){
   float yb=cy-ry+3; pth.reset();
   pth.moveTo(cx-17,yb); pth.lineTo(cx-17,yb-12); pth.lineTo(cx-9,yb-5); pth.lineTo(cx,yb-16); pth.lineTo(cx+9,yb-5); pth.lineTo(cx+17,yb-12); pth.lineTo(cx+17,yb); pth.close();
   cv.drawPath(pth,gold);
   cv.drawCircle(cx,yb-14,2f,cup); cv.drawCircle(cx-17,yb-11,1.6f,cup); cv.drawCircle(cx+17,yb-11,1.6f,cup);
  } else if(cap||study){
   float y0=cy-ry-2; pth.reset();
   pth.moveTo(cx-24,y0); pth.lineTo(cx,y0-9); pth.lineTo(cx+24,y0); pth.lineTo(cx,y0+9); pth.close(); cv.drawPath(pth,dk);
   rr.set(cx-11,y0+3,cx+11,y0+12); cv.drawRoundRect(rr,3f,3f,dk);
   cv.drawLine(cx+20,y0,cx+22,y0+13,goldLine); cv.drawCircle(cx+22,y0+14,2.2f,gold);
  } else if(game){
   rr.set(cx-17,cy-ry-8,cx+17,cy-ry+4); cv.drawRoundRect(rr,5f,5f,dk);
   cv.drawLine(cx-13,cy-ry-2,cx-5,cy-ry-2,star); cv.drawLine(cx-9,cy-ry-6,cx-9,cy-ry+2,star);
   cv.drawCircle(cx+8,cy-ry-3,2f,cup); cv.drawCircle(cx+13,cy-ry+1,2f,green);
  }
  if(glasses||study){
   cv.drawCircle(ex1,ey+2,12f,lens); cv.drawCircle(ex1,ey+2,12f,rim);
   cv.drawCircle(ex2,ey+2,12f,lens); cv.drawCircle(ex2,ey+2,12f,rim);
   cv.drawLine(ex1+11,ey,ex2-11,ey,rim);
   cv.drawLine(ex1-12,ey,ex1-18,ey-2,rim); cv.drawLine(ex2+12,ey,ex2+18,ey-2,rim);
  }
  if(focus){
   cv.drawLine(ex1-12,ey-15,ex1+9,ey-8,rim); cv.drawLine(ex2-9,ey-8,ex2+12,ey-15,rim);
  }
  if(hacker){
   green.setAlpha((int)(120+60*Math.sin(t*3f)));
   rr.set(cx-rx-3,cy-ry-3,cx+rx+3,cy+ry+3); cv.drawOval(rr,green);
   green.setAlpha(255); green.setStyle(Paint.Style.FILL); green.setTextSize(13f);
   cv.drawText(((t*2f)%2f<1f)?">_":">",cx-rx+2,cy-ry+9,green);
   green.setStyle(Paint.Style.STROKE);
  }
  if(sparkle){
   for(int i=0;i<3;i++){
    float a=t*0.9f+i*2.09f;
    float x=cx+(rx+12)*(float)Math.cos(a), y=cy-8+(ry+8)*(float)Math.sin(a)*0.8f;
    star.setAlpha((int)(120+120*Math.abs(Math.sin(t*3f+i))));
    cv.drawLine(x-4,y,x+4,y,star); cv.drawLine(x,y-4,x,y+4,star);
   }
   star.setAlpha(255);
  }
  if(formulas){
   for(int i=0;i<4;i++){
    float a=t*0.5f+i*1.57f;
    float x=cx+(rx+20)*(float)Math.cos(a), y=cy-ry*0.2f+(ry*0.7f)*(float)Math.sin(a);
    sym.setAlpha(170); cv.drawText(SYM[i],x,y,sym);
   }
   sym.setAlpha(255);
  }
  // tier form changes: sage aura (6), archon shield (7), celestial orbit (8)
  if(level>=6){
   aura.setColor(level>=8?Color.parseColor("#F5B041"):Color.parseColor("#9B59B6"));
   aura.setAlpha((int)(90+70*Math.abs(Math.sin(t*1.6f))));
   float rad=rx+10f+2f*(float)Math.sin(t*1.6f);
   cv.drawCircle(cx,cy,rad,aura);
  }
  if(level>=7){
   rr.set(cx-rx-7,cy-ry-7,cx+rx+7,cy+ry+9);
   shield.setAlpha((int)(55+35*Math.abs(Math.sin(t*1.2f))));
   cv.drawOval(rr,shield);
  }
  if(level>=8){
   for(int i=0;i<3;i++){
    float a=t*0.8f+i*2.09f;
    float x=cx+(rx+15f)*(float)Math.cos(a), y=cy+(ry+9f)*(float)Math.sin(a);
    orbitStar.setAlpha((int)(140+100*Math.abs(Math.sin(t*3f+i))));
    cv.drawLine(x-3f,y,x+3f,y,orbitStar); cv.drawLine(x,y-3f,x,y+3f,orbitStar);
   }
   orbitStar.setAlpha(255);
  }
  if(Pouch.count()>0){
   float x=cx+rx+4, y=cy+24;
   rr.set(x-7,y-9,x+7,y+9); cv.drawRoundRect(rr,2f,2f,doc);
   cv.drawLine(x-4,y-3,x+4,y-3,docLine); cv.drawLine(x-4,y+1,x+4,y+1,docLine); cv.drawLine(x-4,y+5,x+1,y+5,docLine);
   if(Pouch.count()>1){ sym.setColor(Color.parseColor("#1B2631")); cv.drawText(""+Pouch.count(),x,y-11,sym); sym.setColor(Color.parseColor("#D6EAF8")); }
  }
 }
}
