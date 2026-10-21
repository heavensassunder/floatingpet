package com.slime;
import android.graphics.Canvas; import android.graphics.Color; import android.graphics.Paint; import android.graphics.Typeface; import android.os.SystemClock;
// The slime's mouth moves while it talks, and sings with a little tongue and floating notes.
public class Mouth {
 static long talkUntil=0; static boolean singing=false;
 static final Paint tongue=new Paint(Paint.ANTI_ALIAS_FLAG), crease=new Paint(Paint.ANTI_ALIAS_FLAG), noteP=new Paint(Paint.ANTI_ALIAS_FLAG);
 static{
  tongue.setColor(Color.parseColor("#FF8FA3"));
  crease.setColor(Color.parseColor("#E0607E")); crease.setStyle(Paint.Style.STROKE); crease.setStrokeWidth(1.6f); crease.setStrokeCap(Paint.Cap.ROUND);
  noteP.setColor(Color.parseColor("#85C1E9")); noteP.setTextAlign(Paint.Align.CENTER); noteP.setTypeface(Typeface.DEFAULT_BOLD);
 }
 static void talkFor(long ms,boolean sing){ talkUntil=SystemClock.uptimeMillis()+ms; singing=sing; }
 static boolean talking(String state,int mood){
  return SystemClock.uptimeMillis()<talkUntil && !state.equals("think") && !state.equals("eat") && mood!=8;
 }
 static void draw(SlimeView v,Canvas cv,float cx,float my){
  float t=SystemClock.uptimeMillis()/1000f;
  float o, hw, hh;
  if(singing){
   o=0.5f+0.5f*(float)Math.sin(t*12.6f);        // about 2 beats a second
   hw=7f+o*8f; hh=3f+o*9f;                      // small O -> wide Ah
  } else {
   float a=(float)Math.abs(Math.sin(t*11f)), b=(float)Math.abs(Math.sin(t*6.7f+1f));
   o=Math.min(1f,a*0.7f+b*0.45f);
   hw=8f+o*4f; hh=2f+o*8f;
  }
  float cy=my+4f;
  v.r.set(cx-hw,cy-hh,cx+hw,cy+hh); cv.drawOval(v.r,v.fill);
  if(hh>5f){ v.r.set(cx-hw*0.55f,cy+hh*0.25f,cx+hw*0.55f,cy+hh*0.9f); cv.drawOval(v.r,tongue); }
  boolean happy=v.mood==0||v.mood==3||v.mood==7;
  if((singing&&o>0.85f)||(happy&&!singing&&o>0.92f)){
   v.r.set(cx-5f,cy+hh-3f,cx+5f,cy+hh+7f); cv.drawOval(v.r,tongue);
   cv.drawLine(cx,cy+hh,cx,cy+hh+4f,crease);
  }
 }
 static void notes(SlimeView v,Canvas cv,float cx,float cy){
  if(!singing||SystemClock.uptimeMillis()>=talkUntil) return;
  float t=SystemClock.uptimeMillis()/1000f;
  for(int i=0;i<3;i++){
   float ph=(t*0.7f+i/3f)%1f;
   float x=cx+44f+(i-1)*14f+(float)Math.sin((t+i)*3f)*5f;
   float y=cy-34f-ph*46f;
   noteP.setAlpha((int)(255*(1f-ph))); noteP.setTextSize(15f+i*2f);
   cv.drawText(i==1?"\u266b":"\u266a",x,y,noteP);
  }
  noteP.setAlpha(255);
 }
}
