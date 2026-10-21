package com.slime;
import android.content.BroadcastReceiver; import android.content.Context; import android.content.Intent;
public class BootReceiver extends BroadcastReceiver { @Override public void onReceive(Context c, Intent i){ try{ if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())){ c.startService(new Intent(c, MainService.class)); } }catch(Exception e){ L.w("BootReceiver",e); } } }
