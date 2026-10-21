package com.slime;
import android.service.notification.NotificationListenerService; import android.service.notification.StatusBarNotification;
public class NotificationEyesService extends NotificationListenerService {
 @Override public void onNotificationPosted(StatusBarNotification sbn){
  try{ String pkg=sbn.getPackageName(); if(pkg.equals("com.slime")) return; String title=""; String text=""; if(sbn.getNotification().extras!=null){ title=sbn.getNotification().extras.getString("android.title",""); text=sbn.getNotification().extras.getString("android.text",""); } if(text==null) text=""; if(title==null) title=pkg; if(!text.isEmpty()){ MemoryManager mem=new MemoryManager(this); mem.saveNotif(title,text); } }catch(Exception e){}
 }
}
