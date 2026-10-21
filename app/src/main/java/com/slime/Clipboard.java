package com.slime;
import android.content.ClipData; import android.content.ClipboardManager; import android.content.Context;
// Watches the system clipboard and runs explain/summarize/translate/reply tasks on copied text.
class Clipboard {
 final MainService s; final ClipboardManager mgr; long lastNudge;
 private final ClipboardManager.OnPrimaryClipChangedListener listener=new ClipboardManager.OnPrimaryClipChangedListener(){ public void onPrimaryClipChanged(){ onChanged(); }};
 Clipboard(MainService s){
  this.s=s;
  ClipboardManager m=null;
  try{ m=(ClipboardManager)s.getSystemService(Context.CLIPBOARD_SERVICE); }catch(Exception e){ L.w("Clipboard",e); }
  mgr=m;
 }
 void attach(){ try{ if(mgr!=null) mgr.addPrimaryClipChangedListener(listener); }catch(Exception e){ L.w("Clipboard",e); } }
 void detach(){ try{ if(mgr!=null) mgr.removePrimaryClipChangedListener(listener); }catch(Exception e){ L.w("Clipboard",e); } }

 void onChanged(){
  long now=System.currentTimeMillis();
  if(now-lastNudge<60000L||!s.canTalk()) return;
  lastNudge=now; s.slimeView.say("copied something? type 'clip' and I'll help"); s.slimeView.bounceAnim();
 }
 boolean isCmd(String low){ return low.equals("clip")||low.startsWith("clip ")||low.endsWith(" clip")||low.contains("clipboard")||low.contains(" clip "); }
 String read(){
  try{
   if(mgr==null||!mgr.hasPrimaryClip()) return "";
   ClipData d=mgr.getPrimaryClip(); if(d==null||d.getItemCount()==0) return "";
   CharSequence t=d.getItemAt(0).coerceToText(s); return t==null?"":t.toString().trim();
  }catch(Exception e){ L.w("Clipboard",e); return ""; }
 }
 void task(String original,String low){
  String clip=read();
  if(clip.isEmpty()){ s.local("can't see your clipboard. copy something, open my chat, then type clip",false); return; }
  if(clip.length()>1500) clip=clip.substring(0,1500);
  String task="Explain this copied text simply and briefly.";
  if(low.contains("summar")) task="Summarize this copied text in 2-3 short sentences.";
  else if(low.contains("translat")) task="Translate this copied text. If it is not English, translate it to English. If it is English, translate it to Swahili.";
  else if(low.contains("repl")) task="Write one short, natural reply I could send to this message.";
  s.askTask(original,task+"\n\nCOPIED TEXT:\n"+clip);
 }
}
