package com.slime;
import android.os.Handler; import android.os.Looper; import java.io.*; import java.net.HttpURLConnection; import java.net.URL;
public class OpenRouterClient {
 public interface Callback{ void onResult(String reply); }
 public static void ask(final String key, final String user, final String screen, final Callback cb){
  new Thread(new Runnable(){ public void run(){
   try{
    URL url=new URL("https://openrouter.ai/api/v1/chat/completions");
    HttpURLConnection c=(HttpURLConnection)url.openConnection();
    c.setRequestMethod("POST"); c.setRequestProperty("Authorization","Bearer "+key); c.setRequestProperty("Content-Type","application/json"); c.setDoOutput(true);
    c.setConnectTimeout(15000); c.setReadTimeout(25000);
    String sys="You are SlimeDroid, a chill blue slime pet living on the phone. You have a simple life, you watch the screen. Be witty, warm, concise, like Meta AI. Don't be cringe. Don't end every reply with a question. 1-2 sentences max. Screen: "+screen.substring(0,Math.min(220,screen.length()));
    String body="{\"model\":\"openrouter/auto\",\"max_tokens\":140,\"temperature\":0.85,\"messages\":[{\"role\":\"system\",\"content\":\""+sys.replace("\"","'").replace("\n"," ")+"\"},{\"role\":\"user\",\"content\":\""+user.replace("\"","'").replace("\n"," ")+"\"}]}";
    c.getOutputStream().write(body.getBytes("UTF-8"));
    BufferedReader br=new BufferedReader(new InputStreamReader(c.getResponseCode()==200?c.getInputStream():c.getErrorStream()));
    StringBuilder sb=new StringBuilder(); String l; while((l=br.readLine())!=null) sb.append(l);
    String resp=sb.toString(); String reply=""; int i=resp.indexOf("\"content\":\"");
    if(i!=-1){ i+=11; int j=resp.indexOf("\"",i); while(j>0&&resp.charAt(j-1)=='\\') j=resp.indexOf("\"",j+1); if(j!=-1) reply=resp.substring(i,j).replace("\\n"," ").replace("\\\"","\"").trim(); }
    if(reply.isEmpty()) reply="heh";
    final String fr=reply; new Handler(Looper.getMainLooper()).post(new Runnable(){ public void run(){ cb.onResult(fr); }});
   }catch(Exception e){ new Handler(Looper.getMainLooper()).post(new Runnable(){ public void run(){ cb.onResult("ugh, net died"); }}); }
  }}).start();
 }
}
