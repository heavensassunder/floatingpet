package com.slime;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
// Fires scheduled slime work (reminders, morning briefing) by waking MainService with the action attached.
public class AlarmReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
  try{
   Intent svc=new Intent(c,MainService.class);
   if(i.getAction()!=null) svc.setAction(i.getAction());
   if(i.getExtras()!=null) svc.putExtras(i.getExtras());
   c.startService(svc);
  }catch(Exception e){ L.w("AlarmReceiver",e); }
 }
}
