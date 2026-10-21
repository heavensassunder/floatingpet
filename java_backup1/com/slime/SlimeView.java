package com.slime;
import android.content.Context; import android.graphics.*; import android.view.View; import android.os.Handler;
public class SlimeView extends View {
 int mood=0; String bubble=""; Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); Handler h=new Handler(); Runnable clearBubble;
 public SlimeView(Context c){ super(c); p.setColor(Color.parseColor("#6EC8FF")); }
 @Override protected void onDraw(Canvas canvas){
  int w=getWidth(), hh=getHeight();
  canvas.drawCircle(w/2, hh/2+10, 48, p);
  Paint white=new Paint(Paint.ANTI_ALIAS_FLAG); white.setColor(Color.WHITE);
  canvas.drawCircle(w/2-12, hh/2, 10, white); canvas.drawCircle(w/2+12, hh/2, 10, white);
  Paint black=new Paint(Paint.ANTI_ALIAS_FLAG); black.setColor(Color.BLACK);
  canvas.drawCircle(w/2-12, hh/2+2, 5, black); canvas.drawCircle(w/2+12, hh/2+2, 5, black);
  if(mood==3){ Paint love=new Paint(); love.setColor(Color.parseColor("#FF8ABF")); love.setAlpha(120); canvas.drawCircle(w/2-22, hh/2+18, 8, love); canvas.drawCircle(w/2+22, hh/2+18, 8, love); }
  if(bubble!=null && !bubble.isEmpty()){ Paint tp=new Paint(); tp.setColor(Color.BLACK); tp.setTextSize(22); tp.setFakeBoldText(true); canvas.drawText(bubble, 5, 22, tp); }
 }
 public void setMood(int m){ mood=m; invalidate(); }
 public void say(String s){ bubble=s.length()>26?s.substring(0,26):s; invalidate(); if(clearBubble!=null) h.removeCallbacks(clearBubble); clearBubble=new Runnable(){ public void run(){ bubble=""; invalidate(); }}; h.postDelayed(clearBubble,2500); }
 public void bounceAnim(){ animate().scaleX(1.2f).scaleY(0.8f).setDuration(120).withEndAction(new Runnable(){ public void run(){ animate().scaleX(1f).scaleY(1f).setDuration(120).start(); }}).start(); }
}
