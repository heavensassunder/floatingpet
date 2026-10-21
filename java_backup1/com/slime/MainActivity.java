package com.slime;
import android.app.Activity; import android.content.Intent; import android.content.SharedPreferences; import android.net.Uri; import android.os.Bundle; import android.provider.Settings; import android.view.View; import android.widget.*;
public class MainActivity extends Activity {
 @Override protected void onCreate(Bundle b){ super.onCreate(b);
  LinearLayout lay=new LinearLayout(this); lay.setOrientation(LinearLayout.VERTICAL); lay.setPadding(30,30,30,30);
  final EditText key=new EditText(this); key.setHint("OpenRouter API Key - get new one!");
  SharedPreferences sp=getSharedPreferences("slime",MODE_PRIVATE); key.setText(sp.getString("key",""));
  Button save=new Button(this); save.setText("Save + Start Slime FULL"); Button overlay=new Button(this); overlay.setText("1. Allow Overlay"); Button acc=new Button(this); acc.setText("2. Allow Accessibility EYES"); Button notif=new Button(this); notif.setText("3. Allow Notifications");
  TextView hint=new TextView(this); hint.setText("Features: 9 moods, chat, memory, see screen, tap, auto-roast, TTS, notifications. Revoke old key!");
  save.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ sp.edit().putString("key",key.getText().toString().trim()).apply(); startService(new Intent(MainActivity.this, MainService.class)); Toast.makeText(MainActivity.this,"Slime Full Started",1).show(); }});
  overlay.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()))); }});
  acc.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }});
  notif.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }});
  lay.addView(key); lay.addView(overlay); lay.addView(acc); lay.addView(notif); lay.addView(save); lay.addView(hint); setContentView(lay);
 }
}
