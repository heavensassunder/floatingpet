package com.slime;
import android.content.Context; import java.io.*; import org.json.*;
public class MemoryManager {
 Context ctx; File chatFile, longFile, notifFile;
 public MemoryManager(Context c){ ctx=c; chatFile=new File(c.getFilesDir(),"chats.json"); longFile=new File(c.getFilesDir(),"long_memory.json"); notifFile=new File(c.getFilesDir(),"notifs.json"); }
 String read(File f){ try{ if(!f.exists()) return ""; BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8")); StringBuilder sb=new StringBuilder(); String l; while((l=br.readLine())!=null) sb.append(l).append("\n"); br.close(); return sb.toString(); }catch(Exception e){ return ""; } }
 void write(File f,String s){ try{ OutputStreamWriter w=new OutputStreamWriter(new FileOutputStream(f),"UTF-8"); w.write(s); w.close(); }catch(Exception e){} }
 JSONArray loadArr(File f){ try{ String s=read(f).trim(); if(s.isEmpty()) return new JSONArray(); return new JSONArray(s); }catch(Exception e){ return new JSONArray(); } }
 JSONArray tail(JSONArray a,int max){ if(a.length()<=max) return a; JSONArray n=new JSONArray(); try{ for(int i=a.length()-max;i<a.length();i++) n.put(a.get(i)); }catch(Exception e){} return n; }
 String cut(String s,int max){ if(s==null) return ""; s=s.replace("\n"," "); return s.length()>max?s.substring(0,max):s; }
 public void saveChat(String user,String slime){ try{ JSONArray arr=loadChats(); JSONObject o=new JSONObject(); o.put("u",user); o.put("s",slime); o.put("t",System.currentTimeMillis()); arr.put(o); write(chatFile,tail(arr,120).toString()); }catch(Exception e){} }
 public JSONArray loadChats(){ return loadArr(chatFile); }
 public String getRecentContext(int n){ try{ JSONArray arr=loadChats(); StringBuilder sb=new StringBuilder(); for(int i=Math.max(0,arr.length()-n);i<arr.length();i++){ JSONObject o=arr.getJSONObject(i); sb.append("U:").append(cut(o.optString("u"),120)).append(" | S:").append(cut(o.optString("s"),120)).append("\n"); } return sb.toString(); }catch(Exception e){ return ""; } }
 public String getLongContext(){ return read(longFile).trim(); }
 public void saveLongFact(String fact){
  if(fact==null) return; fact=cut(fact.trim(),140); if(fact.isEmpty()) return;
  String cur=getLongContext();
  if(cur.toLowerCase().contains(fact.toLowerCase())) return;
  String n=cur.isEmpty()?("- "+fact):(cur+"\n- "+fact);
  if(n.length()>1500){ n=n.substring(n.length()-1500); int nl=n.indexOf("\n- "); if(nl>=0) n=n.substring(nl+1); }
  write(longFile,n);
 }
 public void saveNotif(String title,String text){ try{ JSONArray arr=loadArr(notifFile); JSONObject o=new JSONObject(); o.put("title",cut(title,40)); o.put("text",cut(text,100)); o.put("t",System.currentTimeMillis()); arr.put(o); write(notifFile,tail(arr,30).toString()); }catch(Exception e){} }
 public String getNotifContext(){ try{ JSONArray arr=loadArr(notifFile); StringBuilder sb=new StringBuilder(); for(int i=Math.max(0,arr.length()-8);i<arr.length();i++){ JSONObject o=arr.getJSONObject(i); sb.append(o.optString("title")).append(": ").append(o.optString("text")).append("\n"); } return sb.toString(); }catch(Exception e){ return ""; } }
}
