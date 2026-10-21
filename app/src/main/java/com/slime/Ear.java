package com.slime;
import android.app.Notification; import android.app.NotificationChannel; import android.app.NotificationManager;
import android.app.PendingIntent; import android.content.Intent; import android.content.pm.PackageManager; import android.content.pm.ServiceInfo;
import android.os.Build; import android.os.Bundle; import android.os.Handler; import android.os.Looper;
import android.speech.RecognitionListener; import android.speech.RecognizerIntent; import android.speech.SpeechRecognizer;
import java.util.ArrayList;
// Slime's ear: on-device speech recognition (no API, no cost).
// Screen-on only: the mic is released while the screen is off and re-acquired when it comes back.
class Ear {
 static final int NOTIF_ID=4242; static final String CHANNEL="slime_ear";
 final MainService s; final Handler h=new Handler(Looper.getMainLooper());
 SpeechRecognizer sr; boolean continuous, active, fgs; int backoff; long lastRestart;
 String lastError; long lastErrorAt, lastErrorShown;
 // diagnostics
 int readyCount, beginCount, resultCount, partialCount, failStreak, speakWaits;
 float lastRms, maxRms; long sessionAt;
 final ArrayList<String> errorLog=new ArrayList<String>();
 boolean onDeviceTried; String engineMode;
 private Runnable retry; private boolean testMode;
 Ear(MainService s){ this.s=s; }

 private boolean permitted(){ try{ return s.checkSelfPermission("android.permission.RECORD_AUDIO")==PackageManager.PERMISSION_GRANTED; }catch(Exception e){ L.w("Ear",e); return false; } }
 private boolean available(){ try{ return SpeechRecognizer.isRecognitionAvailable(s); }catch(Exception e){ L.w("Ear",e); return false; } }
 private boolean onDeviceAvailable(){
  if(Build.VERSION.SDK_INT<31) return false;
  try{ return SpeechRecognizer.isOnDeviceRecognitionAvailable(s); }catch(Exception e){ L.w("Ear",e); return false; }
 }
 private boolean googleAppPresent(){
  try{ s.getPackageManager().getPackageInfo("com.google.android.googlequicksearchbox",0); return true; }
  catch(Exception e){ return false; }
 }
 String lang(){ return "sw".equals(s.sp.getString("listenLang","en"))?"sw-KE":"en-US"; }
 static String errorName(int code){
  switch(code){
   case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "network timeout";
   case SpeechRecognizer.ERROR_NETWORK: return "network error (check internet)";
   case SpeechRecognizer.ERROR_AUDIO: return "audio capture failed (mic busy or blocked)";
   case SpeechRecognizer.ERROR_SERVER: return "speech service server error";
   case SpeechRecognizer.ERROR_CLIENT: return "recognizer client error";
   case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "speech timeout (no voice heard)";
   case SpeechRecognizer.ERROR_NO_MATCH: return "no speech matched";
   case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "recognizer busy";
   case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "mic permission denied";
   default: return "recognizer error "+code;
  }
 }
 private void noteError(int code){
  lastError=errorName(code); lastErrorAt=System.currentTimeMillis();
  if(errorLog.size()>=5) errorLog.remove(0);
  errorLog.add(code+" "+lastError);
  L.w("Ear",new Exception("SpeechRecognizer: "+lastError));
 }
 private void showErrorOnce(String msg){
  long now=System.currentTimeMillis();
  if(now-lastErrorShown<15000) return;
  lastErrorShown=now; s.local(msg,false);
 }
 private String guidance(){
  StringBuilder g=new StringBuilder("I can't get the speech engine working. Check: 1) Settings > Apps > SlimeDroid > Permissions > Microphone allowed; 2) Android's mic/privacy toggle is on; ");
  if(!googleAppPresent()) g.append("3) install or enable the Google app (it provides speech recognition) and any 'Speech Recognition & Synthesis' updates; ");
  else g.append("3) the Google app isn't disabled and its microphone permission is allowed; ");
  g.append("then say 'listen test' again.");
  return g.toString();
 }

