package net.coagulate.GPHUD.Modules.Audit;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

/**
 * AuditView page.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AuditView {

	@URLs(url = "/audit", requiresPermission = "audit.view")
	public static void audit(State st, SafeMap values) throws UserException {
		Results rows = net.coagulate.GPHUD.Data.Audit.getAudit(st.getInstance(), null, null);
		Table table = net.coagulate.GPHUD.Data.Audit.formatAudit(rows, st.avatar().getTimeZone());
		st.form.add(table);
	}


}
