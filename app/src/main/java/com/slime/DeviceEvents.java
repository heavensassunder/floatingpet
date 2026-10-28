package com.slime;
import android.content.BroadcastReceiver; import android.content.Context; import android.content.Intent; import android.content.IntentFilter;
import android.hardware.Sensor; import android.hardware.SensorEvent; import android.hardware.SensorEventListener; import android.hardware.SensorManager;
import java.util.Calendar;
// Hardware and system events: power/headset/screen broadcasts, shake gestures, unlock greetings.
class DeviceEvents {
 final MainService s;
 SensorManager sm; Sensor accel; boolean sensing; BroadcastReceiver rx;
 long lastShake, lastShakeReact, lastGreet; int shakeCount;
 private final SensorEventListener shakeListener=new SensorEventListener(){
  public void onSensorChanged(SensorEvent e){
   float x=e.values[0],y=e.values[1],z=e.values[2];
   double g=Math.sqrt(x*x+y*y+z*z)/SensorManager.GRAVITY_EARTH;
   long now=System.currentTimeMillis();
   if(g>2.6&&now-lastShake>350){
    shakeCount=(now-lastShake<1200)?shakeCount+1:1; lastShake=now;
    if(shakeCount>=3){ shakeCount=0; onShake(); }
   }
  }
  public void onAccuracyChanged(Sensor sensor,int a){}
 };

 DeviceEvents(MainService s){
  this.s=s;
  sm=(SensorManager)s.getSystemService(Context.SENSOR_SERVICE);
  try{ accel=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); }catch(Exception e){ L.w("DeviceEvents",e); }
 }
 void start(){
  if(s.screenOn()) startSensor();
  setupReceivers();
 }
 void destroy(){
  stopSensor();
  try{ if(rx!=null) s.unregisterReceiver(rx); }catch(Exception e){ L.w("DeviceEvents",e); }
 }

 void startSensor(){ try{ if(sensing||sm==null||accel==null) return; sm.registerListener(shakeListener,accel,SensorManager.SENSOR_DELAY_UI); sensing=true; }catch(Exception e){ L.w("DeviceEvents",e); } }
 void stopSensor(){ try{ if(sm!=null&&sensing) sm.unregisterListener(shakeListener); }catch(Exception e){ L.w("DeviceEvents",e); } sensing=false; }
 void onShake(){
  long now=System.currentTimeMillis(); if(now-lastShakeReact<4000) return; lastShakeReact=now;
  s.needs.cheer(-2f); s.react(Lines.pick(Lines.SHAKE),8,3500);
 }
 void onUnlock(){
  long now=System.currentTimeMillis(); long last=s.sp.getLong("lastSeen",0); s.sp.edit().putLong("lastSeen",now).apply();
  Routines.fire(s,"unlock",null);
  if(s.asleep()){ if(now-lastGreet>30*60000L){ lastGreet=now; s.react("huh? it's so late... sleep soon?",1,6000); } return; }
  long gone=last==0?0:now-last; String line=null;
  if(gone>5*3600000L) line=Lines.byTime(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
  else if(gone>90*60000L) line=Lines.pick(Lines.BACK);
  if(line!=null&&now-lastGreet>10*60000L){ lastGreet=now; s.needs.cheer(8f); s.react(line,3,6000); return; }
  if(s.notifs.unseen>=3&&now-s.notifs.lastBubble>60000L){ s.notifs.lastBubble=now; s.slimeView.say("you missed "+s.notifs.unseen+" pings. type 'catch me up'"); s.slimeView.bounceAnim(); }
 }

 void setupReceivers(){
  rx=new BroadcastReceiver(){ public void onReceive(Context c,Intent i){
   if(isInitialStickyBroadcast()) return;
   String a=i.getAction(); if(a==null) return;
   if(a.equals(Intent.ACTION_POWER_CONNECTED)){ s.needs.feed(10f); s.react(Lines.pick(Lines.PLUG),3,5000); Routines.fire(s,"charge",null); }
   else if(a.equals(Intent.ACTION_POWER_DISCONNECTED)){ s.react(Lines.pick(Lines.UNPLUG),0,4000); Routines.fire(s,"unplug",null); }
   else if(a.equals(Intent.ACTION_BATTERY_LOW)){ s.react("battery low! "+s.batteryLevel()+"%",5,6000); }
   else if(a.equals(Intent.ACTION_HEADSET_PLUG)){ int st=i.getIntExtra("state",0); if(st==1){ s.react(Lines.pick(Lines.HEAD_ON),7,4000); Routines.fire(s,"headon",null); } else { s.react(Lines.pick(Lines.HEAD_OFF),0,3000); Routines.fire(s,"headoff",null); } }
   else if(a.equals(Intent.ACTION_USER_PRESENT)){ onUnlock(); }
   else if(a.equals(Intent.ACTION_SCREEN_ON)){ startSensor(); if(s.ear!=null) s.ear.onScreenOn(); }
   else if(a.equals(Intent.ACTION_SCREEN_OFF)){ stopSensor(); if(s.ear!=null) s.ear.onScreenOff(); s.sp.edit().putLong("lastSeen",System.currentTimeMillis()).apply(); }
  }};
  IntentFilter f=new IntentFilter();
  f.addAction(Intent.ACTION_POWER_CONNECTED); f.addAction(Intent.ACTION_POWER_DISCONNECTED); f.addAction(Intent.ACTION_BATTERY_LOW);
  f.addAction(Intent.ACTION_HEADSET_PLUG); f.addAction(Intent.ACTION_USER_PRESENT); f.addAction(Intent.ACTION_SCREEN_ON); f.addAction(Intent.ACTION_SCREEN_OFF);
  try{ s.registerReceiver(rx,f); }catch(Exception e){ L.w("DeviceEvents",e); }
 }
}
