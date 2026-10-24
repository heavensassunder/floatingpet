package com.slime;
import android.content.SharedPreferences; import android.os.Handler; import android.os.Looper;
import java.io.BufferedReader; import java.io.InputStream; import java.io.InputStreamReader; import java.io.OutputStream;
import java.net.HttpURLConnection; import java.net.URL;
import java.util.concurrent.CountDownLatch; import java.util.concurrent.TimeUnit;
import org.json.JSONArray; import org.json.JSONObject;
// Fully offline chat: llama.cpp's llama-server runs headless inside Termux (no terminal window needed),
// serving an OpenAI-compatible API on 127.0.0.1:8080. Models are small GGUF files made for 2 GB phones.
public class LocalLlm {
 static final String PORT="8080", BASE="http://127.0.0.1:"+PORT;
 static final String SMOL="smol", QWEN="qwen";
 // id, friendly label, HuggingFace repo, GGUF filename
 static final String[][] MODELS={
  {SMOL,"SmolLM2-360M (fast, English-first, ~390 MB)","bartowski/SmolLM2-360M-Instruct-GGUF","SmolLM2-360M-Instruct-Q4_K_M.gguf"},
  {QWEN,"Qwen3-0.6B (smarter, better Swahili, ~470 MB)","bartowski/Qwen3-0.6B-GGUF","Qwen3-0.6B-Q4_K_M.gguf"}
 };
 static SharedPreferences prefs;
 static final Handler h=new Handler(Looper.getMainLooper());
 static String modelId(){ String m=prefs==null?SMOL:prefs.getString("offlineModel",SMOL); return QWEN.equals(m)?QWEN:SMOL; }
 static String[] model(){ for(String[] m:MODELS){ if(m[0].equals(modelId())) return m; } return MODELS[0]; }
 static String modelLabel(){ for(String[] m:MODELS){ if(m[0].equals(modelId())) return m[1]; } return MODELS[0][1]; }

 // ---------- synchronous bridge over the Termux RUN_COMMAND service ----------
 static String[] shell(String script) throws Exception{
  final MainService s=MainService.instance;
  if(s==null||s.phone==null) throw new Exception("I'm still waking up, try again in a second");
  final CountDownLatch latch=new CountDownLatch(1); final String[][] r=new String[1][];
  s.phone.sh.term(script,new Shell.Result(){ public void done(int code,String out,String err,String info){
   r[0]=new String[]{code+"",out==null?"":out,err==null?"":err,info==null?"":info}; latch.countDown();
  }});
  if(!latch.await(30,TimeUnit.SECONDS)) throw new Exception("Termux didn't answer - is it installed and allowed to run in background?");
  if(r[0]==null) throw new Exception("no response from Termux");
  if(!r[0][3].isEmpty()) throw new Exception(r[0][3]);
  return r[0];
 }
 static boolean health(){
  HttpURLConnection c=null;
  try{
   c=(HttpURLConnection)new URL(BASE+"/health").openConnection();
   c.setConnectTimeout(2500); c.setReadTimeout(2500); c.setRequestMethod("GET");
   int code=c.getResponseCode(); return code>=200&&code<300;
  }catch(Exception e){ return false; }
  finally { if(c!=null) c.disconnect(); }
 }

