package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * Because we need an index for the User Interface.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class HelpPage {
	// ---------- STATICS ----------
	@URLs(url="/help",
	      requiresAuthentication=false)
	public static void index(@Nonnull final State st,
	                         final SafeMap values) {
		final Form f=st.form();
		f.noForm();
		f.add("<table width=100% height=100%><tr width=100% height=100%><td width=100% height=100% valign=top><iframe width=100% height=100% frameborder=0 "+"src=\"https"+"://sl.coagulate.net/Docs/GPHUD/index.php/Main_Page.html\"></iframe></td></tr></table>");
	}

	// ---------- INSTANCE ----------
	@Nullable
	public Form authenticationHook(final State st,
	                               final SafeMap values) { return null; }

}
