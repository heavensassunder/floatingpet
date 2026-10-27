package com.slime;
import android.app.Activity; import android.graphics.Color; import android.graphics.Typeface; import android.os.Bundle; import android.text.Html;
import android.view.ViewGroup; import android.widget.LinearLayout; import android.widget.ScrollView; import android.widget.TextView;
public class CommandsActivity extends Activity {
 LinearLayout col; float d;
 @Override protected void onCreate(Bundle b){
  super.onCreate(b);
  d=getResources().getDisplayMetrics().density;
  int pad=(int)(16*d);
  ScrollView sv=new ScrollView(this); sv.setBackgroundColor(Color.parseColor("#1B2631"));
  col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL); col.setPadding(pad,pad,pad,pad*2);
  sv.addView(col,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
  text("SlimeDroid commands",22f,"#FFFFFF",true,0);
  text("Type these in the slime chat (long-press the slime to open it, or tap the Cmds button). Words in CAPS are things you fill in.",13f,"#AEB6BF",false,6);

  head("Gestures and buttons");
  item("tap the slime","its mood changes and it dashes away");  item("poke it 4 times","it gets annoyed");
  item("drag","move it anywhere");
  item("long-press","open or close the chat");
  item("double-tap","close the pet");
  item("buttons","Send, See (read my screen), TTS (voice on/off), Hist (history), X, and quick buttons Feed, Play, Clip, Catch up, Shh, Status, Cmds");
  item("about","who made SlimeDroid, with the creator's WhatsApp and GitHub links");
  head("Built-in browser");
  item("browser / web","open the full-screen slime browser (or tap the Web button in chat); home page is Google");
  item("browse URL / web SEARCH","open the browser straight at a site, e.g. 'browse wikipedia.org', or run a Google search");

  head("Pet life");
  item("feed","give it a snack");
  item("play","play tag with it");
  item("status","food, energy, joy and battery");
  item("shh","quiet for 2 hours");
  item("talk to me","un-mute it");
  item("talk more / talk less","how often it chats on its own");
  item("say something","make it speak now");
  item("wake up","keep it awake for 5 hours");
  item("pings on / pings off","bubbles for new notifications");
  item("catch me up","summary of your recent notifications");
  head("Reminders");
  item("remind me in 20 minutes to TEXT","also: in 3 hours, in 2 days - it pings and speaks when due");
  item("remind me tomorrow 7am to TEXT / remind me at 14:30 TEXT","clock times, am/pm supported, bare time means next occurrence");
  item("reminders / forget reminder N","list upcoming reminders, or remove one by its number");
  head("Morning briefing and weather");
  item("briefing 7am / briefing 6:30 / briefing off","daily spoken briefing: weather, unread pings, level and streak; 'briefing now' previews it");
  item("weather / weather city Nairobi","current conditions from free Open-Meteo (no API key), or change the city");
  head("Routines (your own if-this-then-that rules)");
  item("when I open APP then ACTION","runs the action whenever that app comes to the foreground, e.g. when I open tiktok then shh");
  item("when charging then ACTION / when I unplug then ACTION","power connect and disconnect triggers");
  item("when headphones connect then ACTION / when headphones disconnect then ACTION","headset plug triggers, e.g. then tts off");
  item("when I unlock then ACTION","runs every time you unlock the phone; 'then say good morning' works too");
  item("routines / forget routine N","list your rules, or remove one by its number");
  head("Pet look and feel");
  item("pet size 130 / pet size small | normal | big | huge","make the slime bigger or smaller (50% to 180%); 'pet bigger', 'pet smaller' step it, 'pet size reset' restores 100%");
  item("wander on / wander off (also: stay / walk around)","off makes the slime stay put until you drag it; on lets it roam by itself. 'wander status' shows the current mode");
  item("what do you remember","what it knows about you");
  item("forget all","wipe what it remembers");
  item("clear history","clear chat and notification history");

  head("Memory");
  item("memories","see everything I remember, numbered");
  item("remember TEXT","save something on purpose");
  item("remember about TOPIC","search memories about a topic (also: memory search TOPIC)");
  item("memory change N TEXT","reword memory N from the memories/search list");
  item("forget N / forget WORDS","remove one memory");
  item("forget all","wipe all memories");
  item("memory export / memory import","back up memories and chats to SlimePouch, or restore the newest backup");
  item("memory on / memory off","allow or stop automatic saving");
  item("what do you remember","short summary by topic");
  head("Modes");
  item("focus mode","red, quiet, notifications muted, do not disturb on");
  item("hacker mode","dark with a green glow, technical answers");
  item("study mode","blue with glasses, step by step teaching");
  item("game mode","yellow, hides while you play games");
  item("chill mode","back to normal (also: mode, focus mode off)");
  head("Files (Shizuku)");
  item("hold NAME","pick up a file (it shows on the slime and makes it chubby)");
  item("what are you holding","list what it carries");
  item("drop","put everything in Downloads (or: drop to documents / pictures / termux / a folder path)");
  item("stash","put what it carries away for later");
  head("AI providers and keys");
  item("use gemini / use openrouter","switch AI provider (the choice is saved) - or tap the AI button in the chat's quick row");
  item("ai provider","active provider, both key counts, today's usage and current model");
  item("keys","list your OpenRouter keys (masked), which one is in use and today usage");
  item("key add KEY / key remove N / key next","manage OpenRouter keys; I switch by myself when one runs out");
  item("gemini keys","list your Google AI Studio keys");
  item("gemini key add KEY / gemini key remove N / gemini key next","manage Gemini keys the same way");
  item("gemini key reset / key reset","clear parked-key marks after a minute-level rate limit or once quota resets");
  item("gemini model NAME / gemini models / gemini model reset","pin a Gemini model, list what your key can use, or restore the default");
  item("gemini diagnose","live test call showing the HTTP code, raw Google response and parse result");
  item("search QUESTION / google QUESTION","live web answer via Gemini Google Search grounding (needs a Gemini key; free, shares the flash-lite pool)");
  head("Offline brain (no internet, runs in Termux; 64-bit devices only)");
  item("offline download smol | qwen | all","download a small on-device GGUF model (~400-500 MB each), installs llama.cpp too; 32-bit phones are told they can't");
  item("use offline / use gemini / use openrouter","switch provider, or just tap the AI button in the chat");
  item("offline status","download progress, server state, free RAM and disk");
  item("offline model smol | qwen","pick SmolLM2-360M (fast) or Qwen3-0.6B (better Swahili)");
  item("offline start / offline stop","load or release the local server and its wake lock");
  item("copy / copy all","copy my last reply, or the whole chat (also: long-press any message)");
  item("guide","open the beginner guide (also on the home screen)");
  item("fix termux","exempt Termux, Shizuku and me from battery limits so Termux always answers");
  head("Growth");
  item("evolution","level, xp, streak, age, today's XP, behaviour stats, level-up history and accessories");
  item("achievements","all awards with unlocked state and progress (also: awards)");
  item("models","list the free AI models available right now");
  head("Vibes");
  item("hum","it hums a little tune (it also does this by itself when music is playing)");
  item("hum on / hum off","allow or stop humming");
  item("peek","it glances at your screen and says what it thinks (it also does this at random times)");
  item("peek on / peek off","allow or stop random peeking");
  item("peek more / peek less","how often it peeks");

  head("Voice and listening");
  item("Mic button / listen on","tap Mic for one utterance, or say listen on for hands-free mode while the screen is on");
  item("listen test","narrated diagnostic: ear open, voice heard, levels, recognition result and engine errors");
  item("listen engine auto / ondevice","use the default speech engine, or force the Android 12+ on-device recognizer");
  item("listen off / listen status","stop continuous listening, or check its state");
  item("listen language en / sw","recognition language (English US or Swahili KE)");
  item("tts on / tts off","let the slime speak replies aloud (also the TTS button)");
  item("tts status / tts cloud test","show voice mode and last cloud error, or test every cloud provider");
  item("tts mode auto / cloud / local","neural cloud voice with fallback, force cloud, or offline engine only");
  item("tts provider auto / google / streamelements","which neural provider to try (auto falls through both)");
  item("tts language en / sw","language of the Google cloud voice");
  item("tts test","hear the current voice");
  item("tts cloud voices / tts cloud voice NAME","list or pick a neural voice (e.g. Brian, Amy, Joanna)");
  item("tts engines / tts engine N","list or switch the on-device speech engine");
  item("voice list / voice N","list or pick an installed on-device voice");
  item("tts speed N / tts pitch N","0.5 to 2.0 adjustments");

  head("Clipboard");
  item("clip","explain what you copied");
  item("clip summarize","short summary of it");
  item("clip translate","translate it");
  item("clip reply","write a reply to it");

  head("Screen and apps (accessibility)");
  item("open APP","open an app or Settings");
  item("tap NAME","tap a button by its label");
  item("type TEXT","type into the focused field");
  item("scroll up / scroll down","scroll the screen");

  head("Phone control (Shizuku)");
  item("back, home, recents","press those buttons");
  item("key enter / volup / voldown / mute / power / menu / play / next / prev","press a key");
  item("tap X Y","real tap at screen position X, Y");
  item("swipe up / down / left / right","real swipe");
  item("swipe X1 Y1 X2 Y2","swipe between two points");
  item("stype TEXT","type text with the real keyboard path");
  item("launch APP","open an app by name or package");
  item("force stop APP","stop an app (also: kill APP)");
  item("goto URL","open a web link (http or https)");
  item("fg","which app is on screen");
  item("shot","save a screenshot to Pictures");
  item("look QUESTION","screenshot, then the slime answers about it");
  item("photo math (also: solve this)","screenshot a math/physics problem and get a full step-by-step worked solution");

  head("Phone settings (Shizuku)");
  item("brightness 0-100 / brightness auto","screen brightness");
  item("wifi on / off","Wi-Fi");
  item("data on / off","mobile data");
  item("bluetooth on / off","Bluetooth");
  item("airplane on / off","airplane mode");
  item("rotate on / off","auto-rotate");
  item("dnd on / off","do not disturb");
  item("dark mode on / off","dark theme");
  item("volume 0-15","media volume");
  item("timeout SECONDS","screen timeout");
  item("screen off / screen on","turn the screen off or on");

  head("Power tools");
  item("model / model NAME","show or pin the AI model; by default free :free models rotate automatically");
  item("diagnose","check my permissions, Termux and Shizuku and tell you the exact fix");
  item("grant self","give SlimeDroid its permissions and switch its eyes back on");
  item("shizuku","test the Shizuku connection");
  item("autoheal on / off","re-enable its eyes after a reboot");
  item("vision model NAME","which AI looks at screenshots");
  item("sh COMMAND","run a command on the phone (Shizuku)");
  item("term COMMAND","run a command in Termux");

  head("AI suggestions");
  text("If you ask the slime to do something on the phone, it may suggest one action and wait. Reply yes or no.",13f,"#D6DBDF",false,2);
  setContentView(sv);
 }
 TextView text(String s,float size,String color,boolean bold,int topDp){
  TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.parseColor(color));
  if(bold) t.setTypeface(Typeface.DEFAULT_BOLD);
  t.setPadding(0,(int)(topDp*d),0,(int)(2*d)); col.addView(t); return t;
 }
 void head(String s){ text(s,16f,"#5DADE2",true,18); }
 void item(String cmd,String desc){
  TextView t=new TextView(this);
  t.setText(Html.fromHtml("<b>"+cmd+"</b><br/>"+desc));
  t.setTextSize(13f); t.setTextColor(Color.parseColor("#E5E8E8"));
  t.setPadding(0,(int)(6*d),0,(int)(2*d)); col.addView(t);
 }
}
