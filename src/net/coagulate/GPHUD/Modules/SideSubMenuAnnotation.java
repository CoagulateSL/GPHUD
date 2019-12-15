package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Modules.URL.URLs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
	@Nullable
	final URL url;
	private boolean generated = true;

	public SideSubMenuAnnotation(@Nonnull Method m) throws UserException, SystemException {
		generated = false;
		this.meta = m.getAnnotation(SideSubMenus.class);
		method = m;
		url = Modules.getURL(null, m.getAnnotation(URLs.class).url());
	}

	@Nonnull
	public String name() { return meta.name(); }

	public int priority() { return meta.priority(); }

	@Nonnull
	public String requiresPermission() { return meta.requiresPermission(); }

	public boolean isGenerated() { return generated; }


	public String getURL() {
		return url.url();
	}

}
