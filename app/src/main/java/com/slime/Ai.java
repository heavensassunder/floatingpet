package com.slime;
import android.content.SharedPreferences;
// Single front door for AI calls. Routes every request to the chosen provider
// (OpenRouter or Google AI Studio/Gemini), each with its own rotating key pool.
public class Ai {
 public interface Callback{ void onResult(String reply); }
 static final String OPENROUTER="openrouter", GEMINI="gemini";
 static String provider(SharedPreferences sp){
  String p=sp==null?OPENROUTER:sp.getString("aiProvider",OPENROUTER);
  return GEMINI.equals(p)?GEMINI:OPENROUTER;
 }
 static boolean hasKeys(SharedPreferences sp){
  return !Keys.all(sp,Keys.OR).isEmpty()||!Keys.all(sp,Keys.G).isEmpty();
 }
 public static void ask(String system,String user,Callback cb){
  SharedPreferences sp=OpenRouterClient.prefs;
  if(GEMINI.equals(provider(sp))){
   if(Keys.all(sp,Keys.G).isEmpty()){ cb.onResult("no Gemini keys yet: add one on the home screen, or say gemini key add YOURKEY (or switch back with: use openrouter)"); return; }
   GeminiClient.ask(system,user,cb);
  } else OpenRouterClient.ask(OpenRouterClient.currentKey(""),system,user,cb);
 }
 public static void askAuto(String system,String user,Callback cb){
  SharedPreferences sp=OpenRouterClient.prefs;
  if(GEMINI.equals(provider(sp))){
   if(Keys.all(sp,Keys.G).isEmpty()){ cb.onResult("no Gemini keys yet: add one on the home screen, or say gemini key add YOURKEY"); return; }
   GeminiClient.askAuto(system,user,cb);
  } else OpenRouterClient.askAuto(OpenRouterClient.currentKey(""),system,user,cb);
 }
 public static String budget(){
  SharedPreferences sp=OpenRouterClient.prefs;
  if(sp==null) return "0/0";
  return GEMINI.equals(provider(sp))?GeminiClient.budget():OpenRouterClient.budget();
 }
 public static boolean autoOk(){
  SharedPreferences sp=OpenRouterClient.prefs;
  if(sp==null) return false;
  return GEMINI.equals(provider(sp))?GeminiClient.autoOk():OpenRouterClient.autoOk();
 }
 public static String currentModelName(){
  SharedPreferences sp=OpenRouterClient.prefs;
  if(sp==null) return "";
  return GEMINI.equals(provider(sp))?GeminiClient.model():FreeModels.last(sp);
 }
 static boolean handle(MainService s,String t,String low){
  low=low.trim();
  if(low.equals("ai provider")||low.equals("provider")){
   StringBuilder sb=new StringBuilder("AI provider: "+provider(s.sp));
   sb.append("\nOpenRouter keys: ").append(Keys.all(s.sp,Keys.OR).size()).append(" | Gemini keys: ").append(Keys.all(s.sp,Keys.G).size());
   sb.append("\ntoday: ").append(budget()).append(" requests, model: ").append(currentModelName());
   sb.append("\nswitch with: use gemini  or  use openrouter");
   s.showChat(); s.local(sb.toString(),false); return true;
  }
  if(low.equals("use gemini")||low.equals("ai provider gemini")||low.equals("gemini provider")){
   if(Keys.all(s.sp,Keys.G).isEmpty()){ s.local("you haven't added a Gemini key yet. Get a free one at aistudio.google.com/apikey, then say: gemini key add YOURKEY (or paste it on the home screen). I'm staying on OpenRouter for now.",false); return true; }
   s.sp.edit().putString("aiProvider",GEMINI).apply(); s.local("now using Google AI Studio (Gemini "+GeminiClient.model()+"). say 'use openrouter' to switch back.",true); return true;
  }
  if(low.equals("use openrouter")||low.equals("ai provider openrouter")||low.equals("openrouter provider")){
   if(Keys.all(s.sp,Keys.OR).isEmpty()){ s.local("you haven't added an OpenRouter key yet. Get one free at openrouter.ai/keys, or stay on Gemini.",false); return true; }
   s.sp.edit().putString("aiProvider",OPENROUTER).apply(); s.local("now using OpenRouter ("+FreeModels.last(s.sp)+"). say 'use gemini' to switch back.",true); return true;
  }
  if(low.equals("gemini model")){ s.local("Gemini model: "+GeminiClient.model()+"  (change: gemini model NAME, or gemini model reset)",false); return true; }
  if(low.equals("gemini model reset")){ s.sp.edit().remove("gmodel").apply(); s.local("Gemini model back to "+GeminiClient.DEFAULT_MODEL,false); return true; }
  if(low.startsWith("gemini model ")){
   String nm=t.substring(t.toLowerCase(java.util.Locale.US).indexOf("model ")+6).trim();
   if(nm.matches("[A-Za-z0-9._-]{3,80}")){ s.sp.edit().putString("gmodel",nm).apply(); s.local("Gemini model is now "+nm,false); }
   else s.local("that doesn't look like a model name. example: gemini model gemini-2.5-flash-lite",false);
   return true;
  }
  if(low.equals("gemini models")){
   s.local("asking Google which Gemini models your key can use...",false);
   GeminiClient.listModels(new GeminiClient.ListCb(){ public void done(String report){ s.showChat(); s.local(report,false); } });
   return true;
  }
  return false;
 }
}