 // ---------- one tap, one utterance ----------
 void tapToTalk(){
  if(!permitted()){ s.local("I can't hear you yet: allow the microphone permission on the home screen first.",false); return; }
  if(!available()){ s.local("this phone has no speech recognizer installed, so I can't listen.",false); return; }
  if(continuous){ s.local("I'm already listening, just talk.",false); return; }
  if(!ensureFgs()) return;
  s.slimeView.say("listening..."); s.slimeView.bounceAnim();
  startOnce(false);
 }

 // ---------- full narrated diagnostic ----------
 void test(){
  if(!permitted()){ s.local("mic permission missing - allow it on the home screen first.",false); return; }
  if(!ensureFgs()) return;
  testMode=true;
  s.local("listen test starting... I'll narrate every step.",false);
  startOnce(false);
 }

 // ---------- continuous mode ----------
 void startContinuous(){
  if(!permitted()){ s.local("I can't hear you yet: allow the microphone permission on the home screen first.",false); return; }
  if(!available()){ s.local("this phone has no speech recognizer installed, so I can't listen.",false); return; }
  if(continuous) return;
  continuous=true; active=true; backoff=0; failStreak=0;
  engineMode=s.sp.getString("listenEngine","auto");
  s.sp.edit().putBoolean("listenOn",true).apply();
  s.voice.setEnabled(true);
  if(!ensureFgs()){ continuous=false; active=false; s.sp.edit().putBoolean("listenOn",false).apply(); return; }
  s.local("listening mode on. I'll hear you while the screen is on. Say 'listen off' to stop.",false);
  begin();
 }
 void stopContinuous(boolean say){
  continuous=false; active=false; testMode=false;
  h.removeCallbacksAndMessages(null);
  teardown();
  endFgs();
  s.sp.edit().putBoolean("listenOn",false).apply();
  if(say) s.local("listening mode off.",false);
 }
 void onScreenOff(){ active=false; teardown(); }
 void onScreenOn(){ if(continuous){ active=true; begin(); } }
 // Called by Voice when speech output finishes, so the mic resumes promptly.
 void resumeIfListening(){ if((continuous||testMode)&&active&&sr==null&&s.screenOn()) begin(); }
 void destroy(){ stopContinuous(false); }

