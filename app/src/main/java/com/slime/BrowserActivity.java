package com.slime;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import java.net.URLEncoder;
// A self-contained full-screen browser: address bar with Google search fallback, history
// navigation, reload, home (Google) and a progress bar. Everything stays inside the WebView.
public class BrowserActivity extends Activity {
 static final String HOME="https://www.google.com";
 WebView web; EditText addr; ProgressBar prog; Button backBtn,fwdBtn;
 @Override protected void onCreate(Bundle b){
  super.onCreate(b);
  final float d=getResources().getDisplayMetrics().density;
  setTitle("SlimeBrowser");
  LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.parseColor("#1B2631"));

  LinearLayout bar=new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL); bar.setGravity(Gravity.CENTER_VERTICAL);
  bar.setPadding((int)(6*d),(int)(6*d),(int)(6*d),(int)(6*d));
  backBtn=tool(d,"<"); fwdBtn=tool(d,">"); Button reload=tool(d,"\u21BB"); Button home=tool(d,"H");
  bar.addView(backBtn); bar.addView(fwdBtn); bar.addView(reload); bar.addView(home);
  addr=new EditText(this); addr.setSingleLine(true); addr.setTextSize(13f); addr.setTextColor(Color.WHITE);
  addr.setHintTextColor(Color.parseColor("#7F8C8D")); addr.setHint("Search Google or type a URL");
  addr.setBackground(round("#2C3E50",10f,d)); addr.setPadding((int)(10*d),(int)(7*d),(int)(10*d),(int)(7*d));
  addr.setImeOptions(EditorInfo.IME_ACTION_GO);
  LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f); alp.setMargins((int)(6*d),0,0,0); bar.addView(addr,alp);
  Button go=tool(d,"Go"); bar.addView(go);
  root.addView(bar);

  prog=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
  prog.setMax(100); prog.setVisibility(View.GONE);
  root.addView(prog,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,(int)(3*d)));

  web=new WebView(this);
  WebSettings st=web.getSettings();
  st.setJavaScriptEnabled(true); st.setDomStorageEnabled(true); st.setDatabaseEnabled(true);
  st.setLoadWithOverviewMode(true); st.setUseWideViewPort(true);
  st.setSupportZoom(true); st.setBuiltInZoomControls(true); st.setDisplayZoomControls(false);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){ return false; }
   @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap fav){ addr.setText(url); updateNav(); }
   @Override public void onPageFinished(WebView v,String url){ addr.setText(url); updateNav(); }
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public void onProgressChanged(WebView v,int p){
    prog.setProgress(p); prog.setVisibility(p>=100?View.GONE:View.VISIBLE);
   }
  });
  root.addView(web,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));
  setContentView(root);

  backBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ if(web.canGoBack()) web.goBack(); }});
  fwdBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ if(web.canGoForward()) web.goForward(); }});
  reload.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ web.reload(); }});
  home.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ web.loadUrl(HOME); }});
  go.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ submitAddress(); }});
  addr.setOnEditorActionListener(new TextView.OnEditorActionListener(){ public boolean onEditorAction(TextView v,int action,KeyEvent e){ submitAddress(); return true; }});

  String start=getIntent()==null?null:getIntent().getStringExtra("url");
  web.loadUrl(resolve(start==null?"":start));
 }
 Button tool(float d,String label){
  Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(13f); b.setTextColor(Color.WHITE);
  b.setMinWidth(0); b.setMinHeight(0); b.setMinimumWidth(0); b.setMinimumHeight(0); b.setPadding((int)(10*d),(int)(6*d),(int)(10*d),(int)(6*d));
  b.setBackground(round("#34495E",10f,d));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins((int)(3*d),0,(int)(3*d),0); b.setLayoutParams(lp);
  return b;
 }
 GradientDrawable round(String hex,float radiusDp,float d){ GradientDrawable g=new GradientDrawable(); g.setColor(Color.parseColor(hex)); g.setCornerRadius(radiusDp*d); return g; }
 void submitAddress(){
  String q=addr.getText().toString().trim();
  if(q.isEmpty()) return;
  try{ ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(addr.getWindowToken(),0); }catch(Exception e){ L.w("Browser",e); }
  web.loadUrl(resolve(q));
 }
 // bare domains get https://, anything else becomes a Google search
 String resolve(String input){
  String q=input.trim();
  if(q.isEmpty()) return HOME;
  if(q.startsWith("http://")||q.startsWith("https://")) return q;
  if(q.startsWith("about:")) return q;
  if(q.matches("(?i)^[a-z0-9-]+(\\.[a-z0-9-]+)+([/?#].*)?$")) return "https://"+q;
  try{ return "https://www.google.com/search?q="+URLEncoder.encode(q,"UTF-8"); }catch(Exception e){ return HOME; }
 }
 void updateNav(){
  backBtn.setEnabled(web.canGoBack()); fwdBtn.setEnabled(web.canGoForward());
  backBtn.setAlpha(web.canGoBack()?1f:0.4f); fwdBtn.setAlpha(web.canGoForward()?1f:0.4f);
 }
 @Override public void onBackPressed(){ if(web!=null&&web.canGoBack()) web.goBack(); else super.onBackPressed(); }
 @Override protected void onDestroy(){
  try{ if(web!=null){ ((ViewGroup)web.getParent()).removeView(web); web.destroy(); web=null; } }catch(Exception e){ L.w("Browser",e); }
  super.onDestroy();
 }
 static void open(android.content.Context c,String url){
  Intent i=new Intent(c,BrowserActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  if(url!=null&&!url.trim().isEmpty()) i.putExtra("url",url.trim());
  c.startActivity(i);
 }
}
