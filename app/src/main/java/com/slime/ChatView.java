package com.slime;
import android.content.ClipData; import android.content.ClipboardManager; import android.content.Context; import android.graphics.Color; import android.graphics.Typeface;
import android.graphics.drawable.Drawable; import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder; import android.text.Spanned; import android.text.style.ForegroundColorSpan;
import android.view.*; import android.view.inputmethod.EditorInfo; import android.view.inputmethod.InputMethodManager; import android.widget.*;
import java.util.regex.Matcher; import java.util.regex.Pattern;
public class ChatView extends LinearLayout {
 public EditText input; public Button send,close,historyBtn,eyesBtn,voiceBtn,micBtn;
 public Button feedBtn,playBtn,clipBtn,catchBtn,muteBtn,statusBtn,cmdBtn;
 public LinearLayout messagesContainer; public ScrollView scroll;
 public boolean kb=false; public Runnable onKeyboard; long kbAt=0;
 public java.util.ArrayList<String> log=new java.util.ArrayList<String>(); String lastBot="";
 public ChatView(Context c){ super(c); setOrientation(VERTICAL); setBackgroundColor(Color.parseColor("#80000000"));
  LinearLayout card=new LinearLayout(c); card.setOrientation(VERTICAL); card.setPadding(12,10,12,10); card.setBackground(createRound(Color.WHITE,18f));
  LinearLayout titleRow=new LinearLayout(c); titleRow.setOrientation(HORIZONTAL); titleRow.setGravity(Gravity.CENTER_VERTICAL);
  TextView title=new TextView(c); title.setText("SlimeDroid"); title.setTextSize(13f); title.setTypeface(Typeface.DEFAULT_BOLD); title.setTextColor(Color.parseColor("#1B2631"));
  TextView sub=new TextView(c); sub.setText("  \u2022 chat, see, voice"); sub.setTextSize(10f); sub.setTextColor(Color.GRAY);
  titleRow.addView(title); titleRow.addView(sub);
  messagesContainer=new LinearLayout(c); messagesContainer.setOrientation(VERTICAL); messagesContainer.setPadding(2,6,2,2);
  scroll=new ScrollView(c); scroll.addView(messagesContainer); scroll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,240));
  input=new EditText(c); input.setHint("Talk to Slime..."); input.setTextSize(12f); input.setSingleLine(true); input.setBackground(createRound(Color.parseColor("#F2F3F4"),12f)); input.setPadding(12,8,12,8);
  input.setImeOptions(EditorInfo.IME_ACTION_SEND);
  LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT); ilp.setMargins(0,8,0,0); input.setLayoutParams(ilp);
  send=mk(c,"Send","#5DADE2",10f,14,6); eyesBtn=mk(c,"See","#48C9B0",10f,10,6); voiceBtn=mk(c,"TTS","#AF7AC5",10f,10,6);
  micBtn=mk(c,"Mic","#E67E22",10f,10,6);
  historyBtn=mk(c,"Hist","#85929E",10f,10,6); close=mk(c,"X","#E74C3C",10f,12,6);
  LinearLayout row=new LinearLayout(c); row.setOrientation(HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
  row.addView(send); row.addView(eyesBtn); row.addView(voiceBtn); row.addView(micBtn); row.addView(historyBtn); row.addView(close);
  HorizontalScrollView rowScroll=new HorizontalScrollView(c); rowScroll.setHorizontalScrollBarEnabled(false); rowScroll.setPadding(0,8,0,0); rowScroll.addView(row);
  feedBtn=mk(c,"Feed","#F5B041",9f,9,5); playBtn=mk(c,"Play","#5DADE2",9f,9,5); clipBtn=mk(c,"Clip","#48C9B0",9f,9,5);
  catchBtn=mk(c,"Catch up","#AF7AC5",9f,9,5); muteBtn=mk(c,"Shh","#85929E",9f,9,5); statusBtn=mk(c,"Status","#5D6D7E",9f,9,5); cmdBtn=mk(c,"Cmds","#AF7AC5",9f,9,5);
  LinearLayout quick=new LinearLayout(c); quick.setOrientation(HORIZONTAL);
  quick.addView(feedBtn); quick.addView(playBtn); quick.addView(clipBtn); quick.addView(catchBtn); quick.addView(muteBtn); quick.addView(statusBtn); quick.addView(cmdBtn);
  HorizontalScrollView quickScroll=new HorizontalScrollView(c); quickScroll.setHorizontalScrollBarEnabled(false); quickScroll.setPadding(0,7,0,0); quickScroll.addView(quick);
  card.addView(titleRow); card.addView(scroll); card.addView(input); card.addView(rowScroll); card.addView(quickScroll);
  addView(card,new LinearLayout.LayoutParams(340,LinearLayout.LayoutParams.WRAP_CONTENT));
  feedBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ runQuick("feed"); }});
  playBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ runQuick("play"); }});
  clipBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ runQuick("clip"); }});
  catchBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ runQuick("catch me up"); }});
  muteBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ runQuick("shh"); }});
  statusBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ runQuick("status"); }});
  cmdBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ runQuick("help"); }});
  // keyboard: the Enter key sends, and tapping the box lifts the chat above the keyboard
  input.setOnEditorActionListener(new TextView.OnEditorActionListener(){ public boolean onEditorAction(TextView v,int a,KeyEvent e){ send.performClick(); return true; }});
  input.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ setKb(true); }});
  setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener(){ public WindowInsets onApplyWindowInsets(View v,WindowInsets in){
   try{ if(kb&&System.currentTimeMillis()-kbAt>900&&in.getSystemWindowInsetBottom()<=150) setKb(false); }catch(Exception e){ L.w("ChatView",e); }
   return v.onApplyWindowInsets(in);
  }});
 }
 public void setKb(boolean open){
  if(kb==open) return;
  kb=open; kbAt=System.currentTimeMillis();
  try{ scroll.getLayoutParams().height=open?150:240; scroll.requestLayout(); }catch(Exception e){ L.w("ChatView",e); }
  if(onKeyboard!=null) onKeyboard.run();
 }
 public void hideKeyboard(){
  try{ InputMethodManager imm=(InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE); imm.hideSoftInputFromWindow(input.getWindowToken(),0); }catch(Exception e){ L.w("ChatView",e); }
  try{ input.clearFocus(); }catch(Exception e){ L.w("ChatView",e); }
  setKb(false);
 }
 Button mk(Context c,String label,String color,float ts,int ph,int pv){
  Button b=new Button(c); b.setText(label); b.setAllCaps(false); b.setTextSize(ts);
  b.setMinWidth(0); b.setMinHeight(0); b.setMinimumWidth(0); b.setMinimumHeight(0);
  b.setPadding(ph,pv,ph,pv); b.setTextColor(Color.WHITE); b.setBackground(createRound(Color.parseColor(color),12f));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0,0,5,0); b.setLayoutParams(lp);
  return b;
 }
 void runQuick(String cmd){ if(input==null||send==null) return; input.setText(cmd); send.performClick(); }

 // ---------- copying ----------
 static void copyText(Context c,String t){
  try{ ClipboardManager cm=(ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE); cm.setPrimaryClip(ClipData.newPlainText("slime",t)); Toast.makeText(c,"copied",Toast.LENGTH_SHORT).show(); }catch(Exception e){ L.w("ChatView",e); }
 }
 View.OnLongClickListener copier(final String t){ return new View.OnLongClickListener(){ public boolean onLongClick(View v){ copyText(getContext(),t); return true; }}; }
 public void copyLast(){ if(lastBot.isEmpty()) Toast.makeText(getContext(),"nothing to copy yet",Toast.LENGTH_SHORT).show(); else copyText(getContext(),lastBot); }
 public void copyAll(){
  StringBuilder sb=new StringBuilder(); for(String m:log){ if(sb.length()>0) sb.append("\n\n"); sb.append(m); }
  if(sb.length()==0) Toast.makeText(getContext(),"nothing to copy yet",Toast.LENGTH_SHORT).show(); else copyText(getContext(),sb.toString());
 }

 // ---------- messages ----------
 GradientDrawable bg(boolean isUser){
  GradientDrawable g=new GradientDrawable();
  g.setColor(isUser?Color.parseColor("#5DADE2"):Color.parseColor("#F4F6F7"));
  if(isUser) g.setCornerRadii(new float[]{16f,16f,16f,16f,4f,4f,16f,16f}); else g.setCornerRadii(new float[]{16f,16f,16f,16f,16f,16f,4f,4f});
  return g;
 }
 TextView bubbleTv(String text,boolean isUser){
  TextView tv=new TextView(getContext()); tv.setText(text); tv.setTextSize(11.5f); tv.setPadding(12,8,12,8); tv.setMaxWidth(250);
  tv.setTextColor(isUser?Color.WHITE:Color.parseColor("#1B2631")); tv.setBackground(bg(isUser));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0,4,0,4);
  tv.setLayoutParams(lp); tv.setOnLongClickListener(copier(text)); return tv;
 }
 void addText(LinearLayout wrap,String text,boolean isUser){
  text=text.trim(); if(text.isEmpty()) return; if(MathView.hasMath(text)) text=MathText.plain(text);
  View mv=MathView.hasMath(text)?MathView.card(getContext(),text,isUser,bg(isUser)):null;
  if(mv!=null) wrap.addView(mv); else if(text.length()>400) wrap.addView(longCard(text,isUser)); else wrap.addView(bubbleTv(text,isUser));
 }
 public void addMessage(String text,boolean isUser){
  Context c=getContext();
  String clean=text==null?"":text; log.add(clean); if(log.size()>200) log.remove(0);
  if(!isUser) lastBot=clean.startsWith("Slime: ")?clean.substring(7):clean;
  LinearLayout wrap=new LinearLayout(c); wrap.setOrientation(VERTICAL); wrap.setGravity(isUser?Gravity.END:Gravity.START);
  String rest=text==null?"":text;
  while(true){
   int a=rest.indexOf("```");
   if(a<0){ addText(wrap,rest,isUser); break; }
   addText(wrap,rest.substring(0,a),isUser);
   int nl=rest.indexOf('\n',a+3), b=rest.indexOf("```",a+3);
   String lang=""; int cs=a+3;
   if(nl>=0&&(b<0||nl<b)){ lang=rest.substring(a+3,nl).trim(); cs=nl+1; }
   if(b<0){ wrap.addView(codeCard(lang,rest.substring(Math.min(cs,rest.length())))); break; }
   if(cs>b) cs=b;
   wrap.addView(codeCard(lang,rest.substring(cs,b)));
   rest=rest.substring(b+3);
  }
  messagesContainer.addView(wrap);
  scroll.post(new Runnable(){ public void run(){ scroll.fullScroll(View.FOCUS_DOWN); }});
 }
 View longCard(final String full,final boolean isUser){
  Context c=getContext();
  final String prev=full.substring(0,Math.min(160,full.length()))+"...";
  final LinearLayout card=new LinearLayout(c); card.setOrientation(VERTICAL); card.setPadding(12,8,12,8); card.setBackground(bg(isUser));
  final TextView head=new TextView(c); head.setTextSize(10f); head.setTypeface(Typeface.DEFAULT_BOLD); head.setTextColor(isUser?Color.WHITE:Color.parseColor("#5D6D7E"));
  final TextView body=new TextView(c); body.setTextSize(11.5f); body.setTextColor(isUser?Color.WHITE:Color.parseColor("#1B2631"));
  card.addView(head); card.addView(body);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0,4,0,4); card.setLayoutParams(lp);
  final boolean[] open={false};
  final Runnable upd=new Runnable(){ public void run(){
   head.setText("Long message - "+full.length()+" chars - tap to "+(open[0]?"collapse":"expand"));
   body.setText(open[0]?full:prev);
  }};
  upd.run();
  card.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ open[0]=!open[0]; upd.run(); }});
  card.setOnLongClickListener(copier(full));
  return card;
 }
 View codeCard(String lang,final String code0){
  final Context c=getContext();
  final String code=code0.replaceAll("^\\n+","").replaceAll("\\s+$","");
  final int lines=code.split("\n",-1).length;
  LinearLayout card=new LinearLayout(c); card.setOrientation(VERTICAL); card.setPadding(8,6,8,6); card.setBackground(createRound(Color.parseColor("#1E2A36"),12f));
  LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT); clp.setMargins(0,4,0,4); card.setLayoutParams(clp);
  LinearLayout head=new LinearLayout(c); head.setOrientation(HORIZONTAL); head.setGravity(Gravity.CENTER_VERTICAL);
  final TextView title=new TextView(c); title.setTextSize(10f); title.setTextColor(Color.parseColor("#AEB6BF"));
  Button copy=mk(c,"Copy","#48C9B0",9f,9,4); final Button tog=mk(c,"Expand","#5DADE2",9f,9,4);
  head.addView(title,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f)); head.addView(copy); head.addView(tog);
  final HorizontalScrollView hs=new HorizontalScrollView(c); hs.setPadding(0,4,0,0);
  TextView body=new TextView(c); body.setTypeface(Typeface.MONOSPACE); body.setTextSize(10.5f); body.setTextColor(Color.parseColor("#E5E8E8")); body.setHorizontallyScrolling(true); body.setText(highlight(code));
  hs.addView(body);
  final boolean[] open={lines<=14};
  final String name=(lang==null||lang.isEmpty())?"code":lang;
  final Runnable upd=new Runnable(){ public void run(){
   title.setText(name+" ["+lines+" lines]"+(open[0]?"":" - tap to expand"));
   hs.setVisibility(open[0]?View.VISIBLE:View.GONE); tog.setText(open[0]?"Collapse":"Expand");
  }};
  upd.run();
  View.OnClickListener t=new View.OnClickListener(){ public void onClick(View v){ open[0]=!open[0]; upd.run(); }};
  tog.setOnClickListener(t); title.setOnClickListener(t);
  copy.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
   try{ ClipboardManager cm=(ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE); cm.setPrimaryClip(ClipData.newPlainText("slime",code)); Toast.makeText(c,"copied",Toast.LENGTH_SHORT).show(); }catch(Exception e){ L.w("ChatView",e); }
  }});
  card.addView(head); card.addView(hs);
  return card;
 }
 SpannableStringBuilder highlight(String code){
  SpannableStringBuilder sb=new SpannableStringBuilder(code);
  paint(sb,code,"\\b(if|then|else|elif|fi|for|while|do|done|case|esac|function|return|import|from|class|def|public|private|static|void|int|float|String|new|try|catch|final|echo|cat|sed|grep|cp|rm|mkdir|chmod|export|true|false|null)\\b","#C792EA");
  paint(sb,code,"\\b\\d+(\\.\\d+)?\\b","#F78C6C");
  paint(sb,code,"\"[^\"\\n]*\"|'[^'\\n]*'","#C3E88D");
  paint(sb,code,"(?m)(#.*|//.*)$","#7F8C8D");
  return sb;
 }
 void paint(SpannableStringBuilder sb,String code,String re,String color){
  try{ Matcher m=Pattern.compile(re).matcher(code); while(m.find()) sb.setSpan(new ForegroundColorSpan(Color.parseColor(color)),m.start(),m.end(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); }catch(Exception e){ L.w("ChatView",e); }
 }
 Drawable createRound(int color,float r){ GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(r); return d; }
}
