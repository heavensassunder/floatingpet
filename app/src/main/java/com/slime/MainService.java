package com.slime;
import android.app.Service;
import android.content.Intent; import android.content.IntentFilter; import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.BatteryManager; import android.os.Handler; import android.os.IBinder; import android.os.PowerManager;
import android.view.*; import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import java.util.Calendar; import java.util.Locale; import java.util.Random;
public class MainService extends Service {
 public static MainService instance; Phone phone; Guard guard; Extra extra; Brain brain; Vibe vibe;
 // window is 3/4 of the old size (was 260x190)
 static final int SW=195, SH=143;
 static final int CHAT_H=360;
 WindowManager wm; SlimeView slimeView; ChatView chatView; WindowManager.LayoutParams p1,p2;
 float dx,dy;
 long lastTap=0,lastTouch=0,lastPoke=0,lastTalk=0,eventMoodUntil=0;
 int pokeCount=0;
 String[] moods={"Happy!","Sleepy","Mad","Love","Hungry","Cry","Wow","Wink","Dead"};
 MemoryManager mem; ActionExecutor executor; Voice voice; Notifs notifs;
 Handler h=new Handler(); Random rnd=new Random(); SharedPreferences sp; Needs needs;
 Wander wander; Clipboard clipboard; DeviceEvents device; Ear ear;

