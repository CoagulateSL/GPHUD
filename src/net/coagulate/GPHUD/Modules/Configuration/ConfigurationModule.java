package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.SideSubMenu;
import net.coagulate.GPHUD.State;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents the Configuration module, extending the annotated types to support dynamic (module based) side sub menus.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ConfigurationModule extends ModuleAnnotation {

	@Nullable
	Set<SideSubMenu> submenus;

	public ConfigurationModule(final String name, final ModuleDefinition def) {
		super(name, def);
		submenus = null;
	}

	@Nullable
	@Override
	public Set<SideSubMenu> getSideSubMenus(final State st) {
		if (submenus == null) {
			submenus = new HashSet<>();
			final Map<String, SideSubMenu> map = new TreeMap<>();
			for (final Module m : Modules.getModules()) {
				if (m.isEnabled(st)) {
					if (m.alwaysHasConfig() || !m.getKVDefinitions(st).isEmpty()) {
						map.put(m.getName(), new ConfigurationSideSubMenu(m));
						submenus.add(map.get(m.getName()));
					}
				}
			}
			int priority = 1;
			for (final SideSubMenu sideSubMenu : map.values()) {
				((ConfigurationSideSubMenu) sideSubMenu).setPriority(priority);
				priority++;
			}
		}

		return submenus;
	}


}
