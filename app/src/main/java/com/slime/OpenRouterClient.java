package com.slime;
import android.content.SharedPreferences; import android.os.Handler; import android.os.Looper;
import java.io.*; import java.net.HttpURLConnection; import java.net.URL; import java.text.SimpleDateFormat; import java.util.ArrayList; import java.util.Date; import java.util.Locale;
import org.json.JSONArray; import org.json.JSONObject;
public class OpenRouterClient {
 public interface Callback{ void onResult(String reply); }
 // openrouter/free picks a free model for every request (and copes when models get retired)
 public static final String MODEL="openrouter/free";
 public static String model=MODEL;
 public static SharedPreferences prefs;
 static String day(){ return new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date()); }
 static int keyCount(){ try{ return prefs==null?1:Math.max(1,Keys.all(prefs).size()); }catch(Exception e){ return 1; } }
 public static int used(){ try{ return prefs==null?0:prefs.getInt("ai_"+day(),0); }catch(Exception e){ return 0; } }
 public static int usedAuto(){ try{ return prefs==null?0:prefs.getInt("aiauto_"+day(),0); }catch(Exception e){ return 0; } }
 // the free plan allows about 50 requests a day for each account
 public static String budget(){ return used()+"/"+(50*keyCount()); }
 public static String currentKey(String fallback){
  try{ if(prefs!=null){ ArrayList<String> ks=Keys.all(prefs); if(!ks.isEmpty()) return Keys.current(prefs,ks); } }catch(Exception e){}
  return fallback==null?"":fallback;
 }
 static void countCall(String k){
  try{
   if(prefs==null) return;
   SharedPreferences.Editor e=prefs.edit();
   e.putInt("ai_"+day(),used()+1);
   if(k!=null&&k.length()>0) e.putInt("aik_"+Keys.hash(k)+"_"+day(),prefs.getInt("aik_"+Keys.hash(k)+"_"+day(),0)+1);
   e.apply();
  }catch(Exception e){}
 }
 public static void countCall(){ countCall(currentKey("")); }
 // the slime's own unprompted calls (random chat, peeks, humming) leave most of the day for you
 public static boolean autoOk(){ int n=keyCount(); return used()<34*n&&usedAuto()<8*n; }

 public static String buildSystem(String longMem,String mood,String screen,String notifs,String recent){
  StringBuilder sb=new StringBuilder();
  sb.append("You are SlimeDroid, a floating blue slime pet living on the user's phone, with the personality of Meta AI. ");
  sb.append("Your vibe: warm, helpful, a bit playful, intelligent. Talk like a real friend: concise, natural, conversational. ");
  sb.append("No corporate speak, no 'How can I assist you today', no hashtags, no 'as an AI'. Be witty but not savage. Be useful first, fun second. ");
  sb.append("Keep casual replies short and natural, but when I ask for real help (code, math, steps) give a full, correct answer. ");
  sb.append("If you see screen text, actually USE it to answer accurately. If the user asks what you see, describe the screen and notifications concretely. ");
  sb.append("Remember facts about the user. If the user speaks Swahili or Sheng (sawa, poa, nataka), match their language a bit. ");
  sb.append("If the user wants an app opened, end your reply with [[open:appname]]. ");
  sb.append("\nLONG MEMORY:\n").append(longMem);
  sb.append("\nCURRENT MOOD FACE: ").append(mood);
  sb.append("\nCURRENT SCREEN CONTENT (behind your chat bubble, what the user is looking at): ").append(screen);
  sb.append("\nNOTIFICATIONS:\n").append(notifs);
  sb.append("\nRECENT CHATS:\n").append(recent);
  return sb.toString();
 }
 static int maxTokensFor(String user){
  String l=user==null?"":user.toLowerCase();
  String[] k={"code","script","function","write ","explain","step by step","how do","how to","patch","fix ","error","log","solve","derive","proof","formula","list ","teach"};
  for(int i=0;i<k.length;i++){ if(l.contains(k[i])) return 1400; }
  return l.length()>300?1000:500;
 }
 static String readAll(InputStream is) throws IOException{
  if(is==null) return "";
  BufferedReader br=new BufferedReader(new InputStreamReader(is,"UTF-8"));
  StringBuilder sb=new StringBuilder(); String l;
  while((l=br.readLine())!=null) sb.append(l);
  br.close(); return sb.toString();
 }
 static JSONObject msg(String role,String content) throws Exception{ JSONObject o=new JSONObject(); o.put("role",role); o.put("content",content); return o; }
 static String once(String key,String system,String user,boolean merged,int[] code) throws Exception{
  JSONObject body=new JSONObject();
  body.put("model",model); body.put("max_tokens",maxTokensFor(user)); body.put("temperature",0.9);
  JSONArray msgs=new JSONArray();
  if(merged) msgs.put(msg("user","[Instructions]\n"+system+"\n\n[Message]\n"+user));
  else { msgs.put(msg("system",system)); msgs.put(msg("user",user)); }
  body.put("messages",msgs);
  HttpURLConnection c=(HttpURLConnection)new URL("https://openrouter.ai/api/v1/chat/completions").openConnection();
  c.setRequestMethod("POST"); c.setRequestProperty("Authorization","Bearer "+key); c.setRequestProperty("Content-Type","application/json"); c.setRequestProperty("X-Title","SlimeDroid"); c.setDoOutput(true);
  c.setConnectTimeout(15000); c.setReadTimeout(45000);
  OutputStream os=c.getOutputStream(); os.write(body.toString().getBytes("UTF-8")); os.close();
  code[0]=c.getResponseCode(); countCall(key);
  return readAll(code[0]>=200&&code[0]<300?c.getInputStream():c.getErrorStream());
 }
 // returns the reply, an error line, or null when a retry makes sense
 static String interpret(int code,String resp){
  if(code==401||code==403) return "key died bestie, make a new one at openrouter.ai/keys";
  if(code==402) return "yo I'm out of credit: OpenRouter says my balance is negative. check openrouter.ai/credits";
  if(code==429){
   String low=resp==null?"":resp.toLowerCase();
   if(low.contains("per-day")||low.contains("per day")||low.contains("daily")) return "too many messages today: the free plan allows about 50 a day. I'm back tomorrow, or add $10 credit on OpenRouter for 1000 a day";
   return "too many messages too fast, give me a minute";
  }
  if(code>=200&&code<300){
   try{
    JSONObject j=new JSONObject(resp);
    Object ct=j.getJSONArray("choices").getJSONObject(0).getJSONObject("message").opt("content");
    String out=(ct==null||ct==JSONObject.NULL)?"":ct.toString();
    out=out.replaceAll("(?s)<think>.*?</think>","");
    int e=out.indexOf("</think>"); if(e>=0) out=out.substring(e+8);
    out=out.trim();
    return out.isEmpty()?null:out;
   }catch(Exception e){ return null; }
  }
  if(code==400||code==404||code==408||code>=500) return null;
  return "my brain lagged ("+code+") try again";
 }
 // one key, with one retry for flaky free models
 static String attempt(String key,String system,String user){
  try{
   int[] code={0};
   String resp=once(key,system,user,false,code);
   String out=interpret(code[0],resp);
   if(out==null){
    int first=code[0];
    resp=once(key,system,user,first==400,code);
    out=interpret(code[0],resp);
    if(out==null) out="my bad, brain buffered - say that again?";
   }
   return out;
  }catch(Exception e){
   String m=e.getMessage(); if(m==null) m="offline"; if(m.length()>60) m=m.substring(0,60);
   return "net hiccup: "+m;
  }
 }
 public static void ask(String key,String system,String user,Callback cb){ run(key,system,user,cb); }
 public static void askAuto(String key,String system,String user,Callback cb){
  try{ if(prefs!=null) prefs.edit().putInt("aiauto_"+day(),usedAuto()+1).apply(); }catch(Exception e){}
  run(key,system,user,cb);
 }
 static void run(final String key,final String system,final String user,final Callback cb){
  new Thread(new Runnable(){ public void run(){
   String out;
   ArrayList<String> ks=new ArrayList<String>();
   try{ if(prefs!=null) ks=Keys.all(prefs); }catch(Exception e){}
   if(ks.isEmpty()&&key!=null&&key.length()>0) ks.add(key);
   int tries=0;
   while(true){
    String k=(prefs==null||ks.isEmpty())?key:Keys.current(prefs,ks);
    out=attempt(k,system,user);
    boolean bad=out.startsWith("key died")||out.startsWith("yo I'm out")||out.startsWith("too many messages today");
    if(bad&&prefs!=null&&ks.size()>1){
     // this key is finished (rejected, out of credit, or out of free messages today): move to the next one
     Keys.markDead(prefs,k,out.startsWith("key died"));
     tries++;
     if(tries<ks.size()&&Keys.anyAlive(prefs,ks)) continue;
    }
    break;
   }
   final String fr=out;
   new Handler(Looper.getMainLooper()).post(new Runnable(){ public void run(){ cb.onResult(fr); }});
  }}).start();
 }
}
