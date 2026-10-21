package com.slime;
import android.animation.ValueAnimator; import android.content.Context; import android.graphics.*; import android.os.Handler; import android.os.SystemClock; import android.view.View; import android.view.ViewGroup; import android.view.WindowManager;
import java.util.ArrayList;
public class SlimeView extends View {
 static final float S=0.75f;
 public int mood=0; String bubble=""; float breathe=0f;
 Paint body=new Paint(Paint.ANTI_ALIAS_FLAG), dark=new Paint(Paint.ANTI_ALIAS_FLAG), shine=new Paint(Paint.ANTI_ALIAS_FLAG), white=new Paint(Paint.ANTI_ALIAS_FLAG), black=new Paint(Paint.ANTI_ALIAS_FLAG), blush=new Paint(Paint.ANTI_ALIAS_FLAG), line=new Paint(Paint.ANTI_ALIAS_FLAG), fill=new Paint(Paint.ANTI_ALIAS_FLAG), tear=new Paint(Paint.ANTI_ALIAS_FLAG), shadow=new Paint(Paint.ANTI_ALIAS_FLAG), bub=new Paint(Paint.ANTI_ALIAS_FLAG), edge=new Paint(Paint.ANTI_ALIAS_FLAG), tp=new Paint(Paint.ANTI_ALIAS_FLAG), heartP=new Paint(Paint.ANTI_ALIAS_FLAG), zzz=new Paint(Paint.ANTI_ALIAS_FLAG), limb=new Paint(Paint.ANTI_ALIAS_FLAG), limbFill=new Paint(Paint.ANTI_ALIAS_FLAG);
 RectF r=new RectF(); Path path=new Path(); Handler h=new Handler(); Runnable clearBubble; ValueAnimator idle;
 float lastX=Float.NaN, lastY=0f; long runUntil=0; int face=1; int[] loc=new int[2];
 public SlimeView(Context c){ super(c);
  shine.setColor(Color.WHITE); shine.setAlpha(110);
  white.setColor(Color.WHITE); black.setColor(Color.parseColor("#1B2631"));
  blush.setColor(Color.parseColor("#FF8ABF")); blush.setAlpha(130);
  line.setColor(Color.parseColor("#1B2631")); line.setStyle(Paint.Style.STROKE); line.setStrokeWidth(3.5f); line.setStrokeCap(Paint.Cap.ROUND);
  fill.setColor(Color.parseColor("#7B241C")); tear.setColor(Color.parseColor("#AED6F1"));
  shadow.setColor(Color.BLACK); shadow.setAlpha(40);
  bub.setColor(Color.WHITE); edge.setColor(Color.parseColor("#C8D6E5")); edge.setStyle(Paint.Style.STROKE); edge.setStrokeWidth(2f);
  tp.setColor(Color.parseColor("#1B2631")); tp.setTextAlign(Paint.Align.CENTER); tp.setTypeface(Typeface.DEFAULT_BOLD);
  heartP.setColor(Color.parseColor("#E74C3C"));
  zzz.setColor(Color.parseColor("#85C1E9")); zzz.setTextAlign(Paint.Align.CENTER); zzz.setTypeface(Typeface.DEFAULT_BOLD);
  limb.setStyle(Paint.Style.STROKE); limb.setStrokeCap(Paint.Cap.ROUND); limb.setStrokeWidth(9f);
  limbFill.setStyle(Paint.Style.FILL);
  startIdle();
 }
 void startIdle(){
  if(idle!=null) idle.cancel();
  idle=ValueAnimator.ofFloat(0f,1f); idle.setDuration(1600); idle.setRepeatCount(ValueAnimator.INFINITE); idle.setRepeatMode(ValueAnimator.REVERSE);
  idle.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){ public void onAnimationUpdate(ValueAnimator a){ breathe=((Float)a.getAnimatedValue()).floatValue(); invalidate(); }});
  idle.start();
 }
 @Override protected void onAttachedToWindow(){ super.onAttachedToWindow(); if(idle==null||!idle.isRunning()) startIdle(); }
 @Override protected void onDetachedFromWindow(){ super.onDetachedFromWindow(); if(idle!=null) idle.cancel(); lastX=Float.NaN; }
 // Watches the overlay window position; if it moved recently the slime runs and faces the way it is going.
 void trackMove(){
  float x,y; ViewGroup.LayoutParams lp=getLayoutParams();
  if(lp instanceof WindowManager.LayoutParams){ x=((WindowManager.LayoutParams)lp).x; y=((WindowManager.LayoutParams)lp).y; }
  else { getLocationOnScreen(loc); x=loc[0]; y=loc[1]; }
  if(!Float.isNaN(lastX)){
   float dx=x-lastX, dy=y-lastY;
   if(Math.abs(dx)+Math.abs(dy)>=2f){ runUntil=SystemClock.uptimeMillis()+300; if(Math.abs(dx)>=1f) face=dx>0?1:-1; }
  }
  lastX=x; lastY=y;
 }
 public boolean isRunning(){ return SystemClock.uptimeMillis()<runUntil; }
 int bodyColor(){ int mc=Modes.tint(mood); if(mc!=0) return mc;
  switch(mood){ case 2: return Color.parseColor("#E8756A"); case 8: return Color.parseColor("#9AA5B1"); case 1: return Color.parseColor("#85C1E9"); case 4: return Color.parseColor("#F5B041"); default: return Color.parseColor("#5DADE2"); }
 }
 int darker(int c){ return Color.rgb((int)(Color.red(c)*0.8f),(int)(Color.green(c)*0.8f),(int)(Color.blue(c)*0.8f)); }
 void eye(Canvas cv,float x,float y,float R,float pr){ cv.drawCircle(x,y,R,white); cv.drawCircle(x,y+2,pr,black); }
 void closed(Canvas cv,float x,float y){ cv.drawLine(x-8,y+2,x+8,y+2,line); }
 void cross(Canvas cv,float x,float y){ cv.drawLine(x-7,y-7,x+7,y+7,line); cv.drawLine(x-7,y+7,x+7,y-7,line); }
 void heart(Canvas cv,float x,float y,float s,Paint p){
  path.reset(); path.moveTo(x,y+s*0.9f);
  path.cubicTo(x-s*1.4f,y-s*0.2f,x-s*0.5f,y-s*1.2f,x,y-s*0.3f);
  path.cubicTo(x+s*0.5f,y-s*1.2f,x+s*1.4f,y-s*0.2f,x,y+s*0.9f);
  path.close(); cv.drawPath(path,p);
 }
 void legs(Canvas cv,float cx,float cy,float ry,float ground,boolean run,float phase){
  limb.setStrokeWidth(9f);
  for(int s=-1;s<=1;s+=2){
   float ph=phase+(s<0?0f:(float)Math.PI);
   float hx=cx+s*16f, hy=cy+ry-6f;
   float fx=hx, fy=ground-3f;
   if(run){ fx+=(float)Math.sin(ph)*11f; fy-=Math.max(0f,(float)Math.cos(ph))*7f; }
   cv.drawLine(hx,hy,fx,fy,limb); cv.drawCircle(fx,fy,6f,limbFill);
  }
 }
 void arms(Canvas cv,float cx,float cy,float rx,boolean run,float phase){
  limb.setStrokeWidth(8f);
  for(int s=-1;s<=1;s+=2){
   float sx=cx+s*(rx-2f), sy=cy+4f;
   float hx=cx+s*(rx+7f), hy=cy+20f+breathe*2f;
   if(run){
    float ph=phase+(s<0?(float)Math.PI:0f);
    hx=sx+s*4f+face*(float)Math.sin(ph)*8f; hy=cy+14f-(float)Math.cos(ph)*6f;
   }
   cv.drawLine(sx,sy,hx,hy,limb); cv.drawCircle(hx,hy,6.5f,limbFill);
  }
 }
 void drawEyes(Canvas cv,float ex1,float ex2,float ey){
  switch(mood){
   case 1: closed(cv,ex1,ey); closed(cv,ex2,ey); break;
   case 2: eye(cv,ex1,ey,9f,4.5f); eye(cv,ex2,ey,9f,4.5f); cv.drawLine(ex1-12,ey-16,ex1+8,ey-9,line); cv.drawLine(ex2-8,ey-9,ex2+12,ey-16,line); break;
   case 3: heart(cv,ex1,ey,7f,heartP); heart(cv,ex2,ey,7f,heartP); break;
   case 5: { eye(cv,ex1,ey,10f,5f); eye(cv,ex2,ey,10f,5f); float ty=ey+14+breathe*8; r.set(ex1-3,ty,ex1+3,ty+9); cv.drawOval(r,tear); r.set(ex2-3,ty,ex2+3,ty+9); cv.drawOval(r,tear); break; }
   case 6: eye(cv,ex1,ey,12f,3f); eye(cv,ex2,ey,12f,3f); break;
   case 7: eye(cv,ex1,ey,10f,5f); closed(cv,ex2,ey); break;
   case 8: cross(cv,ex1,ey); cross(cv,ex2,ey); break;
   default: eye(cv,ex1,ey,10f,5f); eye(cv,ex2,ey,10f,5f);
  }
 }
 void drawMouth(Canvas cv,float cx,float my){
  if(Mouth.talking(mood)){ Mouth.draw(this,cv,cx,my); return; }
  switch(mood){
   case 1: r.set(cx-4,my-2,cx+4,my+8); cv.drawOval(r,fill); break;
   case 2: case 5: r.set(cx-10,my+2,cx+10,my+16); cv.drawArc(r,200f,140f,false,line); break;
   case 4: r.set(cx-9,my-4,cx+9,my+10); cv.drawOval(r,fill); break;
   case 6: cv.drawCircle(cx,my+3,7f,fill); break;
   case 8: cv.drawLine(cx-10,my+4,cx+10,my+4,line); break;
   default: r.set(cx-12,my-8,cx+12,my+8); cv.drawArc(r,20f,140f,false,line);
  }
 }
 ArrayList<String> wrap(String s,float maxW){
  ArrayList<String> all=new ArrayList<String>(); String cur="";
  for(String wd: s.split(" ")){
   if(wd.isEmpty()) continue;
   String t=cur.isEmpty()?wd:cur+" "+wd;
   if(tp.measureText(t)<=maxW){ cur=t; }
   else { if(!cur.isEmpty()) all.add(cur); cur=wd; while(cur.length()>1&&tp.measureText(cur)>maxW) cur=cur.substring(0,cur.length()-1); }
  }
  if(!cur.isEmpty()) all.add(cur);
  if(all.size()>2){ String l2=all.get(1)+"..."; while(l2.length()>4&&tp.measureText(l2)>maxW) l2=l2.substring(0,l2.length()-4)+"..."; ArrayList<String> two=new ArrayList<String>(); two.add(all.get(0)); two.add(l2); return two; }
  return all;
 }
 @Override protected void onDraw(Canvas cv){
  if(getWidth()==0) return;
  trackMove();
  boolean run=isRunning();
  float phase=SystemClock.uptimeMillis()/1000f*14f;
  int w=(int)(getWidth()/S), hh=(int)(getHeight()/S);
  cv.save(); cv.scale(S,S);
  float ground=hh-6f;
  float bob=run?Math.abs((float)Math.sin(phase))*5f:0f;
  float cx=w/2f, cy=hh-72f-bob;
  float sq=breathe*4f+Pouch.chub();
  float rx=50f+sq, ry=46f-sq;
  int bc=bodyColor(); body.setColor(bc); dark.setColor(darker(bc)); limb.setColor(darker(bc)); limbFill.setColor(darker(bc));
  r.set(cx-40+bob,ground-9,cx+40-bob,ground+1); cv.drawOval(r,shadow);
  cv.save();
  if(run) cv.rotate(face*7f,cx,ground);
  legs(cv,cx,cy,ry,ground,run,phase);
  r.set(cx-rx,cy-ry+8,cx+rx,cy+ry+8); cv.drawOval(r,dark);
  r.set(cx-rx,cy-ry,cx+rx,cy+ry); cv.drawOval(r,body);
  r.set(cx-32,cy-ry+7,cx-14,cy-ry+19); cv.drawOval(r,shine);
  cv.save(); cv.translate(run?face*4f:0f,0f);
  drawEyes(cv,cx-17,cx+17,cy-6);
  drawMouth(cv,cx,cy+17);
  cv.restore();
  if(mood==0||mood==3||mood==7){ r.set(cx-36,cy+8,cx-24,cy+16); cv.drawOval(r,blush); r.set(cx+24,cy+8,cx+36,cy+16); cv.drawOval(r,blush); }
  arms(cv,cx,cy,rx,run,phase); Evo.draw(this,cv,cx,cy,rx,ry,run);
  cv.restore();
  if(mood==1){ tp.setTextSize(14f+breathe*6f); cv.drawText("z",cx+40,cy-38,tp); zzz.setTextSize(10f+breathe*4f); cv.drawText("z",cx+52,cy-52,zzz); }
  Mouth.notes(this,cv,cx,cy);
  if(bubble!=null&&!bubble.isEmpty()){
   tp.setTextSize(22f);
   ArrayList<String> ls=wrap(bubble,w-36f);
   if(!ls.isEmpty()){
    float mw=0f; for(String l:ls){ float m=tp.measureText(l); if(m>mw) mw=m; }
    float bw=mw+22f, bh=12f+ls.size()*26f; float left=(w-bw)/2f;
    r.set(left,2,left+bw,2+bh); cv.drawRoundRect(r,14f,14f,bub); cv.drawRoundRect(r,14f,14f,edge);
    for(int i=0;i<ls.size();i++) cv.drawText(ls.get(i),w/2f,26f+i*26f,tp);
   }
  }
  cv.restore();
 }
 public void setMood(int m){ mood=m; invalidate(); }
 public void say(String s){
  if(s==null) s=""; s=s.replace("\n"," ").trim(); s=MathText.plain(s);
  bubble=s.length()>90?s.substring(0,(s.lastIndexOf(32,87)>30?s.lastIndexOf(32,87):87))+"...":s; invalidate();
  if(clearBubble!=null) h.removeCallbacks(clearBubble);
  clearBubble=new Runnable(){ public void run(){ bubble=""; invalidate(); }};
  h.postDelayed(clearBubble,Math.max(2800,1500+bubble.length()*60));
  Mouth.talkFor(Math.min(5000L,600L+bubble.length()*55L),false);
 }
 public void bounceAnim(){
  animate().scaleX(1.2f).scaleY(0.8f).setDuration(120).withEndAction(new Runnable(){ public void run(){ animate().scaleX(1f).scaleY(1f).setDuration(120).start(); }}).start();
 }
}
