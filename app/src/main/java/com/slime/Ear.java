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
 SpeechRecognizer sr; boolean continuous, active; int backoff; long lastRestart;
 private Runnable retry;
 Ear(MainService s){ this.s=s; }

 private boolean permitted(){ try{ return s.checkSelfPermission("android.permission.RECORD_AUDIO")==PackageManager.PERMISSION_GRANTED; }catch(Exception e){ L.w("Ear",e); return false; } }
 private boolean available(){ try{ return SpeechRecognizer.isRecognitionAvailable(s); }catch(Exception e){ L.w("Ear",e); return false; } }
 String lang(){ return "sw".equals(s.sp.getString("listenLang","en"))?"sw-KE":"en-US"; }

 // ---------- one tap, one utterance ----------
 void tapToTalk(){
  if(!permitted()){ s.local("I can't hear you yet: allow the microphone permission on the home screen first.",false); return; }
  if(!available()){ s.local("this phone has no speech recognizer installed, so I can't listen.",false); return; }
  if(continuous){ s.local("I'm already listening, just talk.",false); return; }
  s.slimeView.say("listening..."); s.slimeView.bounceAnim();
  startOnce(false);
 }

 // ---------- continuous mode ----------
 void startContinuous(){
  if(!permitted()){ s.local("I can't hear you yet: allow the microphone permission on the home screen first.",false); return; }
  if(!available()){ s.local("this phone has no speech recognizer installed, so I can't listen.",false); return; }
  if(continuous) return;
  continuous=true; active=true; backoff=0;
  s.sp.edit().putBoolean("listenOn",true).apply();
  s.voice.setEnabled(true);
  if(!startForeground()){ continuous=false; active=false; s.sp.edit().putBoolean("listenOn",false).apply(); return; }
  s.local("listening mode on. I'll hear you while the screen is on. Say 'listen off' to stop.",false);
  begin();
 }
 void stopContinuous(boolean say){
  continuous=false; active=false;
  h.removeCallbacksAndMessages(null);
  teardown();
  try{ s.stopForeground(true); }catch(Exception e){ L.w("Ear",e); }
  s.sp.edit().putBoolean("listenOn",false).apply();
  if(say) s.local("listening mode off.",false);
 }
 void onScreenOff(){ active=false; teardown(); }
 void onScreenOn(){ if(continuous){ active=true; begin(); } }
 // Called by Voice when speech output finishes, so the mic resumes promptly.
 void resumeIfListening(){ if(continuous&&active&&sr==null&&s.screenOn()) begin(); }
 void destroy(){ stopContinuous(false); }

 private boolean startForeground(){
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
   return true;
  }catch(Exception e){ L.w("Ear",e); s.local("Android refused to let me listen in the background. Check the microphone permission and try again.",false); return false; }
 }

 private void begin(){
  if(!active||!s.screenOn()) return;
  if(s.voice.isSpeaking()){ schedule(500); return; }
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
 private void startOnce(final boolean loop){
  teardown();
  try{
   sr=SpeechRecognizer.createSpeechRecognizer(s);
   sr.setRecognitionListener(new RecognitionListener(){
    public void onReadyForSpeech(Bundle b){ backoff=0; }
    public void onBeginningOfSpeech(){}
    public void onRmsChanged(float r){}
    public void onBufferReceived(byte[] b){}
    public void onEndOfSpeech(){}
    public void onError(int error){
     teardown();
     if(!loop){ if(error==SpeechRecognizer.ERROR_NO_MATCH||error==SpeechRecognizer.ERROR_SPEECH_TIMEOUT) s.local("didn't catch that, try again.",false); return; }
     if(error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){ stopContinuous(false); s.local("microphone permission was taken away, listening stopped.",false); return; }
     if(error==SpeechRecognizer.ERROR_CLIENT&&"sw-KE".equals(lang())){ s.sp.edit().putString("listenLang","en").apply(); s.local("Swahili speech pack isn't available here, switching to English.",false); }
     long now=System.currentTimeMillis();
     if(now-lastRestart<2000) backoff=Math.min(backoff+1,5); else backoff=0;
     lastRestart=now;
     schedule(backoff==0?400:Math.min(3000,400*(backoff+1)));
    }
    public void onResults(Bundle b){
     ArrayList<String> r=b!=null?b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION):null;
     teardown();
     if(r!=null&&!r.isEmpty()&&!r.get(0).trim().isEmpty()){
      String text=r.get(0).trim();
      s.addChat("You: "+text,true);
      s.handleHeard(text);
     }
     if(loop) begin();
    }
    public void onPartialResults(Bundle b){}
    public void onEvent(int i,Bundle b){}
   });
   Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
   i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
   i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,lang());
   i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
   i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
   sr.startListening(i);
  }catch(Exception e){ L.w("Ear",e); teardown(); if(loop) schedule(2000); }
 }

 // ---------- commands ----------
 boolean handle(String t,String low){
  low=low.trim();
  if(low.equals("listen on")||low.equals("start listening")){ startContinuous(); return true; }
  if(low.equals("listen off")||low.equals("stop listening")){ stopContinuous(true); return true; }
  if(low.equals("listen status")){
   StringBuilder sb=new StringBuilder("listening: "+(continuous?"on":"off")+"\nlanguage: "+lang()+"\nmic permission: "+(permitted()?"yes":"no")+"\nspeech recognizer on phone: "+(available()?"yes":"no"));
   s.showChat(); s.local(sb.toString(),false); return true;
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
