package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Interfaces.Outputs.HeaderRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Link;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.KVValue;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * The outline of a generic configuration page.
 * Enumerates KVs and types and so forth and allows generic editing.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class GenericConfiguration {

	// ---------- STATICS ----------
	public static void page(@Nonnull final State st,
	                        final SafeMap values,
	                        @Nonnull final TableRow dbo,
	                        @Nonnull final State simulated) { page(st,values,dbo,simulated,null); }

	/**
	 * Shows a generic configuration page for a module, within a given state, in the context of a particular object (always an Instance presently I think).
	 *
	 * @param st     State - infers instance etc
	 * @param values Web form inputs
	 * @param dbo    Context we are looking at (shows stuff specific to this object)
	 */
	public static void page(@Nonnull final State st,
	                        final SafeMap values,
	                        @Nonnull final TableRow dbo,
	                        @Nonnull final State simulated,
	                        @Nullable final Module m) {
		final Form f=st.form();
		f.noForm();
		final Table kvtable=new Table();
		f.add(kvtable);
		kvtable.border(true);
		kvtable.add(new HeaderRow().add("Key")
		                           .add("Description")
		                           .add("Value for "+dbo.getClass().getSimpleName()+":"+dbo.getName())
		                           .add("Example Final Value")
		                           .add("Example Final Value Explanation"));
		for (final Module module: Modules.getModules()) {
			if ((m==null || m==module) && module.isEnabled(st)) {
				final Map<String,KV> kvmapall=module.getKVAppliesTo(simulated,dbo);
				for (final KV kv: kvmapall.values()) {
					if (!kv.hidden()) {
						kvtable.openRow();
						if (dbo instanceof Char) {
							kvtable.add(new Link(module.getName()+"."+kv.name(),st.getFullURL()+"/"+kv.fullname().replace('.','/')));
						}
						else {
							kvtable.add(new Link(module.getName()+"."+kv.name(),"/GPHUD/configuration/view/"+kv.fullname().replace('.','/')));
						}
						kvtable.add(kv.description());
						String raw=simulated.getRawKV(dbo,kv.fullname());
						if (raw==null) { raw=""; }
						kvtable.add(raw);
						try {
							final KVValue kvval=simulated.getKV(kv.fullname());
							kvtable.add(kvval.value());
							kvtable.add(kvval.path());
						}
						catch (@Nonnull final UserException e) {
							kvtable.add("<b>ERROR</b>");
							kvtable.add(e.getLocalizedMessage());
						}

					}
				}
			}
		}
	}

}
