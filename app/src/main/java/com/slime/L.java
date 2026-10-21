package com.slime;
import android.util.Log;
// Tiny logcat wrapper so catch blocks stay one-liners while failures stay visible.
final class L {
 static final String TAG="SlimeDroid";
 private L(){}
 static void w(String where,Throwable e){ Log.w(TAG,where,e); }
}
