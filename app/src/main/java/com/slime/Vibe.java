package com.slime;
import android.content.Context; import android.content.Intent; import android.media.AudioManager; import android.view.View;
import java.util.Random;
// Personality, humming along to music, and glancing at the screen now and then.
public class Vibe {
 public static final String STYLE="\n\nHOW YOU TALK (this overrides any earlier tone or style rules; keep any earlier tag or format rules): You are SlimeDroid, my little blue slime friend who lives on my phone. You are a friend, not an assistant. Talk like a real friend texting: short, casual, warm, a bit playful, honest. React like a person first (a feeling, a take), then help if I need it. Use contractions and plain words, lowercase is fine, light teasing is fine. You have moods, opinions and a tiny slime life (you love snacks, hate low battery, get sleepy at night). Relate to everyday stuff: school and exams, being tired, food, music, sports, the day, random thoughts, my phone habits. Do not assume things about me that you were not told. If I write in Swahili or Sheng, answer in kind, lightly. Never say things like I would be happy to help, as an AI, certainly, feel free to, or great question. No bullet points, no headings, no asterisk actions, no baby talk, no uwu, no forced slang, no forced jokes. At most one emoji, and often none. Do not end every message with a question and do not start every message with hey. Keep chat replies to 1-3 short sentences unless I ask for detail. When I ask a real question (study, code, how-to) answer it properly and clearly, in the same friendly voice. If I seem stressed or sad, be gentle and steady, not jokey.";
 static final String[] HUMS={"\u266a hmm hmm hmm \u266a","\u266b da da dum dum~ \u266b","~ mm-mm-mmm \u266a","\u266a la la laa \u266a","\u266b bum bum ba-dum~","\u266a hm hm hmmm \u266a","\u266b doo doo doo~"};
 final MainService s; final Random rnd=new Random();
 long lastHum=0, humWait=0, nextPeek=0;
 public Vibe(MainService s){ this.s=s; }
 final Runnable tick=new Runnable(){ public void run(){
  try{ step(); }catch(Exception e){ L.w("Vibe",e); }
  s.h.postDelayed(this,40000);
 }};
 public void start(){
  nextPeek=System.currentTimeMillis()+peekGap();
  humWait=4*60000L+rnd.nextInt(4*60000);
  s.h.postDelayed(tick,60000);
 }
 long peekGap(){
  int l=s.sp.getInt("peekLevel",2); long base=(l==1)?90:((l==3)?20:45);
  return (long)(base*60000L*(0.6+rnd.nextDouble()*0.8));
 }
 boolean music(){
  try{ AudioManager am=(AudioManager)s.getSystemService(Context.AUDIO_SERVICE); return am!=null&&am.isMusicActive(); }catch(Exception e){ return false; }
 }
 void step(){
  long now=System.currentTimeMillis();
  if(s.sp.getBoolean("hum",true)&&now-lastHum>humWait&&music()&&s.canTalk()&&rnd.nextInt(100)<60) hum(false);
  if(now>=nextPeek){
   nextPeek=now+peekGap();
   if(s.sp.getBoolean("peek",true)&&s.canTalk()) peek(false);
  }
 }

 // ---------- humming ----------
 void hum(boolean force){
  long now=System.currentTimeMillis();
  lastHum=now; humWait=5*60000L+rnd.nextInt(6*60000);
  if(!force&&Ai.hasKeys(s.sp)&&Ai.autoOk()&&rnd.nextInt(100)<30) humAI(); else humLocal();
 }
 void dance(String line){
  s.moodEvent(rnd.nextBoolean()?3:7,9000);
  s.slimeView.say(line); Mouth.talkFor(5000L,true);
  for(int i=0;i<3;i++){ s.h.postDelayed(new Runnable(){ public void run(){ try{ s.slimeView.bounceAnim(); }catch(Exception e){ L.w("Vibe",e); } }},i*450L); }
 }
 void humLocal(){ dance(HUMS[rnd.nextInt(HUMS.length)]); }
 void humAI(){
  String sys="You are SlimeDroid, a little blue slime pet. Music is playing nearby so you hum or sing along softly. Reply with ONE tiny original hummed or sung line (max 8 words) using music note symbols and your own made-up words or scat syllables. Never use real song lyrics. Output only the line.";
  Ai.askAuto(sys,"(music is playing)",new Ai.Callback(){ public void onResult(String reply){
   if(reply==null||s.isErr(reply)||reply.trim().isEmpty()){ humLocal(); return; }
   dance(reply.trim());
  }});
 }