 // ---------- one-time setup and model downloads (run in Termux's background, never block on them) ----------
 static String downloadScript(String id){
  String repo="", hint="";
  for(String[] m:MODELS){ if(m[0].equals(id)){ repo=m[2]; hint=m[3]; } }
  return
   "DIR=\"$HOME/slimemodels\"; mkdir -p \"$DIR\"\n"+
   // pidfile guard: don't start a second downloader for the same model
   "PIDF=\"$DIR/"+hint+".pid\"\n"+
   "if [ -f \"$PIDF\" ] && kill -0 \"$(cat \"$PIDF\" 2>/dev/null)\" 2>/dev/null; then echo ALREADY_RUNNING; exit 0; fi\n"+
   "nohup bash -c '\n"+
   " DIR=\"$HOME/slimemodels\"; REPO=\""+repo+"\"; HINT=\""+hint+"\"; PIDF=\"$DIR/$HINT.pid\"\n"+
   " echo $$ > \"$PIDF\"; trap \"rm -f \\\"$PIDF\\\"\" EXIT\n"+
   " echo \"[$HINT] downloading ...\" >> \"$DIR/dl.log\"\n"+
   " BIN=$(command -v llama-server || command -v server)\n"+
   " if [ -z \"$BIN\" ]; then echo \"[$HINT] installing llama.cpp ...\" >> \"$DIR/dl.log\"; pkg install -y llama-cpp >> \"$DIR/dl.log\" 2>&1; fi\n"+
   " if [ -s \"$DIR/$HINT\" ]; then echo \"[$HINT] DOWNLOAD_DONE\" >> \"$DIR/dl.log\"; exit 0; fi\n"+
   " curl -fL \"https://huggingface.co/api/models/$REPO\" -o \"$DIR/hf.json\" 2>>\"$DIR/dl.log\"\n"+
   " FNAME=$(grep -o '\"rfilename\":\"[^\"]*\"' \"$DIR/hf.json\" 2>/dev/null | sed 's/\"rfilename\":\"//;s/\"//' | grep 'Q4_K_M.gguf' | head -1)\n"+
   " [ -z \"$FNAME\" ] && FNAME=\"$HINT\"\n"+
   " echo \"[$HINT] fetching $FNAME (resumable)\" >> \"$DIR/dl.log\"\n"+
   // -C - resumes an interrupted .part instead of restarting from zero
   " if curl -fL -C - --retry 3 -o \"$DIR/$HINT.part\" \"https://huggingface.co/$REPO/resolve/main/$FNAME\" >> \"$DIR/dl.log\" 2>&1; then mv \"$DIR/$HINT.part\" \"$DIR/$HINT\"; echo \"[$HINT] DOWNLOAD_DONE\" >> \"$DIR/dl.log\"; else echo \"[$HINT] DOWNLOAD_FAILED - rerun the same command to resume\" >> \"$DIR/dl.log\"; fi\n"+
   "' >/dev/null 2>&1 &\n echo started";
 }
 static String statusScript(){
  StringBuilder files=new StringBuilder();
  for(String[] m:MODELS){
   files.append(" [ -s \"$DIR/").append(m[3]).append("\" ] && echo \"MODEL=").append(m[0]).append(" $(du -h '$DIR/").append(m[3]).append("' | cut -f1)\";");
   files.append(" [ -s \"$DIR/").append(m[3]).append(".part\" ] && echo \"PART=").append(m[0]).append(" $(du -h '$DIR/").append(m[3]).append(".part' | cut -f1) so far\";");
  }
  return
   "DIR=\"$HOME/slimemodels\"; mkdir -p \"$DIR\"\n"+
   "BIN=$(command -v llama-server || command -v server); [ -n \"$BIN\" ] && echo BIN=ok || echo BIN=missing\n"+
   files+
   // name-based matches (no -f) so the downloader script's own text can't trigger false positives
   "\n if pgrep -x pkg >/dev/null 2>&1 || pgrep -x apt >/dev/null 2>&1 || pgrep -x dpkg >/dev/null 2>&1; then echo INSTALL=running; else echo INSTALL=idle; fi\n"+
   "if pgrep -x curl >/dev/null 2>&1; then echo DL=running; else echo DL=idle; fi\n"+
   "tail -1 \"$DIR/dl.log\" 2>/dev/null | sed 's/^/DLLOG=/'\n"+
   "if pgrep -x llama-server >/dev/null 2>&1 || pgrep -x server >/dev/null 2>&1; then echo SERVER=running; else echo SERVER=stopped; fi\n"+
   "free -m 2>/dev/null | awk '/Mem:/{print \"MEM_MB=\"$7}'\n"+
   "df -m \"$HOME\" | tail -1 | awk '{print \"DISK_MB=\"$4}'";
 }
 static String startScript(){
  String hint=model()[3];
  return
   "DIR=\"$HOME/slimemodels\"; BIN=$(command -v llama-server || command -v server)\n"+
   "if [ -z \"$BIN\" ]; then echo NO_BIN; exit 0; fi\n"+
   "if [ ! -s \"$DIR/"+hint+"\" ]; then echo NO_MODEL; exit 0; fi\n"+
   "pkill -x llama-server 2>/dev/null; pkill -x server 2>/dev/null; sleep 1\n"+
   "termux-wake-lock 2>/dev/null\n"+
   "nohup \"$BIN\" -m \"$DIR/"+hint+"\" -c 1024 -t 4 --host 127.0.0.1 --port "+PORT+" > \"$DIR/server.log\" 2>&1 &\n echo STARTED";
 }
 static String stopScript(){
  return "pkill -x llama-server 2>/dev/null; pkill -x server 2>/dev/null; termux-wake-unlock 2>/dev/null; echo stopped";
 }
 static void stopServer(){
  new Thread(new Runnable(){ public void run(){ try{ shell(stopScript()); }catch(Exception e){ L.w("LocalLlm",e); } }}).start();
 }

