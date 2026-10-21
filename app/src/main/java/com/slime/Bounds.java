package com.slime;
import android.content.Context; import android.graphics.Rect; import android.os.Build; import android.util.DisplayMetrics; import android.view.WindowInsets; import android.view.WindowManager;
public class Bounds {
 // The part of the screen where the pet can really be seen and touched:
 // the real display minus the status bar, navigation bar and camera cutout.
 public static Rect usable(Context c,WindowManager wm){
  DisplayMetrics dm=new DisplayMetrics();
  try{ wm.getDefaultDisplay().getRealMetrics(dm); }catch(Exception e){ dm=c.getResources().getDisplayMetrics(); }
  int l=0,t=0,r=0,b=0; boolean got=false;
  if(Build.VERSION.SDK_INT>=30){
   try{
    android.graphics.Insets i=wm.getCurrentWindowMetrics().getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
    l=i.left; t=i.top; r=i.right; b=i.bottom;
    got=(l+t+r+b)>0 && t<dm.heightPixels/4 && b<dm.heightPixels/4 && l<dm.widthPixels/4 && r<dm.widthPixels/4;
   }catch(Throwable th){}
  }
  if(!got){
   l=0; r=0; b=0; t=dimen(c,"status_bar_height");
   DisplayMetrics app=c.getResources().getDisplayMetrics();
   int dh=dm.heightPixels-app.heightPixels, dw=dm.widthPixels-app.widthPixels;
   if(dh>0&&dh<dm.heightPixels/6) b=dh;
   if(dw>0&&dw<dm.widthPixels/6) r=dw;
  }
  return new Rect(l,t,dm.widthPixels-r,dm.heightPixels-b);
 }
 static int dimen(Context c,String n){ int id=c.getResources().getIdentifier(n,"dimen","android"); return id>0?c.getResources().getDimensionPixelSize(id):0; }
}
