package com.slime;
import android.animation.Animator; import android.animation.AnimatorListenerAdapter; import android.animation.ValueAnimator;
import android.app.Service;
import android.content.BroadcastReceiver; import android.content.ClipData; import android.content.ClipboardManager; import android.content.Context; import android.content.Intent; import android.content.IntentFilter; import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.hardware.Sensor; import android.hardware.SensorEvent; import android.hardware.SensorEventListener; import android.hardware.SensorManager;
import android.os.BatteryManager; import android.os.Handler; import android.os.IBinder; import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.view.*; import android.view.accessibility.AccessibilityNodeInfo; import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;
import java.util.Calendar; import java.util.Locale; import java.util.Random;
public class MainService extends Service implements TextToSpeech.OnInitListener {
 public static MainService instance; Phone phone; Guard guard; Extra extra; Brain brain; Vibe vibe;
 // window is 3/4 of the old size (was 260x190)
 static final int SW=195, SH=143;
 static final int CHAT_H=360;
 WindowManager wm; SlimeView slimeView; ChatView chatView; WindowManager.LayoutParams p1,p2;
 float dx,dy;
 long lastTap=0,lastTouch=0,lastPoke=0,lastTalk=0,lastGreet=0,lastClipNudge=0,lastNotifBubble=0,lastShake=0,lastShakeReact=0,eventMoodUntil=0;
 int shakeCount=0,pokeCount=0,unseenNotifs=0;
 String[] moods={"Happy!","Sleepy","Mad","Love","Hungry","Cry","Wow","Wink","Dead"};
 MemoryManager mem; ActionExecutor executor; TextToSpeech tts; boolean ttsOn=false;
 Handler h=new Handler(); Random rnd=new Random(); SharedPreferences sp; Needs needs;
 SensorManager sm; Sensor accel; boolean sensing=false; ValueAnimator wander; BroadcastReceiver rx; ClipboardManager clipMgr;

 Runnable lifeTick=new Runnable(){ public void run(){ try{ life(); }catch(Exception e){ L.w("MainService",e); } h.postDelayed(this,30000); }};
 Runnable wanderTick=new Runnable(){ public void run(){ try{ wanderStep(); }catch(Exception e){ L.w("MainService",e); } h.postDelayed(this,6000+rnd.nextInt(9000)); }};
 SensorEventListener shakeL=new SensorEventListener(){
  public void onSensorChanged(SensorEvent e){
   float x=e.values[0],y=e.values[1],z=e.values[2];
   double g=Math.sqrt(x*x+y*y+z*z)/SensorManager.GRAVITY_EARTH;
   long now=System.currentTimeMillis();
   if(g>2.6&&now-lastShake>350){
    shakeCount=(now-lastShake<1200)?shakeCount+1:1; lastShake=now;
    if(shakeCount>=3){ shakeCount=0; onShake(); }
   }
  }
  public void onAccuracyChanged(Sensor s,int a){}
 };
 ClipboardManager.OnPrimaryClipChangedListener clipL=new ClipboardManager.OnPrimaryClipChangedListener(){ public void onPrimaryClipChanged(){ onClipChanged(); }};

