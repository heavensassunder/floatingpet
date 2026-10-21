package com.slime;
import android.service.notification.NotificationListenerService; import android.service.notification.StatusBarNotification;
public class NotificationEyesService extends NotificationListenerService {
 @Override public void onNotificationPosted(StatusBarNotification sbn){
  try{
   String pkg=sbn.getPackageName();
   if(pkg.equals("com.slime")||EyeService.isPrivate(pkg)) return;
   if(sbn.getNotification().extras==null) return;
   CharSequence t=sbn.getNotification().extras.getCharSequence("android.title");
   CharSequence x=sbn.getNotification().extras.getCharSequence("android.text");
   String title=t==null?pkg:t.toString(); String text=x==null?"":x.toString();
   if(!text.isEmpty()){ new MemoryManager(this).saveNotif(title,text); }
  }catch(Exception e){}
 }
}
