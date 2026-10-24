package com.slime;
import android.content.Context; import java.io.*; import org.json.*;
public class MemoryManager {
 File chatFile, longFile, notifFile;
 public MemoryManager(Context c){ chatFile=new File(c.getFilesDir(),"chats.json"); longFile=new File(c.getFilesDir(),"long_memory.json"); notifFile=new File(c.getFilesDir(),"notifs.json"); }
 String read(File f){ try{ if(!f.exists()) return ""; BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8")); StringBuilder sb=new StringBuilder(); String l; while((l=br.readLine())!=null) sb.append(l).append("\n"); br.close(); return sb.toString(); }catch(Exception e){ return ""; } }
 void write(File f,String s){ try{ OutputStreamWriter w=new OutputStreamWriter(new FileOutputStream(f),"UTF-8"); w.write(s); w.close(); }catch(Exception e){ L.w("MemoryManager",e); } }
 JSONArray loadArr(File f){ try{ String s=read(f).trim(); if(s.isEmpty()) return new JSONArray(); return new JSONArray(s); }catch(Exception e){ return new JSONArray(); } }
 JSONArray tail(JSONArray a,int max){ if(a.length()<=max) return a; JSONArray n=new JSONArray(); try{ for(int i=a.length()-max;i<a.length();i++) n.put(a.get(i)); }catch(Exception e){ L.w("MemoryManager",e); } return n; }
 String cut(String s,int max){ if(s==null) return ""; s=s.replace("\n"," "); return s.length()>max?s.substring(0,max):s; }
 public void saveChat(String user,String slime){ try{ JSONArray arr=loadChats(); JSONObject o=new JSONObject(); o.put("u",user); o.put("s",slime); o.put("t",System.currentTimeMillis()); arr.put(o); write(chatFile,tail(arr,120).toString()); }catch(Exception e){ L.w("MemoryManager",e); } }
 public JSONArray loadChats(){ return loadArr(chatFile); }
 public JSONArray loadNotifs(){ return loadArr(notifFile); }
 int merge(JSONArray extra,File f,int cap){
  try{
   if(extra==null||extra.length()==0) return 0;
   JSONArray cur=loadArr(f); int added=0;
   for(int i=0;i<extra.length();i++){ cur.put(extra.get(i)); added++; }
   write(f,tail(cur,cap).toString()); return added;
  }catch(Exception e){ L.w("MemoryManager",e); return 0; }
 }
 public int mergeChats(JSONArray extra){ return merge(extra,chatFile,120); }
 public int mergeNotifs(JSONArray extra){ return merge(extra,notifFile,30); }
 public String getRecentContext(int n){ try{ JSONArray arr=loadChats(); StringBuilder sb=new StringBuilder(); for(int i=Math.max(0,arr.length()-n);i<arr.length();i++){ JSONObject o=arr.getJSONObject(i); sb.append("U:").append(cut(o.optString("u"),120)).append(" | S:").append(cut(o.optString("s"),120)).append("\n"); } return sb.toString(); }catch(Exception e){ return ""; } }
 public String getLongContext(){ return read(longFile).trim(); }
 public void saveNotif(String title,String text){ try{ JSONArray arr=loadArr(notifFile); JSONObject o=new JSONObject(); o.put("title",cut(title,40)); o.put("text",cut(text,100)); o.put("t",System.currentTimeMillis()); arr.put(o); write(notifFile,tail(arr,30).toString()); }catch(Exception e){ L.w("MemoryManager",e); } }
 public String getNotifContext(){ try{ JSONArray arr=loadArr(notifFile); StringBuilder sb=new StringBuilder(); for(int i=Math.max(0,arr.length()-8);i<arr.length();i++){ JSONObject o=arr.getJSONObject(i); sb.append(o.optString("title")).append(": ").append(o.optString("text")).append("\n"); } return sb.toString(); }catch(Exception e){ return ""; } }
}
