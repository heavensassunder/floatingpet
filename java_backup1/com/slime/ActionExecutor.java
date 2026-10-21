package com.slime;
import android.content.Intent; import android.content.pm.PackageManager;
public class ActionExecutor {
 MainService svc; public ActionExecutor(MainService s){ svc=s; }
 public String execute(String target){
  try{
   PackageManager pm=svc.getPackageManager();
   String pkg=target.toLowerCase();
   if(pkg.contains("whatsapp")) pkg="com.whatsapp";
   else if(pkg.contains("drive")) pkg="com.google.android.apps.docs";
   else if(pkg.contains("downloads")||pkg.contains("files")||pkg.contains("file manager")) pkg="com.google.android.documentsui";
   else if(pkg.contains("termux")) pkg="com.termux";
   else if(pkg.contains("verdroid")) pkg="com.verdroid";
   Intent launch=pm.getLaunchIntentForPackage(pkg);
   if(launch==null){
    for(android.content.pm.ApplicationInfo app: pm.getInstalledApplications(0)){
     String label=pm.getApplicationLabel(app).toString().toLowerCase();
     if(label.contains(target.toLowerCase())){ launch=pm.getLaunchIntentForPackage(app.packageName); if(launch!=null) break; }
    }
   }
   if(launch!=null){ launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); svc.startActivity(launch); return "Opening "+target; }
   return "Can't find "+target;
  }catch(Exception e){ return "Error"; }
 }
}
