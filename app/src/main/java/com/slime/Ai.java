package com.slime;
import android.content.SharedPreferences;
// Single front door for AI calls. Routes every request to the chosen provider:
// OpenRouter, Google AI Studio/Gemini, or the fully offline llama.cpp server.
public class Ai {
 public interface Callback{ void onResult(String reply); }
 static final String OPENROUTER="openrouter", GEMINI="gemini", OFFLINE="offline";
 static String provider(SharedPreferences sp){
  if(sp==null) return OPENROUTER;
  String p=sp.getString("aiProvider",OPENROUTER);
  if(GEMINI.equals(p)) return GEMINI;
  if(OFFLINE.equals(p)) return OFFLINE;
  return OPENROUTER;
 }
 static boolean hasKeys(SharedPreferences sp){
  return !Keys.all(sp,Keys.OR).isEmpty()||!Keys.all(sp,Keys.G).isEmpty();
 }
 public static void ask(String system,String user,Callback cb){
  SharedPreferences sp=OpenRouterClient.prefs;
  String p=provider(sp);
  if(OFFLINE.equals(p)){
   if(!LocalLlm.arch64()){ cb.onResult(LocalLlm.ARCH_MSG); return; }
   LocalLlm.ask(system,user,cb); return;
  }
  if(GEMINI.equals(p)){
   if(Keys.all(sp,Keys.G).isEmpty()){ cb.onResult("no Gemini keys yet: add one on the home screen, or say gemini key add YOURKEY (or switch back with: use openrouter)"); return; }
   GeminiClient.ask(system,user,cb);
  } else OpenRouterClient.ask(OpenRouterClient.currentKey(""),system,user,cb);
 }
 // the slime's own unprompted calls never run on the local model (heat/battery on small phones).
 // while Gemini is selected they also avoid its tiny free daily pool and use OpenRouter instead,
 // so the Gemini quota is spent only on messages the user actually sends.
 public static void askAuto(String system,String user,Callback cb){
  SharedPreferences sp=OpenRouterClient.prefs;
  String p=provider(sp);
  if(OFFLINE.equals(p)){ cb.onResult(null); return; }
  if(GEMINI.equals(p)){
   if(!Keys.all(sp,Keys.OR).isEmpty()){ OpenRouterClient.askAuto(OpenRouterClient.currentKey(""),system,user,cb); return; }
   cb.onResult(null); // no OpenRouter fallback: stay quiet rather than burn the Gemini daily limit
  } else OpenRouterClient.askAuto(OpenRouterClient.currentKey(""),system,user,cb);
 }
 public static String budget(){
  SharedPreferences sp=OpenRouterClient.prefs;
  if(sp==null) return "0/0";
  String p=provider(sp);
  if(OFFLINE.equals(p)) return "local, free";
  return GEMINI.equals(p)?GeminiClient.budget():OpenRouterClient.budget();
 }
 public static boolean autoOk(){
  SharedPreferences sp=OpenRouterClient.prefs;
  if(sp==null||OFFLINE.equals(provider(sp))) return false;
  return GEMINI.equals(provider(sp))?GeminiClient.autoOk():OpenRouterClient.autoOk();
 }
 public static String currentModelName(){
  SharedPreferences sp=OpenRouterClient.prefs;
  if(sp==null) return "";
  String p=provider(sp);
  if(OFFLINE.equals(p)) return "offline: "+LocalLlm.modelId();
  return GEMINI.equals(p)?GeminiClient.model():FreeModels.last(sp);
 }
 static boolean handle(MainService s,String t,String low){
  low=low.trim();
  if(LocalLlm.handle(s,t,low)) return true;
  if(low.equals("ai provider")||low.equals("provider")){
   StringBuilder sb=new StringBuilder("AI provider: "+provider(s.sp));
   sb.append("\nOpenRouter keys: ").append(Keys.all(s.sp,Keys.OR).size()).append(" | Gemini keys: ").append(Keys.all(s.sp,Keys.G).size());
   sb.append("\ntoday: ").append(budget()).append(", model: ").append(currentModelName());
   if(GEMINI.equals(provider(s.sp))) sb.append("\nGemini free limits are set per Google project (see AI Studio); same-account keys share one pool. My own chatter uses OpenRouter to save your Gemini quota.");
   sb.append("\nswitch with: use gemini | use openrouter | use offline (or tap the AI button)");
   s.showChat(); s.local(sb.toString(),false); return true;
  }
  if(low.equals("use offline")||low.equals("ai provider offline")||low.equals("offline provider")){
   if(!LocalLlm.arch64()){ s.local(LocalLlm.ARCH_MSG,false); return true; }
   s.sp.edit().putString("aiProvider",OFFLINE).apply();
   s.local("now using the offline brain ("+LocalLlm.modelId()+"). First message loads llama.cpp in Termux; no internet needed after that. Say 'offline status' anytime.",true); return true;
  }
  if(low.equals("use gemini")||low.equals("ai provider gemini")||low.equals("gemini provider")){
   if(Keys.all(s.sp,Keys.G).isEmpty()){ s.local("you haven't added a Gemini key yet. Get a free one at aistudio.google.com/apikey, then say: gemini key add YOURKEY (or paste it on the home screen). I'm staying where I am for now.",false); return true; }
   leaveOffline(s); s.sp.edit().putString("aiProvider",GEMINI).apply(); s.local("now using Google AI Studio (Gemini "+GeminiClient.model()+").",true); return true;
  }
  if(low.equals("use openrouter")||low.equals("ai provider openrouter")||low.equals("openrouter provider")){
   if(Keys.all(s.sp,Keys.OR).isEmpty()){ s.local("you haven't added an OpenRouter key yet. Get one free at openrouter.ai/keys, or stay on your current provider.",false); return true; }
   leaveOffline(s); s.sp.edit().putString("aiProvider",OPENROUTER).apply(); s.local("now using OpenRouter ("+FreeModels.last(s.sp)+").",true); return true;
  }
  if(low.equals("gemini model")){ s.local("Gemini model: "+GeminiClient.model()+"  (change: gemini model NAME, or gemini model reset)",false); return true; }
  if(low.equals("gemini model reset")){ s.sp.edit().remove("gmodel").apply(); s.local("Gemini model back to "+GeminiClient.DEFAULT_MODEL,false); return true; }
  if(low.startsWith("gemini model ")){
   String nm=t.substring(t.toLowerCase(java.util.Locale.US).indexOf("model ")+6).trim();
   if(nm.matches("[A-Za-z0-9._-]{3,80}")){ s.sp.edit().putString("gmodel",nm).apply(); s.local("Gemini model is now "+nm,false); }
   else s.local("that doesn't look like a model name. example: gemini model gemini-3.5-flash-lite",false);
   return true;
  }
  if(low.equals("gemini models")){
   s.local("asking Google which Gemini models your key can use...",false);
   GeminiClient.listModels(new GeminiClient.ListCb(){ public void done(String report){ s.showChat(); s.local(report,false); } });
   return true;
  }
  if(low.equals("gemini diagnose")||low.equals("gemini test")){
   s.local("running a live Gemini test call...",false);
   GeminiClient.diagnose(new GeminiClient.DiagCb(){ public void done(String report){ s.showChat(); s.local(report,false); } });
   return true;
  }
  return false;
 }
 // free the local server's RAM and wake lock whenever we move back to a cloud provider
 static void leaveOffline(MainService s){
  if(OFFLINE.equals(provider(s.sp))) LocalLlm.stopServer();
 }
}
