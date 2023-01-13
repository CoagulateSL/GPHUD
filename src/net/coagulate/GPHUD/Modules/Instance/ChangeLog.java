package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.ChangeLogging;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class ChangeLog {
	// ---------- STATICS ----------
	@URLs(url="/ChangeLog", requiresAuthentication=false)
	public static void index(@Nonnull final State st,final SafeMap values) {
		final Form f=st.form();
		f.noForm();
		f.add(ChangeLogging.asHtml("GPHUD"));
	}
	
	// ---------- INSTANCE ----------
	@Nullable
	public Form authenticationHook(final State st,final SafeMap values) {
		return null;
	}
	
}
