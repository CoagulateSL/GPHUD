package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * fake url for side sub menus (ugly).
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ConfigurationURL extends URL {

	final String name;
	final String url;

	public ConfigurationURL(final String name,
	                        final String url) {
		this.name=name;
		this.url=url;
	}

	// ---------- INSTANCE ----------
	@Override
	public boolean isGenerated() {
		return true;
	}

	@Override
	public String url() {
		return url;
	}

	@Nonnull
	@Override
	public String requiresPermission() {
		return "";
	}

	@Override
	public boolean requiresAuthentication() {
		return true;
	}

	@Override
	public String getFullName() {
		return name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public String getMethodName() {
		throw new SystemImplementationException("Stub url has no backing method");
	}

	@Override
	public void run(final State st,
	                final SafeMap values) {
		throw new SystemImplementationException("Stub url can not be run");
	}

	@Override
	public Module getModule() {
		return Modules.get(null,"Configuration");
	}

}
