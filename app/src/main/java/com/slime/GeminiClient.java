package com.slime;
import android.content.SharedPreferences; import android.os.Handler; import android.os.Looper;
import android.util.Base64;
import java.io.BufferedReader; import java.io.ByteArrayOutputStream; import java.io.File; import java.io.FileInputStream; import java.io.InputStream; import java.io.InputStreamReader; import java.io.OutputStream;
import java.net.HttpURLConnection; import java.net.URL; import java.net.URLEncoder;
import java.text.SimpleDateFormat; import java.util.ArrayList; import java.util.Date; import java.util.Locale;
import org.json.JSONArray; import org.json.JSONObject;
// Google AI Studio (Gemini) client. Keyless billing: free tier, multiple API keys pooled and rotated
// exactly like OpenRouter. Endpoint: generativelanguage.googleapis.com v1beta generateContent.
public class GeminiClient {
 // flash-lite keeps the large free allowance (~500 RPD); full flash is cut to ~20 RPD on free projects
 public static final String DEFAULT_MODEL="gemini-3.5-flash-lite";
 static final String RATE_MIN_PREFIX="RATE_MIN:"; // internal marker: per-minute burst, retryable without benching the key
 // tried in order when the pinned model answers 404/400 (Google retires model names for new projects; 2.5 names still work on older ones)
 static final String[] FALLBACK={"gemini-3.5-flash-lite","gemini-3.1-flash-lite","gemini-2.5-flash-lite","gemini-2.5-flash"};
 static SharedPreferences prefs;
 static String day(){ return new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date()); }
 static String model(){ return prefs==null?DEFAULT_MODEL:prefs.getString("gmodel",DEFAULT_MODEL); }
 static int keyCount(){ try{ return prefs==null?0:Keys.all(prefs,Keys.G).size(); }catch(Exception e){ return 0; } }
 static int used(){ try{ return prefs==null?0:prefs.getInt("gai_"+day(),0); }catch(Exception e){ return 0; } }
 static int usedAuto(){ try{ return prefs==null?0:prefs.getInt("gaiauto_"+day(),0); }catch(Exception e){ return 0; } }
 // Google no longer publishes fixed free-tier numbers; limits are per project and shown only in AI Studio.
 public static String budget(){ return used()+" req today (Google sets the limit)"; }
 // the slime's own calls are rationed locally; user messages are never blocked by this
 public static boolean autoOk(){ return usedAuto()<6; }
 static void countCall(String k){
  try{
   if(prefs==null) return;
   SharedPreferences.Editor e=prefs.edit();
   e.putInt("gai_"+day(),used()+1);
   if(k!=null&&k.length()>0) e.putInt("gaik_"+Keys.hash(k)+"_"+day(),prefs.getInt("gaik_"+Keys.hash(k)+"_"+day(),0)+1);
   e.apply();
  }catch(Exception e){ L.w("Gemini",e); }
 }
 static int maxTokensFor(String user){
  String l=user==null?"":user.toLowerCase();
  String[] k={"code","script","function","write ","explain","step by step","how do","how to","patch","fix ","error","log","solve","derive","proof","formula","list ","teach"};
  for(int i=0;i<k.length;i++){ if(l.contains(k[i])) return 1400; }
  return l.length()>300?1000:500;
 }
 static String readAll(InputStream is) throws Exception{
  if(is==null) return "";
  BufferedReader br=new BufferedReader(new InputStreamReader(is,"UTF-8"));
  StringBuilder sb=new StringBuilder(); String l;
  while((l=br.readLine())!=null) sb.append(l);
  br.close(); return sb.toString();
 }
 static volatile String lastError="";
 // one HTTP call; code[0] carries the HTTP status. think=false omits thinkingConfig for 400 retries.
 static String once(String key,String model,String system,String user,int[] code,boolean think) throws Exception{
  JSONObject body=new JSONObject();
  if(system!=null&&!system.isEmpty()){
   JSONObject si=new JSONObject(); JSONArray sp=new JSONArray(); sp.put(new JSONObject().put("text",system)); si.put("parts",sp); body.put("systemInstruction",si);
  }
  JSONArray parts=new JSONArray(); parts.put(new JSONObject().put("text",user));
  JSONObject turn=new JSONObject(); turn.put("role","user"); turn.put("parts",parts);
  JSONArray contents=new JSONArray(); contents.put(turn); body.put("contents",contents);
  JSONObject cfg=new JSONObject();
  // floor of 700 so internal tokens can never eat the whole reply; 2.5 thinking is switched off via thinkingBudget.
  // 3.x models use thinking_level and already default to MINIMAL, so they need no config at all.
  cfg.put("maxOutputTokens",Math.max(700,maxTokensFor(user))); cfg.put("temperature",0.9);
  if(think&&model.contains("2.5")){ JSONObject tc=new JSONObject(); tc.put("thinkingBudget",0); cfg.put("thinkingConfig",tc); }
  body.put("generationConfig",cfg);
  String url="https://generativelanguage.googleapis.com/v1beta/models/"+URLEncoder.encode(model,"UTF-8")+":generateContent?key="+URLEncoder.encode(key,"UTF-8");
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
  c.setRequestMethod("POST"); c.setRequestProperty("Content-Type","application/json"); c.setDoOutput(true);
  c.setConnectTimeout(15000); c.setReadTimeout(45000);
  OutputStream os=c.getOutputStream(); os.write(body.toString().getBytes("UTF-8")); os.close();
  code[0]=c.getResponseCode(); countCall(key);
  String resp=readAll(code[0]>=200&&code[0]<300?c.getInputStream():c.getErrorStream());
  if(code[0]<200||code[0]>=300) lastError="HTTP "+code[0]+": "+clip(errMessage(resp));
  return resp;
 }
 static String errMessage(String resp){
  try{ JSONObject e=new JSONObject(resp).optJSONObject("error"); if(e!=null) return e.optString("message","")+" | "+e.optString("status",""); }catch(Exception ex){ L.w("Gemini",ex); }
  return resp==null?"":resp;
 }
 // returns reply text, an error line, or null when a retry makes sense
 static String interpret(int code,String resp){
  if(code==401||code==403) return "key died bestie, make a new Gemini key at aistudio.google.com/apikey";
  if(code==429){
   String low=resp==null?"":resp.toLowerCase(Locale.US);
   String detail=clip(errMessage(resp));
   // Google uses RESOURCE_EXHAUSTED for both per-minute bursts and the real daily quota; inspect the body
   boolean minute=low.contains("per minute")||low.contains("per_minute")||low.contains("rate_limit_exhausted")||low.contains("rate limit");
   boolean daily=low.contains("per day")||low.contains("per_day")||low.contains("daily")||low.contains("quota_exhausted")||low.contains("free tier");
   if(minute&&!daily) return RATE_MIN_PREFIX+"too many messages in one minute, Google's rate limit ("+detail+")";
   if(daily) return "too many messages today: this Gemini project's free quota is used up, it resets around midnight US Pacific time ("+detail+")";
   // ambiguous exhaustion: treat conservatively as a minute-level retry first, not a full-day bench
   return RATE_MIN_PREFIX+"Google rate-limited that request ("+detail+")";
  }
  if(code>=200&&code<300){
   try{
    JSONObject j=new JSONObject(resp);
    JSONArray cand=j.optJSONArray("candidates");
    if(cand==null||cand.length()==0){
     JSONObject fb=j.optJSONObject("promptFeedback");
     String why=fb!=null?fb.optString("blockReason","safety filter"):"no candidates";
     lastError="empty response ("+why+")";
     return "my brain lagged: Google returned nothing ("+why+")";
    }
    JSONObject first=cand.getJSONObject(0); String finish=first.optString("finishReason","");
    JSONArray ps=first.optJSONObject("content")==null?null:first.getJSONObject("content").optJSONArray("parts");
    StringBuilder out=new StringBuilder();
    if(ps!=null){ for(int i=0;i<ps.length();i++){ String tx=ps.getJSONObject(i).optString("text",""); if(tx!=null) out.append(tx); } }
    String s=out.toString().replaceAll("(?s)<think>.*?</think>","").trim();
    if(s.isEmpty()){ lastError="empty text parts, finishReason="+finish; return "my brain lagged: Google sent an empty reply (finishReason "+finish+"), say it again"; }
    return s;
   }catch(Exception e){ lastError="parse error: "+clip(e.getMessage()); return null; }
  }
  if(code==404){ lastError="HTTP 404 model not found: "+clip(errMessage(resp)); return null; } // caller walks the fallback list
  if(code==400){
   String low=resp==null?"":resp.toLowerCase();
   lastError="HTTP 400: "+clip(errMessage(resp));
   if(low.contains("model")||low.contains("not found")||low.contains("not supported")) return null;
   return "my brain lagged (400) "+clip(errMessage(resp));
  }
  if(code==408||code>=500){ lastError="HTTP "+code+" server error"; return null; }
  lastError="HTTP "+code;
  return "my brain lagged ("+code+") try again";
 }
 static String clip(String s){ if(s==null) return ""; return s.length()>80?s.substring(0,80):s; }
 static final String PREF_LAST_GOOD="glast_ok";
 // one key, walking model fallbacks; one retry on flaky answers
 static String attempt(String key,String system,String user){
  try{
   ArrayList<String> models=new ArrayList<String>();
   String cached=prefs==null?"":prefs.getString(PREF_LAST_GOOD,"");
   if(!cached.isEmpty()) models.add(cached); // a model that already worked on this project wins
   String pinned=model(); if(!models.contains(pinned)) models.add(pinned);
   for(String f:FALLBACK){ if(!models.contains(f)) models.add(f); }
   String out=null;
   for(int mi=0;mi<models.size()&&mi<4;mi++){
    String mdl=models.get(mi);
    int[] code={0};
    String resp=once(key,mdl,system,user,code,true);
    out=interpret(code[0],resp);
    if(out==null&&code[0]==404) continue; // try next model name
    if(out==null&&code[0]==400&&(resp.toLowerCase().contains("model")||resp.contains("not found"))) continue;
    if(code[0]==400&&resp.toLowerCase().contains("thinking")){
     // some projects reject thinkingConfig: retry this model once without it
     resp=once(key,mdl,system,user,code,false); out=interpret(code[0],resp);
    }
    if(out!=null&&!out.startsWith("my brain lagged")&&!out.startsWith(RATE_MIN_PREFIX)&&code[0]>=200&&code[0]<300){
     if(prefs!=null&&!mdl.equals(prefs.getString(PREF_LAST_GOOD,""))) prefs.edit().putString(PREF_LAST_GOOD,mdl).apply();
    }
    break;
   }
   // per-minute rate limit: wait out Google's short window and retry once instead of benching the key all day
   if(out!=null&&out.startsWith(RATE_MIN_PREFIX)){
    Thread.sleep(12000);
    int[] code={0};
    String resp=once(key,prefs==null?model():prefs.getString(PREF_LAST_GOOD,model()),system,user,code,true);
    String retry=interpret(code[0],resp);
    if(retry!=null&&!retry.startsWith(RATE_MIN_PREFIX)) out=retry;
    else if(retry==null) out="my bad, brain buffered - say that again? ("+lastError+")";
    else out=retry.substring(RATE_MIN_PREFIX.length());
   }
   if(out==null){
    // one plain retry on transient failures
    int[] code={0};
    String resp=once(key,prefs==null?model():prefs.getString(PREF_LAST_GOOD,model()),system,user,code,true);
    out=interpret(code[0],resp);
    if(out==null) out="my bad, brain buffered - say that again? ("+lastError+")";
   }
   if(out.startsWith(RATE_MIN_PREFIX)) out=out.substring(RATE_MIN_PREFIX.length());
   return out;
  }catch(Exception e){
   String m=e.getMessage(); if(m==null) m="offline"; if(m.length()>60) m=m.substring(0,60);
   return "net hiccup: "+m;
  }
 }
 // minimal end-to-end test used by 'gemini diagnose'
 interface DiagCb{ void done(String report); }
 static void diagnose(final DiagCb cb){
  new Thread(new Runnable(){ public void run(){
   String report;
   try{
    ArrayList<String> ks=Keys.all(prefs,Keys.G);
    if(ks.isEmpty()){ report="no Gemini keys saved"; postDiag(cb,report); return; }
    String key=Keys.current(prefs,Keys.G,ks);
    int[] code={0};
    String resp=once(key,model(),"You are a helpful assistant.","Reply with exactly: ok",code,true);
    StringBuilder sb=new StringBuilder();
    sb.append("model: ").append(model()).append("\nHTTP: ").append(code[0]);
    sb.append("\nkeys: ").append(ks.size()).append(" | requests today: ").append(used());
    sb.append("\nraw response (first 400 chars):\n").append(resp==null?"(empty)":resp.substring(0,Math.min(400,resp.length())));
    String parsed=interpret(code[0],resp);
    sb.append("\nparsed: ").append(parsed==null?"null/retryable":parsed);
    sb.append("\nlast error: ").append(lastError.isEmpty()?"none":lastError);
    report=sb.toString();
   }catch(Exception e){ report="diagnose failed: "+e.getMessage(); }
   postDiag(cb,report);
  }}).start();
 }
 private static void postDiag(final DiagCb cb,final String r){ new Handler(Looper.getMainLooper()).post(new Runnable(){ public void run(){ cb.done(r); }}); }
 public static void ask(String system,String user,Ai.Callback cb){ run(system,user,cb,false); }
 public static void askAuto(String system,String user,Ai.Callback cb){
  try{ if(prefs!=null) prefs.edit().putInt("gaiauto_"+day(),usedAuto()+1).apply(); }catch(Exception e){ L.w("Gemini",e); }
  run(system,user,cb,false);
 }
 static void run(final String system,final String user,final Ai.Callback cb,boolean unused){
  new Thread(new Runnable(){ public void run(){
   String out;
   ArrayList<String> ks=new ArrayList<String>();
   try{ if(prefs!=null) ks=Keys.all(prefs,Keys.G); }catch(Exception e){ L.w("Gemini",e); }
   if(ks.isEmpty()){ post(cb,"no Gemini keys yet: add one on the home screen, or say gemini key add YOURKEY"); return; }
   int tries=0;
   while(true){
    String k=Keys.current(prefs,Keys.G,ks);
    out=attempt(k,system,user);
    boolean dead=out.startsWith("key died")||out.startsWith("too many messages today");
    boolean soft=out.startsWith("my bad")||out.startsWith("my brain lagged")||out.startsWith("net hiccup");
    if(dead&&ks.size()>1){
     Keys.markDead(prefs,Keys.G,k,out.startsWith("key died"));
     tries++;
     if(tries<ks.size()&&Keys.anyAlive(prefs,Keys.G,ks)) continue;
    } else if(soft&&ks.size()>1&&tries==0){
     // empty/server failures aren't this key's quota: move it to the back and give the next key one chance
     String first=ks.remove(0); ks.add(first); tries++; continue;
    }
    break;
   }
   final String fr=out;
   new Handler(Looper.getMainLooper()).post(new Runnable(){ public void run(){ cb.onResult(fr); }});
  }}).start();
 }
 private static void post(final Ai.Callback cb,final String s){ new Handler(Looper.getMainLooper()).post(new Runnable(){ public void run(){ cb.onResult(s); }}); }

 // ---------- vision (inline jpeg, same key pool) ----------
 static String vision(String question,File img) throws Exception{
  ArrayList<String> ks=prefs==null?new ArrayList<String>():Keys.all(prefs,Keys.G);
  if(ks.isEmpty()) throw new Exception("no Gemini keys");
  String key=Keys.current(prefs,Keys.G,ks);
  byte[] raw=readFile(img);
  String b64=Base64.encodeToString(raw,Base64.NO_WRAP);
  JSONObject txt=new JSONObject(); txt.put("text",question);
  JSONObject inline=new JSONObject(); inline.put("mime_type","image/jpeg"); inline.put("data",b64);
  JSONObject im=new JSONObject(); im.put("inline_data",inline);
  JSONArray parts=new JSONArray(); parts.put(txt); parts.put(im);
  JSONObject turn=new JSONObject(); turn.put("role","user"); turn.put("parts",parts);
  JSONArray contents=new JSONArray(); contents.put(turn);
  JSONObject body=new JSONObject(); body.put("contents",contents);
  JSONObject cfg=new JSONObject(); cfg.put("maxOutputTokens",900); cfg.put("temperature",0.8);
  if(model().contains("2.5")){ JSONObject tc=new JSONObject(); tc.put("thinkingBudget",0); cfg.put("thinkingConfig",tc); } // 3.x defaults to minimal thinking
  body.put("generationConfig",cfg);
  String url="https://generativelanguage.googleapis.com/v1beta/models/"+URLEncoder.encode(model(),"UTF-8")+":generateContent?key="+URLEncoder.encode(key,"UTF-8");
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
  c.setRequestMethod("POST"); c.setRequestProperty("Content-Type","application/json"); c.setDoOutput(true);
  c.setConnectTimeout(15000); c.setReadTimeout(60000);
  OutputStream os=c.getOutputStream(); os.write(body.toString().getBytes("UTF-8")); os.close();
  int code=c.getResponseCode(); countCall(key);
  String resp=readAll(code>=200&&code<300?c.getInputStream():c.getErrorStream());
  String out=interpret(code,resp);
  if(out==null) throw new Exception(code==404?"Gemini model unavailable ("+model()+")":"http "+code);
  if(out.startsWith("key died")||out.startsWith("too many")||out.startsWith("my brain lagged")||out.startsWith("net hiccup")) throw new Exception(out);
  return out;
 }
 static byte[] readFile(File f) throws Exception{
  FileInputStream in=new FileInputStream(f); ByteArrayOutputStream bo=new ByteArrayOutputStream(); byte[] b=new byte[4096]; int n;
  while((n=in.read(b))>0) bo.write(b,0,n); in.close(); return bo.toByteArray();
 }

 // ---------- model directory ----------
 interface ListCb{ void done(String report); }
 static void listModels(final ListCb cb){
  new Thread(new Runnable(){ public void run(){
   String report;
   try{
    ArrayList<String> ks=Keys.all(prefs,Keys.G);
    if(ks.isEmpty()){ report="add a Gemini key first (gemini key add YOURKEY), then try again"; postList(cb,report); return; }
    String key=Keys.current(prefs,Keys.G,ks);
    HttpURLConnection c=(HttpURLConnection)new URL("https://generativelanguage.googleapis.com/v1beta/models?key="+URLEncoder.encode(key,"UTF-8")).openConnection();
    c.setConnectTimeout(15000); c.setReadTimeout(25000);
    String raw=readAll(c.getInputStream());
    JSONArray all=new JSONObject(raw).optJSONArray("models");
    ArrayList<String> chat=new ArrayList<String>();
    if(all!=null){ for(int i=0;i<all.length();i++){
     JSONObject o=all.getJSONObject(i); String name=o.optString("name","").replace("models/","");
     JSONArray methods=o.optJSONArray("supportedGenerationMethods"); boolean gc=false;
     if(methods!=null){ for(int j=0;j<methods.length();j++){ if("generateContent".equals(methods.optString(j))){ gc=true; break; } } }
     if(gc&&name.startsWith("gemini")) chat.add(name);
    }}
    StringBuilder sb=new StringBuilder("Gemini models available to your key ("+chat.size()+"):\n");
    for(int i=0;i<Math.min(20,chat.size());i++){ sb.append(chat.get(i)).append(chat.get(i).equals(model())?"  <- current\n":"\n"); }
    if(chat.size()>20) sb.append("...and ").append(chat.size()-20).append(" more\n");
    sb.append("\npin one with: gemini model NAME (gemini model reset to go back to ").append(DEFAULT_MODEL).append(")");
    report=sb.toString();
   }catch(Exception e){ report="couldn't fetch Gemini models: "+clip(e.getMessage()); }
   postList(cb,report);
  }}).start();
 }
 private static void postList(final ListCb cb,final String r){ new Handler(Looper.getMainLooper()).post(new Runnable(){ public void run(){ cb.done(r); }}); }
}
