package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

/**
 * fake url for side sub menus (ugly).
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ConfigurationURL extends URL {

	String name;
	String url;
	public ConfigurationURL(String name, String url) {
		this.name = name;
		this.url = url;
	}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Override
	public String url() {
		return url;
	}

	@Override
	public String requiresPermission() {
		return "";
	}

	@Override
	public String getFullName() {
		return name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getMethodName() {
		throw new SystemException("Stub url has no backing method");
	}

	@Override
	public void run(State st, SafeMap values) {
		throw new SystemException("Stub url can not be run");
	}

	@Override
	public boolean requiresAuthentication() {
		return true;
	}

}
