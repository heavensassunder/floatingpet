package com.slime;
import android.content.SharedPreferences;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;
// Open-Meteo weather: free, no API key. Geocodes the saved city once, caches its coordinates,
// then reads the current conditions and today's min/max for the morning briefing and 'weather'.
class Weather {
 static final String DEFAULT_CITY="Nairobi";
 static final String[] CODES={"clear sky","mainly clear","partly cloudy","overcast","fog","drizzle","drizzle","drizzle",
  "drizzle","drizzle","light rain","rain","heavy rain","showers","light showers","heavy showers",
  "light snow","snow","heavy snow","snow grains","snow showers","snow showers","snow showers","snow showers",
  "freezing rain","freezing rain","freezing rain","freezing rain","freezing rain","freezing rain",
  "thunderstorm","thunderstorm","thunderstorm with hail","thunderstorm with hail","thunderstorm with hail"};

 static String desc(int code){
  if(code<0||code>=CODES.length) return "mixed skies";
  if(code>=51&&code<=57) return "drizzle";
  if(code>=61&&code<=67) return code==65?"heavy rain":"rain";
  if(code>=71&&code<=77) return "snow";
  if(code>=80&&code<=82) return "rain showers";
  if(code>=85&&code<=86) return "snow showers";
  if(code>=95) return "thunderstorm";
  return CODES[code];
 }
 static String read(String u) throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();
  c.setConnectTimeout(12000); c.setReadTimeout(15000);
  java.io.ByteArrayOutputStream bo=new java.io.ByteArrayOutputStream();
  java.io.InputStream is=c.getInputStream(); byte[] b=new byte[4096]; int n;
  while((n=is.read(b))>0) bo.write(b,0,n); is.close();
  return bo.toString("UTF-8");
 }
 static synchronized boolean geocode(SharedPreferences sp,String city) throws Exception{
  String u="https://geocoding-api.open-meteo.com/v1/search?count=1&format=json&name="+URLEncoder.encode(city,"UTF-8");
  JSONObject j=new JSONObject(read(u)); JSONArray r=j.optJSONArray("results");
  if(r==null||r.length()==0) return false;
  JSONObject o=r.getJSONObject(0);
  sp.edit().putString("weatherCity",o.optString("name",city)).putFloat("weatherLat",(float)o.getDouble("latitude"))
   .putFloat("weatherLon",(float)o.getDouble("longitude")).apply();
  return true;
 }
 // blocking; call off the main thread. Returns one spoken-length line.
 static String now(SharedPreferences sp){
  try{
   String city=sp.getString("weatherCity",DEFAULT_CITY);
   if(sp.getFloat("weatherLat",0f)==0f){ if(!geocode(sp,city)) return "I can't find "+city+" on the weather map - try 'weather city ANOTHERNAME'"; }
   float lat=sp.getFloat("weatherLat",0f), lon=sp.getFloat("weatherLon",0f);
   String u="https://api.open-meteo.com/v1/forecast?latitude="+lat+"&longitude="+lon
    +"&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,weather_code&forecast_days=1&timezone=auto";
   JSONObject j=new JSONObject(read(u));
   JSONObject cur=j.getJSONObject("current"); JSONObject daily=j.getJSONObject("daily");
   int code=cur.optInt("weather_code",0);
   return sp.getString("weatherCity",city)+": "+desc(code)+", "+Math.round(cur.optDouble("temperature_2m"))
    +"C (feels like "+Math.round(cur.optDouble("apparent_temperature"))+"), today "+Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0))
    +" to "+Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0))+"C, wind "+Math.round(cur.optDouble("wind_speed_10m"))+" km/h";
  }catch(Exception e){ L.w("Weather",e); return ""; }
 }
 static void showNow(final MainService s){
  s.local("checking the sky...",false);
  new Thread(new Runnable(){ public void run(){
   final String r=now(s.sp);
   s.h.post(new Runnable(){ public void run(){ s.speak(r.isEmpty()?"weather service is unreachable right now":r); }});
  }}).start();
 }
 static void setCity(final MainService s,final String city){
  new Thread(new Runnable(){ public void run(){
   String r;
   try{ r=geocode(s.sp,city)?("weather city set to "+s.sp.getString("weatherCity",city)+" - say 'weather' to check it"):"no place called "+city+" found, check the spelling"; }
   catch(Exception e){ r="weather lookup failed: "+e.getMessage(); }
   final String fr=r; s.h.post(new Runnable(){ public void run(){ s.local(fr,false); }});
  }}).start();
 }
}
