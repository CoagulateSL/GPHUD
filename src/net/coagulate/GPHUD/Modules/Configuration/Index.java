package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.Hidden;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * Front page and default config page.
 * <p>
 * Front page lists modules and enable/disable options, and links to config pages, which are mostly made of "GenericConfiguration" pages
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Index {

	@URLs(url = "/configuration/")
	public static void createForm(@Nonnull State st, SafeMap values) throws UserException, SystemException {
		Form f = st.form;
		f.noForm();
		f.add(new TextHeader("GPHUD Module Configuration"));
		f.add(new Paragraph("Here, the <b>INSTANCE OWNER</b> may enable and disable certain (non-core) modules to enable or disable functionality."));
		f.add(new TextSubHeader("Core Modules"));
		f.add(new Paragraph("These may not be disabled as they provide core functionality needed for the system to operate"));
		Table core = new Table();
		f.add(core);
		core.add(new HeaderRow().add("Name").add("Description"));
		f.add(new TextSubHeader("Optional Modules"));
		f.add(new Paragraph("These modules can be disabled as they are optional, though you may consider some of them, such as the 'roller', to be 'core', it can be disabled, but probably makes no sense to do so.  Be aware certain modules may depend on other modules and may refuse to enable themselves."));
		Table configurable = new Table();
		f.add(configurable);
		configurable.add(new HeaderRow().add("Name").add("Description").add("Status"));
		for (Module m : Modules.getModules()) {
			boolean hasconfig = !(m.getKVDefinitions(st).isEmpty());
			if (m.hasConfig(st)) { hasconfig = true; }
			if (m.canDisable()) {
				configurable.openRow();
				if (hasconfig && m.isEnabled(st)) {
					configurable.add(new Link(m.getName(), "/GPHUD/configuration/" + m.getName()));
				} else {
					configurable.add(m.getName());
				}
				configurable.add(m.description());
				if (m.isEnabled(st)) { //IF DISABLED etc etc check new Boolean(st.getInstanceKV("modules."+m.getName()))) {
					configurable.add(new Color("green", "ENABLED"));
					if (st.hasPermission("instance.moduleenablement")) {
						Form disable = new Form();
						disable.setAction("./disablemodule");
						disable.add(new Hidden("module", m.getName()));
						disable.add(new Hidden("okreturnurl", st.getFullURL()));
						disable.add(new Button("Disable " + m.getName(), true));
						configurable.add(disable);
					}
				} else {
					configurable.add(new Color("red", "Disabled"));
					if (st.hasPermission("instance.moduleenablement")) {
						// only enableable if all the dependancies are enabled.  Note we dont check this on disables because reverse deps are annoying, and deps just disable themselves :P
						Form enable = new Form();
						enable.setAction("./enablemodule");
						enable.add(new Hidden("module", m.getName()));
						enable.add(new Hidden("okreturnurl", st.getFullURL()));
						enable.add(new Button("Enable " + m.getName(), true));
						configurable.add(enable);
					}
				}
			} else {  // note how this renders in one loop but produces two tables.  it feels a bit odd.
				core.openRow();
				if (hasconfig) {
					core.add(new Link(m.getName(), "/GPHUD/configuration/" + m.getName()));
				} else {
					core.add(m.getName());
				}
				core.add(m.description());
			}
		}
		f.add(new TextSubHeader("Cookbooks"));
		f.add(new Paragraph("Cookbooks provide a set of sequenced operations that modify your configuration and add features.  Everything these do can be done by hand and they merely provide a convenience measure."));
		f.add(new Paragraph("Clicking a cookbook will show you the steps that would be enacted.  Anyone may view a cookbook, but only the Instance.PlayCookBook may enact the cookbook due to the scale of changes"));
		Table books = new Table();
		f.add(books);
		books.add(new HeaderRow().add("Name").add("Description"));
		books.openRow().add(new Link("User Changable Titler", "/GPHUD/configuration/cookbooks/user-titler")).add("Allows the character to modify a text string that is appended to their titler");
		books.openRow().add(new Link("User Changable Titler Color", "/GPHUD/configuration/cookbooks/user-titler-color")).add("Allows the character to modify a text string that changes their titler color (should be an LSL color, e.g. <0,1,0> for green");
		books.openRow().add(new Link("Allow Multiple Characters", "/GPHUD/configuration/cookbooks/multi-char")).add("Allows the user to create and name multiple characters");
		books.openRow().add(new Link("Allow Self Retirement", "/GPHUD/configuration/cookbooks/self-retire")).add("Allows a character to elect to 'retire' themselves");
	}

	@URLs(url = "/configuration/view/*")
	public static void kvDetailPage(@Nonnull State st, SafeMap values) {
		String kvname = st.getDebasedURL().replaceFirst("/configuration/view/", "").replaceFirst("/", ".");
		KV kv = st.getKVDefinition(kvname);
		st.form.noForm();
		st.form.add(new ConfigurationHierarchy(st, kv, st, values));
	}

	@URLs(url = "/configuration/*")
	public static void genericConfigurationPage(@Nonnull State st, SafeMap values) throws UserException, SystemException {
		String module = st.getDebasedURL().replaceFirst("/configuration/", "");
		String key = null;
		st.form.noForm();
		if (module.contains("/")) {
			String[] split = module.split("/");
			if (split.length == 2) {
				module = split[0];
				key = split[1];
			}
		}
		Module m = Modules.get(st, module);
		if (key == null) {
			m.kvConfigPage(st);
		} else {
			KV kv = st.getKVDefinition(module + "." + key);

			st.form.add(new ConfigurationHierarchy(st, kv, st.simulate(st.getCharacter()), values));
		}
	}
}
