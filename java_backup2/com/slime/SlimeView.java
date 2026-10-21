package com.slime;
import android.animation.ValueAnimator; import android.content.Context; import android.graphics.*; import android.os.Handler; import android.view.View;
public class SlimeView extends View {
 public int mood=0; String bubble=""; float breathe=0f;
 Paint body=new Paint(Paint.ANTI_ALIAS_FLAG), dark=new Paint(Paint.ANTI_ALIAS_FLAG), shine=new Paint(Paint.ANTI_ALIAS_FLAG), white=new Paint(Paint.ANTI_ALIAS_FLAG), black=new Paint(Paint.ANTI_ALIAS_FLAG), blush=new Paint(Paint.ANTI_ALIAS_FLAG), line=new Paint(Paint.ANTI_ALIAS_FLAG), fill=new Paint(Paint.ANTI_ALIAS_FLAG), tear=new Paint(Paint.ANTI_ALIAS_FLAG), shadow=new Paint(Paint.ANTI_ALIAS_FLAG), bub=new Paint(Paint.ANTI_ALIAS_FLAG), edge=new Paint(Paint.ANTI_ALIAS_FLAG), tp=new Paint(Paint.ANTI_ALIAS_FLAG), heartP=new Paint(Paint.ANTI_ALIAS_FLAG);
 RectF r=new RectF(); Path path=new Path(); Handler h=new Handler(); Runnable clearBubble; ValueAnimator idle;
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
  startIdle();
 }
 void startIdle(){
  idle=ValueAnimator.ofFloat(0f,1f); idle.setDuration(1600); idle.setRepeatCount(ValueAnimator.INFINITE); idle.setRepeatMode(ValueAnimator.REVERSE);
  idle.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){ public void onAnimationUpdate(ValueAnimator a){ breathe=((Float)a.getAnimatedValue()).floatValue(); invalidate(); }});
  idle.start();
 }
 @Override protected void onDetachedFromWindow(){ super.onDetachedFromWindow(); if(idle!=null) idle.cancel(); }
 int bodyColor(){
  switch(mood){ case 2: return Color.parseColor("#E8756A"); case 8: return Color.parseColor("#9AA5B1"); case 1: return Color.parseColor("#85C1E9"); default: return Color.parseColor("#5DADE2"); }
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
 void drawEyes(Canvas cv,float ex1,float ex2,float ey){
  switch(mood){
   case 1: closed(cv,ex1,ey); closed(cv,ex2,ey); break;
   case 2: eye(cv,ex1,ey,9f,4.5f); eye(cv,ex2,ey,9f,4.5f); cv.drawLine(ex1-12,ey-16,ex1+8,ey-9,line); cv.drawLine(ex2-8,ey-9,ex2+12,ey-16,line); break;
   case 3: heart(cv,ex1,ey,7f,heartP); heart(cv,ex2,ey,7f,heartP); break;
   case 5: eye(cv,ex1,ey,10f,5f); eye(cv,ex2,ey,10f,5f); float ty=ey+14+breathe*8; r.set(ex1-3,ty,ex1+3,ty+9); cv.drawOval(r,tear); r.set(ex2-3,ty,ex2+3,ty+9); cv.drawOval(r,tear); break;
   case 6: eye(cv,ex1,ey,12f,3f); eye(cv,ex2,ey,12f,3f); break;
   case 7: eye(cv,ex1,ey,10f,5f); closed(cv,ex2,ey); break;
   case 8: cross(cv,ex1,ey); cross(cv,ex2,ey); break;
   default: eye(cv,ex1,ey,10f,5f); eye(cv,ex2,ey,10f,5f);
  }
 }
 void drawMouth(Canvas cv,float cx,float my){
  switch(mood){
   case 1: r.set(cx-4,my-2,cx+4,my+8); cv.drawOval(r,fill); break;
   case 2: case 5: r.set(cx-10,my+2,cx+10,my+16); cv.drawArc(r,200f,140f,false,line); break;
   case 4: r.set(cx-9,my-4,cx+9,my+10); cv.drawOval(r,fill); break;
   case 6: cv.drawCircle(cx,my+3,7f,fill); break;
   case 8: cv.drawLine(cx-10,my+4,cx+10,my+4,line); break;
   default: r.set(cx-12,my-8,cx+12,my+8); cv.drawArc(r,20f,140f,false,line);
  }
 }
 @Override protected void onDraw(Canvas cv){
  int w=getWidth(), hh=getHeight(); if(w==0) return;
  float cx=w/2f, cy=hh-64f; float sq=breathe*4f; float rx=50f+sq, ry=46f-sq;
  int bc=bodyColor(); body.setColor(bc); dark.setColor(darker(bc));
  r.set(cx-40,hh-16,cx+40,hh-4); cv.drawOval(r,shadow);
  r.set(cx-rx,cy-ry+8,cx+rx,cy+ry+8); cv.drawOval(r,dark);
  r.set(cx-rx,cy-ry,cx+rx,cy+ry); cv.drawOval(r,body);
  r.set(cx-32,cy-ry+7,cx-14,cy-ry+19); cv.drawOval(r,shine);
  drawEyes(cv,cx-17,cx+17,cy-6);
  drawMouth(cv,cx,cy+17);
  if(mood==0||mood==3||mood==7){ r.set(cx-36,cy+8,cx-24,cy+16); cv.drawOval(r,blush); r.set(cx+24,cy+8,cx+36,cy+16); cv.drawOval(r,blush); }
  if(mood==1){ tp.setTextSize(14f+breathe*6f); cv.drawText("z",cx+40,cy-38,tp); }
  if(bubble!=null&&!bubble.isEmpty()){
   float size=20f; tp.setTextSize(size);
   while(tp.measureText(bubble)>w-28f&&size>10f){ size-=1f; tp.setTextSize(size); }
   float bw=tp.measureText(bubble)+22f; float left=(w-bw)/2f;
   r.set(left,2,left+bw,34); cv.drawRoundRect(r,14f,14f,bub); cv.drawRoundRect(r,14f,14f,edge);
   cv.drawText(bubble,w/2f,24f,tp);
  }
 }
 public void setMood(int m){ mood=m; invalidate(); }
 public void say(String s){
  if(s==null) s=""; s=s.replace("\n"," ").trim();
  bubble=s.length()>24?s.substring(0,23)+"...":s; invalidate();
  if(clearBubble!=null) h.removeCallbacks(clearBubble);
  clearBubble=new Runnable(){ public void run(){ bubble=""; invalidate(); }};
  h.postDelayed(clearBubble,2800);
 }
 public void bounceAnim(){ animate().scaleX(1.2f).scaleY(0.8f).setDuration(120).withEndAction(new Runnable(){ public void run(){ animate().scaleX(1f).scaleY(1f).setDuration(120).start(); }}).start(); }
}
