package net.coagulate.GPHUD.Modules.Audit;

import net.coagulate.GPHUD.Data.Views.AuditTable;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * AuditView page.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AuditView {
	
	// ---------- STATICS ----------
	@URLs(url="/audit", requiresPermission="audit.view")
	public static void audit(@Nonnull final State st,final SafeMap values) {
		final AuditTable table=new AuditTable(st,"audit",values);
		st.form().add(table);
	}
	
	
}