 // ---------- chat ----------
 interface ReadyCb{ void ready(String error); }
 static void ensureServer(final ReadyCb cb){
  new Thread(new Runnable(){ public void run(){
   if(health()){ cb.ready(null); return; }
   String err=null;
   try{
    String[] r=shell(startScript());
    String out=r[1];
    if(out.contains("NO_BIN")) err="llama.cpp isn't installed in Termux yet - say 'offline download smol' and I'll install it with the model";
    else if(out.contains("NO_MODEL")) err="the "+modelId()+" model isn't downloaded yet - say 'offline download "+modelId()+"'";
    else if(!out.contains("STARTED")) err="couldn't start the local server in Termux: "+out.trim();
    if(err==null){
     for(int i=0;i<45;i++){ Thread.sleep(2000); if(health()) break; }
     if(!health()) err="the local model took too long to load (low free RAM?). Say 'offline status' to check";
    }
   }catch(Exception e){ err=e.getMessage(); }
   final String f=err; h.post(new Runnable(){ public void run(){ cb.ready(f); }});
  }}).start();
 }
 static void postBubble(final String partial){
  h.post(new Runnable(){ public void run(){ try{ if(MainService.instance!=null) MainService.instance.slimeView.say(partial); }catch(Exception e){ L.w("LocalLlm",e); } }});
 }
 static String sanitize(String text){
  return text.replaceAll("(?s)<think>.*?</think>","").replaceAll("\\[\\[[^\\]]*\\]\\]","").trim();
 }
 static void ask(final String system,final String user,final Ai.Callback cb){
  ensureServer(new ReadyCb(){ public void ready(final String startErr){
   if(startErr!=null){ cb.onResult(startErr); return; }
   new Thread(new Runnable(){ public void run(){
    String out;
    try{ out=chat(system,user); }
    catch(Exception e){ String m=e.getMessage(); out="local brain hiccup: "+(m==null?"offline":m.length()>70?m.substring(0,70):m); }
    final String fr=out;
    h.post(new Runnable(){ public void run(){ cb.onResult(fr); }});
   }}).start();
  }});
 }
 static String chat(String system,String user) throws Exception{
  JSONObject body=new JSONObject();
  JSONArray msgs=new JSONArray();
  msgs.put(new JSONObject().put("role","system").put("content",system+"\n\nYou are running fully offline on a small phone model. Answer in plain text only, no tags or bracket markers, under 60 words."));
  String u=user;
  if(QWEN.equals(modelId())){
   body.put("chat_template_kwargs",new JSONObject().put("enable_thinking",false));
   u=user+"\n/no_think";
  }
  msgs.put(new JSONObject().put("role","user").put("content",u));
  body.put("messages",msgs).put("max_tokens",320).put("temperature",0.8).put("stream",true);
  HttpURLConnection c=(HttpURLConnection)new URL(BASE+"/v1/chat/completions").openConnection();
  c.setRequestMethod("POST"); c.setRequestProperty("Content-Type","application/json"); c.setDoOutput(true);
  c.setConnectTimeout(10000); c.setReadTimeout(180000);
  OutputStream os=c.getOutputStream(); os.write(body.toString().getBytes("UTF-8")); os.close();
  int code=c.getResponseCode();
  if(code<200||code>=300){
   String es=readAll(c.getErrorStream());
   throw new Exception("server said "+code+" "+es.substring(0,Math.min(80,es.length())));
  }
  StringBuilder acc=new StringBuilder();
  BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"));
  String line;
  while((line=br.readLine())!=null){
   if(!line.startsWith("data:")) continue;
   String d=line.substring(5).trim();
   if(d.equals("[DONE]")) break;
   try{
    JSONObject j=new JSONObject(d);
    JSONArray ch=j.optJSONArray("choices");
    if(ch==null||ch.length()==0) continue;
    JSONObject delta=ch.getJSONObject(0).optJSONObject("delta");
    if(delta==null) continue;
    String piece=delta.optString("content","");
    if(!piece.isEmpty()){ acc.append(piece); postBubble(acc.toString()); }
   }catch(Exception e){ L.w("LocalLlm",e); }
  }
  br.close(); c.disconnect();
  String out=sanitize(acc.toString());
  return out.isEmpty()?"my local brain drew a blank - say that again?":out;
 }
 static String readAll(InputStream is) throws Exception{
  if(is==null) return "";
  BufferedReader br=new BufferedReader(new InputStreamReader(is,"UTF-8")); StringBuilder sb=new StringBuilder(); String l;
  while((l=br.readLine())!=null) sb.append(l);
  br.close(); return sb.toString();
 }

