package net.coagulate.GPHUD.Modules.Publishing;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import java.util.TreeMap;

public class Status extends Publishing {
	@URL.URLs(url="/publishing/status")
	public static void statusSample(State st, SafeMap values) {
		st.form.add(new TextHeader("Instance Status"));
		published(st,"status/" + st.getInstance().getId());
	}

	@URL.URLs(url="/published/status/*",requiresAuthentication = false)
	public static void status(State st,SafeMap values) {
		status(st, values, false);
	}

	@URL.URLs(url="/publishing/statusfull")
	public static void statusFullSample(State st, SafeMap values) {
		st.form.add(new TextHeader("Instance Status"));
		published(st,"statusfull/" + st.getInstance().getId());
	}

	@URL.URLs(url="/published/statusfull/*",requiresAuthentication = false)
	public static void statusFull(State st,SafeMap values) {
		status(st, values, true);
	}

	private static void status(State st,SafeMap values,boolean listusers) {
		Instance instance = Instance.get(getPartInt(st, 1));
		st.setInstance(instance);
		st.form.add("<b>"+instance.getName()+"</b> <i>GPHUD</i><br><br>");
		String line="Regions: ";
		boolean first=true;
		int players=0;
		TreeMap<String,String> playerlist=new TreeMap<>();
		for(Region r:instance.getRegions(false)) {
			String statuscolor="#c00000";
			if (r.getOnlineStatus("Europe/London").toLowerCase().startsWith("online")) { statuscolor="#00c000"; }
			if (!first) { line+=", "; } else { first=false; }
			line+="<font color=\""+statuscolor+"\">"+r.getName()+"</font>";
			players=r.getOpenVisitCount();
			for (Char c:r.getOpenVisits()) {
				playerlist.put(c.getOwner().getName(),c.getOwner().getName()+" <i>as</i> "+c.getName()+"<br>");
			}
		}
		st.form.add(line+"<br>");
		st.form.add(players+" players online.<br>");
		if (listusers) {
			for (String s : playerlist.values()) {
				st.form.add(s);
			}
		}
		contentResizer(st);
	}
}
