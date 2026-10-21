package com.slime;
import java.util.regex.Pattern;
// Turns LaTeX into readable Unicode (no WebView, no internet): fractions, roots, powers, subscripts, Greek, symbols.
public class MathText {
 static final String[][] SYM={
  {"alpha","\u03b1"},{"beta","\u03b2"},{"gamma","\u03b3"},{"delta","\u03b4"},{"epsilon","\u03b5"},{"varepsilon","\u03b5"},{"zeta","\u03b6"},{"eta","\u03b7"},{"theta","\u03b8"},{"vartheta","\u03b8"},
  {"iota","\u03b9"},{"kappa","\u03ba"},{"lambda","\u03bb"},{"mu","\u03bc"},{"nu","\u03bd"},{"xi","\u03be"},{"pi","\u03c0"},{"rho","\u03c1"},{"sigma","\u03c3"},{"tau","\u03c4"},
  {"upsilon","\u03c5"},{"phi","\u03c6"},{"varphi","\u03c6"},{"chi","\u03c7"},{"psi","\u03c8"},{"omega","\u03c9"},
  {"Gamma","\u0393"},{"Delta","\u0394"},{"Theta","\u0398"},{"Lambda","\u039b"},{"Xi","\u039e"},{"Pi","\u03a0"},{"Sigma","\u03a3"},{"Phi","\u03a6"},{"Psi","\u03a8"},{"Omega","\u03a9"},
  {"nabla","\u2207"},{"partial","\u2202"},{"infty","\u221e"},{"sum","\u2211"},{"prod","\u220f"},{"int","\u222b"},{"iint","\u222c"},{"oint","\u222e"},
  {"cdot","\u00b7"},{"times","\u00d7"},{"div","\u00f7"},{"pm","\u00b1"},{"mp","\u2213"},{"ast","*"},
  {"leq","\u2264"},{"le","\u2264"},{"geq","\u2265"},{"ge","\u2265"},{"neq","\u2260"},{"ne","\u2260"},{"approx","\u2248"},{"sim","\u223c"},{"equiv","\u2261"},{"propto","\u221d"},{"ll","\u226a"},{"gg","\u226b"},
  {"to","\u2192"},{"rightarrow","\u2192"},{"leftarrow","\u2190"},{"Rightarrow","\u21d2"},{"Leftarrow","\u21d0"},{"Leftrightarrow","\u21d4"},{"leftrightarrow","\u2194"},{"implies","\u21d2"},{"iff","\u21d4"},{"mapsto","\u21a6"},
  {"in","\u2208"},{"notin","\u2209"},{"subset","\u2282"},{"subseteq","\u2286"},{"cup","\u222a"},{"cap","\u2229"},{"forall","\u2200"},{"exists","\u2203"},{"emptyset","\u2205"},
  {"hbar","\u0127"},{"ell","\u2113"},{"ldots","\u2026"},{"dots","\u2026"},{"cdots","\u22ef"},{"degree","\u00b0"},{"angle","\u2220"},{"perp","\u22a5"},{"parallel","\u2225"},{"therefore","\u2234"},{"because","\u2235"},
  {"sin","sin"},{"cos","cos"},{"tan","tan"},{"cot","cot"},{"sec","sec"},{"csc","csc"},{"sinh","sinh"},{"cosh","cosh"},{"tanh","tanh"},{"arcsin","arcsin"},{"arccos","arccos"},{"arctan","arctan"},
  {"log","log"},{"ln","ln"},{"exp","exp"},{"lim","lim"},{"max","max"},{"min","min"},{"det","det"},{"Re","Re"},{"Im","Im"}
 };
 static final String SUP_FROM="0123456789+-=()n", SUP_TO="\u2070\u00b9\u00b2\u00b3\u2074\u2075\u2076\u2077\u2078\u2079\u207a\u207b\u207c\u207d\u207e\u207f";
 static final String SUB_FROM="0123456789+-=()", SUB_TO="\u2080\u2081\u2082\u2083\u2084\u2085\u2086\u2087\u2088\u2089\u208a\u208b\u208c\u208d\u208e";
 static final Pattern INLINE=Pattern.compile("\\$[^$\\n]*[\\\\^_{][^$\\n]*\\$");

