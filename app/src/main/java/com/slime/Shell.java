package com.slime;
import android.app.PendingIntent; import android.content.BroadcastReceiver; import android.content.Context; import android.content.Intent; import android.content.IntentFilter;
import android.os.Build; import android.os.Bundle; import android.os.Handler; import android.os.Looper;
// Runs commands inside Termux (RUN_COMMAND). "rish" there is Shizuku's shell, so commands run with adb-shell power.
public class Shell {
 public interface Result{ void done(int code,String out,String err,String info); }
 static final String TERMUX="com.termux", HOME="/data/data/com.termux/files/home", BIN="/data/data/com.termux/files/usr/bin/";
 final Context c; final Handler main=new Handler(Looper.getMainLooper()); int seq=0;
 public Shell(Context c){ this.c=c; }
 public boolean installed(){ try{ c.getPackageManager().getPackageInfo(TERMUX,0); return true; }catch(Exception e){ return false; } }
 public void rish(String script,Result cb){ run(BIN+"env",new String[]{"RISH_APPLICATION_ID=com.termux",BIN+"rish","-c",script},cb); }
 public void term(String script,Result cb){ run(BIN+"bash",new String[]{"-c",script},cb); }
 static String nz(String x){ return x==null?"":x; }
 void fail(boolean[] fin,BroadcastReceiver r,Runnable t,Result cb,String msg){
  if(fin[0]) return; fin[0]=true; main.removeCallbacks(t);
  try{ c.unregisterReceiver(r); }catch(Exception e){}
  cb.done(-1,"","",msg);
 }
 void run(String path,String[] args,final Result cb){
  if(!installed()){ cb.done(-1,"","","Termux isn't installed"); return; }
  final String action="com.slime.SHELL."+System.currentTimeMillis()+"."+(++seq);
  final boolean[] fin={false};
  final BroadcastReceiver[] rc=new BroadcastReceiver[1];
  final Runnable[] to=new Runnable[1];
  rc[0]=new BroadcastReceiver(){ public void onReceive(Context ctx,Intent in){
   if(fin[0]) return; fin[0]=true; main.removeCallbacks(to[0]);
   try{ c.unregisterReceiver(rc[0]); }catch(Exception e){}
   Bundle b=in.getBundleExtra("result");
   String out="", err="", info=null; int code=-1;
   if(b!=null){
    out=nz(b.getString("stdout")); err=nz(b.getString("stderr")); code=b.getInt("exitCode",-1);
    String em=b.getString("errmsg"); if(em!=null&&!em.isEmpty()) info="Termux said: "+em;
   } else info="no result came back from Termux";
   cb.done(code,out,err,info);
  }};
  to[0]=new Runnable(){ public void run(){
   if(fin[0]) return; fin[0]=true;
   try{ c.unregisterReceiver(rc[0]); }catch(Exception e){}
   cb.done(-1,"","","Termux didn't answer. Check allow-external-apps=true in ~/.termux/termux.properties, that SlimeDroid has the 'Run commands in Termux' permission, and that Shizuku is running.");
  }};
  try{
   IntentFilter f=new IntentFilter(action);
   if(Build.VERSION.SDK_INT>=33) c.registerReceiver(rc[0],f,Context.RECEIVER_NOT_EXPORTED); else c.registerReceiver(rc[0],f);
   Intent ri=new Intent(action); ri.setPackage(c.getPackageName());
   int fl=PendingIntent.FLAG_UPDATE_CURRENT; if(Build.VERSION.SDK_INT>=31) fl|=PendingIntent.FLAG_MUTABLE;
   PendingIntent pi=PendingIntent.getBroadcast(c,seq,ri,fl);
   Intent i=new Intent("com.termux.RUN_COMMAND"); i.setClassName(TERMUX,"com.termux.app.RunCommandService");
   i.putExtra("com.termux.RUN_COMMAND_PATH",path);
   i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS",args);
   i.putExtra("com.termux.RUN_COMMAND_WORKDIR",HOME);
   i.putExtra("com.termux.RUN_COMMAND_BACKGROUND",true);
   i.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT",pi);
   main.postDelayed(to[0],25000);
   c.startService(i);
  }catch(SecurityException e){
   fail(fin,rc[0],to[0],cb,"SlimeDroid needs the 'Run commands in Termux' permission (App info > Permissions). Run setup_rish.sh in Termux and I'll get it for myself.");
  }catch(Exception e){
   fail(fin,rc[0],to[0],cb,"couldn't reach Termux: "+e.getMessage());
  }
 }
}
