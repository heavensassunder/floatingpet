package com.slime;
import android.accessibilityservice.AccessibilityService; import android.accessibilityservice.GestureDescription;
import android.content.Intent; import android.graphics.Path; import android.graphics.Rect; import android.os.Build; import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent; import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;
public class EyeService extends AccessibilityService {
 public static EyeService instance;
 static final String[] PRIVATE={"bank","mpesa","safaricom","wallet","pay","authenticator","password","keepass","bitwarden","lastpass","1password","vault"};
 public static boolean isPrivate(String pkg){
  if(pkg==null) return false; String p=pkg.toLowerCase();
  for(String k:PRIVATE){ if(p.contains(k)) return true; }
  return false;
 }
 @Override public void onServiceConnected(){ instance=this; }
 @Override public boolean onUnbind(Intent i){ instance=null; return super.onUnbind(i); }
 @Override public void onAccessibilityEvent(AccessibilityEvent e){}
 @Override public void onInterrupt(){}
 boolean privateRoot(AccessibilityNodeInfo root){ CharSequence pk=root.getPackageName(); return pk!=null&&isPrivate(pk.toString()); }

 public String dumpScreen(){
  AccessibilityNodeInfo root=getRootInActiveWindow(); if(root==null) return "home screen";
  if(privateRoot(root)) return "private app (screen hidden for safety)";
  StringBuilder sb=new StringBuilder(); collect(root,sb,0);
  String s=sb.toString(); if(s.length()>1200) s=s.substring(0,1200);
  return s.isEmpty()?"home screen":s;
 }
 void collect(AccessibilityNodeInfo n,StringBuilder sb,int d){
  if(n==null||d>12||sb.length()>1300) return;
  if(n.isPassword()) return;
  CharSequence t=n.getText(); if(t==null||t.length()==0) t=n.getContentDescription();
  if(t!=null&&t.length()>1&&t.length()<60){ String tt=t.toString(); if(!tt.contains("SlimeDroid")) sb.append(tt).append(" | "); }
  for(int i=0;i<n.getChildCount();i++) collect(n.getChild(i),sb,d+1);
 }

 public String tapText(String text){
  AccessibilityNodeInfo root=getRootInActiveWindow(); if(root==null) return "can't see the screen";
  if(privateRoot(root)) return "that's a private app, not touching it";
  List<AccessibilityNodeInfo> list=root.findAccessibilityNodeInfosByText(text);
  if(list==null||list.isEmpty()) return "Not found on screen: "+text;
  for(AccessibilityNodeInfo n:list){
   AccessibilityNodeInfo c=n; int up=0;
   while(c!=null&&!c.isClickable()&&up<6){ c=c.getParent(); up++; }
   if(c!=null&&c.isClickable()){ if(c.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return "Tapped "+text; }
  }
  Rect r=new Rect(); list.get(0).getBoundsInScreen(r);
  if(r.width()>0){ tapAt(r.centerX(),r.centerY()); return "Tapped "+text; }
  return "Not found on screen: "+text;
 }
 public void tapAt(float x,float y){
  if(Build.VERSION.SDK_INT<24) return;
  Path p=new Path(); p.moveTo(x,y);
  GestureDescription.Builder b=new GestureDescription.Builder();
  b.addStroke(new GestureDescription.StrokeDescription(p,0,60));
  dispatchGesture(b.build(),null,null);
 }

 public String typeText(String text){
  AccessibilityNodeInfo root=getRootInActiveWindow(); if(root==null) return "can't see the screen";
  if(privateRoot(root)) return "that's a private app, not typing there";
  AccessibilityNodeInfo f=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
  if(f==null||!f.isEditable()) f=findEditable(root,0);
  if(f==null) return "no text box found";
  if(f.isPassword()) return "that's a password field, not typing there";
  Bundle b=new Bundle(); b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);
  return f.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b)?"Typed it":"couldn't type there";
 }
 AccessibilityNodeInfo findEditable(AccessibilityNodeInfo n,int d){
  if(n==null||d>14) return null;
  if(n.isEditable()&&!n.isPassword()) return n;
  for(int i=0;i<n.getChildCount();i++){ AccessibilityNodeInfo r=findEditable(n.getChild(i),d+1); if(r!=null) return r; }
  return null;
 }

 public String scroll(String dir){
  AccessibilityNodeInfo root=getRootInActiveWindow(); if(root==null) return "can't see the screen";
  if(privateRoot(root)) return "that's a private app, not touching it";
  AccessibilityNodeInfo s=findScrollable(root,0);
  if(s==null) return "nothing to scroll";
  boolean up=dir!=null&&dir.startsWith("up");
  boolean ok=s.performAction(up?AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD:AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
  return ok?("Scrolled "+(up?"up":"down")):"can't scroll more";
 }
 AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo n,int d){
  if(n==null||d>14) return null;
  if(n.isScrollable()) return n;
  for(int i=0;i<n.getChildCount();i++){ AccessibilityNodeInfo r=findScrollable(n.getChild(i),d+1); if(r!=null) return r; }
  return null;
 }
}
