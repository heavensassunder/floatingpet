package com.slime;
import android.app.Service; import android.content.Intent; import android.content.SharedPreferences; import android.graphics.PixelFormat; import android.os.Handler; import android.os.IBinder; import android.speech.tts.TextToSpeech; import android.util.DisplayMetrics; import android.view.*; import android.widget.Toast; import java.util.Calendar; import java.util.Locale; import java.util.Random;
public class MainService extends Service implements TextToSpeech.OnInitListener {
 static final int SW=260, SH=160;
 WindowManager wm; SlimeView slimeView; ChatView chatView; WindowManager.LayoutParams p1,p2; float dx,dy; long lastTap=0, lastTouch=0;
 String[] moods={"Happy!","Sleepy","Mad","Love","Hungry","Cry","Wow","Wink","Dead"};
 MemoryManager mem; ActionExecutor executor; TextToSpeech tts; boolean ttsOn=false; Handler h=new Handler(); Random rnd=new Random(); SharedPreferences sp;

 @Override public void onCreate(){
  super.onCreate(); sp=getSharedPreferences("slime",MODE_PRIVATE);
  mem=new MemoryManager(this); executor=new ActionExecutor(this); tts=new TextToSpeech(this,this);
  wm=(WindowManager)getSystemService(WINDOW_SERVICE);
  slimeView=new SlimeView(this);
  p1=new WindowManager.LayoutParams(SW,SH,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
  p1.gravity=Gravity.TOP|Gravity.START; p1.x=sp.getInt("x",100); p1.y=sp.getInt("y",400);
  chatView=new ChatView(this);
  p2=new WindowManager.LayoutParams(340,WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
  p2.gravity=Gravity.TOP|Gravity.START; p2.x=p1.x; p2.y=Math.max(0,p1.y-360); chatView.setVisibility(View.GONE);
  try{ wm.addView(slimeView,p1); wm.addView(chatView,p2); }catch(Exception e){ Toast.makeText(this,"Allow overlay first",Toast.LENGTH_LONG).show(); stopSelf(); return; }
  int hr=Calendar.getInstance().get(Calendar.HOUR_OF_DAY); if(hr>=23||hr<5) slimeView.setMood(1);

  chatView.close.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ chatView.setVisibility(View.GONE); }});
  chatView.historyBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ Intent i=new Intent(MainService.this,HistoryActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }});
  chatView.voiceBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ ttsOn=!ttsOn; Toast.makeText(MainService.this,ttsOn?"TTS ON":"TTS OFF",Toast.LENGTH_SHORT).show(); }});
  chatView.eyesBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
   if(EyeService.instance==null){ Toast.makeText(MainService.this,"Enable Accessibility!",Toast.LENGTH_SHORT).show(); return; }
   chatView.setVisibility(View.GONE);
   h.postDelayed(new Runnable(){ public void run(){
    String s=EyeService.instance!=null?EyeService.instance.dumpScreen():"no screen";
    showChat(); chatView.addMessage("👀 "+s,false);
   }},400);
  }});
  chatView.send.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ String txt=chatView.input.getText().toString().trim(); if(txt.isEmpty()) return; handle(txt); }});
  chatView.input.setOnTouchListener(new View.OnTouchListener(){ public boolean onTouch(View vv,MotionEvent ee){ lastTouch=System.currentTimeMillis(); return false; }});

  slimeView.setOnTouchListener(new View.OnTouchListener(){
   float downX,downY; boolean moved=false;
   public boolean onTouch(View v,MotionEvent e){
    lastTouch=System.currentTimeMillis();
    switch(e.getAction()){
     case MotionEvent.ACTION_DOWN: { downX=e.getRawX(); downY=e.getRawY(); dx=p1.x-e.getRawX(); dy=p1.y-e.getRawY(); moved=false; return true; }
     case MotionEvent.ACTION_MOVE: {
      if(Math.abs(e.getRawX()-downX)>15||Math.abs(e.getRawY()-downY)>15) moved=true;
      if(moved){ p1.x=(int)(e.getRawX()+dx); p1.y=(int)(e.getRawY()+dy); updateViews(); }
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
       int m=(slimeView.mood+1)%9; slimeView.setMood(m); slimeView.bounceAnim(); slimeView.say(moods[m]);
       fleeFrom(e.getRawX(),e.getRawY());
      }
      return true;
     }
    }
    return false;
   }
  });
 }

 void updateViews(){
  p2.x=p1.x; p2.y=Math.max(0,p1.y-360);
  try{ wm.updateViewLayout(slimeView,p1); }catch(Exception e){}
  try{ wm.updateViewLayout(chatView,p2); }catch(Exception e){}
 }
 void showChat(){ chatView.setVisibility(View.VISIBLE); updateViews(); }

 void fleeFrom(float tx,float ty){
  DisplayMetrics dm=getResources().getDisplayMetrics(); int sw=dm.widthPixels, sh=dm.heightPixels;
  int nx=p1.x+((p1.x+SW/2)>tx?250:-250);
  int ny=p1.y+((p1.y+SH/2)>ty?200:-200);
  nx=Math.max(0,Math.min(nx,sw-SW)); ny=Math.max(80,Math.min(ny,sh-SH-120));
  p1.x=nx; p1.y=ny; updateViews();
  slimeView.bounceAnim(); slimeView.say("eep!");
  sp.edit().putInt("x",p1.x).putInt("y",p1.y).apply();
 }

 void learn(String low,String t){
  String[] keys={"my name is","i like","i live","i love","i study","remember"};
  for(String k:keys){ if(low.contains(k)){ mem.saveLongFact(t); return; } }
 }

 void handle(String txt){
  final String t=txt.trim();
  chatView.addMessage("You: "+t,true); chatView.input.setText("");
  String low=t.toLowerCase(); learn(low,t);
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
   showChat(); chatView.addMessage("Slime: "+res,false); slimeView.say(res); slimeView.bounceAnim(); mem.saveChat(original,res);
  }},350);
 }

 void askAI(final String t){
  final String key=sp.getString("key","");
  if(key.isEmpty()){ chatView.addMessage("Slime: add API key first bestie",false); return; }
  slimeView.say("peeking..."); chatView.setVisibility(View.GONE);
  h.postDelayed(new Runnable(){ public void run(){
   String screen=EyeService.instance!=null?EyeService.instance.dumpScreen():"no screen (accessibility is off)";
   String sys=OpenRouterClient.buildSystem(mem.getLongContext(),moods[slimeView.mood],screen,mem.getNotifContext(),mem.getRecentContext(6));
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
  String clean=reply; String openTarget=null;
  int a=reply.indexOf("[[open:");
  if(a>=0){ int b=reply.indexOf("]]",a); if(b>a){ openTarget=reply.substring(a+7,b).trim(); clean=(reply.substring(0,a)+reply.substring(b+2)).trim(); } }
  if(clean.isEmpty()) clean="on it";
  showChat();
  slimeView.setMood(moodFor(clean)); slimeView.say(clean); slimeView.bounceAnim();
  chatView.addMessage("Slime: "+clean,false);
  if(ttsOn){ try{ tts.speak(clean,TextToSpeech.QUEUE_FLUSH,null,null); }catch(Exception e){} }
  mem.saveChat(user,clean);
  if(openTarget!=null&&!openTarget.isEmpty()){ chatView.addMessage("Slime: "+executor.run("open",openTarget),false); }
  h.postDelayed(new Runnable(){ public void run(){
   if(System.currentTimeMillis()-lastTouch>2000&&chatView.getVisibility()==View.VISIBLE){
    int sw=getResources().getDisplayMetrics().widthPixels;
    p1.x=Math.max(20,Math.min(p1.x+rnd.nextInt(120)-60,sw-SW)); updateViews();
   }
  }},2100);
 }

 @Override public void onInit(int s){ if(s==TextToSpeech.SUCCESS) tts.setLanguage(Locale.US); }
 @Override public void onDestroy(){
  super.onDestroy();
  try{ if(tts!=null) tts.shutdown(); }catch(Exception e){}
  try{ wm.removeView(slimeView); }catch(Exception e){}
  try{ wm.removeView(chatView); }catch(Exception e){}
 }
 @Override public IBinder onBind(Intent i){ return null; }
}
