package com.slime;
import android.content.Intent; import android.content.pm.PackageManager; import android.provider.Settings;
public class ActionExecutor {
 MainService svc; public ActionExecutor(MainService s){ svc=s; }
 public String run(String cmd,String arg){
  if("open".equals(cmd)) return open(arg);
  EyeService eye=EyeService.instance;
  if(eye==null) return "Enable Accessibility!";
  if("tap".equals(cmd)){
   String[] pr=arg.trim().split("\\s+");
   if(pr.length==2&&pr[0].matches("\\d+")&&pr[1].matches("\\d+")){ eye.tapAt(Integer.parseInt(pr[0]),Integer.parseInt(pr[1])); return "Tapped "+pr[0]+","+pr[1]; }
   return eye.tapText(arg);
  }
  if("type".equals(cmd)) return eye.typeText(arg);
  if("scroll".equals(cmd)) return eye.scroll(arg);
  return "don't know how to "+cmd;
 }
 public String open(String target){
  String t=target==null?"":target.toLowerCase().trim();
  if(t.isEmpty()) return "open what?";
  try{
   if(t.equals("settings")||t.equals("setting")){ Intent i=new Intent(Settings.ACTION_SETTINGS); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); svc.startActivity(i); return "Settings opened"; }
   PackageManager pm=svc.getPackageManager(); String pkg=t;
   if(t.contains("whatsapp")) pkg=t.contains("business")?"com.whatsapp.w4b":"com.whatsapp";
   else if(t.contains("youtube")) pkg="com.google.android.youtube";
   else if(t.contains("instagram")||t.equals("ig")) pkg="com.instagram.android";
   else if(t.contains("drive")) pkg="com.google.android.apps.docs";
   else if(t.contains("downloads")||t.contains("files")||t.contains("file manager")) pkg="com.google.android.documentsui";
   else if(t.contains("termux")) pkg="com.termux";
   else if(t.contains("verdroid")) pkg="com.verdroid";
   else if(t.contains("chrome")) pkg="com.android.chrome";
   Intent launch=pm.getLaunchIntentForPackage(pkg);
   if(launch==null){
    for(android.content.pm.ApplicationInfo app: pm.getInstalledApplications(0)){
     String label=pm.getApplicationLabel(app).toString().toLowerCase();
     if(label.contains(t)){ launch=pm.getLaunchIntentForPackage(app.packageName); if(launch!=null) break; }
    }
   }
   if(launch!=null){ launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); svc.startActivity(launch); return "Opening "+target; }
   return "App not found: "+target;
  }catch(Exception e){ return "Error opening "+target; }
 }
}