 // A microphone-typed foreground service is required to hold the mic while only an overlay is on screen.
 private boolean ensureFgs(){
  if(fgs) return true;
  try{
   NotificationManager nm=(NotificationManager)s.getSystemService("notification");
   if(Build.VERSION.SDK_INT>=26){
    NotificationChannel ch=new NotificationChannel(CHANNEL,"SlimeDroid listening",NotificationManager.IMPORTANCE_LOW);
    ch.setDescription("Shown while SlimeDroid is listening for your voice");
    nm.createNotificationChannel(ch);
   }
   Intent open=new Intent(s,MainActivity.class);
   PendingIntent pi=PendingIntent.getActivity(s,0,open,Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0);
   Notification.Builder nb=Build.VERSION.SDK_INT>=26?new Notification.Builder(s,CHANNEL):new Notification.Builder(s);
   Notification n=nb.setContentTitle("SlimeDroid is listening").setContentText("Tap to open - say \"listen off\" to stop").setSmallIcon(android.R.drawable.ic_btn_speak_now).setOngoing(true).setContentIntent(pi).build();
   if(Build.VERSION.SDK_INT>=29) s.startForeground(NOTIF_ID,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
   else s.startForeground(NOTIF_ID,n);
   fgs=true; return true;
  }catch(Exception e){
   L.w("Ear",e);
   s.local("Android blocked the listening service: "+e.getMessage()+". Open SlimeDroid once, then try again; also check Settings > Apps > SlimeDroid > microphone is allowed.",false);
   return false;
  }
 }
 private void endFgs(){
  if(!fgs) return;
  try{ s.stopForeground(true); }catch(Exception e){ L.w("Ear",e); }
  fgs=false;
 }

 private void begin(){
  if(!active||!s.screenOn()) return;
  if(s.voice.isSpeaking()){
   if(++speakWaits>10){ speakWaits=0; }  // safety: never get stuck behind a voice flag forever
   else { schedule(500); return; }
  }
  speakWaits=0;
  startOnce(true);
 }
 private void schedule(long ms){
  if(retry!=null) h.removeCallbacks(retry);
  retry=new Runnable(){ public void run(){ begin(); }};
  h.postDelayed(retry,ms);
 }
 private void teardown(){
  try{ if(sr!=null){ sr.stopListening(); sr.cancel(); sr.destroy(); } }catch(Exception e){ L.w("Ear",e); }
  sr=null;
 }
 private SpeechRecognizer createRecognizer(){
  String pref=s.sp.getString("listenEngine","auto");
  boolean wantOnDevice="ondevice".equals(pref)||("auto".equals(pref)&&onDeviceTried);
  if(wantOnDevice&&Build.VERSION.SDK_INT>=31&&onDeviceAvailable()){
   engineMode="ondevice";
   return SpeechRecognizer.createOnDeviceSpeechRecognizer(s);
  }
  engineMode="default";
  return SpeechRecognizer.createSpeechRecognizer(s);
 }
 private void startOnce(final boolean loop){
  teardown();
  sessionAt=System.currentTimeMillis();
  try{
   sr=createRecognizer();
   sr.setRecognitionListener(new RecognitionListener(){
    public void onReadyForSpeech(Bundle b){
     readyCount++; failStreak=0; backoff=0;
     if(testMode) s.local("1/4 ear open - speak now",false);
    }
    public void onBeginningOfSpeech(){
     beginCount++; maxRms=0f;
     if(testMode) s.local("2/4 hearing your voice",false);
    }
    public void onRmsChanged(float r){
     lastRms=r; if(r>maxRms) maxRms=r;
     if(testMode&&r>1f) s.slimeView.say("hearing... "+(int)r);
    }
    public void onBufferReceived(byte[] b){}
    public void onEndOfSpeech(){
     if(testMode) s.local("3/4 processing what you said",false);
    }
    public void onError(int error){
     noteError(error); failStreak++;
     teardown();
     if(testMode){
      testMode=false;
      s.local("listen test failed at the engine: "+lastError+".\n"+diagnostic(),false);
      if(!continuous) endFgs();
      return;
     }
     if(!loop){
      s.local("I couldn't hear that ("+lastError+"). Tap Mic and try again.",false);
      if(!continuous) endFgs();
      return;
     }
     if(error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){ stopContinuous(false); s.local("microphone permission was taken away, listening stopped.",false); return; }
     boolean serious=error!=SpeechRecognizer.ERROR_NO_MATCH&&error!=SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
     if(serious&&failStreak>=4){
      stopContinuous(false);
      s.local(guidance(),false);
      return;
     }
     if(serious){
      if(error==SpeechRecognizer.ERROR_CLIENT&&"sw-KE".equals(lang())){ s.sp.edit().putString("listenLang","en").apply(); s.local("Swahili speech pack isn't available here, switching to English.",false); }
      else if((error==SpeechRecognizer.ERROR_CLIENT||error==SpeechRecognizer.ERROR_SERVER)&&!"ondevice".equals(engineMode)&&Build.VERSION.SDK_INT>=31&&onDeviceAvailable()&&!onDeviceTried){
       onDeviceTried=true; showErrorOnce("the regular speech engine failed, trying the on-device recognizer once.");
      }
      else showErrorOnce("listening hiccup: "+lastError+". I'll keep trying, or check 'listen status'.");
     }
     long now=System.currentTimeMillis();
     if(now-lastRestart<2000) backoff=Math.min(backoff+1,5); else backoff=0;
     lastRestart=now;
     schedule(backoff==0?400:Math.min(3000,400*(backoff+1)));
    }
    public void onResults(Bundle b){
     ArrayList<String> r=b!=null?b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION):null;
     teardown();
     failStreak=0;
     if(r!=null&&!r.isEmpty()&&!r.get(0).trim().isEmpty()){
      String text=r.get(0).trim();
      resultCount++;
      s.addChat("You: "+text,true);
      if(testMode){ testMode=false; s.local("4/4 recognized: \""+text+"\". The mic and speech engine work.",false); if(!continuous) endFgs(); }
      s.handleHeard(text);
     } else if(testMode){
      testMode=false; s.local("listen test: engine answered but with empty text.\n"+diagnostic(),false); if(!continuous) endFgs();
     }
     if(loop) begin();
     else if(!continuous&&!testMode) endFgs();
    }
    public void onPartialResults(Bundle b){
     ArrayList<String> r=b!=null?b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION):null;
     if(r!=null&&!r.isEmpty()&&!r.get(0).trim().isEmpty()){
      partialCount++;
      String p=r.get(0).trim();
      if(testMode) s.local("partial: "+p,false);
      else s.slimeView.say(p);
     }
    }
    public void onEvent(int i,Bundle b){}
   });
   Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
   i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
   i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,lang());
   i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
   i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
   if("ondevice".equals(engineMode)) i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);
   sr.startListening(i);
  }catch(Exception e){
   L.w("Ear",e); lastError=e.getClass().getSimpleName()+": "+e.getMessage(); lastErrorAt=System.currentTimeMillis();
   if(errorLog.size()>=5) errorLog.remove(0);
   errorLog.add(lastError);
   teardown();
   if(testMode){ testMode=false; s.local("listen test crashed starting: "+lastError+"\n"+diagnostic(),false); if(!continuous) endFgs(); return; }
   if(loop) schedule(2000);
   else { s.local("couldn't start listening: "+e.getMessage(),false); if(!continuous) endFgs(); }
  }
 }
 private String diagnostic(){
  StringBuilder sb=new StringBuilder();
  sb.append("engine: ").append(engineMode==null?"-":engineMode)
   .append(" | recognizer present: ").append(available())
   .append(" | on-device option: ").append(onDeviceAvailable())
   .append(" | Google app: ").append(googleAppPresent()?"yes":"no")
   .append(" | mic permission: ").append(permitted()?"yes":"no")
   .append(" | FGS notification: ").append(fgs?"active":"off")
   .append("\near opened ").append(readyCount).append("x, speech seen ").append(beginCount)
   .append("x, partials ").append(partialCount).append(", results ").append(resultCount)
   .append(", loudest mic level ").append((int)maxRms);
  if(!errorLog.isEmpty()){
   sb.append("\nrecent errors:");
   for(String e:errorLog) sb.append('\n').append(e);
  }
  return sb.toString();
 }

 // ---------- commands ----------
 boolean handle(String t,String low){
  low=low.trim();
  if(low.equals("listen on")||low.equals("start listening")){ startContinuous(); return true; }
  if(low.equals("listen off")||low.equals("stop listening")){ stopContinuous(true); return true; }
  if(low.equals("listen test")){ test(); return true; }
  if(low.startsWith("listen engine ")){
   String arg=low.substring(14).trim();
   if(arg.equals("auto")||arg.equals("default")||arg.equals("ondevice")){
    String v=arg.equals("default")?"auto":arg;
    s.sp.edit().putString("listenEngine",v).apply(); onDeviceTried=false;
    s.local("recognition engine: "+v+(v.equals("ondevice")&&!onDeviceAvailable()?" (unavailable on this phone, will use default)":""),false);
    if(continuous){ teardown(); begin(); }
   } else s.local("say: listen engine auto or listen engine ondevice",false);
   return true;
  }
  if(low.equals("listen status")){
   StringBuilder sb=new StringBuilder("listening: "+(continuous?"on":"off")+"\nlanguage: "+lang()+"\nmic permission: "+(permitted()?"yes":"no")+"\nspeech recognizer on phone: "+(available()?"yes":"no")+"\nlistening notification: "+(fgs?"active":"off"));
   if(lastError!=null){ long ago=(System.currentTimeMillis()-lastErrorAt)/1000; sb.append("\nlast error: ").append(lastError).append(" (").append(ago).append("s ago)"); }
   s.showChat(); s.local(sb.toString()+"\n\nSay 'listen test' for a full diagnostic.",false); return true;
  }
  if(low.startsWith("listen language ")){
   String arg=low.substring(16).trim();
   if(arg.startsWith("sw")){ s.sp.edit().putString("listenLang","sw").apply(); s.local("recognition language: Swahili (sw-KE)",false); if(continuous){ teardown(); begin(); } }
   else if(arg.startsWith("en")){ s.sp.edit().putString("listenLang","en").apply(); s.local("recognition language: English (en-US)",false); if(continuous){ teardown(); begin(); } }
   else s.local("say: listen language en or listen language sw",false);
   return true;
  }
  return false;
 }
}
