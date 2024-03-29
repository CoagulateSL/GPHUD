package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.Hidden;
import net.coagulate.GPHUD.Interfaces.Outputs.Row;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;

/**
 * A row representing a configuration item.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ConfigurationRow extends Row {
	public ConfigurationRow(@Nonnull final State st,
	                        @Nonnull final TableRow dbo,
	                        @Nonnull final KV kv,
	                        @Nonnull final State simulated) {
		final String kvname=kv.fullName();
		//kv=st.getKVDefinition(kvname);
		add(kv.name());
		add(kv.description());
		add(kv.editPermission());
		if (kv.type()==KVTYPE.UUID) {
			add(kv.defaultValue()+"<br><img height=48 width=48 src=\""+SL.textureURL(kv.defaultValue())+"\">");
			add(simulated.getRawKV(dbo,kvname)+"<br><img height=48 width=48 src=\""+
			    SL.textureURL(simulated.getRawKV(dbo,kvname)+"\">"));
		} else {
			add(kv.defaultValue());
			add(simulated.getRawKV(dbo,kv.fullName()));
		}
		if (st.hasPermission(kv.editPermission())) {
			final Form ev=new Form();
			ev.setAction("./setinstancevalue");
			ev.add(new Hidden("key",kvname));
			ev.add(new Hidden("value",simulated.getRawKV(dbo,kvname)));
			ev.add(new Hidden("okreturnurl",st.getFullURL()));
			ev.add(new Button("Edit Value",true));
			add(ev);
			final Form rv=new Form();
			rv.setAction("./setinstancevalue");
			rv.add(new Hidden("key",kvname));
			rv.add(new Hidden("value",kv.defaultValue()));
			rv.add(new Hidden("okreturnurl",st.getFullURL()));
			rv.add(new Button("Reset Value",true));
			add(rv);
		}
	}
	
	
}
