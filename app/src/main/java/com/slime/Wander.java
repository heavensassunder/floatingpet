package com.slime;
import android.animation.Animator; import android.animation.AnimatorListenerAdapter; import android.animation.ValueAnimator;
import android.view.View; import android.view.animation.LinearInterpolator;
import java.util.Random;
// The slime's autonomous wandering and flee glides, rendered by moving its overlay window.
class Wander {
 final MainService s; final Random rnd=new Random(); ValueAnimator anim;
 private final Runnable tick=new Runnable(){ public void run(){ try{ step(); }catch(Exception e){ L.w("Wander",e); } s.h.postDelayed(this,6000+rnd.nextInt(9000)); }};
 Wander(MainService s){ this.s=s; }
 void start(){ s.h.postDelayed(tick,9000); }
 void destroy(){ if(anim!=null) anim.cancel(); s.h.removeCallbacks(tick); }
 void cancel(){ if(anim!=null) anim.cancel(); }
 boolean isRunning(){ return anim!=null&&anim.isRunning(); }

 void step(){
  long now=System.currentTimeMillis();
  if(!s.sp.getBoolean("wander",true)) return; // static mode: only drags, pokes and 'play' move it
  if(s.chatView.getVisibility()==View.VISIBLE||now-s.lastTouch<8000) return;
  if(anim!=null&&anim.isRunning()) return;
  if(!s.screenOn()) return;
  int sw=s.scrW();
  if(s.slimeView.mood==1){
   int tx=(s.p1.x<sw/2)?s.minX():s.maxX(); int ty=s.maxY()-100;
   if(Math.abs(s.p1.x-tx)<30&&Math.abs(s.p1.y-ty)<30) return;
   return;
  }
  if(rnd.nextInt(100)<35) return;
  int tx; int roll=rnd.nextInt(100);
  if(roll<25) tx=s.minX(); else if(roll<50) tx=s.maxX(); else tx=s.minX()+rnd.nextInt(Math.max(1,s.maxX()-s.minX()));
  int ty=s.minY()+rnd.nextInt(Math.max(1,s.maxY()-s.minY()));
  glideTo(tx,ty);
 }
 void glideRandom(){
  glideTo(s.minX()+rnd.nextInt(Math.max(1,s.maxX()-s.minX())),s.minY()+rnd.nextInt(Math.max(1,s.maxY()-s.minY())));
 }
 void glideTo(int tx0,int ty0){ glideTo(tx0,ty0,-1); }
 void glideTo(int tx0,int ty0,final long fixedMs){
  if(anim!=null) anim.cancel();
  final int tx=Math.max(s.minX(),Math.min(tx0,s.maxX()));
  final int ty=Math.max(s.minY(),Math.min(ty0,s.maxY()));
  final int sx=s.p1.x, sy=s.p1.y;
  double dist=Math.sqrt((tx-sx)*(double)(tx-sx)+(ty-sy)*(double)(ty-sy));
  anim=ValueAnimator.ofFloat(0f,1f); anim.setDuration(fixedMs>0?fixedMs:(long)Math.min(6000,300+dist*2.2)); anim.setInterpolator(new LinearInterpolator());
  anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){ public void onAnimationUpdate(ValueAnimator a){ float f=((Float)a.getAnimatedValue()).floatValue(); s.p1.x=(int)(sx+(tx-sx)*f); s.p1.y=(int)(sy+(ty-sy)*f); s.updateViews(); }});
  anim.addListener(new AnimatorListenerAdapter(){ public void onAnimationEnd(Animator a){ s.sp.edit().putInt("x",s.p1.x).putInt("y",s.p1.y).apply(); }});
  anim.start();
 }
 void fleeFrom(float tx,float ty){
  if(anim!=null) anim.cancel();
  float k=Appearance.scale(s.sp);
  int fx=s.p1.x+((s.p1.x+s.p1.width/2f)>tx?Math.round(220*k):-Math.round(220*k));
  int fy=s.p1.y+((s.p1.y+s.p1.height/2f)>ty?Math.round(170*k):-Math.round(170*k));
  glideTo(fx,fy,420); s.slimeView.bounceAnim();
  s.sp.edit().putInt("x",s.p1.x).putInt("y",s.p1.y).apply();
 }
}
