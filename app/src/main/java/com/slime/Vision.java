package com.slime;
import android.graphics.Bitmap; import android.graphics.BitmapFactory; import android.os.Handler; import android.os.Looper; import android.util.Base64;
import java.io.ByteArrayOutputStream; import java.io.File; import java.io.InputStream; import java.io.OutputStream; import java.net.HttpURLConnection; import java.net.URL;
import org.json.JSONArray; import org.json.JSONObject;
// Screenshot understanding. Routes to the active provider: Gemini takes inline base64 images,
// OpenRouter takes the image_url envelope. Falls back across providers when one has no keys.
public class Vision {
 public static final String DEFAULT="openrouter/free";
 public interface Callback{ void onResult(String reply); }
 public static void ask(final String question,final File img,final Callback cb){
  new Thread(new Runnable(){ public void run(){
   String r;
   try{
    android.content.SharedPreferences sp=OpenRouterClient.prefs;
    boolean gemini=Ai.GEMINI.equals(Ai.provider(sp));
    if(gemini&&!Keys.all(sp,Keys.G).isEmpty()){
     try{ r=GeminiClient.vision(question,img); }
     catch(Exception e){
      if(Keys.all(sp,Keys.OR).isEmpty()) throw e;
      r=openRouter(question,img); // Gemini hiccup and an OpenRouter key exists
     }
    } else if(!Keys.all(sp,Keys.OR).isEmpty()) r=openRouter(question,img);
    else throw new Exception("add an OpenRouter or Gemini key first");
   }catch(Exception e){ r="couldn't look: "+e.getMessage(); }
   final String out=r;
   new Handler(Looper.getMainLooper()).post(new Runnable(){ public void run(){ cb.onResult(out); }});
  }}).start();
 }

 static String openRouter(String question,File img) throws Exception{
  Bitmap b=BitmapFactory.decodeFile(img.getAbsolutePath());
  if(b==null) throw new Exception("I can't read the screenshot file");
  int mx=Math.max(b.getWidth(),b.getHeight());
  if(mx>1280){ float f=1280f/mx; b=Bitmap.createScaledBitmap(b,Math.round(b.getWidth()*f),Math.round(b.getHeight()*f),true); }
  ByteArrayOutputStream bo=new ByteArrayOutputStream(); b.compress(Bitmap.CompressFormat.JPEG,70,bo);
  String data="data:image/jpeg;base64,"+Base64.encodeToString(bo.toByteArray(),Base64.NO_WRAP);
  String model=FreeModels.nextVision(OpenRouterClient.prefs);
  JSONObject txt=new JSONObject(); txt.put("type","text"); txt.put("text",question);
  JSONObject url=new JSONObject(); url.put("url",data);
  JSONObject im=new JSONObject(); im.put("type","image_url"); im.put("image_url",url);
  JSONArray parts=new JSONArray(); parts.put(txt); parts.put(im);
  JSONObject user=new JSONObject(); user.put("role","user"); user.put("content",parts);
  JSONObject sys=new JSONObject(); sys.put("role","system"); sys.put("content","You are SlimeDroid, a chill blue slime pet who lives on the phone. You are looking at a screenshot of the phone screen. Answer the question warmly and briefly, max 60 words, plain text.");
  JSONArray msgs=new JSONArray(); msgs.put(sys); msgs.put(user);
  JSONObject body=new JSONObject(); body.put("model",model); body.put("max_tokens",900); body.put("messages",msgs);
  HttpURLConnection c=(HttpURLConnection)new URL("https://openrouter.ai/api/v1/chat/completions").openConnection();
  c.setRequestMethod("POST"); c.setRequestProperty("Authorization","Bearer "+OpenRouterClient.currentKey("")); c.setRequestProperty("Content-Type","application/json");
  c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(60000);
  OutputStream os=c.getOutputStream(); os.write(body.toString().getBytes("UTF-8")); os.close();
  int code=c.getResponseCode(); OpenRouterClient.countCall();
  InputStream is=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();
  ByteArrayOutputStream ob=new ByteArrayOutputStream(); byte[] buf=new byte[4096]; int n;
  if(is!=null){ while((n=is.read(buf))>0) ob.write(buf,0,n); is.close(); }
  JSONObject j=new JSONObject(ob.toString("UTF-8"));
  JSONArray ch=j.optJSONArray("choices");
  if(ch==null||ch.length()==0){ JSONObject er=j.optJSONObject("error"); throw new Exception(er!=null?er.optString("message","error"):("http "+code)); }
  Object ct=ch.getJSONObject(0).getJSONObject("message").opt("content");
  String t=(ct==null||ct==JSONObject.NULL)?"":ct.toString().trim();
  return t.isEmpty()?"hmm, I looked but got nothing back":t;
 }
}
