package net.coagulate.GPHUD.Modules.Audit;

import net.coagulate.Core.Database.Results;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
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

	@URLs(url = "/audit", requiresPermission = "audit.view")
	public static void audit(@Nonnull final State st, final SafeMap values) {
		final Results rows = net.coagulate.GPHUD.Data.Audit.getAudit(st.getInstance(), null, null);
		final Table table = net.coagulate.GPHUD.Data.Audit.formatAudit(rows, st.getAvatar().getTimeZone());
		st.form().add(table);
	}


}
