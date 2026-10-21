package com.slime;
import java.util.ArrayList; import org.json.JSONArray;
public class Pouch {
 static final String DIR="/sdcard/SlimePouch";
 static final ArrayList<String> items=new ArrayList<String>();
 static String[] cand=null;
 static void load(MainService s){
  items.clear();
  try{ JSONArray a=new JSONArray(s.sp.getString("pouch","[]")); for(int i=0;i<a.length();i++) items.add(a.getString(i)); }catch(Exception e){ L.w("Pouch",e); }
 }
 static void save(MainService s){
  try{ JSONArray a=new JSONArray(); for(String x:items) a.put(x); s.sp.edit().putString("pouch",a.toString()).apply(); }catch(Exception e){ L.w("Pouch",e); }
  try{ s.slimeView.invalidate(); }catch(Exception e){ L.w("Pouch",e); }
 }
 static int count(){ return items.size(); }
 static int chub(){ return Math.min(6,items.size()*2); }
 static boolean safe(String p){ return p!=null&&p.matches("[A-Za-z0-9._ ()\\[\\]/+,&-]{1,200}"); }
 static boolean handle(final MainService s,String t,String low){
  low=low.trim();
  if(low.equals("what are you holding")||low.equals("pouch")||low.equals("holding")){
   if(items.isEmpty()) s.local("my hands are empty. give me something: hold FILENAME",false);
   else{ StringBuilder sb=new StringBuilder("I'm carrying "+items.size()+":\n"); for(String x:items) sb.append("- ").append(x).append("\n"); sb.append("\nsay drop to put them in Downloads, or drop to documents / pictures / termux"); s.local(sb.toString(),false); }
   return true;
  }
  if(low.startsWith("hold ")){
   String arg=t.substring(Math.min(5,t.length())).trim();
   if(arg.matches("\\d{1}")&&cand!=null){ int i=Integer.parseInt(arg)-1; if(i>=0&&i<cand.length){ copyIn(s,cand[i]); return true; } }
   if(!safe(arg)){ s.local("keep it simple: a file name, or a full path",false); return true; }
   if(arg.startsWith("/")){ copyIn(s,arg); return true; }
   find(s,arg); return true;
  }
  if(low.equals("drop")||low.startsWith("drop ")){
   String to=low.length()>5?low.substring(5).trim():"";
   if(to.startsWith("to ")) to=to.substring(3).trim();
   dropAll(s,to,t.length()>5?t.substring(Math.min(5,t.length())).trim():""); return true;
  }
  if(low.equals("stash")){
   if(items.isEmpty()){ s.local("nothing in my hands to stash",false); return true; }
   s.phone.sh.rish("mkdir -p "+DIR+"/stash; for f in "+DIR+"/*; do [ -f \"$f\" ] && mv \"$f\" "+DIR+"/stash/; done; echo done",new Shell.Result(){ public void done(int code,String out,String err,String info){
    if(info!=null||code!=0){ s.phone.report(code,out,err,info,null,true); return; }
    items.clear(); save(s); s.local("stashed in "+DIR+"/stash. my hands are free",true);
   }});
   return true;
  }
  return false;
 }
 static void find(final MainService s,String name){
  String script="N='"+Phone.q(name)+"'; for d in /sdcard/Download /sdcard/Documents /sdcard/Pictures /sdcard/DCIM /sdcard/Movies /sdcard/Music /sdcard; do find \"$d\" -maxdepth 4 -type f -iname \"*$N*\" 2>/dev/null; done | sort -u | head -n 6";
  s.slimeView.say("looking for it...");
  s.phone.sh.rish(script,new Shell.Result(){ public void done(int code,String out,String err,String info){
   if(info!=null||code!=0){ s.phone.report(code,out,err,info,null,true); return; }
   ArrayList<String> ok=new ArrayList<String>();
   for(String l:out.split("\n")){ l=l.trim(); if(safe(l)) ok.add(l); }
   if(ok.isEmpty()){ s.local("can't find a file like that in your Download, Documents, Pictures, DCIM, Movies or Music",false); return; }
   if(ok.size()==1){ copyIn(s,ok.get(0)); return; }
   cand=ok.toArray(new String[0]);
   StringBuilder sb=new StringBuilder("found a few, which one?\n");
   for(int i=0;i<cand.length;i++) sb.append(i+1).append(". ").append(cand[i]).append("\n");
   sb.append("\nsay hold 1, hold 2 ...");
   s.showChat(); s.local(sb.toString(),false);
  }});
 }
 static void copyIn(final MainService s,final String path){
  if(!safe(path)){ s.local("that path has odd characters, I won't touch it",false); return; }
  s.phone.sh.rish("mkdir -p "+DIR+" && cp -f '"+Phone.q(path)+"' "+DIR+"/ && echo ok",new Shell.Result(){ public void done(int code,String out,String err,String info){
   if(info!=null||code!=0){ s.phone.report(code,out,err,info,null,true); return; }
   String nm=path.substring(path.lastIndexOf('/')+1);
   if(!items.contains(nm)) items.add(nm);
   save(s); cand=null;
   s.moodEvent(3,6000); s.local("got it, I'm carrying "+nm,true);
  }});
 }
 static void dropAll(final MainService s,String to,String raw){
  if(items.isEmpty()){ s.local("my hands are empty",false); return; }
  String low=to.toLowerCase();
  if(low.equals("termux")){
   s.phone.sh.term("for f in "+DIR+"/*; do [ -f \"$f\" ] && cp -f \"$f\" ~/ && rm -f \"$f\"; done; echo done",new Shell.Result(){ public void done(int code,String out,String err,String info){
    if(info!=null||code!=0){ s.phone.report(code,out,err,info,null,true); return; }
    items.clear(); save(s); s.local("dropped everything in Termux's home folder",true);
   }});
   return;
  }
  String target="/sdcard/Download";
  if(low.equals("documents")) target="/sdcard/Documents";
  else if(low.equals("pictures")) target="/sdcard/Pictures";
  else if(low.equals("downloads")||low.equals("download")||low.isEmpty()) target="/sdcard/Download";
  else if(raw.startsWith("to ")&&safe(raw.substring(3).trim())&&raw.substring(3).trim().startsWith("/")) target=raw.substring(3).trim();
  else if(raw.startsWith("/")&&safe(raw)) target=raw;
  else { s.local("drop where? try: drop, drop to documents, drop to pictures, drop to termux, or a full folder path",false); return; }
  final String tg=target;
  s.phone.sh.rish("T='"+Phone.q(tg)+"'; mkdir -p \"$T\"; for f in "+DIR+"/*; do [ -f \"$f\" ] && cp -f \"$f\" \"$T/\" && rm -f \"$f\"; done; echo done",new Shell.Result(){ public void done(int code,String out,String err,String info){
   if(info!=null||code!=0){ s.phone.report(code,out,err,info,null,true); return; }
   items.clear(); save(s); s.local("dropped it in "+tg,true);
  }});
 }
}
