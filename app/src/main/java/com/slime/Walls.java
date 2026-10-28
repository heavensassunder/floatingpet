package com.slime;
import android.graphics.PixelFormat; import android.graphics.Rect; import android.util.DisplayMetrics;
import android.view.Gravity; import android.view.View; import android.view.ViewTreeObserver; import android.view.WindowManager;
public class Walls {
 // user pet-size multiplier (1.0 = normal); Appearance keeps it in sync
 static float petScale=1f;
 // the visible creature (body, hands, feet) inside the 195x143 base window, scaled with pet size
 static float bl(){ return (MainService.SW/2f-51f)*petScale; }
 static float br(){ return (MainService.SW/2f+51f)*petScale; }
 static float bt(){ return 50f*petScale; }
 static float bb(){ return 141f*petScale; }
 static int offX=0, offY=0;
 static Rect cache; static boolean first=true;
 // The usable screen, measured with a tiny invisible full-screen probe window, so it is in the same
 // units as the slime window (this app is scaled up by Android, so raw screen pixels were wrong).
 static Rect area(MainService s){
  if(cache!=null) return cache;
  DisplayMetrics m=s.getResources().getDisplayMetrics();
  return new Rect(0,0,m.widthPixels,m.heightPixels);
 }
 static int W(MainService s){ return area(s).right; }
 static int H(MainService s){ return area(s).bottom; }
 static int minX(MainService s){ return Math.round(area(s).left-bl()-offX); }
 static int maxX(MainService s){ return Math.round(area(s).right-br()-offX); }
 static int minY(MainService s){ return Math.round(area(s).top-bt()-offY); }
 static int maxY(MainService s){ return Math.round(area(s).bottom-bb()-offY); }
 static void reset(){ }
 static void done(MainService s){
  if(first){ first=false; recenter(s); } else { s.clampPos(); s.updateViews(); }
 }
 static void measure(final MainService s){
  if(s.wm==null) return;
  try{
   final View v=new View(s);
   WindowManager.LayoutParams lp=new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);
   lp.gravity=Gravity.TOP|Gravity.START;
   final boolean[] got={false};
   final ViewTreeObserver.OnGlobalLayoutListener[] ll=new ViewTreeObserver.OnGlobalLayoutListener[1];
   ll[0]=new ViewTreeObserver.OnGlobalLayoutListener(){ public void onGlobalLayout(){
    if(got[0]||v.getWidth()<=0||v.getHeight()<=0) return;
    got[0]=true;
    try{ v.getViewTreeObserver().removeOnGlobalLayoutListener(ll[0]); }catch(Exception e){ L.w("Walls",e); }
    int[] l=new int[2]; v.getLocationOnScreen(l);
    cache=new Rect(l[0],l[1],l[0]+v.getWidth(),l[1]+v.getHeight());
    s.h.post(new Runnable(){ public void run(){ try{ s.wm.removeView(v); }catch(Exception e){ L.w("Walls",e); } done(s); }});
   }};
   v.getViewTreeObserver().addOnGlobalLayoutListener(ll[0]);
   s.wm.addView(v,lp);
   s.h.postDelayed(new Runnable(){ public void run(){
    if(got[0]) return; got[0]=true;
    try{ s.wm.removeView(v); }catch(Exception e){ L.w("Walls",e); }
    done(s);
   }},1500);
  }catch(Exception e){ done(s); }
 }
 static void recenter(MainService s){
  Rect a=area(s);
  s.p1.x=(a.left+a.right)/2-s.p1.width/2-offX;
  s.p1.y=(a.top+a.bottom)/2-s.p1.height/2-offY;
  s.clampPos(); s.updateViews();
  s.sp.edit().putInt("x",s.p1.x).putInt("y",s.p1.y).apply();
 }
}
