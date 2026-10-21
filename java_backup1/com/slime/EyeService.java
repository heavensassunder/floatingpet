package com.slime;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;
public class EyeService extends AccessibilityService {
 public static EyeService instance;
 @Override public void onServiceConnected(){ instance=this; }
 @Override public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent e){}
 @Override public void onInterrupt(){}
 public String dumpScreen(){
  AccessibilityNodeInfo root=getRootInActiveWindow(); if(root==null) return "home screen";
  StringBuilder sb=new StringBuilder(); collect(root,sb,0);
  String s=sb.toString(); if(s.length()>1200) s=s.substring(0,1200);
  return s.isEmpty()?"home screen":s;
 }
 void collect(AccessibilityNodeInfo n, StringBuilder sb, int d){
  if(n==null||d>12) return;
  CharSequence t=n.getText(); if(t!=null&&t.length()>1&&t.length()<60){ String tt=t.toString(); if(!tt.contains("SlimeDroid")) sb.append(tt).append(" | "); }
  for(int i=0;i<n.getChildCount();i++) collect(n.getChild(i),sb,d+1);
 }
}
