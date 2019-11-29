package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Characters.CharacterAttribute;
import net.coagulate.GPHUD.Modules.Configuration.GenericConfiguration;
import net.coagulate.GPHUD.Modules.Pool.Pools;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Represents a module.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Module {

	String name;
	ModuleDefinition annotation;


	public Module(String name, ModuleDefinition annotation) {
		this.name = name;
		this.annotation = annotation;
		Modules.register(this);

	}

	protected static void checkPublicStatic(Method m) throws SystemException {
		if (!Modifier.isStatic(m.getModifiers())) {
			throw new SystemException("Method " + m.getDeclaringClass().getName() + "/" + m.getName() + " must be static");
		}
		if (!Modifier.isPublic(m.getModifiers())) {
			throw new SystemException("Method " + m.getDeclaringClass().getName() + "/" + m.getName() + " must be public");
		}
	}

	public void kvConfigPage(State st) {
		st.form.add(new TextHeader("KV Configuration for module " + getName()));
		GenericConfiguration.page(st, new SafeMap(), st.getInstance(), st.simulate(st.getCharacterNullable()), this);
	}

	public abstract boolean isGenerated();

	public String getName() { return name; }

	Response run(State st, String commandname, String[] args) throws UserException, SystemException {
		Command command = getCommand(st, commandname);
		return command.run(st, args);
	}

	public abstract Set<SideSubMenu> getSideSubMenus(State st);

	public String requires(State st) { return annotation.requires(); }

	public abstract URL getURL(State st, String url);

	public abstract Map<String, KV> getKVDefinitions(State st);

	public abstract KV getKVDefinition(State st, String qualifiedname);

	public abstract Command getCommand(State st, String commandname);

	public abstract Pool getPool(State st, String itemname);

	public abstract Permission getPermission(State st, String itemname);

	public abstract Map<String, Pool> getPoolMap(State st);

	public abstract boolean hasPool(State st, Pools p);

	public abstract Map<String, Command> getCommands(State st);

	public boolean hasConfig(State st) { return alwaysHasConfig(); }

	public boolean dependanciesEnabled(State st) {
		if (requires(st).isEmpty()) { return true; }
		String[] deps = requires(st).split(",");
		for (String dep : deps) {
			if (!Modules.get(null, dep).isEnabled(st)) { return false; }
		}
		return true;
	}

	public boolean isEnabled(State st) throws UserException, SystemException {
		boolean debug = false;
		if (debug) { System.out.println("check module enable " + getName() + " in state " + st); }
		if (!canDisable()) {
			if (debug) { System.out.println("cant disable so true"); }
			return true;
		}
		if (st == null) {
			if (debug) { System.out.println("No state so true"); }
			return true;
		}
		if (!dependanciesEnabled(st)) {
			if (debug) { System.out.println("dep failure so false"); }
			return false;
		}
		String enabled = st.getKV(getName() + ".enabled").value();
		if (enabled == null || enabled.isEmpty()) {
			if (debug) { System.out.println("not set so opposing defaultDisable - " + (!defaultDisable())); }
			return !defaultDisable();
		}
		if (debug) { System.out.println("Return value " + enabled); }
		return Boolean.valueOf(enabled);
	}

	public abstract Map<String, Permission> getPermissions(State st);

	public abstract SideMenu getSideMenu(State st);

	public abstract Set<URL> getAllContents(State st);

	public String description() { return annotation.description(); }

	public boolean canDisable() { return annotation.canDisable(); }

	public boolean defaultDisable() { return annotation.defaultDisable(); }

	protected abstract void initialiseInstance(State st);

	public Map<String, KV> getKVAppliesTo(State st, TableRow dbo) {
		Map<String, KV> fullset = getKVDefinitions(st);
		Map<String, KV> filtered = new TreeMap<>();
		for (String k : fullset.keySet()) {
			KV v = fullset.get(k);
			if (v.appliesTo(dbo)) {
				filtered.put(k, v);
			}
		}
		return filtered;
	}
    
    /*
dead code?
    public Map<String, KV> getKVExclusiveTo(State st, DBObject dbo) {
        boolean debug=false;
        Map<String, KV> fullset = getKVDefinitions(st);
        Map<String, KV> filtered = new TreeMap<>();
        for (String k : fullset.keySet()) {
            KV v = fullset.get(k);
            if (debug) { System.out.println(v.fullname()+" exclusive to "+dbo.getClass().getName()+"("+dbo.getName()+") exclusive:"+v.exclusiveTo(dbo)); }
            if (v.exclusiveTo(dbo)) {
                filtered.put(k, v);
            }
        }
        return filtered;
    }
    */

	public void validateKV(State st, String key) {
		if (getKVDefinitions(st).containsKey(key.toLowerCase())) {
			throw new SystemException("KV does not exist [" + key + "] in [" + this.getName() + "]");
		}
	}

	public void validatePermission(State st, String permission) {
		Map<String, Permission> perms = getPermissions(st);
		if (!perms.containsKey(permission.toLowerCase())) {
			throw new SystemException("Permission does not exist [" + permission + "] in [" + this.getName() + "]");
		}
	}

	public void validateCommand(State st, String command) {
		if (!getCommands(st).containsKey(command.toLowerCase())) {
			throw new SystemException("Command does not exist [" + command + "] in [" + this.getName() + "]");
		}
	}

	public boolean alwaysHasConfig() {
		return annotation.forceConfig();
	}

	public void addTemplateDescriptions(State st, Map<String, String> templates) {}

	public void addTemplateMethods(State st, Map<String, Method> ret) { }

	public Set<CharacterAttribute> getAttributes(State st) {
		return new TreeSet<CharacterAttribute>();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	public @interface ModuleDefinition {
		String description();

		boolean canDisable() default true;

		boolean defaultDisable() default false;

		String implementation() default "";

		boolean forceConfig() default false;

		String requires() default "";
	}

}
