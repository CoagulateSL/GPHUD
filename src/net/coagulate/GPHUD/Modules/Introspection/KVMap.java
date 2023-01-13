package net.coagulate.GPHUD.Modules.Introspection;

import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.SideSubMenu.SideSubMenus;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * API Introspection.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class KVMap {
	// ---------- STATICS ----------
	@URLs(url="/introspection/kvmap")
	@SideSubMenus(name="KeyValue Map", priority=10)
	public static void kvmap(@Nonnull final State st,final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("KV Mappings"));
		final Table t=new Table();
		f.add(t);
		for (final Module m: Modules.getModules()) {
			final Map<String,KV> kvmap=m.getKVDefinitions(st);
			if (!kvmap.isEmpty()) {
				t.openRow();
				t.add(new HeaderRow().add(new TextSubHeader(m.getName())));
				t.add(new HeaderRow().add("Name")
				                     .add("Scope")
				                     .add("Type")
				                     .add("Hierarchy")
				                     .add("Permission")
				                     .add("Default")
				                     .add("Description")
				                     .add("Convey")
				                     .add("Template")
				                     .add("Hooked"));
				for (final KV kv: kvmap.values()) {
					final Row r=new Row();
					if (kv.hidden()) {
						r.setbgcolor("#e0e0e0");
					}
					t.add(r);
					r.add(kv.name());
					r.add(kv.scope().toString());
					r.add(kv.type().toString());
					r.add(kv.hierarchy().toString());
					r.add(kv.editPermission());
					r.add(kv.defaultValue());
					r.add(kv.description());
					r.add(kv.conveyAs().isEmpty()?"":"Conv");
					r.add(kv.template()?"Tmpl":"");
					r.add(kv.onUpdate().isEmpty()?"":"Upd");
					if (kv.isGenerated()) {
						r.add(new Color("blue","Generated"));
					} else {
						r.add("");
					}
				}
			}
		}
	}
	
	
}