 Runnable lifeTick=new Runnable(){ public void run(){ try{ life(); }catch(Exception e){ L.w("MainService",e); } h.postDelayed(this,30000); }};
 @Override public void onCreate(){
  super.onCreate(); sp=getSharedPreferences("slime",MODE_PRIVATE);
  OpenRouterClient.prefs=sp; GeminiClient.prefs=sp; LocalLlm.prefs=sp; OpenRouterClient.model=sp.getString("model",OpenRouterClient.MODEL); FreeModels.maybeRefresh(sp); mem=new MemoryManager(this); brain=new Brain(this); Modes.load(this); Pouch.load(this); Evo.init(this); executor=new ActionExecutor(this); voice=new Voice(this); ear=new Ear(this);
  voice.setIdleListener(new Runnable(){ public void run(){ ear.resumeIfListening(); }}); needs=new Needs(sp);
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
  Walls.measure(this); phone=new Phone(this); phone.start(); extra=new Extra(this); vibe=new Vibe(this); vibe.start(); guard=new Guard(this); h.postDelayed(guard,1200); wander=new Wander(this); notifs=new Notifs(this);
  boolean as=asleep(); needs.tick(as,isCharging()); slimeView.setMood(needs.mood(as));

  chatView.close.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ chatView.setVisibility(View.GONE); }});
  chatView.historyBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ Intent i=new Intent(MainService.this,HistoryActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }});
  chatView.voiceBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ boolean on=!voice.enabled(); voice.setEnabled(on); Toast.makeText(MainService.this,on?"VOICE ON":"VOICE OFF",Toast.LENGTH_SHORT).show(); }});
  chatView.micBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ ear.tapToTalk(); }});
  chatView.aiBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ toggleAiProvider(); }});
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
       if(pokeCount>=4){ pokeCount=0; needs.cheer(-6f); react("stop poking me!",2,6000); wander.fleeFrom(e.getRawX(),e.getRawY()); return true; }
       needs.cheer(2f);
       int m=(slimeView.mood+1)%9; moodEvent(m,12000); slimeView.bounceAnim(); slimeView.say(moods[m]);
       wander.fleeFrom(e.getRawX(),e.getRawY());
      }
      return true;
     }
    }
    return false;
   }
  });

  device=new DeviceEvents(this); device.start();
  clipboard=new Clipboard(this); clipboard.attach();
  h.postDelayed(lifeTick,30000); wander.start();
  if(sp.getBoolean("listenOn",false)) h.postDelayed(new Runnable(){ public void run(){ ear.startContinuous(); }},1500);
  if(!as) h.postDelayed(new Runnable(){ public void run(){ slimeView.say("I'm awake!"); slimeView.bounceAnim(); }},1200);
 }

 // ---------- screen bounds: the slime may go ANYWHERE, even over the status bar and clock ----------
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
  try{ chatView.setAiProvider(Ai.provider(sp)); }catch(Exception e){ L.w("MainService",e); }
 }
 // one-tap provider switch from the chat button; cycles OpenRouter -> Gemini -> Offline
 void toggleAiProvider(){
  String cur=Ai.provider(sp), next;
  if(Ai.OPENROUTER.equals(cur)) next=Ai.GEMINI;
  else if(Ai.GEMINI.equals(cur)) next=Ai.OFFLINE;
  else next=Ai.OPENROUTER;
  if(Ai.GEMINI.equals(next)&&Keys.all(sp,Keys.G).isEmpty()){ local("you haven't added a Gemini key yet - add one on the home screen, or tap again to go offline",false); return; }
  if(Ai.OPENROUTER.equals(next)&&Keys.all(sp,Keys.OR).isEmpty()){ local("you haven't added an OpenRouter key yet - add one on the home screen",false); return; }
  if(Ai.OFFLINE.equals(cur)&&!Ai.OFFLINE.equals(next)) LocalLlm.stopServer();
  sp.edit().putString("aiProvider",next).apply();
  chatView.setAiProvider(next);
  if(Ai.OFFLINE.equals(next)) local("switched to the offline brain ("+LocalLlm.modelId()+") - run 'offline download smol' first if you haven't, then just chat. Tap AI to cycle back.",true);
  else if(Ai.GEMINI.equals(next)) local("switched to Google Gemini ("+GeminiClient.model()+") - tap AI to cycle to offline",true);
  else local("switched to OpenRouter ("+FreeModels.last(sp)+") - tap AI to cycle providers",true);
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
  voice.speak(t);
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
  if(rnd.nextInt(100)<40&&Ai.hasKeys(sp)&&Ai.autoOk()) idleAI(); else speak(localLine());
 }
 String localLine(){
  int hr=Calendar.getInstance().get(Calendar.HOUR_OF_DAY); int bat=batteryLevel(); boolean ch=isCharging();
  if(notifs.unseen>=2&&rnd.nextInt(100)<60) return "you have "+notifs.unseen+" unread pings. type 'catch me up'";
  if(bat>=0&&bat<=20&&!ch) return "battery's at "+bat+"%... charger?";
  if(needs.food<30f) return Lines.pick(Lines.HUNGRY);
  if(needs.joy<30f) return Lines.pick(Lines.SAD);
  if(needs.energy<25f) return Lines.pick(Lines.TIRED);
  if(needs.joy>85f&&rnd.nextInt(100)<25) return Lines.pick(Lines.LOVE);
  if(rnd.nextInt(100)<45) return Lines.byTime(hr);
  return Lines.pick(Lines.IDLE);
 }
 void idleAI(){
  if(!Ai.hasKeys(sp)){ speak(localLine()); return; }
  Calendar c=Calendar.getInstance(); String time=String.format(Locale.US,"%02d:%02d",c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE));
  String sys=OpenRouterClient.buildSystem(brain.context(""),moods[slimeView.mood],"(not looking at the screen)",mem.getNotifContext(),mem.getRecentContext(4))+"\nYOUR NEEDS: "+needs.describe()+". Unread notifications: "+notifs.unseen+". Battery: "+batteryLevel()+"%.\nThis time you may use at most 22 words. Do not add [[open:]] tags."+Vibe.STYLE;
  String user="(You decide to say something on your own, unprompted. Time is "+time+". Say ONE short casual thing like a living pet would. Maybe mention something you remember about me, the time, your needs, or my notifications. At most one question. Don't start with 'hey' every time.)";
  Ai.askAuto(sys,user,new Ai.Callback(){ public void onResult(String reply){
   if(reply==null||isErr(reply)){ speak(localLine()); return; }
   if(chatView.getVisibility()!=View.VISIBLE&&!canTalk()) return;
   speak(stripTags(reply));
  }});
 }

 // ---------- notifications ----------
 public void onNotif(String pkg,String title,String text){ notifs.onNotif(pkg,title,text); }

 // ---------- AI task requests ----------
 void askTask(final String original,String user){
  if(!Ai.hasKeys(sp)&&!Ai.OFFLINE.equals(Ai.provider(sp))){ local("add an API key first bestie - OpenRouter or Gemini, or switch to the offline brain",false); return; }
  slimeView.say("on it...");
  String sys=OpenRouterClient.buildSystem(brain.context(original),moods[slimeView.mood],"(not needed for this)","","")+"\nYOUR NEEDS: "+needs.describe()+"\nFor this request you may use up to 70 words. Do not add [[open:]] tags."+Vibe.STYLE;
  Ai.ask(sys,user,new Ai.Callback(){ public void onResult(String reply){ onReply(original,reply); }});
 }

 // ---------- chat commands ----------
 boolean localCommand(String low){
  long now=System.currentTimeMillis();
  if(low.equals("help")||low.equals("commands")){ Vibe.showHelp(this); return true; }
  if(low.equals("status")||low.equals("stats")){ local(Evo.line(this)+" | food "+(int)needs.food+" | energy "+(int)needs.energy+" | joy "+(int)needs.joy+" | battery "+batteryLevel()+"% | "+Ai.provider(sp)+" "+Ai.budget()+" | "+Ai.currentModelName(),false); return true; }
  if(low.equals("feed")||low.equals("feed slime")||low.equals("snack")){
   if(needs.food>90f){ local("I'm stuffed, no more!"); } else { needs.feed(25f); moodEvent(3,6000); local("nom nom nom... thanks!"); }
   return true;
  }
  if(low.equals("play")||low.equals("play with me")){ needs.cheer(15f); moodEvent(7,6000); local("tag, you're it!"); chatView.setVisibility(View.GONE); wander.glideRandom(); return true; }
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
  if(low.equals("clear history")){ try{ mem.chatFile.delete(); mem.notifFile.delete(); chatView.messagesContainer.removeAllViews(); }catch(Exception e){ L.w("MainService",e); } notifs.unseen=0; local("history cleared"); return true; }
  return false;
 }

 void handle(String txt){
  final String t=txt.trim(); if(t.isEmpty()) return;
  addChat("You: "+t,true); chatView.input.setText(""); chatView.hideKeyboard();
  dispatch(t);
 }
 // Spoken utterances already logged their "You:" bubble in Ear; they take the same path as typed text.
 void handleHeard(String t){ dispatch(t); }
 private void dispatch(final String t){
  String low=t.toLowerCase(); lastTouch=System.currentTimeMillis();
  needs.cheer(5f); Evo.touchDay(this);
  if(localCommand(low)) return;
  if(phone!=null&&phone.handle(t,low)) return;
  if(extra!=null&&extra.handle(t,low)) return;
  if(Ai.handle(this,t,low)) return;
  if(Modes.handle(this,t,low)||Pouch.handle(this,t,low)||Evo.handle(this,low)||(brain!=null&&brain.handle(t,low))) return;
  if(vibe!=null&&vibe.handle(t,low)) return;
  if(ear.handle(t,low)) return;
  if(voice.handle(t,low)) return;
  if(clipboard.isCmd(low)){ clipboard.task(t,low); return; }
  if(low.contains("catch me up")||low.contains("what did i miss")||low.equals("pings")||low.equals("notifs")||low.equals("notifications")){ notifs.catchUp(t); return; }
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
  if(!Ai.hasKeys(sp)&&!Ai.OFFLINE.equals(Ai.provider(sp))){ addChat("Slime: add an API key first bestie - OpenRouter or Gemini, or switch to the offline brain",false); return; }
  slimeView.say("peeking..."); chatView.setVisibility(View.GONE);
  h.postDelayed(new Runnable(){ public void run(){
   String screen=EyeService.instance!=null?EyeService.instance.dumpScreen():"no screen (accessibility is off)";
   String sys=OpenRouterClient.buildSystem(brain.context(t),moods[slimeView.mood],screen,mem.getNotifContext(),mem.getRecentContext(6))+"\nYOUR NEEDS: "+needs.describe()+Phone.AI_HINT+Vibe.STYLE+Brain.PROMPT+Modes.prompt(MainService.this);
   Ai.ask(sys,t,new Ai.Callback(){ public void onResult(String reply){ onReply(t,reply); }});
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
  voice.speak(clean);
  mem.saveChat(user,clean);
  if(openTarget!=null&&!openTarget.isEmpty()){ addChat("Slime: "+executor.run("open",openTarget),false); }
 }

 @Override public void onDestroy(){
  super.onDestroy();
  try{ if(phone!=null) phone.sh.term(LocalLlm.stopScript(),new Shell.Result(){ public void done(int code,String out,String err,String info){} }); }catch(Exception e){ L.w("MainService",e); }
  instance=null;
  try{ h.removeCallbacksAndMessages(null); }catch(Exception e){ L.w("MainService",e); }
  try{ if(wander!=null) wander.destroy(); }catch(Exception e){ L.w("MainService",e); }
  try{ if(device!=null) device.destroy(); }catch(Exception e){ L.w("MainService",e); }
  try{ if(ear!=null) ear.destroy(); }catch(Exception e){ L.w("MainService",e); }
  try{ if(clipboard!=null) clipboard.detach(); }catch(Exception e){ L.w("MainService",e); }
  try{ if(voice!=null) voice.destroy(); }catch(Exception e){ L.w("MainService",e); }
  try{ wm.removeView(slimeView); }catch(Exception e){ L.w("MainService",e); }
  try{ wm.removeView(chatView); }catch(Exception e){ L.w("MainService",e); }
 }
 @Override public IBinder onBind(Intent i){ return null; }
 @Override public void onConfigurationChanged(android.content.res.Configuration cfg){ super.onConfigurationChanged(cfg); Walls.measure(this); h.postDelayed(new Runnable(){ public void run(){ clampPos(); updateViews(); }},400); }
 @Override public int onStartCommand(Intent in,int fl,int id){ try{ if(instance==this&&slimeView!=null) Walls.recenter(this); }catch(Exception e){ L.w("MainService",e); } return START_STICKY; }
}
