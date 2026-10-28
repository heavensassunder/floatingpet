package com.slime;
import android.graphics.Rect;
// Watches where the pet REALLY is. If any part of it is outside the usable screen it is pulled back in.
public class Guard implements Runnable {
 final MainService s; final int[] loc=new int[2]; int lx=Integer.MIN_VALUE, ly=Integer.MIN_VALUE;
 Guard(MainService s){ this.s=s; }
 public void run(){
  try{ check(); }catch(Exception e){ L.w("Guard",e); }
  if(MainService.instance==s) s.h.postDelayed(this,800);
 }
 void check(){
  if(s.slimeView==null||!s.slimeView.isAttachedToWindow()) return;
  if(Math.abs(s.slimeView.getScaleX()-1f)>0.02f||Math.abs(s.slimeView.getScaleY()-1f)>0.02f) return;
  boolean moving=(s.wander!=null&&s.wander.isRunning())||s.p1.x!=lx||s.p1.y!=ly;
  lx=s.p1.x; ly=s.p1.y;
  if(moving) return;
  s.slimeView.getLocationOnScreen(loc);
  Rect a=Walls.area(s);
  int bl=loc[0]+Math.round(Walls.bl()), br=loc[0]+Math.round(Walls.br()), bt=loc[1]+Math.round(Walls.bt()), bb=loc[1]+Math.round(Walls.bb());
  if(bl>a.left+60&&br<a.right-60&&bt>a.top+60&&bb<a.bottom-60){
   int ox=loc[0]-s.p1.x, oy=loc[1]-s.p1.y;
   if(Math.abs(ox)<60&&Math.abs(oy)<60){ Walls.offX=ox; Walls.offY=oy; }
  }
  int dx=0, dy=0;
  if(bl<a.left-2) dx=a.left-bl; else if(br>a.right+2) dx=a.right-br;
  if(bt<a.top-2) dy=a.top-bt; else if(bb>a.bottom+2) dy=a.bottom-bb;
  if(dx!=0||dy!=0){
   if(s.wander!=null) s.wander.cancel();
   s.p1.x+=dx; s.p1.y+=dy; s.updateViews();
   s.sp.edit().putInt("x",s.p1.x).putInt("y",s.p1.y).apply();
  }
 }
}
