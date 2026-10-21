package com.slime;
import android.graphics.Canvas; import android.graphics.Color; import android.graphics.Paint; import android.graphics.Path; import android.graphics.RectF; import android.graphics.Typeface; import android.os.SystemClock;
public class Evo {
 static boolean glasses,cap,phones,sparkle,formulas,crown; static int level=1;
 static final String[] NAMES={"baby slime","slimelet","slime cadet","slime knight","slime king"};
 static final int[] NEED={0,60,160,340,640};
 static final String[] CODE={"code","script","bug","error","compile","java","python","termux","function","patch","apk","build"};
 static final String[] MATH={"integral","derivative","equation","solve","math","circuit","voltage","current","resistor","matrix","calculus","formula","laplace","fourier"};
 static final String[] STUDY={"exam","test","assignment","homework","lecture","notes","revision","study","eet"};
 static final String[] MUSIC={"song","music","hum","sing","playlist"};
 static final String[] FIX={"fixed","it works","works now","solved"};
 static final String[] GAME={"game","gaming","pubg","fifa","minecraft"};
 static boolean any(String l,String[] k){ for(String w:k){ if(l.contains(w)) return true; } return false; }
 static int lvl(int xp){ int l=1; for(int i=0;i<NEED.length;i++){ if(xp>=NEED[i]) l=i+1; } return l; }
 static void init(MainService s){ refresh(s); }
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
 static void count(MainService s,String low){
  try{
   int oldLevel=level; boolean og=glasses,oc=cap,op=phones,os=sparkle;
   int add=1;
   if(any(low,CODE)){ bump(s,"c_code",1); add+=2; }
   if(any(low,MATH)){ bump(s,"c_math",1); add+=2; }
   if(any(low,STUDY)){ bump(s,"c_study",1); add+=1; }
   if(any(low,MUSIC)) bump(s,"c_music",1);
   if(any(low,FIX)){ bump(s,"c_fix",1); add+=3; }
   if(any(low,GAME)) bump(s,"c_game",1);
   bump(s,"xp",add);
   refresh(s);
   if(level>oldLevel) s.react("level up! I'm a "+NAMES[level-1]+" now",3,6000);
   else if((glasses&&!og)||(cap&&!oc)||(phones&&!op)||(sparkle&&!os)) s.react("ooh, I got a new accessory!",7,5000);
  }catch(Exception e){}
 }
 static String line(MainService s){
  int xp=s.sp.getInt("xp",0); refresh(s);
  String nx=level>=5?"max":(xp+"/"+NEED[level]);
  return "lvl "+level+" "+NAMES[level-1]+" (xp "+nx+") | ai "+OpenRouterClient.budget()+" today";
 }
 static boolean handle(MainService s,String low){
  low=low.trim();
  if(low.equals("evolution")||low.equals("evo")||low.equals("level")){
   refresh(s);
   StringBuilder sb=new StringBuilder(line(s)).append("\n");
   sb.append("code chats: ").append(s.sp.getInt("c_code",0)).append(glasses?"  (glasses unlocked)":"  (25 for glasses)").append("\n");
   sb.append("math/study chats: ").append(s.sp.getInt("c_math",0)).append("/").append(s.sp.getInt("c_study",0)).append(cap?"  (grad cap unlocked)":"  (15 math or 20 study for the cap)").append("\n");
   sb.append("music chats: ").append(s.sp.getInt("c_music",0)).append(phones?"  (headphones unlocked)":"  (12 for headphones)").append("\n");
   sb.append("bugs fixed: ").append(s.sp.getInt("c_fix",0)).append(sparkle?"  (sparkles unlocked)":"  (8 for sparkles)").append("\n");
   sb.append(crown?"crown unlocked!":"level 5 gets a crown");
   s.showChat(); s.local(sb.toString(),false); return true;
  }
  return false;
 }
 // ---------- drawing ----------
 static final Paint dk=new Paint(Paint.ANTI_ALIAS_FLAG), rim=new Paint(Paint.ANTI_ALIAS_FLAG), lens=new Paint(Paint.ANTI_ALIAS_FLAG), gold=new Paint(Paint.ANTI_ALIAS_FLAG), goldLine=new Paint(Paint.ANTI_ALIAS_FLAG);
 static final Paint band=new Paint(Paint.ANTI_ALIAS_FLAG), cup=new Paint(Paint.ANTI_ALIAS_FLAG), green=new Paint(Paint.ANTI_ALIAS_FLAG), star=new Paint(Paint.ANTI_ALIAS_FLAG), sym=new Paint(Paint.ANTI_ALIAS_FLAG), doc=new Paint(Paint.ANTI_ALIAS_FLAG), docLine=new Paint(Paint.ANTI_ALIAS_FLAG);
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
  if(Pouch.count()>0){
   float x=cx+rx+4, y=cy+24;
   rr.set(x-7,y-9,x+7,y+9); cv.drawRoundRect(rr,2f,2f,doc);
   cv.drawLine(x-4,y-3,x+4,y-3,docLine); cv.drawLine(x-4,y+1,x+4,y+1,docLine); cv.drawLine(x-4,y+5,x+1,y+5,docLine);
   if(Pouch.count()>1){ sym.setColor(Color.parseColor("#1B2631")); cv.drawText(""+Pouch.count(),x,y-11,sym); sym.setColor(Color.parseColor("#D6EAF8")); }
  }
 }
}
