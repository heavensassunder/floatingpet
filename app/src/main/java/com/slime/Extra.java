package com.slime;
import android.content.pm.PackageManager; import android.provider.Settings;
public class Extra {
 final MainService s;
 public Extra(MainService s){ this.s=s; }
 static String m(boolean ok){ return ok?"\u2713":"\u2717"; }
 public boolean handle(String t,String low){
  low=low.trim();
  if(More.handle(s,t,low)) return true;
  if(low.equals("model")){ s.local("model: "+OpenRouterClient.model+"  (change: model NAME, or model reset)",false); return true; }
  if(low.equals("model reset")){ OpenRouterClient.model=OpenRouterClient.MODEL; s.sp.edit().remove("model").apply(); s.local("model back to "+OpenRouterClient.model,false); return true; }
  if(low.startsWith("model ")){
   String nm=t.substring(Math.min(6,t.length())).trim();
   if(nm.matches("[A-Za-z0-9._:/-]{3,80}")){ OpenRouterClient.model=nm; s.sp.edit().putString("model",nm).apply(); s.local("model is now "+nm,false); }
   else s.local("that doesn't look like a model name. example: model openai/gpt-4o-mini",false);
   return true;
  }
  if(low.equals("diagnose")||low.equals("diagnosis")){ diagnose(); return true; }
  if(low.equals("models")){ Models.list(s); return true; }
  return false;
 }
 void diagnose(){
  final StringBuilder sb=new StringBuilder("checking myself:\n");
  boolean overlay=true; try{ overlay=Settings.canDrawOverlays(s); }catch(Exception e){}
  sb.append(m(overlay)).append(" overlay permission\n");
  sb.append(m(EyeService.instance!=null)).append(" accessibility (my eyes)\n");
  String nl=null; try{ nl=Settings.Secure.getString(s.getContentResolver(),"enabled_notification_listeners"); }catch(Exception e){}
  sb.append(m(nl!=null&&nl.contains("com.slime"))).append(" notification access\n");
  boolean term=s.phone!=null&&s.phone.sh.installed();
  boolean perm=false; try{ perm=s.checkSelfPermission("com.termux.permission.RUN_COMMAND")==PackageManager.PERMISSION_GRANTED; }catch(Exception e){}
  sb.append(m(term)).append(" Termux installed\n");
  sb.append(m(perm)).append(" run-commands-in-Termux permission\n");
  if(!term){ s.showChat(); s.local(sb.toString()+"fix: install Termux, then run setup_rish.sh in it.",false); return; }
  if(!perm){ s.showChat(); s.local(sb.toString()+"fix: Settings > Apps > SlimeDroid > Permissions > allow 'Run commands in Termux'. Or in Termux: bash ~/setup_rish.sh",false); return; }
  s.slimeView.say("checking...");
  s.phone.sh.rish("id",new Shell.Result(){ public void done(int code,String out,String err,String info){
   String fix;
   if(info!=null&&info.startsWith("Termux didn't answer")){
    sb.append(m(false)).append(" Termux answering me\n");
    fix="fix, 2 steps: 1) in Termux run: echo allow-external-apps=true >> ~/.termux/termux.properties && termux-reload-settings   2) force-stop Termux, open it once, then try again.";
   } else if(info!=null){
    sb.append(m(false)).append(" Termux: ").append(info).append("\n"); fix="fix: "+info;
   } else if(code==127){
    sb.append(m(false)).append(" rish (Shizuku shell)\n"); fix="fix: in Termux run: bash ~/setup_rish.sh";
   } else if(code!=0){
    sb.append(m(false)).append(" Shizuku shell: ").append(Phone.clip(err)).append("\n"); fix="fix: open Shizuku and check it says running (start it with wireless debugging), and that Termux is allowed in Shizuku.";
   } else {
    sb.append(m(true)).append(" Shizuku shell works\n"); fix="all good, nothing to fix.";
   }
   s.showChat(); s.local(sb.toString()+fix,false);
  }});
 }
}
