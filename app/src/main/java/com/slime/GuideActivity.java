package com.slime;
import android.app.Activity; import android.graphics.Color; import android.graphics.Typeface; import android.graphics.drawable.GradientDrawable; import android.os.Bundle;
import android.view.View; import android.view.ViewGroup; import android.widget.Button; import android.widget.LinearLayout; import android.widget.ScrollView; import android.widget.TextView;
import java.util.ArrayList;
public class GuideActivity extends Activity {
 LinearLayout col; float d;
 final ArrayList<TextView> heads=new ArrayList<TextView>(), bodies=new ArrayList<TextView>(), titles=new ArrayList<TextView>();
 @Override protected void onCreate(Bundle b){
  super.onCreate(b);
  d=getResources().getDisplayMetrics().density; int pad=(int)(16*d);
  ScrollView sv=new ScrollView(this); sv.setBackgroundColor(Color.parseColor("#1B2631"));
  col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL); col.setPadding(pad,pad,pad,pad*2);
  sv.addView(col,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
  text("SlimeDroid guide",22f,"#FFFFFF",true,0);
  text("Everything about the app, from the very beginning to the end. Tap a title to open it.",13f,"#AEB6BF",false,4);
  LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
  row.addView(btn("Open all",true)); row.addView(btn("Close all",false)); col.addView(row);

  sec("1. What is SlimeDroid?",
   "SlimeDroid is a little blue slime that lives on top of your other apps.\n\n- It reacts to what you do: charging, headphones, shaking, unlocking, night time.\n- It chats with you like a friend and remembers things you tell it.\n- It can read what is on your screen and talk about it.\n- With Shizuku and Termux it can press buttons, swipe, open apps, change settings and take screenshots for you.\n- It grows up (hats, glasses, a crown) the more you use it.\n\nIt is built to cost nothing: it uses OpenRouter's free AI models.");

  sec("2. Quick start (about 5 minutes)",
   "Do these on the home screen of this app, in order:\n\n1. Tap 'Allow overlay' and switch it on. This lets the slime float over other apps.\n2. Tap 'Allow accessibility' and switch on SlimeDroid. This is the slime's eyes and hands.\n3. Tap 'Allow notifications' and switch on SlimeDroid. This lets it see your notifications.\n4. Tap 'Allow battery unrestricted' and say yes, so Android does not put it to sleep.\n5. Get a free OpenRouter key: open openrouter.ai, make an account, go to Keys, create a key and copy it. Paste it in the key box on the home screen and tap 'Add key'.\n6. Tap 'Start slime'. A slime appears in the middle of the screen.\n7. Long-press the slime to open the chat, and say hi.\n\nThe checklist at the top of the home screen shows a tick next to everything that is ready.");

  sec("3. What each permission does",
   "Overlay: draws the slime on top of other apps. Without it nothing shows.\n\nAccessibility: lets the slime read the text on your screen and tap, type and scroll when you ask. It is the 'See' button and the 'open', 'tap', 'type', 'scroll' commands. The slime tries to stay out of private apps like banking.\n\nNotification access: lets it read incoming notifications so it can show 'ping from...' bubbles and summarise them when you say 'catch me up'.\n\nBattery unrestricted: stops Android from killing the slime in the background.\n\nRun commands in Termux: lets SlimeDroid ask Termux to run phone commands (see section 13).\n\nNothing is sent anywhere except to OpenRouter (the AI) when you chat, when it peeks at your screen, or when you use 'look'.");

  sec("4. The free AI and the daily limit",
   "SlimeDroid talks to OpenRouter's free models. The free plan allows about 50 requests a day (and 20 a minute) for each OpenRouter account.\n\n- Every chat message you send uses 1 request.\n- The slime's own unprompted things (random chat, peeks, humming songs) are capped at about 8 a day so most of your requests stay yours.\n- Type 'status' to see how many you used today, for example 'ai 12/50'.\n- When the limit is reached the slime tells you. It resets every day. Buying $10 of credit on OpenRouter once raises the free limit to 1000 a day.\n\nThe default model is 'openrouter/free'. It picks a free model for each message, so the slime's voice can change a little from message to message. 'models' lists the free models right now, 'model NAME' pins one, and 'model reset' goes back to the default.");

  sec("5. Using more than one key",
   "You can add several OpenRouter keys. When one stops working (rejected, out of credit, or out of free messages for the day) the slime switches to the next one by itself.\n\nImportant: keys that belong to the same OpenRouter account share one limit. Only keys from different accounts add more room. Follow OpenRouter's own rules when you make accounts.\n\nCommands: 'keys' shows them (masked), 'key add KEY', 'key remove N', 'key next'. You can also add and remove keys on the home screen.");

  sec("6. Meeting your slime",
   "Gestures:\n- Tap: its mood changes and it dashes away.\n- Poke it 4 times quickly: it gets annoyed.\n- Drag: move it anywhere (it stays inside the screen).\n- Long-press: open or close the chat.\n- Double-tap: close the slime. Open the app and tap 'Start slime' to bring it back.\n\nIt has needs: food, energy and joy. Feed it with 'feed', play with 'play', and 'status' shows the numbers. When it is hungry, sleepy or sad it tells you. It sleeps at night (before 6 am) unless you say 'wake up'.\n\nMoods: Happy, Sleepy, Mad, Love, Hungry, Cry, Wow, Wink and Dead. They change its face and color. It also runs when it moves and walks around by itself.");

  sec("7. The chat window",
   "Long-press the slime to open it. Type in the box and press Enter or 'Send'. The chat lifts above the keyboard while you type.\n\nButtons: Send, See (read my screen), TTS (voice on and off), Hist (history), X (close). Quick buttons: Feed, Play, Clip, Catch up, Shh, Status, Cmds.\n\nCopying: long-press any message to copy it. Code boxes have a Copy button. Type 'copy' to copy the last reply, or 'copy all' for the whole chat.\n\nLong replies fold into a card: tap it to open or close. Code gets its own box with the language, the line count, Copy and Collapse.\n\nMath: when the slime writes LaTeX it is drawn as real math. It needs internet the first time. Offline you see the plain text.\n\nThe speech bubble above the slime shows a short version of what it says.");

  sec("8. Memory",
   "The slime remembers things you tell it, one fact at a time.\n\n- It picks facts up from what you say (your name, school, projects, likes, plans, people you mention) and from tags the AI adds for free.\n- 'remember I have an exam on Friday' saves something on purpose.\n- 'memories' shows everything, numbered. 'forget 3' removes one. 'forget all' wipes it.\n- 'what do you remember' gives a short summary.\n- 'memory off' stops automatic saving. 'memory on' turns it back on.\n\nIt does not keep passwords, PINs, codes, card or long ID numbers, email addresses, or some very sensitive topics.\n\nMemories are stored on your phone. The ones that fit the moment are sent to the AI along with your message so it can use them.");

  sec("9. Looking at your screen",
   "'See' or a question like 'what do you think of this?' lets the slime read the text on your screen (needs accessibility).\n\nRandom peeks: now and then (about every 45 minutes when you are using the phone) it glances at your screen and says what it thinks. 'peek' does it now, 'peek off' stops it, 'peek more' and 'peek less' change how often. It skips private-looking screens and stays quiet in focus and game mode.\n\n'look what is this?' takes a real screenshot and asks a vision AI about it (needs Shizuku and Termux, section 13). It refuses private apps.\n\nOnly text (or the screenshot you asked for) goes to OpenRouter.");

  sec("10. Modes",
   "Say 'focus mode', 'hacker mode', 'study mode', 'game mode' or 'chill mode'. 'mode' shows the current one.\n\n- Focus: red, very short answers, quiet, notifications muted and do not disturb on.\n- Hacker: dark with a green glow, technical answers.\n- Study: blue with glasses and a cap, teaches step by step.\n- Game: yellow with a controller hat, quiet, and it hides while a game is on screen.\n- Chill: the normal playful slime.");

  sec("11. Growing up",
   "Every message earns xp. Level names: baby slime, slimelet, slime cadet, slime knight, slime king.\n\nAccessories: glasses after 25 coding chats, a grad cap after 15 math or 20 study chats, floating math symbols with the cap, headphones after 12 music chats, sparkles after 8 'fixed it' chats, a crown at level 5.\n\nType 'evolution' to see your progress.");

  sec("12. Music, humming and notifications",
   "When music is playing it sometimes hums along with a little tune, a moving mouth and floating notes. 'hum' makes it hum now, 'hum off' stops it. It makes up its own tunes and never copies real lyrics.\n\nNotifications: it shows a short bubble when a new one arrives and flags urgent ones. 'catch me up' summarises what you missed. 'pings off' and 'pings on' control the bubbles. 'shh' keeps it quiet for 2 hours.");

  sec("13. Phone control with Shizuku and Termux",
   "Shizuku lets apps run commands with the power of a computer connected by adb. Termux is the terminal app that carries those commands.\n\nOne-time setup:\n1. Install Termux and Shizuku. Start Shizuku (it needs wireless debugging) and allow it.\n2. In the Shizuku app tap 'Use Shizuku in terminal apps' and export the two files (rish and rish_shizuku.dex) to your Download folder.\n3. In Termux run: termux-setup-storage (allow it).\n4. Run setup_rish.sh in Termux with: bash ~/setup_rish.sh. If it ends with '[ok] Shizuku shell works', you are done.\n5. In the slime chat, type 'shizuku' to test.\n\nThen you can say things like 'swipe up', 'tap 500 900', 'back', 'home', 'launch whatsapp', 'brightness 40', 'wifi off', 'shot' and 'look what is this?'. The Commands screen lists everything.\n\nSafety: the AI may suggest a phone action, but it always waits for your 'yes'. Only you can use 'sh' and 'term'.\n\nShizuku stops when you restart the phone (without root). Open Shizuku and start it again.");

  sec("14. Keeping Termux reachable",
   "The Termux commands need Termux to be allowed to run in the background. Phone makers often kill it. Do these three things:\n\n1. In Termux run these lines (once):\nmkdir -p ~/.termux/boot\nprintf '#!/data/data/com.termux/files/usr/bin/sh\\ntermux-wake-lock\\n' > ~/.termux/boot/slime.sh\nchmod +x ~/.termux/boot/slime.sh\ntermux-wake-lock\nThen install Termux:Boot (from the same place you got Termux) and open it once. The wake lock keeps Termux alive; you will see its notification.\n\n2. In phone settings open Termux's app info (the home screen has a button) and set Battery to unrestricted. If your phone has an Autostart list, switch Termux and Shizuku on. Lock Termux in the recent apps if your phone allows.\n\n3. In the slime chat type 'fix termux'. It uses Shizuku to exempt Termux, Shizuku and SlimeDroid from battery limits.\n\nIf a command says 'Termux didn't answer', type 'diagnose'. It checks everything and gives the exact fix.");

  sec("15. Files: hold, drop, stash",
   "'hold report.pdf' makes the slime pick up a file (it finds it in Download, Documents, Pictures, DCIM, Movies and Music). A little file shows by its hand and it gets chubby.\n\n- 'what are you holding' lists the files.\n- 'drop' puts them in Downloads. 'drop to documents', 'drop to pictures', 'drop to termux' or 'drop to /sdcard/SomeFolder' choose another place.\n- 'stash' puts what it carries away in /sdcard/SlimePouch/stash.\n\nIt copies the files, so the originals stay where they were. This needs Shizuku and Termux.");

  sec("16. If something goes wrong",
   "The slime is missing: open this app and tap 'Start slime'. It comes back to the middle of the screen.\n\nCommands fail or say 'Termux didn't answer': type 'diagnose'. Check Shizuku is running and read section 14.\n\n'too many messages today': you used the free limit. Wait for tomorrow, add another key, or add credit.\n\nReplies feel odd or change style: the free router picks a different model each time. Pin one with 'model NAME'.\n\nMath shows as plain text: it needs internet the first time.\n\nThe keyboard hides the box: tap the box once more; the chat jumps to the top while you type.\n\nSomething else: type 'help' for the commands screen, 'status' for numbers, or 'diagnose' for a check-up.");

  sec("17. Privacy and safety in one page",
   "- Your memories, key list and files live on your phone.\n- Messages, relevant memories and, if you ask or allow peeks, screen text go to OpenRouter and the free model behind it.\n- Private-looking apps are skipped for peeks and 'look'.\n- Passwords, codes and card numbers are never saved to memory.\n- The AI can never run phone commands on its own: a short list of safe actions, and only after you say yes.\n- Anything dangerous (sh, term, settings, grant) only runs when you type it yourself.\n- Revoke an old API key at openrouter.ai/keys if you ever share it by mistake.");

  sec("18. Words explained",
   "Overlay: something drawn on top of other apps.\nAccessibility: an Android feature that lets an app read the screen and press things.\nShizuku: an app that gives other apps adb-level power.\nTermux: a terminal app for Android.\nrish: the Shizuku shell inside Termux.\nOpenRouter: a website that gives access to many AI models, some of them free.\nFree router: the model name 'openrouter/free', which picks a free model for you.\nLaTeX: the way math is written, like $x^2$.\nXP: experience points the slime earns as you chat.");

  setContentView(sv);
 }
 TextView text(String s,float size,String color,boolean bold,int topDp){
  TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.parseColor(color));
  if(bold) t.setTypeface(Typeface.DEFAULT_BOLD);
  t.setPadding(0,(int)(topDp*d),0,(int)(2*d)); col.addView(t); return t;
 }
 Button btn(String label,final boolean open){
  Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(12f); b.setTextColor(Color.WHITE);
  GradientDrawable g=new GradientDrawable(); g.setColor(Color.parseColor("#5DADE2")); g.setCornerRadius(12*d); b.setBackground(g);
  b.setPadding((int)(14*d),(int)(6*d),(int)(14*d),(int)(6*d));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(0,(int)(8*d),(int)(8*d),0); b.setLayoutParams(lp);
  b.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ for(int i=0;i<bodies.size();i++) setOpen(i,open); }});
  return b;
 }
 void setOpen(int i,boolean open){
  bodies.get(i).setVisibility(open?View.VISIBLE:View.GONE);
  heads.get(i).setText((open?"-  ":"+  ")+titles.get(i).getText());
 }
 void sec(String title,String body){
  final TextView h=new TextView(this); h.setTextSize(15f); h.setTypeface(Typeface.DEFAULT_BOLD); h.setTextColor(Color.parseColor("#5DADE2"));
  h.setPadding(0,(int)(16*d),0,(int)(4*d));
  final TextView t=new TextView(this); t.setText(title);
  final TextView b=new TextView(this); b.setText(body); b.setTextSize(13f); b.setTextColor(Color.parseColor("#E5E8E8")); b.setLineSpacing(0f,1.15f);
  b.setPadding(0,0,0,(int)(6*d));
  heads.add(h); bodies.add(b); titles.add(t);
  final int idx=heads.size()-1;
  col.addView(h); col.addView(b);
  setOpen(idx,false);
  h.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ setOpen(idx,bodies.get(idx).getVisibility()!=View.VISIBLE); }});
 }
}