 @Override public void onCreate(){
  super.onCreate(); sp=getSharedPreferences("slime",MODE_PRIVATE);
  OpenRouterClient.prefs=sp; OpenRouterClient.model=sp.getString("model",OpenRouterClient.MODEL); mem=new MemoryManager(this); brain=new Brain(this); Modes.load(this); Pouch.load(this); Evo.init(this); executor=new ActionExecutor(this); tts=new TextToSpeech(this,this); needs=new Needs(sp);
  wm=(WindowManager)getSystemService(WINDOW_SERVICE);
  slimeView=new SlimeView(this);
  p1=new WindowManager.LayoutParams(SW,SH,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT);
  p1.gravity=Gravity.TOP|Gravity.START; p1.x=sp.getInt("x",100); p1.y=sp.getInt("y",400);
  try{ if(android.os.Build.VERSION.SDK_INT>=28) p1.layoutInDisplayCutoutMode=(android.os.Build.VERSION.SDK_INT>=30)?WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS:WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES; }catch(Throwable th){ L.w("MainService",th); }
  clampPos();
  chatView=new ChatView(this);
  p2=new WindowManager.LayoutParams(340,WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
  p2.gravity=Gravity.TOP|Gravity.START; p2.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING; chatView.onKeyboard=new Runnable(){ public void run(){ updateViews(); }}; placeChat(); chatView.setVisibility(View.GONE);
  try{ wm.addView(slimeView,p1); wm.addView(chatView,p2); }catch(Exception e){ Toast.makeText(this,"Allow overlay first",Toast.LENGTH_LONG).show(); stopSelf(); return; }
  instance=this;
  Walls.measure(this); phone=new Phone(this); phone.start(); extra=new Extra(this); vibe=new Vibe(this); vibe.start(); guard=new Guard(this); h.postDelayed(guard,1200);
  boolean as=asleep(); needs.tick(as,isCharging()); slimeView.setMood(needs.mood(as));

  chatView.close.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ chatView.setVisibility(View.GONE); }});
  chatView.historyBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ Intent i=new Intent(MainService.this,HistoryActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }});
  chatView.voiceBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ ttsOn=!ttsOn; Toast.makeText(MainService.this,ttsOn?"TTS ON":"TTS OFF",Toast.LENGTH_SHORT).show(); }});
  chatView.eyesBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
   if(EyeService.instance==null){ Toast.makeText(MainService.this,"Enable Accessibility!",Toast.LENGTH_SHORT).show(); return; }
   chatView.setVisibility(View.GONE);
   h.postDelayed(new Runnable(){ public void run(){
    String s=EyeService.instance!=null?EyeService.instance.dumpScreen():"no screen";
    showChat(); addChat("👀 "+s,false);
   }},400);
  }});
  chatView.send.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ String txt=chatView.input.getText().toString().trim(); if(txt.isEmpty()) return; handle(txt); }});
  chatView.input.setOnTouchListener(new View.OnTouchListener(){ public boolean onTouch(View vv,MotionEvent ee){ lastTouch=System.currentTimeMillis(); return false; }});

  slimeView.setOnTouchListener(new View.OnTouchListener(){
   float downX,downY; boolean moved=false;
   public boolean onTouch(View v,MotionEvent e){
    lastTouch=System.currentTimeMillis();
    switch(e.getAction()){
     case MotionEvent.ACTION_DOWN: { if(wander!=null) wander.cancel(); downX=e.getRawX(); downY=e.getRawY(); dx=p1.x-e.getRawX(); dy=p1.y-e.getRawY(); moved=false; return true; }
     case MotionEvent.ACTION_MOVE: {
      if(Math.abs(e.getRawX()-downX)>15||Math.abs(e.getRawY()-downY)>15) moved=true;
      if(moved){ p1.x=(int)(e.getRawX()+dx); p1.y=(int)(e.getRawY()+dy); clampPos(); updateViews(); }
      return true;
     }
     case MotionEvent.ACTION_UP: {
      if(moved){ sp.edit().putInt("x",p1.x).putInt("y",p1.y).apply(); return true; }
      long now=System.currentTimeMillis();
      if(now-lastTap<300){ stopSelf(); return true; }
      lastTap=now;
      if(e.getEventTime()-e.getDownTime()>450){
       if(chatView.getVisibility()!=View.VISIBLE) showChat(); else chatView.setVisibility(View.GONE);
      } else {
       if(now-lastPoke<1500) pokeCount++; else pokeCount=1;
       lastPoke=now;
       if(pokeCount>=4){ pokeCount=0; needs.cheer(-6f); react("stop poking me!",2,6000); fleeFrom(e.getRawX(),e.getRawY()); return true; }
       needs.cheer(2f);
       int m=(slimeView.mood+1)%9; moodEvent(m,12000); slimeView.bounceAnim(); slimeView.say(moods[m]);
       fleeFrom(e.getRawX(),e.getRawY());
      }
      return true;
     }
    }
    return false;
   }
  });

  sm=(SensorManager)getSystemService(SENSOR_SERVICE);
  try{ accel=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); }catch(Exception e){ L.w("MainService",e); }
  if(screenOn()) startSensor();
  setupReceivers();
  try{ clipMgr=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE); clipMgr.addPrimaryClipChangedListener(clipL); }catch(Exception e){ L.w("MainService",e); }
  h.postDelayed(lifeTick,30000); h.postDelayed(wanderTick,9000);
  if(!as) h.postDelayed(new Runnable(){ public void run(){ slimeView.say("I'm awake!"); slimeView.bounceAnim(); }},1200);
 }

 // ---------- screen bounds: the slime may go ANYWHERE, even over the status bar and clock ----------
 DisplayMetrics real(){
  DisplayMetrics dm=new DisplayMetrics();
  try{ wm.getDefaultDisplay().getRealMetrics(dm); }catch(Exception e){ dm=getResources().getDisplayMetrics(); }
  return dm;
 }
 int scrW(){ return Walls.W(this); }
 int scrH(){ return Walls.H(this); }
 int minX(){ return Walls.minX(this); }
 int maxX(){ return Walls.maxX(this); }
 int minY(){ return Walls.minY(this); }
 int maxY(){ return Walls.maxY(this); }
 void clampPos(){
  p1.x=Math.max(minX(),Math.min(p1.x,maxX()));
  p1.y=Math.max(minY(),Math.min(p1.y,maxY()));
 }
 // chat sits above the slime; if he is near the top it opens below him instead
 void placeChat(){
  int sw=scrW(), sh=scrH();
  p2.x=Math.max(0,Math.min(p1.x+(SW-340)/2,sw-340));
  int above=p1.y-CHAT_H;
  if(above>=0) p2.y=above;
  else p2.y=Math.max(0,Math.min(p1.y+SH-10,sh-CHAT_H-20));
  if(chatView.kb) p2.y=Walls.area(this).top;
 }

 // ---------- helpers ----------
 void updateViews(){
  placeChat();
  try{ wm.updateViewLayout(slimeView,p1); }catch(Exception e){ L.w("MainService",e); }
  if(chatView.getVisibility()==View.VISIBLE){ try{ wm.updateViewLayout(chatView,p2); }catch(Exception e){ L.w("MainService",e); } }
 }
 void showChat(){ Phone.focusable(this); chatView.setVisibility(View.VISIBLE); updateViews(); }
 void addChat(String t,boolean user){
  chatView.addMessage(t,user);
  try{ while(chatView.messagesContainer.getChildCount()>60) chatView.messagesContainer.removeViewAt(0); }catch(Exception e){ L.w("MainService",e); }
 }
 void local(String s){ local(s,true); }
 void local(String s,boolean bubble){ addChat("Slime: "+s,false); if(bubble){ slimeView.say(s); slimeView.bounceAnim(); } }
 void speak(String t){
  if(t==null) return; t=t.trim(); if(t.isEmpty()) return;
  slimeView.say(t); slimeView.bounceAnim(); addChat("Slime: "+t,false);
  if(ttsOn){ try{ tts.speak(MathText.speakable(t),TextToSpeech.QUEUE_FLUSH,null,null); }catch(Exception e){ L.w("MainService",e); } }
 }
 void moodEvent(int m,long ms){ eventMoodUntil=System.currentTimeMillis()+ms; slimeView.setMood(m); }
 void react(String line,int m,long ms){ moodEvent(m,ms); slimeView.say(line); slimeView.bounceAnim(); }
 boolean screenOn(){ try{ return ((PowerManager)getSystemService(POWER_SERVICE)).isInteractive(); }catch(Exception e){ return true; } }
 boolean asleep(){
  if(System.currentTimeMillis()<sp.getLong("wakeUntil",0)) return false;
  return Calendar.getInstance().get(Calendar.HOUR_OF_DAY)<6;
 }
 boolean isCharging(){
  try{ Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); if(i==null) return false; int s=i.getIntExtra(BatteryManager.EXTRA_STATUS,-1); return s==BatteryManager.BATTERY_STATUS_CHARGING||s==BatteryManager.BATTERY_STATUS_FULL; }catch(Exception e){ return false; }
 }
 int batteryLevel(){
  try{ Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); if(i==null) return -1; int l=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1), sc=i.getIntExtra(BatteryManager.EXTRA_SCALE,100); return sc>0?(l*100/sc):l; }catch(Exception e){ return -1; }
 }
 boolean privateNow(){
  try{ EyeService e=EyeService.instance; if(e==null) return false; AccessibilityNodeInfo r=e.getRootInActiveWindow(); if(r==null) return false; CharSequence pk=r.getPackageName(); return pk!=null&&EyeService.isPrivate(pk.toString()); }catch(Exception ex){ return false; }
 }
 String stripTags(String r){ int a=r.indexOf("[[open:"); if(a>=0){ int b=r.indexOf("]]",a); if(b>a) return (r.substring(0,a)+r.substring(b+2)).trim(); } return r; }
 boolean isErr(String r){ return r.startsWith("net hiccup")||r.startsWith("key died")||r.startsWith("yo I'm out")||r.startsWith("my brain lagged")||r.startsWith("my bad")||r.startsWith("too many"); }

 // ---------- life loop: needs, moods, random talk ----------
 void life(){
  boolean as=asleep(); needs.tick(as,isCharging());
  if(System.currentTimeMillis()>eventMoodUntil){ int m=needs.mood(as); if(m!=slimeView.mood) slimeView.setMood(m); }
  clampPos(); updateViews();
  maybeTalk(false);
 }
 long talkGap(){ int l=sp.getInt("talkLevel",2); return l==1?25*60000L:(l==3?5*60000L:12*60000L); }
 boolean canTalk(){ if(Modes.quiet(this)) return false;
  long now=System.currentTimeMillis();
  if(!screenOn()||asleep()||now<sp.getLong("muteUntil",0)) return false;
  if(chatView.getVisibility()==View.VISIBLE) return false;
  if(now-lastTouch<20000) return false;
  if(privateNow()) return false;
  return true;
 }
 void maybeTalk(boolean force){
  long now=System.currentTimeMillis();
  if(!force){ if(!canTalk()||now-lastTalk<talkGap()||rnd.nextInt(100)>=35) return; }
  lastTalk=now;
  if(rnd.nextInt(100)<40&&sp.getString("key","").length()>0&&OpenRouterClient.autoOk()) idleAI(); else speak(localLine());
 }
 String localLine(){
  int hr=Calendar.getInstance().get(Calendar.HOUR_OF_DAY); int bat=batteryLevel(); boolean ch=isCharging();
  if(unseenNotifs>=2&&rnd.nextInt(100)<60) return "you have "+unseenNotifs+" unread pings. type 'catch me up'";
  if(bat>=0&&bat<=20&&!ch) return "battery's at "+bat+"%... charger?";
  if(needs.food<30f) return Lines.pick(Lines.HUNGRY);
  if(needs.joy<30f) return Lines.pick(Lines.SAD);
  if(needs.energy<25f) return Lines.pick(Lines.TIRED);
  if(needs.joy>85f&&rnd.nextInt(100)<25) return Lines.pick(Lines.LOVE);
  if(rnd.nextInt(100)<45) return Lines.byTime(hr);
  return Lines.pick(Lines.IDLE);
 }
 void idleAI(){
  String key=sp.getString("key","");
  if(key.isEmpty()){ speak(localLine()); return; }
  Calendar c=Calendar.getInstance(); String time=String.format(Locale.US,"%02d:%02d",c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE));
  String sys=OpenRouterClient.buildSystem(brain.context(""),moods[slimeView.mood],"(not looking at the screen)",mem.getNotifContext(),mem.getRecentContext(4))+"\nYOUR NEEDS: "+needs.describe()+". Unread notifications: "+unseenNotifs+". Battery: "+batteryLevel()+"%.\nThis time you may use at most 22 words. Do not add [[open:]] tags."+Vibe.STYLE;
  String user="(You decide to say something on your own, unprompted. Time is "+time+". Say ONE short casual thing like a living pet would. Maybe mention something you remember about me, the time, your needs, or my notifications. At most one question. Don't start with 'hey' every time.)";
  OpenRouterClient.askAuto(key,sys,user,new OpenRouterClient.Callback(){ public void onResult(String reply){
   if(reply==null||isErr(reply)){ speak(localLine()); return; }
   if(chatView.getVisibility()!=View.VISIBLE&&!canTalk()) return;
   speak(stripTags(reply));
  }});
 }

 // ---------- wandering: anywhere on the screen ----------
 void wanderStep(){
  long now=System.currentTimeMillis();
  if(chatView.getVisibility()==View.VISIBLE||now-lastTouch<8000) return;
  if(wander!=null&&wander.isRunning()) return;
  if(!screenOn()) return;
  int sw=scrW();
  if(slimeView.mood==1){
   int tx=(p1.x<sw/2)?minX():maxX(); int ty=maxY()-100;
   if(Math.abs(p1.x-tx)<30&&Math.abs(p1.y-ty)<30) return;
   return;
  }
  if(rnd.nextInt(100)<35) return;
  int tx; int roll=rnd.nextInt(100);
  if(roll<25) tx=minX(); else if(roll<50) tx=maxX(); else tx=minX()+rnd.nextInt(Math.max(1,maxX()-minX()));
  int ty=minY()+rnd.nextInt(Math.max(1,maxY()-minY()));
  glideTo(tx,ty);
 }
 void glideRandom(){
  glideTo(minX()+rnd.nextInt(Math.max(1,maxX()-minX())),minY()+rnd.nextInt(Math.max(1,maxY()-minY())));
 }
 void glideTo(int tx0,int ty0){ glideTo(tx0,ty0,-1); }
 void glideTo(int tx0,int ty0,final long fixedMs){
  if(wander!=null) wander.cancel();
  final int tx=Math.max(minX(),Math.min(tx0,maxX()));
  final int ty=Math.max(minY(),Math.min(ty0,maxY()));
  final int sx=p1.x, sy=p1.y;
  double dist=Math.sqrt((tx-sx)*(double)(tx-sx)+(ty-sy)*(double)(ty-sy));
  wander=ValueAnimator.ofFloat(0f,1f); wander.setDuration(fixedMs>0?fixedMs:(long)Math.min(6000,300+dist*2.2)); wander.setInterpolator(new android.view.animation.LinearInterpolator());
  wander.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){ public void onAnimationUpdate(ValueAnimator a){ float f=((Float)a.getAnimatedValue()).floatValue(); p1.x=(int)(sx+(tx-sx)*f); p1.y=(int)(sy+(ty-sy)*f); updateViews(); }});
  wander.addListener(new AnimatorListenerAdapter(){ public void onAnimationEnd(Animator a){ sp.edit().putInt("x",p1.x).putInt("y",p1.y).apply(); }});
  wander.start();
 }
 void fleeFrom(float tx,float ty){
  if(wander!=null) wander.cancel();
  int fx=p1.x+((p1.x+SW/2)>tx?220:-220);
  int fy=p1.y+((p1.y+SH/2)>ty?170:-170);
  glideTo(fx,fy,420); slimeView.bounceAnim();
  sp.edit().putInt("x",p1.x).putInt("y",p1.y).apply();
 }

 // ---------- phone reactions ----------
 void setupReceivers(){
  rx=new BroadcastReceiver(){ public void onReceive(Context c,Intent i){
   if(isInitialStickyBroadcast()) return;
   String a=i.getAction(); if(a==null) return;
   if(a.equals(Intent.ACTION_POWER_CONNECTED)){ needs.feed(10f); react(Lines.pick(Lines.PLUG),3,5000); }
   else if(a.equals(Intent.ACTION_POWER_DISCONNECTED)){ react(Lines.pick(Lines.UNPLUG),0,4000); }
   else if(a.equals(Intent.ACTION_BATTERY_LOW)){ react("battery low! "+batteryLevel()+"%",5,6000); }
   else if(a.equals(Intent.ACTION_HEADSET_PLUG)){ int st=i.getIntExtra("state",0); if(st==1) react(Lines.pick(Lines.HEAD_ON),7,4000); else react(Lines.pick(Lines.HEAD_OFF),0,3000); }
   else if(a.equals(Intent.ACTION_USER_PRESENT)){ onUnlock(); }
   else if(a.equals(Intent.ACTION_SCREEN_ON)){ startSensor(); }
   else if(a.equals(Intent.ACTION_SCREEN_OFF)){ stopSensor(); sp.edit().putLong("lastSeen",System.currentTimeMillis()).apply(); }
  }};
  IntentFilter f=new IntentFilter();
  f.addAction(Intent.ACTION_POWER_CONNECTED); f.addAction(Intent.ACTION_POWER_DISCONNECTED); f.addAction(Intent.ACTION_BATTERY_LOW);
  f.addAction(Intent.ACTION_HEADSET_PLUG); f.addAction(Intent.ACTION_USER_PRESENT); f.addAction(Intent.ACTION_SCREEN_ON); f.addAction(Intent.ACTION_SCREEN_OFF);
  try{ registerReceiver(rx,f); }catch(Exception e){ L.w("MainService",e); }
 }
 void startSensor(){ try{ if(sensing||sm==null||accel==null) return; sm.registerListener(shakeL,accel,SensorManager.SENSOR_DELAY_UI); sensing=true; }catch(Exception e){ L.w("MainService",e); } }
 void stopSensor(){ try{ if(sm!=null&&sensing) sm.unregisterListener(shakeL); }catch(Exception e){ L.w("MainService",e); } sensing=false; }
 void onShake(){
  long now=System.currentTimeMillis(); if(now-lastShakeReact<4000) return; lastShakeReact=now;
  needs.cheer(-2f); react(Lines.pick(Lines.SHAKE),8,3500);
 }
 void onUnlock(){
  long now=System.currentTimeMillis(); long last=sp.getLong("lastSeen",0); sp.edit().putLong("lastSeen",now).apply();
  if(asleep()){ if(now-lastGreet>30*60000L){ lastGreet=now; react("huh? it's so late... sleep soon?",1,6000); } return; }
  long gone=last==0?0:now-last; String line=null;
  if(gone>5*3600000L) line=Lines.byTime(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
  else if(gone>90*60000L) line=Lines.pick(Lines.BACK);
  if(line!=null&&now-lastGreet>10*60000L){ lastGreet=now; needs.cheer(8f); react(line,3,6000); return; }
  if(unseenNotifs>=3&&now-lastNotifBubble>60000L){ lastNotifBubble=now; slimeView.say("you missed "+unseenNotifs+" pings. type 'catch me up'"); slimeView.bounceAnim(); }
 }

 // ---------- notifications ----------
 boolean looksUrgent(String s){
  String l=s.toLowerCase();
  String[] k={"urgent","asap","emergency","call me","immediately","important","where are you","are you there","!!!"};
  for(String w:k){ if(l.contains(w)) return true; }
  return false;
 }
 public void onNotif(String pkg,String title,String text){
  try{
   unseenNotifs++;
   long now=System.currentTimeMillis();
   if(!sp.getBoolean("pings",true)||!screenOn()||Modes.quiet(this)) return;
   if(chatView.getVisibility()==View.VISIBLE||asleep()||privateNow()) return;
   if(now<sp.getLong("muteUntil",0)||now-lastNotifBubble<20000) return;
   lastNotifBubble=now;
   if(looksUrgent(title+" "+text)) react(title+" sounds urgent!",6,5000);
   else { slimeView.say("ping from "+title); slimeView.bounceAnim(); }
  }catch(Exception e){ L.w("MainService",e); }
 }
 String notifList(int n){
  try{
   org.json.JSONArray a=mem.loadArr(mem.notifFile); StringBuilder sb=new StringBuilder(); long now=System.currentTimeMillis();
   for(int i=Math.max(0,a.length()-n);i<a.length();i++){ org.json.JSONObject o=a.getJSONObject(i); long age=(now-o.optLong("t",now))/60000; sb.append("- ").append(o.optString("title")).append(": ").append(o.optString("text")).append(" (").append(age).append(" min ago)\n"); }
   return sb.toString();
  }catch(Exception e){ return ""; }
 }
 void catchUp(String original){
  String list=notifList(15); unseenNotifs=0;
  if(list.isEmpty()){ local("nothing new, your inbox is quiet"); return; }
  askTask(original,"Catch me up on my notifications. Say how many there are, who matters most, and flag anything that sounds urgent.\n\nNOTIFICATIONS (oldest first):\n"+list);
 }

 // ---------- clipboard ----------
 void onClipChanged(){
  long now=System.currentTimeMillis();
  if(now-lastClipNudge<60000L||!canTalk()) return;
  lastClipNudge=now; slimeView.say("copied something? type 'clip' and I'll help"); slimeView.bounceAnim();
 }
 boolean isClipCmd(String low){ return low.equals("clip")||low.startsWith("clip ")||low.endsWith(" clip")||low.contains("clipboard")||low.contains(" clip "); }
 String readClip(){
  try{
   if(clipMgr==null||!clipMgr.hasPrimaryClip()) return "";
   ClipData d=clipMgr.getPrimaryClip(); if(d==null||d.getItemCount()==0) return "";
   CharSequence t=d.getItemAt(0).coerceToText(this); return t==null?"":t.toString().trim();
  }catch(Exception e){ return ""; }
 }
 void clipTask(String original,String low){
  String clip=readClip();
  if(clip.isEmpty()){ local("can't see your clipboard. copy something, open my chat, then type clip",false); return; }
  if(clip.length()>1500) clip=clip.substring(0,1500);
  String task="Explain this copied text simply and briefly.";
  if(low.contains("summar")) task="Summarize this copied text in 2-3 short sentences.";
  else if(low.contains("translat")) task="Translate this copied text. If it is not English, translate it to English. If it is English, translate it to Swahili.";
  else if(low.contains("repl")) task="Write one short, natural reply I could send to this message.";
  askTask(original,task+"\n\nCOPIED TEXT:\n"+clip);
 }
 void askTask(final String original,String user){
  String key=sp.getString("key","");
  if(key.isEmpty()){ local("add API key first bestie",false); return; }
  slimeView.say("on it...");
  String sys=OpenRouterClient.buildSystem(brain.context(original),moods[slimeView.mood],"(not needed for this)","","")+"\nYOUR NEEDS: "+needs.describe()+"\nFor this request you may use up to 70 words. Do not add [[open:]] tags."+Vibe.STYLE;
  OpenRouterClient.ask(key,sys,user,new OpenRouterClient.Callback(){ public void onResult(String reply){ onReply(original,reply); }});
 }

 // ---------- chat commands ----------
 boolean localCommand(String low){
  long now=System.currentTimeMillis();
  if(low.equals("help")||low.equals("commands")){ Vibe.showHelp(this); return true; }
  if(low.equals("status")||low.equals("stats")){ local(Evo.line(this)+" | food "+(int)needs.food+" | energy "+(int)needs.energy+" | joy "+(int)needs.joy+" | battery "+batteryLevel()+"%",false); return true; }
  if(low.equals("feed")||low.equals("feed slime")||low.equals("snack")){
   if(needs.food>90f){ local("I'm stuffed, no more!"); } else { needs.feed(25f); moodEvent(3,6000); local("nom nom nom... thanks!"); }
   return true;
  }
  if(low.equals("play")||low.equals("play with me")){ needs.cheer(15f); moodEvent(7,6000); local("tag, you're it!"); chatView.setVisibility(View.GONE); glideRandom(); return true; }
  if(low.equals("shh")||low.equals("be quiet")||low.equals("quiet")||low.equals("mute")){ sp.edit().putLong("muteUntil",now+2*3600000L).apply(); local("ok, quiet for 2 hours. say 'talk to me' when you want me back.",false); return true; }
  if(low.equals("talk to me")||low.equals("unmute")){ sp.edit().putLong("muteUntil",0).apply(); local("yay, I'm back to chatting!"); return true; }
  if(low.equals("talk more")||low.equals("talk less")){
   int l=sp.getInt("talkLevel",2); l=low.equals("talk more")?Math.min(3,l+1):Math.max(1,l-1); sp.edit().putInt("talkLevel",l).apply();
   local(l==1?"ok, quiet-ish (about every 25 min)":(l==2?"normal chatter (about every 12 min)":"chatty mode (about every 5 min)"),false); return true;
  }
  if(low.equals("say something")||low.equals("talk")){ maybeTalk(true); return true; }
  if(low.equals("wake up")){ sp.edit().putLong("wakeUntil",now+5*3600000L).apply(); moodEvent(0,10000); local("I'm up! awake for the next 5 hours."); return true; }
  if(low.equals("pings off")||low.equals("pings on")){ sp.edit().putBoolean("pings",low.endsWith("on")).apply(); local(low.endsWith("on")?"ok, I'll tell you about new pings":"ok, no more ping bubbles",false); return true; }
  if(low.contains("what do you remember")||low.contains("what do you know about me")){ brain.showSummary(); return true; }
  if(low.equals("forget all")||low.equals("forget everything")){ if(brain!=null) brain.clearAll(); try{ mem.longFile.delete(); }catch(Exception e){ L.w("MainService",e); } local("done, I forgot everything I knew about you."); return true; }
  if(low.equals("clear history")){ try{ mem.chatFile.delete(); mem.notifFile.delete(); chatView.messagesContainer.removeAllViews(); }catch(Exception e){ L.w("MainService",e); } unseenNotifs=0; local("history cleared"); return true; }
  return false;
 }

 void handle(String txt){
  final String t=txt.trim(); if(t.isEmpty()) return;
  addChat("You: "+t,true); chatView.input.setText(""); chatView.hideKeyboard();
  String low=t.toLowerCase(); lastTouch=System.currentTimeMillis();
  needs.cheer(5f);
  if(localCommand(low)) return;
  if(phone!=null&&phone.handle(t,low)) return;
  if(extra!=null&&extra.handle(t,low)) return;
  if(Modes.handle(this,t,low)||Pouch.handle(this,t,low)||Evo.handle(this,low)||(brain!=null&&brain.handle(t,low))) return;
  if(vibe!=null&&vibe.handle(t,low)) return;
  if(isClipCmd(low)){ clipTask(t,low); return; }
  if(low.contains("catch me up")||low.contains("what did i miss")||low.equals("pings")||low.equals("notifs")||low.equals("notifications")){ catchUp(t); return; }
  if(brain!=null) brain.observe(t); Evo.count(this,low);
  String cmd=null, arg="";
  if(low.equals("settings")||low.equals("open settings")){ cmd="open"; arg="settings"; }
  else if(low.startsWith("open ")){ cmd="open"; arg=t.substring(5).trim(); }
  else if(low.startsWith("tap ")){ cmd="tap"; arg=t.substring(4).trim(); }
  else if(low.startsWith("type ")){ cmd="type"; arg=t.substring(5).trim(); }
  else if(low.equals("scroll")||low.startsWith("scroll ")){ cmd="scroll"; arg=low.length()>6?low.substring(7).trim():"down"; }
  if(cmd!=null){ runAction(cmd,arg,t); return; }
  askAI(t);
 }

 void runAction(final String cmd,final String arg,final String original){
  slimeView.say("on it..."); chatView.setVisibility(View.GONE);
  h.postDelayed(new Runnable(){ public void run(){
   String res=executor.run(cmd,arg);
   showChat(); addChat("Slime: "+res,false); slimeView.say(res); slimeView.bounceAnim(); mem.saveChat(original,res);
  }},350);
 }

 void askAI(final String t){
  final String key=sp.getString("key","");
  if(key.isEmpty()){ addChat("Slime: add API key first bestie",false); return; }
  slimeView.say("peeking..."); chatView.setVisibility(View.GONE);
  h.postDelayed(new Runnable(){ public void run(){
   String screen=EyeService.instance!=null?EyeService.instance.dumpScreen():"no screen (accessibility is off)";
   String sys=OpenRouterClient.buildSystem(brain.context(t),moods[slimeView.mood],screen,mem.getNotifContext(),mem.getRecentContext(6))+"\nYOUR NEEDS: "+needs.describe()+Phone.AI_HINT+Vibe.STYLE+Brain.PROMPT+Modes.prompt(MainService.this);
   OpenRouterClient.ask(key,sys,t,new OpenRouterClient.Callback(){ public void onResult(String reply){ onReply(t,reply); }});
  }},420);
 }

 int moodFor(String s){
  String l=s.toLowerCase();
  if(l.contains("haha")||l.contains("lol")) return 7;
  if(l.contains("sorry")||l.contains("sad")||l.contains("unfortunately")) return 5;
  if(l.contains("wow")||l.contains("whoa")||l.contains("damn")) return 6;
  if(l.contains("love")||l.contains("thanks")||l.contains("thank")) return 3;
  if(l.contains("hungry")||l.contains("food")) return 4;
  if(l.contains("grr")||l.contains("annoy")) return 2;
  return 0;
 }

 void onReply(String user,String reply){
  String clean=reply; String openTarget=null; if(phone!=null){ clean=phone.extractDo(clean); reply=clean; } if(brain!=null){ clean=brain.fromTags(clean); reply=clean; }
  int a=reply.indexOf("[[open:");
  if(a>=0){ int b=reply.indexOf("]]",a); if(b>a){ openTarget=reply.substring(a+7,b).trim(); clean=(reply.substring(0,a)+reply.substring(b+2)).trim(); } }
  if(clean.isEmpty()) clean="on it";
  showChat();
  int m=moodFor(clean); moodEvent(m==0?needs.mood(asleep()):m,8000);
  slimeView.say(clean); slimeView.bounceAnim();
  addChat("Slime: "+clean,false);
  if(ttsOn){ try{ tts.speak(MathText.speakable(clean),TextToSpeech.QUEUE_FLUSH,null,null); }catch(Exception e){ L.w("MainService",e); } }
  mem.saveChat(user,clean);
  if(openTarget!=null&&!openTarget.isEmpty()){ addChat("Slime: "+executor.run("open",openTarget),false); }
 }

 @Override public void onInit(int s){ if(s==TextToSpeech.SUCCESS) tts.setLanguage(Locale.US); }
 @Override public void onDestroy(){
  super.onDestroy();
  instance=null;
  try{ h.removeCallbacksAndMessages(null); }catch(Exception e){ L.w("MainService",e); }
  try{ if(wander!=null) wander.cancel(); }catch(Exception e){ L.w("MainService",e); }
  stopSensor();
  try{ if(rx!=null) unregisterReceiver(rx); }catch(Exception e){ L.w("MainService",e); }
  try{ if(clipMgr!=null) clipMgr.removePrimaryClipChangedListener(clipL); }catch(Exception e){ L.w("MainService",e); }
  try{ if(tts!=null) tts.shutdown(); }catch(Exception e){ L.w("MainService",e); }
  try{ wm.removeView(slimeView); }catch(Exception e){ L.w("MainService",e); }
  try{ wm.removeView(chatView); }catch(Exception e){ L.w("MainService",e); }
 }
 @Override public IBinder onBind(Intent i){ return null; }
 @Override public void onConfigurationChanged(android.content.res.Configuration cfg){ super.onConfigurationChanged(cfg); Walls.measure(this); h.postDelayed(new Runnable(){ public void run(){ clampPos(); updateViews(); }},400); }
 @Override public int onStartCommand(Intent in,int fl,int id){ try{ if(instance==this&&slimeView!=null) Walls.recenter(this); }catch(Exception e){ L.w("MainService",e); } return START_STICKY; }
}
