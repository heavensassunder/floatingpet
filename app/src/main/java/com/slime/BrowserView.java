package com.slime;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.net.URLEncoder;
import java.util.TreeSet;
// A compact Via-style browser that lives INSIDE the overlay system, like the chat box:
// slim top toolbar, address bar with Google search fallback, and a WebView underneath.
// It floats over other apps without leaving them and never takes the full screen.
// Remembers the last page (resumes on reopen, X only hides) and keeps bookmarks.
class BrowserView extends LinearLayout {
 static final String HOME="https://www.google.com";
 static final String PREF_LAST_URL="browser_last_url";
 static final String PREF_BOOKMARKS="browser_bookmarks";
 final MainService s; WindowManager.LayoutParams lp;
 WebView web; EditText addr; ProgressBar prog; Button backBtn,fwdBtn,star; LinearLayout toolbar;
 TreeSet<String> bookmarks=new TreeSet<>();
 BrowserView(MainService s){
  super(s); this.s=s;
  float d=s.getResources().getDisplayMetrics().density;
  setOrientation(VERTICAL);
  GradientDrawable panel=new GradientDrawable(); panel.setColor(Color.parseColor("#F4F6F7")); panel.setCornerRadius(16*d); setBackground(panel);
  setPadding((int)(6*d),(int)(6*d),(int)(6*d),(int)(6*d));

  toolbar=new LinearLayout(s); toolbar.setOrientation(HORIZONTAL); toolbar.setGravity(Gravity.CENTER_VERTICAL);
  backBtn=tool(d,"<"); fwdBtn=tool(d,">"); Button reload=tool(d,"\u21BB"); Button home=tool(d,"H"); Button close=tool(d,"X");
  close.setBackground(round("#E74C3C",10f,d));
  toolbar.addView(backBtn); toolbar.addView(fwdBtn); toolbar.addView(reload); toolbar.addView(home);
  star=tool(d,"☆"); star.setTag(0); toolbar.addView(star);
  addr=new EditText(s); addr.setSingleLine(true); addr.setTextSize(12f); addr.setTextColor(Color.parseColor("#1B2631"));
  addr.setHintTextColor(Color.parseColor("#7F8C8D")); addr.setHint("Search Google or type URL");
  addr.setBackground(round("#FFFFFF",10f,d)); addr.setPadding((int)(10*d),(int)(6*d),(int)(10*d),(int)(6*d));
  addr.setImeOptions(EditorInfo.IME_ACTION_GO);
  LayoutParams alp=new LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f); alp.setMargins((int)(5*d),0,(int)(5*d),0); toolbar.addView(addr,alp);
  Button go=tool(d,"Go"); toolbar.addView(go); toolbar.addView(close);
  addView(toolbar,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));

  prog=new ProgressBar(s,null,android.R.attr.progressBarStyleHorizontal); prog.setMax(100); prog.setVisibility(GONE);
  addView(prog,new LayoutParams(LayoutParams.MATCH_PARENT,(int)(3*d)));

  web=new WebView(s); web.setBackgroundColor(Color.WHITE);
  WebSettings st=web.getSettings();
  st.setJavaScriptEnabled(true); st.setDomStorageEnabled(true); st.setDatabaseEnabled(true);
  st.setUseWideViewPort(false);
  st.setLoadWithOverviewMode(true);
  try{ st.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS); }catch(Exception e){ L.w("BrowserView","layout algo",e); }
  st.setSupportZoom(true); st.setBuiltInZoomControls(true); st.setDisplayZoomControls(false);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){ return false; }
   @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap fav){ addr.setText(url); updateNav(); }
   @Override public void onPageFinished(WebView v,String url){ addr.setText(url); updateNav(); saveLastUrl(url); refreshStar(); }
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public void onProgressChanged(WebView v,int p){ prog.setProgress(p); prog.setVisibility(p>=100?GONE:VISIBLE); }
  });
  addView(web,new LayoutParams(LayoutParams.MATCH_PARENT,0,1f));

  backBtn.setOnClickListener(new OnClickListener(){ public void onClick(View v){ if(web.canGoBack()) web.goBack(); }});
  fwdBtn.setOnClickListener(new OnClickListener(){ public void onClick(View v){ if(web.canGoForward()) web.goForward(); }});
  reload.setOnClickListener(new OnClickListener(){ public void onClick(View v){ web.reload(); }});
  home.setOnClickListener(new OnClickListener(){ public void onClick(View v){ web.loadUrl(HOME); }});
  close.setOnClickListener(new OnClickListener(){ public void onClick(View v){ s.closeBrowser(); }});
  go.setOnClickListener(new OnClickListener(){ public void onClick(View v){ submit(); }});
  addr.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener(){ public boolean onEditorAction(android.widget.TextView v,int action,KeyEvent e){ submit(); return true; }});
  enableDrag(d);
  loadBookmarks();
  star.setOnClickListener(new OnClickListener(){ public void onClick(View v){ toggleBookmark(web.getUrl()); } });
  star.setOnLongClickListener(new OnLongClickListener(){ public boolean onLongClick(View v){ showBookmarks(); return true; } });
 }
 Button tool(float d,String label){
  Button b=new Button(s); b.setText(label); b.setAllCaps(false); b.setTextSize(12f); b.setTextColor(Color.WHITE);
  b.setMinWidth(0); b.setMinHeight(0); b.setMinimumWidth(0); b.setMinimumHeight(0); b.setPadding((int)(8*d),(int)(5*d),(int)(8*d),(int)(5*d));
  b.setBackground(round("#5D6D7E",10f,d));
  LayoutParams lp=new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT); lp.setMargins((int)(2*d),0,(int)(2*d),0); b.setLayoutParams(lp);
  return b;
 }
 GradientDrawable round(String hex,float radiusDp,float d){ GradientDrawable g=new GradientDrawable(); g.setColor(Color.parseColor(hex)); g.setCornerRadius(radiusDp*d); return g; }
 void enableDrag(float d){
  toolbar.setOnTouchListener(new OnTouchListener(){
   float dx,dy; boolean moved=false;
   public boolean onTouch(View v,MotionEvent e){
    if(lp==null) return false;
    switch(e.getAction()){
     case MotionEvent.ACTION_DOWN: dx=lp.x-e.getRawX(); dy=lp.y-e.getRawY(); moved=false; return true;
     case MotionEvent.ACTION_MOVE:
      if(Math.abs(e.getRawX()+dx-lp.x)+Math.abs(e.getRawY()+dy-lp.y)>8) moved=true;
      if(moved){ lp.x=(int)(e.getRawX()+dx); lp.y=(int)(e.getRawY()+dy); clamp(); try{ s.wm.updateViewLayout(BrowserView.this,lp); }catch(Exception ex){ L.w("BrowserView",ex); } }
      return true;
     case MotionEvent.ACTION_UP: return moved;
    }
    return false;
   }
  });
 }
 void clamp(){
  lp.x=Math.max(0,Math.min(lp.x,s.scrW()-lp.width));
  lp.y=Math.max(0,Math.min(lp.y,s.scrH()-lp.height));
 }
 void submit(){
  String q=addr.getText().toString().trim();
  if(q.isEmpty()) return;
  try{ ((InputMethodManager)s.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(addr.getWindowToken(),0); }catch(Exception e){ L.w("BrowserView",e); }
  web.loadUrl(resolve(q));
 }
 String resolve(String input){
  String q=input.trim();
  if(q.isEmpty()) return HOME;
  if(q.startsWith("http://")||q.startsWith("https://")||q.startsWith("about:")) return q;
  if(q.matches("(?i)^[a-z0-9-]+(\\.[a-z0-9-]+)+([/?#].*)?$")) return "https://"+q;
  try{ return "https://www.google.com/search?q="+URLEncoder.encode(q,"UTF-8"); }catch(Exception e){ return HOME; }
 }
 void updateNav(){
  backBtn.setEnabled(web.canGoBack()); fwdBtn.setEnabled(web.canGoForward());
  backBtn.setAlpha(web.canGoBack()?1f:0.4f); fwdBtn.setAlpha(web.canGoForward()?1f:0.4f);
 }
 void open(String url){
  String u=url==null?"":url.trim();
  if(u.isEmpty()){ u=loadLastUrl(); if(u==null||u.isEmpty()) u=HOME; }
  web.loadUrl(resolve(u));
 }
 boolean goBack(){ if(web.canGoBack()){ web.goBack(); return true; } return false; }
 void destroy(){ try{ ((ViewGroup)web.getParent()).removeView(web); web.destroy(); }catch(Exception e){ L.w("BrowserView",e); } }
 void saveLastUrl(String u){ if(u==null)return; s.sp.edit().putString(PREF_LAST_URL,u).apply(); }
 String loadLastUrl(){ return s.sp.getString(PREF_LAST_URL,null); }
 void loadBookmarks(){ bookmarks.clear(); bookmarks.addAll(s.sp.getStringSet(PREF_BOOKMARKS,new TreeSet<String>())); }
 void persistBookmarks(){ s.sp.edit().putStringSet(PREF_BOOKMARKS,bookmarks).apply(); }
 boolean isBookmarked(String u){ return u!=null&&bookmarks.contains(u); }
 void refreshStar(){ star.setText(isBookmarked(web.getUrl())?"★":"☆"); }
 void toggleBookmark(String u){
  if(u==null||u.isEmpty()||u.equals("about:blank"))return;
  if(bookmarks.contains(u)){ bookmarks.remove(u); } else { bookmarks.add(u); }
  persistBookmarks(); refreshStar();
 }
 void showBookmarks(){
  if(bookmarks.isEmpty()){ Toast.makeText(s,"no bookmarks yet",Toast.LENGTH_SHORT).show(); return; }
  final String[] items=bookmarks.toArray(new String[0]);
  new AlertDialog.Builder(s).setTitle("Bookmarks").setItems(items,new DialogInterface.OnClickListener(){
   public void onClick(DialogInterface d,int which){ web.loadUrl(items[which]); }
  }).show();
 }
}
