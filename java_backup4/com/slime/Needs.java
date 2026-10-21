package com.slime;
import android.content.SharedPreferences;
public class Needs {
 public float food, energy, joy; long last; boolean first=true; SharedPreferences sp;
 public Needs(SharedPreferences s){ sp=s; food=s.getFloat("n_food",80f); energy=s.getFloat("n_energy",80f); joy=s.getFloat("n_joy",80f); last=s.getLong("n_last",System.currentTimeMillis()); }
 static float cl(float v){ return v<0f?0f:(v>100f?100f:v); }
 public void tick(boolean asleep,boolean charging){
  long now=System.currentTimeMillis(); float min=(now-last)/60000f; last=now;
  if(min<=0f) return;
  float cap=first?120f:240f; first=false; if(min>cap) min=cap;
  if(asleep){ food-=0.05f*min; joy-=0.03f*min; energy+=0.5f*min; }
  else { food-=0.12f*min; joy-=0.10f*min; energy-=0.08f*min; }
  if(charging){ food+=1.2f*min; energy+=0.4f*min; }
  food=cl(food); joy=cl(joy); energy=cl(energy); save();
 }
 public int mood(boolean asleep){
  if(asleep||energy<12f) return 1;
  if(food<20f) return 4;
  if(joy<20f) return 5;
  if(joy>90f&&food>50f) return 3;
  return 0;
 }
 public void feed(float v){ food=cl(food+v); joy=cl(joy+3f); save(); }
 public void cheer(float v){ joy=cl(joy+v); save(); }
 public String describe(){ return "food "+(int)food+", energy "+(int)energy+", joy "+(int)joy; }
 void save(){ sp.edit().putFloat("n_food",food).putFloat("n_energy",energy).putFloat("n_joy",joy).putLong("n_last",last).apply(); }
}
