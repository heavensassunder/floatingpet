package com.slime;
import android.app.Activity; import android.content.Intent; import android.content.SharedPreferences; import android.graphics.Color; import android.graphics.Typeface; import android.graphics.drawable.GradientDrawable;
import android.net.Uri; import android.os.Bundle; import android.os.PowerManager; import android.provider.Settings; import android.view.Gravity; import android.view.View; import android.view.ViewGroup;
import android.widget.Button; import android.widget.EditText; import android.widget.LinearLayout; import android.widget.ScrollView; import android.widget.TextView; import android.widget.Toast;
import java.util.ArrayList;
public class MainActivity extends Activity {
 SharedPreferences sp; LinearLayout col, keyList; TextView status; EditText keyIn; float d;
 @Override protected void onCreate(Bundle b){
  super.onCreate(b);
  sp=getSharedPreferences("slime",MODE_PRIVATE); d=getResources().getDisplayMetrics().density;
  int pad=(int)(16*d);
  ScrollView sv=new ScrollView(this); sv.setBackgroundColor(Color.parseColor("#1B2631"));
  col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL); col.setPadding(pad,pad,pad,pad*2);
  sv.addView(col,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

  text("SlimeDroid",26f,"#FFFFFF",true,0);
  text("Your little blue slime that lives on your phone. New here? Read the guide first.",13f,"#AEB6BF",false,2);
  btn("Read the guide (start here)","#AF7AC5",new Runnable(){ public void run(){ startActivity(new Intent(MainActivity.this,GuideActivity.class)); }});
  status=text("",13f,"#E5E8E8",false,12);

  head("Step 1: allow the basics");
  note("Tap each button, then switch SlimeDroid on in the screen that opens. Come back here and the checklist above updates.");
  btn("1. Allow overlay (so I can float)","#5DADE2",new Runnable(){ public void run(){ open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))); }});
  btn("2. Allow accessibility (my eyes and hands)","#5DADE2",new Runnable(){ public void run(){ open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }});
  btn("3. Allow notifications (so I can see pings)","#5DADE2",new Runnable(){ public void run(){ open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }});
  btn("4. Allow battery unrestricted (so I stay alive)","#5DADE2",new Runnable(){ public void run(){
   try{ open(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName()))); }
   catch(Exception e){ open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); }
  }});

  head("Step 2: OpenRouter keys (free)");
  note("Get a free key at openrouter.ai/keys. You can add more than one: when a key runs out I switch to the next.");
  btn("Open openrouter.ai/keys","#48C9B0",new Runnable(){ public void run(){ open(new Intent(Intent.ACTION_VIEW,Uri.parse("https://openrouter.ai/keys"))); }});
  keyList=new LinearLayout(this); keyList.setOrientation(LinearLayout.VERTICAL); keyList.setPadding(0,(int)(8*d),0,0); col.addView(keyList);
  keyIn=new EditText(this); keyIn.setHint("paste one or more keys here"); keyIn.setTextColor(Color.WHITE); keyIn.setHintTextColor(Color.parseColor("#7F8C8D")); keyIn.setTextSize(13f);
  keyIn.setMinLines(2); keyIn.setGravity(Gravity.TOP);
  col.addView(keyIn,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
  btn("Add key(s)","#5DADE2",new Runnable(){ public void run(){ addKeys(); }});

  head("Step 3: wake me up");
  btn("Start slime","#E67E22",new Runnable(){ public void run(){ addKeys(); startService(new Intent(MainActivity.this,MainService.class)); Toast.makeText(MainActivity.this,"Slime started. Long-press it to chat.",Toast.LENGTH_SHORT).show(); refresh(); }});
  btn("Stop slime","#85929E",new Runnable(){ public void run(){ stopService(new Intent(MainActivity.this,MainService.class)); refresh(); }});

  head("Learn more");
  btn("Read the guide","#AF7AC5",new Runnable(){ public void run(){ startActivity(new Intent(MainActivity.this,GuideActivity.class)); }});
  btn("All commands","#5D6D7E",new Runnable(){ public void run(){ startActivity(new Intent(MainActivity.this,CommandsActivity.class)); }});

  head("Phone control helpers (optional)");
  note("For commands that use Shizuku and Termux. Termux must be allowed to run in the background: set its battery to unrestricted and read guide section 14.");
  btn("Termux app settings","#5D6D7E",new Runnable(){ public void run(){ open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:com.termux"))); }});
  btn("Battery optimisation list","#5D6D7E",new Runnable(){ public void run(){ open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); }});

  setContentView(sv);
 }
 @Override protected void onResume(){ super.onResume(); refresh(); }
 void open(Intent i){ try{ startActivity(i); }catch(Exception e){ Toast.makeText(this,"Couldn't open that screen",Toast.LENGTH_SHORT).show(); } }
 void addKeys(){
  try{
   String txt=keyIn.getText().toString();
   if(txt.trim().length()>0){
    int n=Keys.addText(sp,txt);
    Toast.makeText(this,n>0?"Added "+n+" key"+(n>1?"s":""):"That doesn't look like a key (or it is already saved)",Toast.LENGTH_SHORT).show();
    keyIn.setText("");
   }
  }catch(Exception e){}
  refresh();
 }
 String tick(boolean ok){ return ok?"\u2713 ":"\u2717 "; }
 void refresh(){
  try{
   StringBuilder sb=new StringBuilder();
   boolean ov=true; try{ ov=Settings.canDrawOverlays(this); }catch(Exception e){}
   sb.append(tick(ov)).append("overlay permission\n");
   sb.append(tick(EyeService.instance!=null)).append("accessibility (my eyes)\n");
   String nl=null; try{ nl=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners"); }catch(Exception e){}
   sb.append(tick(nl!=null&&nl.contains(getPackageName()))).append("notification access\n");
   boolean bat=false; try{ bat=((PowerManager)getSystemService(POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName()); }catch(Exception e){}
   sb.append(tick(bat)).append("battery unrestricted\n");
   int nk=Keys.all(sp).size();
   sb.append(tick(nk>0)).append("OpenRouter key saved (").append(nk).append(")\n");
   sb.append(tick(MainService.instance!=null)).append("slime is running\n");
   boolean term=false; try{ getPackageManager().getPackageInfo("com.termux",0); term=true; }catch(Exception e){}
   sb.append(tick(term)).append("Termux installed (only needed for phone control)");
   status.setText(sb.toString());
   rebuildKeys();
  }catch(Exception e){}
 }
 void rebuildKeys(){
  keyList.removeAllViews();
  ArrayList<String> ks=Keys.all(sp);
  if(ks.isEmpty()){ TextView t=new TextView(this); t.setText("No key yet. Add one below."); t.setTextColor(Color.parseColor("#F5B041")); t.setTextSize(13f); keyList.addView(t); return; }
  for(int i=0;i<ks.size();i++){
   final int idx=i; String k=ks.get(i);
   LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
   TextView tv=new TextView(this); tv.setTextSize(12.5f); tv.setTextColor(Keys.alive(sp,k)?Color.parseColor("#E5E8E8"):Color.parseColor("#E67E22"));
   tv.setText((i+1)+". "+Keys.mask(k)+"\n"+(Keys.alive(sp,k)?"ok":"used up / not working today")+", "+Keys.usedFor(sp,k)+" requests today");
   Button rm=new Button(this); rm.setText("Remove"); rm.setAllCaps(false); rm.setTextSize(11f); rm.setTextColor(Color.WHITE);
   GradientDrawable g=new GradientDrawable(); g.setColor(Color.parseColor("#E74C3C")); g.setCornerRadius(10*d); rm.setBackground(g); rm.setPadding((int)(10*d),(int)(4*d),(int)(10*d),(int)(4*d));
   rm.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ ArrayList<String> all=Keys.all(sp); if(idx<all.size()){ all.remove(idx); Keys.setAll(sp,all); } refresh(); }});
   row.addView(tv,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f)); row.addView(rm);
   keyList.addView(row);
  }
 }
 TextView text(String s,float size,String color,boolean bold,int topDp){
  TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.parseColor(color));
  if(bold) t.setTypeface(Typeface.DEFAULT_BOLD);
  t.setPadding(0,(int)(topDp*d),0,(int)(2*d)); col.addView(t); return t;
 }
 void head(String s){ text(s,16f,"#5DADE2",true,18); }
 void note(String s){ text(s,12.5f,"#AEB6BF",false,2); }
 void btn(String label,String color,final Runnable r){
  Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(13f); b.setTextColor(Color.WHITE);
  GradientDrawable g=new GradientDrawable(); g.setColor(Color.parseColor(color)); g.setCornerRadius(12*d); b.setBackground(g);
  b.setPadding((int)(14*d),(int)(8*d),(int)(14*d),(int)(8*d));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(0,(int)(8*d),0,0); b.setLayoutParams(lp);
  b.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ r.run(); }});
  col.addView(b);
 }
}
