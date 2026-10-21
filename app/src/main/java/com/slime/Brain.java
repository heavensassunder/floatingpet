package com.slime;
import java.io.ByteArrayOutputStream; import java.io.File; import java.io.FileInputStream; import java.io.FileOutputStream;
import java.util.ArrayList; import java.util.Collections; import java.util.Comparator; import java.util.HashSet; import java.util.Locale;
import java.util.regex.Matcher; import java.util.regex.Pattern;
import org.json.JSONArray; import org.json.JSONObject;
// One memory per entry: what you said, a category, when, and how often. Recall is by relevance, not a dump.
public class Brain {
 public static final String PROMPT="\nMEMORY TAGS: if I tell you something lasting about myself or my life in this message (name, school, projects, people, preferences, plans), end your reply with up to 2 tags like [[mem:studies electrical engineering | school]]. Categories: identity, people, school, work, projects, preferences, plans, life. Only things I actually said, never guesses, never passwords, codes or numbers. If nothing lasting was said, add no tag.";
 static final String[] CATS={"identity","people","school","work","projects","preferences","plans","life"};
 static final String[] STOP={"the","and","for","that","this","with","you","are","was","has","have","his","her","their","about","from","like","into","your","very","just","not","but"};
 static final Pattern BLOCK=Pattern.compile("(?i)(password|passcode|\\bpin\\b|\\botp\\b|\\bcvv\\b|api[ _-]?key|secret key|sk-[a-z0-9]{8,}|\\b\\d{9,}\\b|[\\w.+-]+@[\\w-]+\\.[\\w.]+|suicid|self[- ]?harm|kill myself|\\babus(e|ed)\\b|\\brape|arrested|criminal record|immigration|asylum)");
 static class E{ String id,t,c,prev; long ts,upd; int imp,n; double sc; }
 final MainService s; final File f; final ArrayList<E> list=new ArrayList<E>(); ArrayList<String> last=new ArrayList<String>();
 public Brain(MainService s){
  this.s=s; f=new File(s.getFilesDir(),"brain.json"); load();
  if(!s.sp.getBoolean("brainImported",false)) importOld();
 }
 // ---------- storage ----------
 void load(){
  try{
   if(!f.exists()) return;
   FileInputStream in=new FileInputStream(f); ByteArrayOutputStream bo=new ByteArrayOutputStream(); byte[] b=new byte[4096]; int n;
   while((n=in.read(b))>0) bo.write(b,0,n); in.close();
   JSONArray a=new JSONArray(bo.toString("UTF-8"));
   for(int i=0;i<a.length();i++){
    JSONObject o=a.getJSONObject(i); E e=new E();
    e.id=o.optString("id"); e.t=o.optString("t"); e.c=o.optString("c","life"); e.prev=o.optString("prev","");
    e.ts=o.optLong("ts"); e.upd=o.optLong("upd"); e.imp=o.optInt("imp",2); e.n=o.optInt("n",1);
    if(e.t.length()>0) list.add(e);
   }
  }catch(Exception e){}
 }
 void save(){
  try{
   JSONArray a=new JSONArray();
   for(E e:list){ JSONObject o=new JSONObject(); o.put("id",e.id); o.put("t",e.t); o.put("c",e.c); o.put("prev",e.prev); o.put("ts",e.ts); o.put("upd",e.upd); o.put("imp",e.imp); o.put("n",e.n); a.put(o); }
   FileOutputStream out=new FileOutputStream(f); out.write(a.toString().getBytes("UTF-8")); out.close();
  }catch(Exception e){}
 }
 void importOld(){
  try{
   String old=s.mem.getLongContext();
   if(old!=null){ for(String line:old.split("\n")){ line=line.replaceFirst("^\\[[^\\]]{1,12}\\]\\s*",""); if(line.trim().length()>=6) add(line,"life",1,false); } }
  }catch(Exception e){}
  s.sp.edit().putBoolean("brainImported",true).apply();
 }
 // ---------- helpers ----------
 static HashSet<String> toks(String x){
  HashSet<String> r=new HashSet<String>();
  for(String w:x.toLowerCase(Locale.US).split("[^a-z0-9]+")){
   if(w.length()<3) continue;
   boolean st=false; for(String sw:STOP){ if(sw.equals(w)){ st=true; break; } }
   if(!st) r.add(w);
  }
  return r;
 }
 static double sim(String a,String b){
  HashSet<String> x=toks(a), y=toks(b);
  if(x.isEmpty()||y.isEmpty()) return a.equalsIgnoreCase(b)?1:0;
  int inter=0; for(String w:x){ if(y.contains(w)) inter++; }
  return inter/(double)(x.size()+y.size()-inter);
 }
 static boolean blocked(String x){ return BLOCK.matcher(x).find(); }
 static String okCat(String c){
  if(c==null) return "life"; c=c.trim().toLowerCase(Locale.US);
  for(String k:CATS){ if(k.equals(c)) return c; }
  return "life";
 }
 static String key(String t){
  String l=t.toLowerCase(Locale.US);
  if(l.startsWith("name is")||l.startsWith("likes to be called")) return "name";
  if(l.startsWith("lives in")) return "lives";
  if(l.startsWith("is from")) return "from";
  if(l.startsWith("birthday")) return "birthday";
  return null;
 }
 String newId(){
  String id;
  do{ id=Integer.toString((int)(Math.random()*46656),36); }while(has(id));
  return id;
 }
 boolean has(String id){ for(E e:list){ if(e.id.equals(id)) return true; } return false; }
 void prune(){
  while(list.size()>280){
   E worst=null; double ws=1e9;
   for(E e:list){ double v=e.imp*10+e.n+(e.upd/86400000.0)/1000.0; if(v<ws){ ws=v; worst=e; } }
   if(worst==null) break; list.remove(worst);
  }
 }
 // ---------- adding ----------
 public boolean add(String fact,String cat,int imp,boolean explicit){
  if(!explicit&&!s.sp.getBoolean("brainOn",true)) return false;
  fact=fact==null?"":fact.trim().replaceAll("\\s+"," ").replaceAll("[.!?,;:]+$","");
  if(fact.length()<4) return false;
  if(fact.length()>180) fact=fact.substring(0,180);
  if(blocked(fact)) return false;
  cat=okCat(cat);
  long now=System.currentTimeMillis();
  E best=null; double bs=0;
  String k=key(fact);
  if(k!=null){ for(E e:list){ if(k.equals(key(e.t))){ best=e; bs=1; break; } } }
  if(best==null){ for(E e:list){ double v=sim(e.t,fact); if(v>bs){ bs=v; best=e; } } }
  if(best!=null&&bs>=0.55){
   if(!best.t.equalsIgnoreCase(fact)){ best.prev=best.t; best.t=fact; }
   best.upd=now; best.n++; best.imp=Math.max(best.imp,imp); if(!cat.equals("life")) best.c=cat;
  } else {
   E e=new E(); e.id=newId(); e.t=fact; e.c=cat; e.prev=""; e.ts=now; e.upd=now; e.imp=imp; e.n=1;
   list.add(e); if(list.size()>300) prune();
  }
  save(); return true;
 }
 static final Pattern[] PAT={
  Pattern.compile("(?i)\\bmy name is ([A-Za-z][\\w'-]{1,20})"),
  Pattern.compile("(?i)\\bcall me ([A-Za-z][\\w'-]{1,20})"),
  Pattern.compile("(?i)\\bi(?:'m| am) from ([^.,!?]{2,40})"),
  Pattern.compile("(?i)\\bi live in ([^.,!?]{2,40})"),
  Pattern.compile("(?i)\\bi (?:study|am studying|'m studying) ([^.,!?]{2,50})"),
  Pattern.compile("(?i)\\bi work (?:at|as|in) ([^.,!?]{2,40})"),
  Pattern.compile("(?i)\\bi (?:really )?(like|love|enjoy|hate|prefer) ([^.,!?]{2,50})"),
  Pattern.compile("(?i)\\bmy favou?rite ([a-z ]{2,20}) is ([^.,!?]{2,40})"),
  Pattern.compile("(?i)\\bmy birthday is ([^.,!?]{2,30})"),
  Pattern.compile("(?i)\\bmy (exam|test|assignment|project|deadline|presentation)(?: is| for)? ([^.,!?]{2,60})"),
  Pattern.compile("(?i)\\bi(?:'m| am) (?:working on|building|making) ([^.,!?]{2,60})"),
  Pattern.compile("(?i)\\bmy (friend|brother|sister|mom|mum|dad|girlfriend|boyfriend|roommate|teacher|lecturer)(?:'s name)? (?:is|called) ([A-Z][\\w'-]{1,20})")
 };
 // quick, free extraction from what you typed (no AI request needed)
 public void observe(String text){
  try{
   if(text==null) return; text=text.trim();
   if(text.length()<8||text.endsWith("?")) return;
   for(int i=0;i<PAT.length;i++){
    Matcher m=PAT[i].matcher(text);
    if(!m.find()) continue;
    switch(i){
     case 0: add("name is "+m.group(1),"identity",3,false); break;
     case 1: add("likes to be called "+m.group(1),"identity",3,false); break;
     case 2: add("is from "+m.group(1),"identity",2,false); break;
     case 3: add("lives in "+m.group(1),"identity",2,false); break;
     case 4: add("studies "+m.group(1),"school",3,false); break;
     case 5: add("works "+text.substring(m.start()+8,m.end()),"work",2,false); break;
     case 6: add(m.group(1).toLowerCase(Locale.US).replace("like","likes").replace("love","loves").replace("enjoy","enjoys").replace("hate","hates").replace("prefer","prefers")+" "+m.group(2),"preferences",2,false); break;
     case 7: add("favorite "+m.group(1)+" is "+m.group(2),"preferences",2,false); break;
     case 8: add("birthday is "+m.group(1),"identity",3,false); break;
     case 9: add("has a "+m.group(1)+" "+m.group(2),"plans",2,false); break;
     case 10: add("is working on "+m.group(1),"projects",2,false); break;
     case 11: add("has a "+m.group(1).toLowerCase(Locale.US)+" named "+m.group(2),"people",2,false); break;
    }
   }
  }catch(Exception e){}
 }
 // the AI can add memories for free by ending its reply with [[mem:fact | category]]
 public String fromTags(String reply){
  try{
   String out=reply; int guard=0;
   while(guard++<4){
    int a=out.indexOf("[[mem:"); if(a<0) break;
    int b=out.indexOf("]]",a); if(b<0) break;
    String body=out.substring(a+6,b); String fact=body, cat="";
    int bar=body.lastIndexOf('|'); if(bar>0){ fact=body.substring(0,bar); cat=body.substring(bar+1); }
    add(fact,cat,2,false);
    out=(out.substring(0,a)+out.substring(b+2)).trim();
   }
   return out;
  }catch(Exception e){ return reply; }
 }
 // ---------- recall ----------
 public String context(String q){
  if(list.isEmpty()) return "(nothing remembered yet)";
  HashSet<String> qt=toks(q==null?"":q); long now=System.currentTimeMillis();
  ArrayList<E> sorted=new ArrayList<E>(list);
  for(E e:sorted){
   double sc=e.imp*1.0+Math.min(e.n,5)*0.3;
   if(now-e.upd<7L*86400000L) sc+=1;
   if(e.c.equals("identity")&&e.imp>=3) sc+=2;
   HashSet<String> et=toks(e.t); for(String w:qt){ if(et.contains(w)) sc+=3; }
   e.sc=sc;
  }
  Collections.sort(sorted,new Comparator<E>(){ public int compare(E a,E b){ return Double.compare(b.sc,a.sc); } });
  StringBuilder sb=new StringBuilder("(things the user told me. Use only when it fits the moment, never recite the list)\n");
  int n=0;
  for(E e:sorted){ if(n>=12||sb.length()>1400) break; sb.append("- ").append(e.t).append(" [").append(e.c).append("]\n"); n++; }
  return sb.toString();
 }
 public void showSummary(){
  if(list.isEmpty()){ s.showChat(); s.local("nothing yet. tell me about you, or say: remember ...",false); return; }
  StringBuilder sb=new StringBuilder("here's what I know about you:\n");
  for(String c:CATS){
   StringBuilder line=new StringBuilder(); int k=0;
   for(E e:list){ if(e.c.equals(c)){ if(k>0) line.append("; "); line.append(e.t); k++; } }
   if(k>0) sb.append("\n").append(c).append(": ").append(line);
  }
  sb.append("\n\nsay 'memories' to see them one by one, 'forget N' to remove one.");
  s.showChat(); s.local(sb.toString(),false);
 }
 void buildList(ArrayList<E> items){
  last=new ArrayList<String>();
  for(E e:items) last.add(e.id);
 }
 ArrayList<E> ordered(){
  ArrayList<E> r=new ArrayList<E>();
  for(String c:CATS){ for(E e:list){ if(e.c.equals(c)) r.add(e); } }
  return r;
 }
 void listAll(){
  if(list.isEmpty()){ s.showChat(); s.local("my memory of you is empty. tell me things, or say: remember ...",false); return; }
  ArrayList<E> o=ordered(); buildList(o);
  StringBuilder sb=new StringBuilder("what I remember (" + o.size() + "):\n");
  for(int i=0;i<o.size();i++){ E e=o.get(i); sb.append(i+1).append(". ").append(e.t).append("  [").append(e.c).append("]\n"); }
  sb.append("\nremove one with: forget N");
  s.showChat(); s.local(sb.toString(),false);
 }
 void remove(E e){ list.remove(e); save(); }
 void forget(String arg){
  arg=arg.trim();
  if(arg.isEmpty()){ s.local("forget what? try: memories, then forget 3",false); return; }
  if(arg.matches("\\d{1,3}")){
   if(last.isEmpty()) buildList(ordered());
   int i=Integer.parseInt(arg)-1;
   if(i>=0&&i<last.size()){
    for(E e:list){ if(e.id.equals(last.get(i))){ String t=e.t; remove(e); last.remove(i); s.local("ok, forgot: "+t,false); return; } }
   }
   s.local("I don't have a number "+arg+". say memories to see the list",false); return;
  }
  String low=arg.toLowerCase(Locale.US);
  ArrayList<E> hits=new ArrayList<E>(); HashSet<String> at=toks(arg);
  for(E e:list){
   boolean ok=e.id.equals(low)||e.t.toLowerCase(Locale.US).contains(low);
   if(!ok){ HashSet<String> et=toks(e.t); int c=0; for(String w:at){ if(et.contains(w)) c++; } ok=!at.isEmpty()&&c>=Math.max(1,at.size()/2); }
   if(ok) hits.add(e);
  }
  if(hits.isEmpty()){ s.local("I don't have anything like that",false); return; }
  if(hits.size()==1){ E e=hits.get(0); String t=e.t; remove(e); s.local("ok, forgot: "+t,false); return; }
  buildList(hits);
  StringBuilder sb=new StringBuilder("which one?\n");
  for(int i=0;i<hits.size();i++) sb.append(i+1).append(". ").append(hits.get(i).t).append("\n");
  sb.append("\nsay forget N");
  s.showChat(); s.local(sb.toString(),false);
 }
 public void clearAll(){ list.clear(); last=new ArrayList<String>(); save(); }
 public boolean handle(String t,String low){
  low=low.trim();
  if(low.equals("memories")||low.equals("memory")||low.equals("memory list")||low.equals("list memories")){ listAll(); return true; }
  if(low.equals("memory stats")){ s.local(list.size()+" memories. saving is "+(s.sp.getBoolean("brainOn",true)?"on":"off"),false); return true; }
  Boolean b=Phone.onoff(low,"memory ");
  if(b!=null){ s.sp.edit().putBoolean("brainOn",b.booleanValue()).apply(); s.local("auto-saving memories "+(b.booleanValue()?"on":"off")+" (you can still say remember ...)",false); return true; }
  if(low.startsWith("remember ")){
   String x=t.substring(Math.min(9,t.length())).trim().replaceFirst("(?i)^that\\s+","");
   if(add(x,"life",3,true)) s.local("got it, I'll remember that",true);
   else s.local("I can't keep that one (passwords, codes, private numbers and some sensitive things stay out)",false);
   return true;
  }
  if(low.startsWith("forget ")&&!low.equals("forget all")&&!low.equals("forget everything")){ forget(t.substring(7)); return true; }
  return false;
 }
}
