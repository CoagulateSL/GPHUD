package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Modules.URL.URLs;

import java.lang.reflect.Method;

/**
 * Wraps a side sub menu.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class SideSubMenuAnnotation extends SideSubMenu {
	SideSubMenus meta;
	Method method;
	URL url;
	private boolean generated = true;

	public SideSubMenuAnnotation(Method m) throws UserException, SystemException {
		generated = false;
		this.meta = m.getAnnotation(SideSubMenus.class);
		method = m;
		url = Modules.getURL(null, ((URLs) m.getAnnotation(URLs.class)).url());
	}

	public String name() { return meta.name(); }

	public int priority() { return meta.priority(); }

	public String requiresPermission() { return meta.requiresPermission(); }

	public boolean isGenerated() { return generated; }


	public String getURL() {
		return url.url();
	}

}
