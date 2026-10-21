package com.slime;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat; import java.util.ArrayList; import java.util.Date; import java.util.Locale;
public class Keys {
 static String day(){ return new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date()); }
 static String hash(String k){ return Integer.toHexString(k.hashCode()); }
 public static ArrayList<String> all(SharedPreferences sp){
  ArrayList<String> r=new ArrayList<String>();
  if(sp==null) return r;
  for(String l:sp.getString("keys","").split("\n")){ l=l.trim(); if(l.length()>=10&&!r.contains(l)) r.add(l); }
  String one=sp.getString("key","").trim();
  if(r.isEmpty()&&one.length()>=10) r.add(one);
  return r;
 }
 public static void setAll(SharedPreferences sp,ArrayList<String> ks){
  StringBuilder sb=new StringBuilder();
  for(String k:ks){ if(sb.length()>0) sb.append("\n"); sb.append(k); }
  SharedPreferences.Editor e=sp.edit();
  e.putString("keys",sb.toString()); e.putString("key",ks.isEmpty()?"":ks.get(0));
  for(String k:new ArrayList<String>(sp.getAll().keySet())){ if(k.startsWith("kd_")) e.remove(k); }
  e.apply();
 }
 // accepts one or many keys (separated by lines or spaces). returns how many were new
 public static int addText(SharedPreferences sp,String text){
  ArrayList<String> ks=all(sp); int n=0;
  for(String tok:text.split("\\s+")){ tok=tok.trim(); if(tok.length()>=20&&!ks.contains(tok)){ ks.add(tok); n++; } }
  if(n>0) setAll(sp,ks);
  return n;
 }
 public static boolean alive(SharedPreferences sp,String k){
  String v=sp.getString("kd_"+hash(k),"");
  return v.isEmpty()||(!v.equals("x")&&!v.equals(day()));
 }
 public static void markDead(SharedPreferences sp,String k,boolean forever){ sp.edit().putString("kd_"+hash(k),forever?"x":day()).apply(); }
 public static boolean anyAlive(SharedPreferences sp,ArrayList<String> ks){ for(String k:ks){ if(alive(sp,k)) return true; } return false; }
 public static String current(SharedPreferences sp,ArrayList<String> ks){
  for(String k:ks){ if(alive(sp,k)) return k; }
  return ks.isEmpty()?"":ks.get(0);
 }
 public static int usedFor(SharedPreferences sp,String k){ return sp.getInt("aik_"+hash(k)+"_"+day(),0); }
 public static String mask(String k){ return k.length()<=12?"****":k.substring(0,6)+"..."+k.substring(k.length()-4); }
 static boolean handle(MainService s,String t,String low){
  low=low.trim();
  if(low.equals("keys")||low.equals("key")||low.equals("key list")){
   ArrayList<String> ks=all(s.sp);
   if(ks.isEmpty()){ s.local("no keys yet. add one on the home screen, or say: key add YOURKEY",false); return true; }
   StringBuilder sb=new StringBuilder("my keys:\n");
   for(int i=0;i<ks.size();i++){ String k=ks.get(i); sb.append(i+1).append(". ").append(mask(k)).append(alive(s.sp,k)?"  ok":"  used up / not working today").append("  (").append(usedFor(s.sp,k)).append(" today)").append(k.equals(current(s.sp,ks))?"  <- using now":"").append("\n"); }
   sb.append("\nkey add KEY | key remove N | key next");
   s.showChat(); s.local(sb.toString(),false); return true;
  }
  if(low.startsWith("key add ")){
   int n=addText(s.sp,t.substring(Math.min(8,t.length())));
   s.local(n>0?"added "+n+" key"+(n>1?"s":"")+". say keys to see them":"that doesn't look like a key (or I already have it)",false); return true;
  }
  if(low.startsWith("key remove ")){
   ArrayList<String> ks=all(s.sp);
   try{ int i=Integer.parseInt(low.substring(11).trim())-1; if(i>=0&&i<ks.size()){ ks.remove(i); setAll(s.sp,ks); s.local("removed it. "+ks.size()+" key"+(ks.size()==1?"":"s")+" left",false); return true; } }catch(Exception e){ L.w("Keys",e); }
   s.local("which one? say keys, then key remove N",false); return true;
  }
  if(low.equals("key next")||low.equals("next key")){
   ArrayList<String> ks=all(s.sp);
   if(ks.size()<2){ s.local("I only have one key",false); return true; }
   String first=ks.remove(0); ks.add(first); setAll(s.sp,ks); s.local("switched. now using "+mask(ks.get(0)),false); return true;
  }
  return false;
 }
}
