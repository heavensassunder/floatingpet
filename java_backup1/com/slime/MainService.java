package com.slime;
import android.app.Service; import android.content.Intent; import android.content.SharedPreferences; import android.graphics.PixelFormat; import android.os.Handler; import android.os.IBinder; import android.speech.tts.TextToSpeech; import android.view.*; import android.widget.Toast; import java.util.Locale; import java.util.Random;
public class MainService extends Service implements TextToSpeech.OnInitListener {
 WindowManager wm; SlimeView slimeView; ChatView chatView; WindowManager.LayoutParams p1,p2; float dx,dy; long lastTap=0, lastTouch=0;
 String[] moods={"Happy!","Sleepy","Mad","Love","Hungry","Cry","Wow","Wink","Dead"};
 MemoryManager mem; ActionExecutor executor; TextToSpeech tts; boolean ttsOn=false; Handler h=new Handler(); Random rnd=new Random();
 @Override public void onCreate(){
  super.onCreate(); final SharedPreferences sp=getSharedPreferences("slime",MODE_PRIVATE);
  mem=new MemoryManager(this); executor=new ActionExecutor(this); tts=new TextToSpeech(this,this);
  wm=(WindowManager)getSystemService(WINDOW_SERVICE);
  slimeView=new SlimeView(this); p1=new WindowManager.LayoutParams(130,130, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT); p1.gravity=Gravity.TOP|Gravity.START; p1.x=sp.getInt("x",100); p1.y=sp.getInt("y",400); wm.addView(slimeView,p1);
  chatView=new ChatView(this); p2=new WindowManager.LayoutParams(340,WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT); p2.gravity=Gravity.TOP|Gravity.START; p2.x=p1.x; p2.y=p1.y-360; chatView.setVisibility(View.GONE); wm.addView(chatView,p2);
  chatView.close.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ chatView.setVisibility(View.GONE); }});
  chatView.historyBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ Intent i=new Intent(MainService.this, HistoryActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }});
  chatView.voiceBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ ttsOn=!ttsOn; Toast.makeText(MainService.this, ttsOn?"TTS ON":"OFF",0).show(); }});
  chatView.eyesBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
   if(EyeService.instance==null){ Toast.makeText(MainService.this,"Enable Accessibility",0).show(); return; }
   chatView.setVisibility(View.GONE);
   h.postDelayed(new Runnable(){ public void run(){
    String s=EyeService.instance.dumpScreen(); chatView.setVisibility(View.VISIBLE); p2.x=p1.x; p2.y=p1.y-360; try{ wm.updateViewLayout(chatView,p2);}catch(Exception e){}
    chatView.addMessage("👀 "+s,false);
   }},400);
  }});
  chatView.send.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ String txt=chatView.input.getText().toString().trim(); if(txt.isEmpty()) return; handleWithPeek(txt); }});
  chatView.input.setOnTouchListener(new View.OnTouchListener(){ public boolean onTouch(View vv, MotionEvent ee){ lastTouch=System.currentTimeMillis(); return false; }});
  slimeView.setOnTouchListener(new View.OnTouchListener(){
   public boolean onTouch(View v, MotionEvent e){
    lastTouch=System.currentTimeMillis();
    switch(e.getAction()){
     case MotionEvent.ACTION_DOWN: dx=p1.x-e.getRawX(); dy=p1.y-e.getRawY(); return true;
     case MotionEvent.ACTION_MOVE: p1.x=(int)(e.getRawX()+dx); p1.y=(int)(e.getRawY()+dy); p2.x=p1.x; p2.y=p1.y-360; try{ wm.updateViewLayout(slimeView,p1); if(chatView.getVisibility()==View.VISIBLE) wm.updateViewLayout(chatView,p2);}catch(Exception ex){} return true;
     case MotionEvent.ACTION_UP:
      if(Math.abs(e.getRawX()+dx-p1.x)<10){
       long now=System.currentTimeMillis(); if(now-lastTap<300){ stopSelf(); return true; } lastTap=now;
       if(e.getEventTime()-e.getDownTime()>450){
        if(chatView.getVisibility()!=View.VISIBLE){ p2.x=p1.x; p2.y=p1.y-360; try{ wm.updateViewLayout(chatView,p2);}catch(Exception ex){} chatView.setVisibility(View.VISIBLE); } else chatView.setVisibility(View.GONE);
       } else {
        int m=(slimeView.mood+1)%9; slimeView.setMood(m); slimeView.bounceAnim(); slimeView.say(moods[m]);
        fleeFrom(e.getRawX(), e.getRawY());
       }
      } else sp.edit().putInt("x",p1.x).putInt("y",p1.y).apply();
      return true;
    } return false;
   }
  });
 }
 void fleeFrom(float touchX, float touchY){
  int screenW=wm.getDefaultDisplay().getWidth(); int screenH=wm.getDefaultDisplay().getHeight();
  int newX=p1.x + (p1.x>touchX? 250 : -250);
  int newY=p1.y + (p1.y>touchY? 200 : -200);
  if(newX<0) newX=20; if(newX>screenW-150) newX=screenW-150;
  if(newY<0) newY=100; if(newY>screenH-300) newY=screenH-300;
  p1.x=newX; p1.y=newY; p2.x=p1.x; p2.y=p1.y-360;
  try{ wm.updateViewLayout(slimeView,p1); if(chatView.getVisibility()==View.VISIBLE) wm.updateViewLayout(chatView,p2);}catch(Exception e){}
  slimeView.bounceAnim(); slimeView.say("eep!");
 }
 void handleWithPeek(final String txt){
  chatView.addMessage("You: "+txt,true); chatView.input.setText("");
  String low=txt.toLowerCase();
  if(low.startsWith("open ")){
   String target=low.replace("open","").trim();
   chatView.setVisibility(View.GONE);
   h.postDelayed(new Runnable(){ public void run(){
    String res=executor.execute(target); chatView.setVisibility(View.VISIBLE);
    chatView.addMessage("Slime: "+res,false); slimeView.say(res); slimeView.bounceAnim(); mem.saveChat(txt,res);
   }},350);
   return;
  }
  slimeView.say("peeking..."); chatView.setVisibility(View.GONE);
  h.postDelayed(new Runnable(){ public void run(){
   String screen=EyeService.instance!=null?EyeService.instance.dumpScreen():"home screen";
   SharedPreferences sp=getSharedPreferences("slime",MODE_PRIVATE); String key=sp.getString("key","");
   if(key.isEmpty()){ chatView.setVisibility(View.VISIBLE); chatView.addMessage("Slime: no key",false); return; }
   OpenRouterClient.ask(key,txt,screen,new OpenRouterClient.Callback(){ public void onResult(String reply){
    chatView.setVisibility(View.VISIBLE); p2.x=p1.x; p2.y=p1.y-360; try{ wm.updateViewLayout(chatView,p2);}catch(Exception e){}
    slimeView.say(reply.length()>25?reply.substring(0,25):reply); slimeView.bounceAnim();
    chatView.addMessage("Slime: "+reply,false);
    if(ttsOn) try{ tts.speak(reply,TextToSpeech.QUEUE_FLUSH,null,null);}catch(Exception e){}
    mem.saveChat(txt,reply);
    h.postDelayed(new Runnable(){
     public void run(){
      if(System.currentTimeMillis()-lastTouch>2000 && chatView.getVisibility()==View.VISIBLE){
       int ddx=rnd.nextInt(120)-60; p1.x+=ddx; if(p1.x<0) p1.x=20;
       try{ wm.updateViewLayout(slimeView,p1); wm.updateViewLayout(chatView,p2);}catch(Exception e){}
      }
     }
    },2100);
   }});
  }},420);
 }
 @Override public void onInit(int s){ if(s==TextToSpeech.SUCCESS) tts.setLanguage(Locale.US); }
 @Override public void onDestroy(){ super.onDestroy(); try{ if(tts!=null) tts.shutdown(); wm.removeView(slimeView); wm.removeView(chatView);}catch(Exception e){} }
 @Override public IBinder onBind(Intent i){ return null; }
}
