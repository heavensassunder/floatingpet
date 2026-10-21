package com.slime;
import android.app.Notification; import android.service.notification.NotificationListenerService; import android.service.notification.StatusBarNotification;
public class NotificationEyesService extends NotificationListenerService {
 static final String[] SECRET={"otp","verification code","one-time","one time","passcode","password","m-pesa","mpesa","confirmed. ksh"};
 @Override public void onNotificationPosted(StatusBarNotification sbn){
  try{
   String pkg=sbn.getPackageName();
   if(pkg==null||pkg.equals("com.slime")||EyeService.isPrivate(pkg)) return;
   Notification n=sbn.getNotification();
   if(n==null||n.extras==null) return;
   if((n.flags&Notification.FLAG_ONGOING_EVENT)!=0) return;
   if((n.flags&Notification.FLAG_GROUP_SUMMARY)!=0) return;
   CharSequence t=n.extras.getCharSequence("android.title");
   CharSequence x=n.extras.getCharSequence("android.text");
   String title=t==null?pkg:t.toString(); String text=x==null?"":x.toString();
   if(text.isEmpty()) return;
   String low=(title+" "+text).toLowerCase();
   for(String s:SECRET){ if(low.contains(s)) return; }
   new MemoryManager(this).saveNotif(title,text);
   MainService m=MainService.instance; if(m!=null) m.onNotif(pkg,title,text);
  }catch(Exception e){}
 }
}
