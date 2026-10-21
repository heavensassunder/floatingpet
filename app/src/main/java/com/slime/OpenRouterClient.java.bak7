package com.slime;
import android.os.Handler; import android.os.Looper;
import java.io.*; import java.net.HttpURLConnection; import java.net.URL;
import org.json.JSONArray; import org.json.JSONObject;
public class OpenRouterClient {
 public interface Callback{ void onResult(String reply); }
 public static final String MODEL="openai/gpt-3.5-turbo";
 public static String buildSystem(String longMem,String mood,String screen,String notifs,String recent){
  StringBuilder sb=new StringBuilder();
  sb.append("You are SlimeDroid, a floating blue slime pet living on the user's phone, with the personality of Meta AI. ");
  sb.append("Your vibe: warm, helpful, a bit playful, intelligent. Talk like a real friend: concise, natural, conversational. ");
  sb.append("No corporate speak, no 'How can I assist you today', no hashtags, no 'as an AI'. Be witty but not savage. Be useful first, fun second. ");
  sb.append("Keep replies 15-35 words. Lead with the best point and say why it matters. ");
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
 static String readAll(InputStream is) throws IOException{
  if(is==null) return "";
  BufferedReader br=new BufferedReader(new InputStreamReader(is,"UTF-8"));
  StringBuilder sb=new StringBuilder(); String l;
  while((l=br.readLine())!=null) sb.append(l);
  br.close(); return sb.toString();
 }
 public static void ask(final String key, final String system, final String user, final Callback cb){
  new Thread(new Runnable(){ public void run(){
   String out="";
   try{
    JSONObject body=new JSONObject();
    body.put("model",MODEL); body.put("max_tokens",180); body.put("temperature",0.85);
    JSONArray msgs=new JSONArray();
    msgs.put(new JSONObject().put("role","system").put("content",system));
    msgs.put(new JSONObject().put("role","user").put("content",user));
    body.put("messages",msgs);
    HttpURLConnection c=(HttpURLConnection)new URL("https://openrouter.ai/api/v1/chat/completions").openConnection();
    c.setRequestMethod("POST"); c.setRequestProperty("Authorization","Bearer "+key); c.setRequestProperty("Content-Type","application/json"); c.setDoOutput(true);
    c.setConnectTimeout(15000); c.setReadTimeout(30000);
    OutputStream os=c.getOutputStream(); os.write(body.toString().getBytes("UTF-8")); os.close();
    int code=c.getResponseCode();
    String resp=readAll(code>=200&&code<300?c.getInputStream():c.getErrorStream());
    if(code==401||code==403) out="key died bestie, make new one at openrouter.ai/keys";
    else if(code==402) out="yo I'm out of tokens, throw $1 credit on OpenRouter so I can keep yapping";
    else if(code==429) out="too many messages too fast, give me a sec";
    else if(code>=200&&code<300){
     JSONObject j=new JSONObject(resp);
     out=j.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
     if(out.isEmpty()) out="my bad, brain buffered - say that again?";
    } else out="my brain lagged ("+code+") try again";
   }catch(Exception e){
    String m=e.getMessage(); if(m==null) m="offline"; if(m.length()>60) m=m.substring(0,60);
    out="net hiccup: "+m;
   }
   final String fr=out;
   new Handler(Looper.getMainLooper()).post(new Runnable(){ public void run(){ cb.onResult(fr); }});
  }}).start();
 }
}
