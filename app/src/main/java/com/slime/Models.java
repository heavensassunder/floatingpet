package com.slime;
import java.util.ArrayList;
public class Models {
 static void list(final MainService s){
  s.local("looking up free models...",false);
  FreeModels.refresh(s.sp,new FreeModels.Done(){ public void done(boolean ok){
   ArrayList<String> free=FreeModels.chat(s.sp), see=FreeModels.vision(s.sp);
   if(!ok&&free.isEmpty()){ s.local("couldn't fetch the list right now. I'll keep using openrouter/free until the network is back.",false); return; }
   StringBuilder sb=new StringBuilder("free models right now ("+free.size()+", "+see.size()+" can see images):\n");
   for(int i=0;i<Math.min(25,free.size());i++) sb.append(free.get(i)).append('\n');
   if(free.size()>25) sb.append("...and ").append(free.size()-25).append(" more\n");
   sb.append("\nI rotate through these automatically. Pin one with: model NAME (model reset to go back). Last reply came from: "+FreeModels.last(s.sp));
   s.showChat(); s.local(sb.toString(),false);
  }});
 }
}