 static boolean looks(String s){
  return s.contains("$$")||s.contains("\\(")||s.contains("\\[")||INLINE.matcher(s).find()||s.matches("(?s).*\\\\[a-zA-Z]{2,}.*");
 }
 public static String plain(String s){
  if(s==null||s.length()<3) return s;
  try{
   if(!looks(s)) return s;
   s=s.replace("$$","\n").replace("$","").replace("\\(","").replace("\\)","").replace("\\[","\n").replace("\\]","\n");
   String r=new Conv(s).run();
   r=r.replaceAll("[ \\t]{3,}","  ").replaceAll("\\n{3,}","\n\n");
   return r.trim();
  }catch(Throwable e){ return s; }
 }
 // what the voice should say: math as words, code skipped, markdown removed, kept under the TTS size limit
 public static String speakable(String s){
  if(s==null) return "";
  try{
   s=s.replaceAll("(?s)```.*?```"," The code is on the screen. ");
   if(looks(s)){
    s=plain(s);
    StringBuilder b=new StringBuilder(); StringBuilder seq=new StringBuilder();
    for(int i=0;i<=s.length();i++){
     if(i<s.length()){ int sb=SUB_TO.indexOf(s.charAt(i)); if(sb>=0){ b.append(" sub ").append(SUB_FROM.charAt(sb)).append(" "); continue; } }
     int p=i<s.length()?SUP_TO.indexOf(s.charAt(i)):-1;
     if(p>=0){ seq.append(SUP_FROM.charAt(p)); continue; }
     if(seq.length()>0){
      String q=seq.toString();
      if(q.equals("2")) b.append(" squared ");
      else if(q.equals("3")) b.append(" cubed ");
      else b.append(" to the power of ").append(q.replace("-","minus ").replace("+","plus ").replace("(","").replace(")","")).append(" ");
      seq.setLength(0);
     }
     if(i<s.length()) b.append(s.charAt(i));
    }
    s=b.toString();
    String[][] w={{"\u221a"," square root of "},{"\u03c0"," pi "},{"\u03c9"," omega "},{"\u03bb"," lambda "},{"\u03b8"," theta "},{"\u03b1"," alpha "},{"\u03b2"," beta "},{"\u03b3"," gamma "},{"\u03bc"," mu "},{"\u03c3"," sigma "},{"\u03c6"," phi "},{"\u0394"," delta "},{"\u03a9"," ohms "},{"\u00d7"," times "},{"\u00b7"," times "},{"\u00b1"," plus or minus "},{"\u2248"," approximately "},{"\u2264"," less than or equal to "},{"\u2265"," greater than or equal to "},{"\u2260"," not equal to "},{"\u221e"," infinity "},{"\u2192"," goes to "},{"\u222b"," integral of "},{"\u2211"," sum of "},{"\u2202"," partial "},{"\u2207"," nabla "},{"/"," over "},{"="," equals "}};
    for(String[] p:w) s=s.replace(p[0],p[1]);
   }
   s=s.replace("*","").replace("#","").replace("`","");
   s=s.replaceAll("\\s+"," ").trim();
   if(s.length()>3500){ int c=s.lastIndexOf(". ",3500); s=s.substring(0,c>1000?c+1:3500); }
   return s;
  }catch(Throwable e){ return s; }
 }
 static String map(String a,boolean sup){
  if(a.isEmpty()) return null;
  String from=sup?SUP_FROM:SUB_FROM, to=sup?SUP_TO:SUB_TO; StringBuilder b=new StringBuilder();
  for(int k=0;k<a.length();k++){ int p=from.indexOf(a.charAt(k)); if(p<0) return null; b.append(to.charAt(p)); }
  return b.toString();
 }
 static String wrap(String a){
  if(a.length()<=3||a.matches("[A-Za-z0-9.\\u00b0\\u00b2\\u00b3\\u2070-\\u2079\\u03b1-\\u03c9\\u0391-\\u03a9]+")) return a;
  return "("+a+")";
 }
 static class Conv{
  final String s; int i=0;
  Conv(String s){ this.s=s; }
  String run(){ StringBuilder o=new StringBuilder(); while(i<s.length()) o.append(next()); return o.toString(); }
  String next(){
   char c=s.charAt(i);
   if(c=='\\') return cmd();
   if(c=='^'){ i++; return script(true); }
   if(c=='_'){ i++; return script(false); }
   if(c=='{'||c=='}'){ i++; return ""; }
   if(c=='&'){ i++; return " "; }
   i++; return String.valueOf(c);
  }
  String arg(){
   while(i<s.length()&&s.charAt(i)==' ') i++;
   if(i>=s.length()) return "";
   char c=s.charAt(i);
   if(c=='{'){
    int depth=0, st=i+1;
    while(i<s.length()){ char d=s.charAt(i); if(d=='{') depth++; else if(d=='}'){ depth--; if(depth==0) break; } i++; }
    String inner=s.substring(st,Math.min(i,s.length())); if(i<s.length()) i++;
    return new Conv(inner).run();
   }
   if(c=='\\') return cmd();
   i++; return String.valueOf(c);
  }
  String script(boolean sup){
   String a=arg();
   if(sup&&a.equals("\u00b0")) return a;
   String m=map(a,sup);
   if(m!=null) return m;
   if(a.matches("[A-Za-z0-9]+")) return (sup?"^":"_")+a;
   return (sup?"^(":"_(")+a+")";
  }
  String cmd(){
   int j=i+1;
   if(j>=s.length()){ i=j; return ""; }
   char e=s.charAt(j);
   if(!Character.isLetter(e)){
    i=j+1;
    if(e==','||e==';'||e==':'||e==' ') return " ";
    if(e=='!') return "";
    if(e=='\\') return "\n";
    return String.valueOf(e);
   }
   int k=j; while(k<s.length()&&Character.isLetter(s.charAt(k))) k++;
   String name=s.substring(j,k); i=k;
   if(name.equals("frac")||name.equals("dfrac")||name.equals("tfrac")){ String a=arg(), b=arg(); return wrap(a)+"/"+wrap(b); }
   if(name.equals("sqrt")){
    String idx=null;
    if(i<s.length()&&s.charAt(i)=='['){ int e2=s.indexOf(']',i); if(e2>0){ idx=s.substring(i+1,e2); i=e2+1; } }
    String a=arg();
    return ("3".equals(idx)?"\u221b":"\u221a")+(a.length()>1?"("+a+")":a);
   }
   if(name.equals("text")||name.equals("mathrm")||name.equals("mathbf")||name.equals("textbf")||name.equals("mathit")||name.equals("operatorname")||name.equals("boldsymbol")||name.equals("mathcal")||name.equals("mathbb")||name.equals("vec")||name.equals("overrightarrow")||name.equals("underline")||name.equals("overline")) return arg();
   if(name.equals("hat")){ String a=arg(); return a+"\u0302"; }
   if(name.equals("bar")){ String a=arg(); return a+"\u0304"; }
   if(name.equals("left")||name.equals("right")||name.equals("big")||name.equals("Big")||name.equals("bigg")||name.equals("displaystyle")||name.equals("limits")){ if(i<s.length()&&s.charAt(i)=='.') i++; return ""; }
   if(name.equals("begin")||name.equals("end")){ arg(); return ""; }
   if(name.equals("quad")||name.equals("qquad")) return "  ";
   if(name.equals("circ")) return "\u00b0";
   for(String[] p:SYM){ if(p[0].equals(name)) return p[1]; }
   return name;
  }
 }
}
