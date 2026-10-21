package com.slime;
import android.media.MediaPlayer;
import android.os.Handler; import android.os.Looper;
import android.speech.tts.TextToSpeech;
import java.io.File; import java.io.FileOutputStream; import java.io.InputStream; import java.io.OutputStream;
import java.net.HttpURLConnection; import java.net.URL; import java.net.URLEncoder;
import java.util.ArrayList; import java.util.Collections; import java.util.HashSet; import java.util.Locale; import java.util.Set;
// Slime's voice. Neural cloud TTS (keyless, MP3) with automatic fallback to an on-device engine.
// auto (default): try cloud, use local when offline or the endpoint fails. local/cloud force one path.
class Voice {
 static final String MODE_AUTO="auto", MODE_CLOUD="cloud", MODE_LOCAL="local";
 static final String DEFAULT_CLOUD="Brian";
 static final HashSet<String> CLOUD_VOICES=new HashSet<String>();
 static{
  String[] v={"Brian","Amy","Emma","Joanna","Matthew","Salli","Kimberly","Kendra","Ivy","Justin","Joey","Nicole","Russell","Raveena","Zhiyu","Aditi"};
  for(String x:v) CLOUD_VOICES.add(x);
 }
 final MainService s; final Handler h=new Handler(Looper.getMainLooper());
 TextToSpeech tts; boolean ready;
 MediaPlayer player; final ArrayList<File> queue=new ArrayList<File>();
 boolean speaking; Runnable idleListener; String pending;
 Voice(MainService s){ this.s=s; initLocal(s.sp.getString("voiceEngine","")); }

 // ---------- local engine ----------
 private void initLocal(final String enginePkg){
  ready=false;
  try{ if(tts!=null) tts.shutdown(); }catch(Exception e){ L.w("Voice",e); }
  TextToSpeech.OnInitListener init=new TextToSpeech.OnInitListener(){ public void onInit(int status){
   if(status!=TextToSpeech.SUCCESS){ L.w("Voice",new Exception("local TTS init failed "+status)); return; }
   ready=true;
   try{
    tts.setLanguage(Locale.US);
    String vn=s.sp.getString("voiceName","");
    if(!vn.isEmpty()){ try{ Set<android.speech.tts.Voice> all=tts.getVoices(); if(all!=null){ for(android.speech.tts.Voice vc:all){ if(vc.getName().equals(vn)){ tts.setVoice(vc); break; } } } }catch(Exception e){ L.w("Voice",e); } }
    tts.setSpeechRate(s.sp.getFloat("voiceRate",1f)); tts.setPitch(s.sp.getFloat("voicePitch",1f));
   }catch(Exception e){ L.w("Voice",e); }
   if(pending!=null){ String p=pending; pending=null; speak(p); }
  }};
  try{ tts=(enginePkg==null||enginePkg.isEmpty())?new TextToSpeech(s,init):new TextToSpeech(s,init,enginePkg); }
  catch(Exception e){ L.w("Voice",e); tts=new TextToSpeech(s,init); }
 }
 void setIdleListener(Runnable r){ idleListener=r; }
 boolean isSpeaking(){ return speaking; }
 private void markSpeaking(boolean v){ speaking=v; if(!v&&idleListener!=null) idleListener.run(); }

 // ---------- public API ----------
 boolean enabled(){ return s.sp.getBoolean("ttsOn",false); }
 void setEnabled(boolean on){ s.sp.edit().putBoolean("ttsOn",on).apply(); if(!on) flush(); }
 String mode(){ return s.sp.getString("voiceMode",MODE_AUTO); }
 void speak(String text){
  if(text==null) return;
  text=MathText.speakable(text).trim();
  if(text.isEmpty()||!enabled()) return;
  flush();
  markSpeaking(true);
  String mode=mode();
  if(MODE_LOCAL.equals(mode)){ localSpeak(text); return; }
  cloudSpeak(text,MODE_AUTO.equals(mode));
 }
 void flush(){
  h.removeCallbacksAndMessages(null);
  queue.clear();
  try{ if(player!=null){ player.release(); } }catch(Exception e){ L.w("Voice",e); }
  player=null;
  try{ if(tts!=null) tts.stop(); }catch(Exception e){ L.w("Voice",e); }
  markSpeaking(false);
 }
 void destroy(){ flush(); try{ if(tts!=null) tts.shutdown(); }catch(Exception e){ L.w("Voice",e); } tts=null; }