 // ---------- commands ----------
 static boolean handle(final MainService s,String t,String low){
  low=low.trim();
  if(low.equals("offline download")||low.startsWith("offline download ")){
   String arg=low.length()>17?low.substring(17).trim():"all";
   final java.util.ArrayList<String> ids=new java.util.ArrayList<String>();
   if(arg.equals("all")){ for(String[] m:MODELS) ids.add(m[0]); }
   else if(arg.equals(SMOL)||arg.equals(QWEN)) ids.add(arg);
   else { s.local("say: offline download smol | qwen | all",false); return true; }
   new Thread(new Runnable(){ public void run(){
    try{
     final StringBuilder msg=new StringBuilder();
     for(String id:ids){
      String[] r=shell(downloadScript(id));
      if(r[1].contains("ALREADY_RUNNING")) msg.append(id).append(" is already downloading; ");
     }
     h.post(new Runnable(){ public void run(){
      if(msg.length()>0) s.local(msg+"not starting a duplicate. Say 'offline status' to watch it.",false);
      else s.local("download started in Termux's background - about 400-500 MB per model, works on Wi-Fi or mobile data. Say 'offline status' to watch progress, then 'use offline'.",false);
     }});
    }catch(final Exception e){ h.post(new Runnable(){ public void run(){ s.local("couldn't start the download: "+e.getMessage(),false); }}); }
   }}).start();
   return true;
  }
  if(low.equals("offline status")){
   new Thread(new Runnable(){ public void run(){
    try{
     final String[] r=shell(statusScript());
     final StringBuilder sb=new StringBuilder("offline brain:\nactive model: ").append(modelId()).append(" - ").append(modelLabel()).append("\n");
     for(String ln:r[1].split("\n")){ if(ln.startsWith("BIN=")||ln.startsWith("MODEL=")||ln.startsWith("PART=")||ln.startsWith("INSTALL=")||ln.startsWith("DL=")||ln.startsWith("SERVER=")||ln.startsWith("MEM_MB=")||ln.startsWith("DISK_MB=")||ln.startsWith("DLLOG=")) sb.append(ln).append("\n"); }
     sb.append("\noffline download smol|qwen|all | offline model smol|qwen | use offline");
     h.post(new Runnable(){ public void run(){ s.showChat(); s.local(sb.toString(),false); }});
    }catch(final Exception e){ h.post(new Runnable(){ public void run(){ s.local("status failed: "+e.getMessage(),false); }}); }
   }}).start();
   return true;
  }
  if(low.equals("offline model")||low.equals("offline models")){
   StringBuilder sb=new StringBuilder("offline models (active: "+modelId()+"):\n");
   for(String[] m:MODELS) sb.append("- ").append(m[0]).append(": ").append(m[1]).append("\n");
   sb.append("switch with: offline model smol  or  offline model qwen");
   s.showChat(); s.local(sb.toString(),false); return true;
  }
  if(low.startsWith("offline model ")){
   String id=low.substring(14).trim();
   if(!id.equals(SMOL)&&!id.equals(QWEN)){ s.local("say: offline model smol or offline model qwen",false); return true; }
   s.sp.edit().putString("offlineModel",id).apply(); stopServer();
   s.local("offline model is now "+id+" - the server will load it on your next offline message",true); return true;
  }
  if(low.equals("offline start")){
   s.local("loading the local brain, this can take about 10-20 seconds...",false);
   ensureServer(new ReadyCb(){ public void ready(String err){ s.local(err!=null?err:"local brain running on 127.0.0.1:"+PORT+" - say 'use offline' to chat with it",false); }});
   return true;
  }
  if(low.equals("offline stop")){ stopServer(); s.local("local server stopped, RAM and wake lock released",false); return true; }
  return false;
 }
}
