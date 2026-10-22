package com.slime;
import java.io.File; import java.io.FileOutputStream; import java.text.SimpleDateFormat; import java.util.Date; import java.util.Locale;
import org.json.JSONObject;
// Backs up the brain, chats and notification memory to shared storage so an uninstall can't wipe them.
// Primary path goes through Shizuku (rish) like Pouch; a no-shell copy is also kept in app external storage.
class Backup {
 static final String DIR="/sdcard/SlimePouch/backups";
 final MainService s;
 Backup(MainService s){ this.s=s; }
 private String stamp(){ return new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date()); }
 private String writeFallback(String json){
  try{
   File dir=s.getExternalFilesDir(null); if(dir==null) return "";
   File f=new File(dir,"slime-backup-"+stamp()+".json");
   FileOutputStream o=new FileOutputStream(f); o.write(json.getBytes("UTF-8")); o.close();
   return f.getAbsolutePath();
  }catch(Exception e){ L.w("Backup",e); return ""; }
 }
 void export(){
  try{
   JSONObject root=new JSONObject();
   root.put("app","slime"); root.put("ver",1); root.put("when",System.currentTimeMillis());
   root.put("brain",s.brain.exportArray());
   root.put("chats",s.mem.loadChats());
   root.put("notifs",s.mem.loadNotifs());
   final String json=root.toString(2);
   final String fallback=writeFallback(json);
   final String target=DIR+"/slime-backup-"+stamp()+".json";
   final String marker="SLIME_BACKUP_"+Integer.toString((int)(Math.random()*1000000));
   String script="mkdir -p '"+DIR+"'; cat > '"+target+"' <<'"+marker+"'\n"+json+"\n"+marker+"\nchmod 666 '"+target+"'; echo ok";
   if(s.phone==null){ fail(fallback); return; }
   s.phone.sh.rish(script,new Shell.Result(){ public void done(int code,String out,String err,String info){
    if(info!=null||code!=0){ fail(fallback); return; }
    s.local("backup saved: "+target+"\ncontains "+s.brain.exportArray().length()+" memories, "+s.mem.loadChats().length()+" chats. Say 'memory import' to restore the newest one.",false);
   }});
  }catch(Exception e){ L.w("Backup",e); s.local("backup failed: "+e.getMessage(),false); }
 }
 private void fail(String fallback){
  if(!fallback.isEmpty()) s.local("Shizuku shell isn't available, so I saved a copy here instead: "+fallback+"\nNote: that folder is removed if you uninstall. Set up Shizuku/rish for a backup in "+DIR+".",false);
  else s.local("couldn't write the backup. Check that Shizuku is running and Termux can run commands.",false);
 }
 void import_(String name){
  if(s.phone==null){ s.local("restoring needs the Termux/Shizuku shell, same as hold and drop.",false); return; }
  if(name.isEmpty()){
   String script="D='"+DIR+"'; F=$(ls -t \"$D\" 2>/dev/null | head -1); if [ -n \"$F\" ]; then cat \"$D/$F\"; else echo NONE; fi";
   s.phone.sh.rish(script,new Shell.Result(){ public void done(int code,String out,String err,String info){ parse(out,info,code); }});
   return;
  }
  if(!Pouch.safe(name)){ s.local("that file name has odd characters, I won't touch it",false); return; }
  final String target=name.startsWith("/")?name:DIR+"/"+name;
  s.phone.sh.rish("cat '"+target+"'",new Shell.Result(){ public void done(int code,String out,String err,String info){
   if(info!=null||code!=0||!out.trim().startsWith("{")){
    s.phone.sh.rish("ls -t '"+DIR+"' 2>/dev/null | head -5",new Shell.Result(){ public void done(int c2,String o2,String e2,String i2){
     s.local("can't read '"+target+"'. Available backups:\n"+(o2==null||o2.trim().isEmpty()?"(none - say memory export first)":o2),false);
    }});
    return;
   }
   parse(out,info,code);
  }});
 }
 private void parse(String out,String info,int code){
  try{
   if(info!=null||code!=0||out==null||!out.trim().startsWith("{")){ s.local("no backup found in "+DIR+". Say memory export first.",false); return; }
   JSONObject root=new JSONObject(out.trim());
   int before=s.brain.list.size();
   int mems=s.brain.importArray(root.optJSONArray("brain"));
   int chats=s.mem.mergeChats(root.optJSONArray("chats"));
   int notes=s.mem.mergeNotifs(root.optJSONArray("notifs"));
   s.local("restore done: "+mems+" memories ("+before+" -> "+s.brain.list.size()+"), "+chats+" chats, "+notes+" notifications merged. Duplicates were combined automatically.",false);
  }catch(Exception e){ L.w("Backup",e); s.local("that file isn't a SlimeDroid backup ("+e.getClass().getSimpleName()+")",false); }
 }
}