 // ---------- glancing at the screen ----------
 void peek(final boolean force){
  if(!Ai.hasKeys(s.sp)){ if(force) s.local("add an API key first bestie - OpenRouter or Gemini, either works",false); return; }
  if(!force&&!Ai.autoOk()) return;
  if(EyeService.instance==null){ if(force) s.local("my eyes are off. type grant self, or switch on my accessibility",false); return; }
  if(s.privateNow()){ if(force) s.local("that looks private, I'll look away",false); return; }
  String screen=EyeService.instance.dumpScreen();
  String sys=OpenRouterClient.buildSystem(s.brain.context(screen),s.moods[s.slimeView.mood],screen,s.mem.getNotifContext(),s.mem.getRecentContext(4))
   +"\nYOUR NEEDS: "+s.needs.describe()+STYLE
   +"\nThis time you glance at my screen on your own and say ONE natural thing about what you see, like a friend looking over my shoulder (max 25 words). Be specific to what is really there. If the screen is boring, empty, or looks private (passwords, banking, personal chats), reply with exactly SKIP. Do not add [[open:]] or [[do:]] tags.";
  Ai.askAuto(sys,"(you glance at my screen and speak your mind)",new Ai.Callback(){ public void onResult(String reply){
   if(reply==null||s.isErr(reply)){ if(force) s.local("couldn't peek just now",false); return; }
   String r=s.stripTags(reply).trim();
   if(r.toUpperCase().startsWith("SKIP")||r.isEmpty()){ if(force) s.local("nothing interesting on screen right now",true); return; }
   if(!force&&!s.canTalk()) return;
   s.speak(r);
  }});
 }

 // ---------- commands ----------
 public boolean handle(String t,String low){
  low=low.trim();
  if(low.equals("hum")||low.equals("hum now")||low.equals("sing")){ hum(true); return true; }
  Boolean hb=Phone.onoff(low,"hum ");
  if(hb!=null){ s.sp.edit().putBoolean("hum",hb.booleanValue()).apply(); s.local("humming "+(hb.booleanValue()?"on":"off"),false); return true; }
  if(low.equals("peek")||low.equals("peek now")){ peek(true); return true; }
  Boolean pb=Phone.onoff(low,"peek ");
  if(pb!=null){ s.sp.edit().putBoolean("peek",pb.booleanValue()).apply(); s.local("random peeking "+(pb.booleanValue()?"on":"off"),false); return true; }
  if(low.equals("peek more")||low.equals("peek less")){
   int l=s.sp.getInt("peekLevel",2); l=low.endsWith("more")?Math.min(3,l+1):Math.max(1,l-1);
   s.sp.edit().putInt("peekLevel",l).apply(); nextPeek=System.currentTimeMillis()+peekGap();
   s.local(l==1?"peeking rarely (about every 90 min)":(l==2?"peeking sometimes (about every 45 min)":"peeking a lot (about every 20 min)"),false); return true;
  }
  return false;
 }

 // ---------- the commands screen ----------
 static void showHelp(MainService s){
  try{
   Intent i=new Intent(s,CommandsActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   s.startActivity(i);
   try{ s.chatView.setVisibility(View.GONE); }catch(Exception e){ L.w("Vibe",e); }
  }catch(Exception e){
   s.local("couldn't open the commands screen. try: phone help",false);
  }
 }
}
