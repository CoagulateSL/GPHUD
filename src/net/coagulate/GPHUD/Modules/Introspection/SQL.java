package net.coagulate.GPHUD.Modules.Introspection;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.SideSubMenu;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Introspect the SQL audit logs.
 * For superadmin only :P
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class SQL {
	// ---------- STATICS ----------
	@URLs(url="/introspection/sql", requiresPermission="User.SuperAdmin")
	@SideSubMenu.SideSubMenus(name="SQL", priority=9999, requiresPermission="User.SuperAdmin")
	public static void sqlIndex(@Nonnull final State st,final SafeMap values) {
		if (!DBConnection.sqlLogging()) {
			st.form().add(new TextError("SQL auditing is disabled in this installation."));
			return;
		}
		
		if (!st.isSuperUser()) {
			st.form().add(new TextError("You are not permitted to view this information"));
			return;
		}
		
		final Map<String,Integer> count=new HashMap<>();
		final Map<String,Long> time=new HashMap<>();
		final Map<String,Double> per=new HashMap<>();
		
		final long minutes=Duration.between(GPHUD.getDB().getSqlLogs(count,time,per),Instant.now())
		                           .dividedBy(Duration.ofMinutes(1));
		
		
		final Map<Integer,Set<Row>> bycount=new TreeMap<>(Collections.reverseOrder());
		final Map<Long,Set<Row>> bytime=new TreeMap<>(Collections.reverseOrder());
		final Map<Double,Set<Row>> byper=new TreeMap<>(Collections.reverseOrder());
		
		for (final Map.Entry<String,Integer> entry: count.entrySet()) {
			final String sql=entry.getKey();
			final int c=entry.getValue();
			final Long t=time.get(sql);
			final Row newrow=new Row();
			newrow.add(c);
			if (minutes>0) {
				newrow.add(Integer.toString(Math.round(c/minutes)));
			} else { newrow.add("-"); }
			newrow.add(sql);
			if (t==null) {
				newrow.add("-");
			} else {
				newrow.add(t+"ms");
			}
			if (c>0&&t!=null) {
				newrow.add(t/c+"ms");
			}
			Set<Row> rowset=new HashSet<>();
			if (bycount.containsKey(c)) {
				rowset=bycount.get(c);
			}
			rowset.add(newrow);
			bycount.put(c,rowset);
			if (t!=null) {
				rowset=new HashSet<>();
				if (bytime.containsKey(t)) {
					rowset=bytime.get(t);
				}
				rowset.add(newrow);
				bytime.put(t,rowset);
				final double avg=((double)t)/c;
				rowset=new HashSet<>();
				if (byper.containsKey(avg)) {
					rowset=byper.get(avg);
				}
				rowset.add(newrow);
				byper.put(avg,rowset);
			}
		}
		
		
		final Form f=st.form();
		f.add(new TextSubHeader("By per call execution time"));
		Table t=new Table();
		f.add(t);
		t.add(new HeaderRow().add("Count").add("C/Min").add("Query").add("Total time").add("Avg time"));
		for (final Set<Row> set: byper.values()) {
			for (final Row r: set) {
				t.add(r);
			}
		}
		f.add(new TextSubHeader("By total execution time"));
		t=new Table();
		f.add(t);
		t.add(new HeaderRow().add("Count").add("C/Min").add("Query").add("Total time").add("Avg time"));
		for (final Set<Row> set: bytime.values()) {
			for (final Row r: set) {
				t.add(r);
			}
		}
		f.add(new TextSubHeader("By execution count"));
		t=new Table();
		f.add(t);
		t.add(new HeaderRow().add("Count").add("C/Min").add("Query").add("Total time").add("Avg time"));
		for (final Set<Row> set: bycount.values()) {
			for (final Row r: set) {
				t.add(r);
			}
		}
		
		
	}
}
