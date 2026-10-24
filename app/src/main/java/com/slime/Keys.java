package com.slime;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat; import java.util.ArrayList; import java.util.Date; import java.util.Locale;
// API keys for both providers, namespaced: "or" (OpenRouter, legacy pref names) and "g" (Google AI Studio).
// Each pool rotates keys and parks dead/quota-spent ones until the next day.
public class Keys {
 static final String OR="or", G="g";
 static String day(){ return new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date()); }
 static String hash(String k){ return Integer.toHexString(k.hashCode()); }
 static String storeKey(String ns){ return G.equals(ns)?"gkeys":"keys"; }
 static String singleKey(String ns){ return G.equals(ns)?"gkey":"key"; }
 static String deadPrefix(String ns){ return G.equals(ns)?"kd_g_":"kd_"; }
 static String usedPrefix(String ns){ return G.equals(ns)?"gaik_":"aik_"; }
 static String label(String ns){ return G.equals(ns)?"Gemini":"OpenRouter"; }

 public static ArrayList<String> all(SharedPreferences sp){ return all(sp,OR); }
 public static ArrayList<String> all(SharedPreferences sp,String ns){
  ArrayList<String> r=new ArrayList<String>();
  if(sp==null) return r;
  for(String l:sp.getString(storeKey(ns),"").split("\n")){ l=l.trim(); if(l.length()>=10&&!r.contains(l)) r.add(l); }
  String one=sp.getString(singleKey(ns),"").trim();
  if(r.isEmpty()&&one.length()>=10) r.add(one);
  return r;
 }
 public static void setAll(SharedPreferences sp,ArrayList<String> ks){ setAll(sp,OR,ks); }
 public static void setAll(SharedPreferences sp,String ns,ArrayList<String> ks){
  StringBuilder sb=new StringBuilder();
  for(String k:ks){ if(sb.length()>0) sb.append("\n"); sb.append(k); }
  SharedPreferences.Editor e=sp.edit();
  e.putString(storeKey(ns),sb.toString()).putString(singleKey(ns),ks.isEmpty()?"":ks.get(0));
  String dp=deadPrefix(ns);
  for(String k:new ArrayList<String>(sp.getAll().keySet())){ if(k.startsWith(dp)) e.remove(k); }
  e.apply();
 }
 // accepts one or many keys (separated by lines or spaces). returns how many were new
 public static int addText(SharedPreferences sp,String text){ return addText(sp,OR,text); }
 public static int addText(SharedPreferences sp,String ns,String text){
  ArrayList<String> ks=all(sp,ns); int n=0;
  for(String tok:text.split("\\s+")){ tok=tok.trim(); if(tok.length()>=20&&!ks.contains(tok)){ ks.add(tok); n++; } }
  if(n>0) setAll(sp,ns,ks);
  return n;
 }
 public static boolean alive(SharedPreferences sp,String k){ return alive(sp,OR,k); }
 public static boolean alive(SharedPreferences sp,String ns,String k){
  String v=sp.getString(deadPrefix(ns)+hash(k),"");
  return v.isEmpty()||(!v.equals("x")&&!v.equals(day()));
 }
 public static void markDead(SharedPreferences sp,String k,boolean forever){ markDead(sp,OR,k,forever); }
 public static void markDead(SharedPreferences sp,String ns,String k,boolean forever){ sp.edit().putString(deadPrefix(ns)+hash(k),forever?"x":day()).apply(); }
 public static boolean anyAlive(SharedPreferences sp,ArrayList<String> ks){ return anyAlive(sp,OR,ks); }
 public static boolean anyAlive(SharedPreferences sp,String ns,ArrayList<String> ks){ for(String k:ks){ if(alive(sp,ns,k)) return true; } return false; }
 public static String current(SharedPreferences sp,ArrayList<String> ks){ return current(sp,OR,ks); }
 public static String current(SharedPreferences sp,String ns,ArrayList<String> ks){
  for(String k:ks){ if(alive(sp,ns,k)) return k; }
  return ks.isEmpty()?"":ks.get(0);
 }
 public static int usedFor(SharedPreferences sp,String k){ return usedFor(sp,OR,k); }
 public static int usedFor(SharedPreferences sp,String ns,String k){ return sp.getInt(usedPrefix(ns)+hash(k)+"_"+day(),0); }
 public static String mask(String k){ return k.length()<=12?"****":k.substring(0,6)+"..."+k.substring(k.length()-4); }

 static int resetDead(MainService s,String ns){
  String dp=deadPrefix(ns); int n=0;
  SharedPreferences.Editor e=s.sp.edit();
  for(String k:new ArrayList<String>(s.sp.getAll().keySet())){ if(k.startsWith(dp)){ e.remove(k); n++; } }
  e.apply(); return n;
 }
 static boolean handle(MainService s,String t,String low){
  low=low.trim();
  // ---------- Gemini (Google AI Studio) keys ----------
  if(low.startsWith("gemini key")||low.equals("gemini keys")){
   if(low.equals("gemini keys")||low.equals("gemini key")||low.equals("gemini key list")){ list(s,G); return true; }
   if(low.startsWith("gemini key add ")){
    int n=addText(s.sp,G,t.substring(t.toLowerCase(Locale.US).indexOf("add ")+4));
    s.local(n>0?"added "+n+" Gemini key"+(n>1?"s":"")+". say gemini keys to see them":"that doesn't look like a key (or I already have it)",false); return true;
   }
   if(low.startsWith("gemini key remove ")){ remove(s,G,low.substring(18).trim()); return true; }
   if(low.equals("gemini key next")){ rotate(s,G); return true; }
   if(low.equals("gemini key reset")){ int n=resetDead(s,G); s.local("cleared "+n+" parked-key mark"+(n==1?"":"s")+" - all Gemini keys are active again",false); return true; }
  }
  // ---------- OpenRouter keys (legacy commands) ----------
  if(low.equals("keys")||low.equals("key")||low.equals("key list")){ list(s,OR); return true; }
  if(low.startsWith("key add ")){
   int n=addText(s.sp,OR,t.substring(Math.min(8,t.length())));
   s.local(n>0?"added "+n+" key"+(n>1?"s":"")+". say keys to see them":"that doesn't look like a key (or I already have it)",false); return true;
  }
  if(low.startsWith("key remove ")){ remove(s,OR,low.substring(11).trim()); return true; }
  if(low.equals("key next")||low.equals("next key")){ rotate(s,OR); return true; }
  if(low.equals("key reset")){ int n=resetDead(s,OR); s.local("cleared "+n+" parked-key mark"+(n==1?"":"s")+" - all OpenRouter keys are active again",false); return true; }
  return false;
 }
 static void list(MainService s,String ns){
  ArrayList<String> ks=all(s.sp,ns);
  if(ks.isEmpty()){ s.local("no "+label(ns)+" keys yet. add one on the home screen, or say: "+(G.equals(ns)?"gemini key add YOURKEY":"key add YOURKEY"),false); return; }
  StringBuilder sb=new StringBuilder(label(ns)+" keys:\n");
  for(int i=0;i<ks.size();i++){ String k=ks.get(i); sb.append(i+1).append(". ").append(mask(k)).append(alive(s.sp,ns,k)?"  ok":"  used up / not working today").append("  (").append(usedFor(s.sp,ns,k)).append(" today)").append(k.equals(current(s.sp,ns,ks))?"  <- using now":"").append("\n"); }
  sb.append("\n").append(G.equals(ns)?"gemini key add KEY | gemini key remove N | gemini key next":"key add KEY | key remove N | key next");
  if(ns.equals(OR)){ int g=all(s.sp,G).size(); if(g>0) sb.append("\n\nyou also have ").append(g).append(" Gemini key").append(g>1?"s":"").append(" - say gemini keys"); }
  s.showChat(); s.local(sb.toString(),false);
 }
 static void remove(MainService s,String ns,String num){
  ArrayList<String> ks=all(s.sp,ns);
  try{ int i=Integer.parseInt(num)-1; if(i>=0&&i<ks.size()){ ks.remove(i); setAll(s.sp,ns,ks); s.local("removed it. "+ks.size()+" key"+(ks.size()==1?"":"s")+" left",false); return; } }catch(Exception e){ L.w("Keys",e); }
  s.local("which one? say "+(G.equals(ns)?"gemini keys":"keys")+", then "+(G.equals(ns)?"gemini key remove N":"key remove N"),false);
 }
 static void rotate(MainService s,String ns){
  ArrayList<String> ks=all(s.sp,ns);
  if(ks.size()<2){ s.local("I only have one "+label(ns)+" key",false); return; }
  String first=ks.remove(0); ks.add(first); setAll(s.sp,ns,ks); s.local("switched. now using "+mask(ks.get(0)),false);
 }
}
