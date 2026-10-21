package com.slime;
import java.util.Random;
public class Lines {
 static Random r=new Random();
 public static String pick(String[] a){ return a[r.nextInt(a.length)]; }
 public static final String[] IDLE={"just vibing on your screen","this phone is a cozy home","still here, still blue","tell me something interesting","ngl I'm a little bored","you good?","want me to open something?","psst... hi","I wonder what's on your mind"};
 public static final String[] MORNING={"morning! ready for today?","good morning, bestie","rise and shine, sort of","new day, still blue though"};
 public static final String[] AFTERNOON={"hope your day's going okay","water break? just saying","afternoon slump is real","how's the day treating you?"};
 public static final String[] EVENING={"evening! how was today?","long day? you can tell me","time to relax a bit?","did you eat today?"};
 public static final String[] LATE={"it's getting late, sleep soon?","night owl mode, huh","don't stay up too late"};
 public static final String[] HUNGRY={"my tummy is empty... charger?","feed me? plug me in or type 'feed'","so hungry...","a snack would be amazing"};
 public static final String[] SAD={"psst, you forgot about me","I'm lonely, talk to me?","tap me? pls","hello? anyone there?"};
 public static final String[] TIRED={"so sleepy...","I need a nap","my eyes are drooping"};
 public static final String[] LOVE={"I like you, you know","best human ever","you're my favorite person"};
 public static final String[] SHAKE={"Dizzy!","whoa whoa whoa!","stop shaking me!","I'm gonna be sick"};
 public static final String[] PLUG={"Yum! power!","mmm tasty electricity","charging time!"};
 public static final String[] UNPLUG={"aw, snack time's over","unplugged! I'll manage","bye bye charger"};
 public static final String[] HEAD_ON={"ooh, music time?","headphones? nice","vibes incoming"};
 public static final String[] HEAD_OFF={"silence...","headphones out"};
 public static final String[] BACK={"welcome back!","oh hi, I missed you","you're back!"};
 public static String byTime(int hr){
  if(hr>=5&&hr<12) return pick(MORNING);
  if(hr<17) return pick(AFTERNOON);
  if(hr<22) return pick(EVENING);
  return pick(LATE);
 }
}
