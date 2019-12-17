package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Modules.URL.URLs;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

/**
 * Wraps a side sub menu.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class SideSubMenuAnnotation extends SideSubMenu {
	final SideSubMenus meta;
	@Nonnull
	final Method method;
	@Nonnull
	final URL url;
	private final boolean generated;

	public SideSubMenuAnnotation(@Nonnull final Method m) throws UserException, SystemException {
		generated = false;
		meta = m.getAnnotation(SideSubMenus.class);
		method = m;
		url = Modules.getURL(null, m.getAnnotation(URLs.class).url());
	}

	@Nonnull
	public String name() { return meta.name(); }

	public int priority() { return meta.priority(); }

	@Nonnull
	public String requiresPermission() { return meta.requiresPermission(); }

	public boolean isGenerated() { return generated; }


	@Nonnull
	public String getURL() {
		return url.url();
	}

}
