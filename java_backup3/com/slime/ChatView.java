package com.slime;
import android.content.Context; import android.graphics.Color; import android.graphics.drawable.Drawable; import android.view.*; import android.widget.*;
public class ChatView extends LinearLayout {
 public EditText input; public Button send,close,historyBtn,eyesBtn,voiceBtn; public LinearLayout messagesContainer; public ScrollView scroll;
 public ChatView(Context c){ super(c); setOrientation(VERTICAL); setBackgroundColor(Color.parseColor("#80000000")); LinearLayout card=new LinearLayout(c); card.setOrientation(VERTICAL); card.setPadding(10,8,10,8); card.setBackground(createRound(Color.WHITE,14f));
  TextView title=new TextView(c); title.setText("SlimeDroid • TTS/See/Hist/X"); title.setTextSize(10f); title.setTextColor(Color.GRAY);
  messagesContainer=new LinearLayout(c); messagesContainer.setOrientation(VERTICAL); scroll=new ScrollView(c); scroll.addView(messagesContainer); scroll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,220));
  addMessage("Slime: Fixed! Small chat, internet works now",false);
  input=new EditText(c); input.setHint("type..."); input.setTextSize(11f); input.setSingleLine(true); input.setBackground(createRound(Color.parseColor("#F2F3F4"),10f)); input.setPadding(10,6,10,6);
  LinearLayout row=new LinearLayout(c); row.setOrientation(HORIZONTAL); row.setPadding(0,6,0,0);
  send=new Button(c); send.setText("Send"); send.setTextSize(9f); send.setMinWidth(0); send.setMinHeight(0); send.setPadding(10,4,10,4);
  eyesBtn=new Button(c); eyesBtn.setText("See"); eyesBtn.setTextSize(9f); eyesBtn.setMinWidth(0); eyesBtn.setMinHeight(0); eyesBtn.setPadding(8,4,8,4);
  voiceBtn=new Button(c); voiceBtn.setText("TTS"); voiceBtn.setTextSize(9f); voiceBtn.setMinWidth(0); voiceBtn.setMinHeight(0); voiceBtn.setPadding(8,4,8,4);
  historyBtn=new Button(c); historyBtn.setText("Hist"); historyBtn.setTextSize(9f); historyBtn.setMinWidth(0); historyBtn.setMinHeight(0); historyBtn.setPadding(8,4,8,4);
  close=new Button(c); close.setText("X"); close.setTextSize(9f); close.setBackgroundColor(Color.parseColor("#FFCCCC")); close.setMinWidth(0); close.setMinHeight(0); close.setPadding(12,4,12,4);
  row.addView(input,new LayoutParams(0,-2,1f)); row.addView(send,new LayoutParams(-2,-2)); row.addView(eyesBtn,new LayoutParams(-2,-2)); row.addView(voiceBtn,new LayoutParams(-2,-2)); row.addView(historyBtn,new LayoutParams(-2,-2)); row.addView(close,new LayoutParams(-2,-2));
  card.addView(title); card.addView(scroll); card.addView(row); addView(card,new LayoutParams(320,LayoutParams.WRAP_CONTENT));
 }
 public void addMessage(String text, boolean isUser){ Context c=getContext(); TextView tv=new TextView(c); tv.setText(text); tv.setTextSize(11f); tv.setPadding(10,6,10,6); tv.setTextColor(isUser?Color.parseColor("#1A73E8"):Color.BLACK); android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable(); bg.setColor(isUser?Color.parseColor("#E8F0FE"):Color.parseColor("#F3F3F3")); bg.setCornerRadius(12f); tv.setBackground(bg); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT); lp.setMargins(0,4,0,4); tv.setLayoutParams(lp); messagesContainer.addView(tv); scroll.post(new Runnable(){ public void run(){ scroll.fullScroll(View.FOCUS_DOWN); }}); }
 Drawable createRound(int color,float r){ android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable(); d.setColor(color); d.setCornerRadius(r); d.setStroke(1,Color.parseColor("#E5E7EB")); return d; }
}