 // ---------- local path ----------
 private void localSpeak(final String text){
  if(tts==null){ markSpeaking(false); return; }
  if(!ready){ pending=text; return; }
  try{
   tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener(){
    public void onStart(String id){}
    public void onDone(String id){ markSpeaking(false); }
    public void onError(String id){ markSpeaking(false); }
   });
   int r=tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"slime");
   if(r==TextToSpeech.ERROR){ markSpeaking(false); }
  }catch(Exception e){ L.w("Voice",e); markSpeaking(false); }
 }

 // ---------- cloud path ----------
 private void cloudSpeak(final String text,final boolean fallbackLocal){
  new Thread(new Runnable(){ public void run(){
   ArrayList<File> files=new ArrayList<File>();
   try{
    String voice=s.sp.getString("cloudVoice",DEFAULT_CLOUD);
    if(!CLOUD_VOICES.contains(voice)) voice=DEFAULT_CLOUD;
    for(String chunk:chunks(text,300)){
     String u="https://api.streamelements.com/kappa/v2/speech?voice="+URLEncoder.encode(voice,"UTF-8")+"&text="+URLEncoder.encode(chunk,"UTF-8");
     HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();
     c.setConnectTimeout(12000); c.setReadTimeout(20000); c.setRequestProperty("Accept","audio/mpeg");
     if(c.getResponseCode()!=200) throw new Exception("tts http "+c.getResponseCode());
     File f=File.createTempFile("slime_tts_",".mp3",s.getCacheDir()); f.deleteOnExit();
     InputStream in=c.getInputStream(); OutputStream out=new FileOutputStream(f);
     byte[] buf=new byte[4096]; int n; while((n=in.read(buf))>0) out.write(buf,0,n); out.close(); in.close();
     files.add(f);
    }
    final ArrayList<File> got=files;
    h.post(new Runnable(){ public void run(){ playQueue(got); }});
   }catch(Exception e){
    L.w("Voice",e);
    for(File f:files){ try{ f.delete(); }catch(Exception x){ L.w("Voice",x); } }
    if(fallbackLocal) h.post(new Runnable(){ public void run(){ localSpeak(text); }});
    else h.post(new Runnable(){ public void run(){ markSpeaking(false); }});
   }
  }}).start();
 }
 private void playQueue(ArrayList<File> files){
  queue.addAll(files); playNext();
 }
 private void playNext(){
  if(queue.isEmpty()){ markSpeaking(false); return; }
  final File f=queue.remove(0);
  try{
   player=new MediaPlayer();
   player.setDataSource(f.getAbsolutePath());
   player.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){ public void onCompletion(MediaPlayer mp){
    try{ mp.release(); }catch(Exception e){ L.w("Voice",e); }
    player=null; try{ f.delete(); }catch(Exception e){ L.w("Voice",e); }
    playNext();
   }});
   player.setOnErrorListener(new MediaPlayer.OnErrorListener(){ public boolean onError(MediaPlayer mp,int what,int extra){
    try{ mp.release(); }catch(Exception e){ L.w("Voice",e); }
    player=null; try{ f.delete(); }catch(Exception e){ L.w("Voice",e); }
    queue.clear(); markSpeaking(false); return true;
   }});
   player.prepare(); player.start();
  }catch(Exception e){ L.w("Voice",e); try{ f.delete(); }catch(Exception x){ L.w("Voice",x); } queue.clear(); markSpeaking(false); }
 }
 private ArrayList<String> chunks(String text,int max){
  ArrayList<String> out=new ArrayList<String>();
  String[] parts=text.split("(?<=[.!?])\\s+|\n+");
  StringBuilder cur=new StringBuilder();
  for(String p:parts){
   p=p.trim(); if(p.isEmpty()) continue;
   while(p.length()>max){
    if(cur.length()>0){ out.add(cur.toString()); cur.setLength(0); }
    int cut=p.lastIndexOf(' ',max); if(cut<20) cut=max;
    out.add(p.substring(0,cut).trim()); p=p.substring(cut).trim();
   }
   if(cur.length()+p.length()+1>max){ out.add(cur.toString()); cur.setLength(0); }
   if(cur.length()>0) cur.append(' '); cur.append(p);
  }
  if(cur.length()>0) out.add(cur.toString());
  return out;
 }

 // ---------- commands ----------
 boolean handle(String t,String low){
  low=low.trim();
  if(low.equals("tts")||low.equals("voice on")||low.equals("tts on")){ setEnabled(true); s.local("voice on ("+mode()+" mode)",false); return true; }
  if(low.equals("tts off")||low.equals("voice off")){ setEnabled(false); s.local("voice off",false); return true; }
  if(low.startsWith("tts mode ")){
   String m=low.substring(9).trim();
   if(m.equals(MODE_AUTO)||m.equals(MODE_CLOUD)||m.equals(MODE_LOCAL)){ s.sp.edit().putString("voiceMode",m).apply(); s.local("voice mode: "+m,false); }
   else s.local("say: tts mode auto, cloud or local",false);
   return true;
  }
  if(low.equals("tts test")){ setEnabled(true); s.speak("hey, it's me, slime. this is my voice."); return true; }
  if(low.startsWith("tts cloud voice ")||low.startsWith("tts voice ")){
   String name=t.substring(low.startsWith("tts cloud")?16:10).trim();
   if(CLOUD_VOICES.contains(name)){ s.sp.edit().putString("cloudVoice",name).apply(); s.local("cloud voice: "+name+". say tts test to hear it.",false); }
   else s.local("pick one of: "+new ArrayList<String>(CLOUD_VOICES).subList(0,8)+"... (say tts cloud voices for all)",false);
   return true;
  }
  if(low.equals("tts cloud voices")){ ArrayList<String> all=new ArrayList<String>(CLOUD_VOICES); Collections.sort(all); s.local("cloud voices: "+all,false); return true; }
  if(low.equals("tts engines")){ listEngines(); return true; }
  if(low.startsWith("tts engine ")){
   String arg=low.substring(11).trim();
   TextToSpeech.EngineInfo pick=findEngine(arg);
   if(pick==null) s.local("no engine matching that. say tts engines for the list",false);
   else { s.sp.edit().putString("voiceEngine",pick.name).apply(); initLocal(pick.name); s.local("local engine: "+pick.label,false); }
   return true;
  }
  if(low.equals("voice list")){ listVoices(); return true; }
  if(low.startsWith("voice ")){
   String arg=low.substring(6).trim();
   if(tts==null||!ready){ s.local("local engine still waking up, try again in a second",false); return true; }
   android.speech.tts.Voice pick=findVoice(arg);
   if(pick==null) s.local("no voice matching that. say voice list for options",false);
   else { tts.setVoice(pick); s.sp.edit().putString("voiceName",pick.getName()).apply(); s.local("local voice: "+pick.getName(),false); }
   return true;
  }
  if(low.startsWith("tts speed ")){ Float f=num(low.substring(10)); if(f!=null&&f>=0.5f&&f<=2f){ s.sp.edit().putFloat("voiceRate",f).apply(); if(tts!=null) tts.setSpeechRate(f); s.local("speed "+f,false); } else s.local("give a number from 0.5 to 2",false); return true; }
  if(low.startsWith("tts pitch ")){ Float f=num(low.substring(10)); if(f!=null&&f>=0.5f&&f<=2f){ s.sp.edit().putFloat("voicePitch",f).apply(); if(tts!=null) tts.setPitch(f); s.local("pitch "+f,false); } else s.local("give a number from 0.5 to 2",false); return true; }
  return false;
 }
 private Float num(String x){ try{ return Float.parseFloat(x.trim()); }catch(Exception e){ return null; } }
 private void listEngines(){
  if(tts==null){ s.local("engine not ready yet",false); return; }
  ArrayList<TextToSpeech.EngineInfo> es=new ArrayList<TextToSpeech.EngineInfo>(tts.getEngines());
  if(es.isEmpty()){ s.local("no other TTS engines installed; the cloud voice still works",false); return; }
  StringBuilder sb=new StringBuilder("local engines (switch with tts engine N or name):\n");
  String cur=s.sp.getString("voiceEngine","");
  for(int i=0;i<es.size();i++) sb.append(i+1).append(". ").append(es.get(i).label).append(es.get(i).name.equals(cur)?" *\n":"\n");
  s.showChat(); s.local(sb.toString(),false);
 }
 private TextToSpeech.EngineInfo findEngine(String arg){
  if(tts==null) return null;
  ArrayList<TextToSpeech.EngineInfo> es=new ArrayList<TextToSpeech.EngineInfo>(tts.getEngines());
  if(arg.matches("\\d+")){ int i=Integer.parseInt(arg)-1; return (i>=0&&i<es.size())?es.get(i):null; }
  for(TextToSpeech.EngineInfo e:es){ if(e.name.toLowerCase().contains(arg)||e.label.toLowerCase().contains(arg)) return e; }
  return null;
 }
 private void listVoices(){
  if(tts==null||!ready){ s.local("local engine still waking up, try again in a second",false); return; }
  Set<android.speech.tts.Voice> all=tts.getVoices();
  ArrayList<android.speech.tts.Voice> en=new ArrayList<android.speech.tts.Voice>();
  if(all!=null){ for(android.speech.tts.Voice v:all){ if(v.getLocale()!=null&&"en".equals(v.getLocale().getLanguage())&&!v.isNetworkConnectionRequired()) en.add(v); } }
  if(en.isEmpty()){ s.local("no offline English voices found; the cloud voice still works",false); return; }
  StringBuilder sb=new StringBuilder("local voices (pick with voice N or name):\n");
  for(int i=0;i<Math.min(20,en.size());i++) sb.append(i+1).append(". ").append(en.get(i).getName()).append('\n');
  s.showChat(); s.local(sb.toString(),false);
 }
 private android.speech.tts.Voice findVoice(String arg){
  if(tts==null) return null;
  Set<android.speech.tts.Voice> all=tts.getVoices(); if(all==null) return null;
  if(arg.matches("\\d+")){
   ArrayList<android.speech.tts.Voice> en=new ArrayList<android.speech.tts.Voice>();
   for(android.speech.tts.Voice v:all){ if(v.getLocale()!=null&&"en".equals(v.getLocale().getLanguage())&&!v.isNetworkConnectionRequired()) en.add(v); }
   int i=Integer.parseInt(arg)-1; return (i>=0&&i<en.size())?en.get(i):null;
  }
  for(android.speech.tts.Voice v:all){ if(v.getName().toLowerCase().contains(arg)) return v; }
  return null;
 }
}
