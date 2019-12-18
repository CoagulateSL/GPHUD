package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.Hidden;
import net.coagulate.GPHUD.Interfaces.Outputs.Row;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.State;

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
	                        @Nonnull final State simulated)
	{
		final String kvname=kv.fullname();
		//kv=st.getKVDefinition(kvname);
		add(kv.name());
		add(kv.description());
		add(kv.editpermission());
		if (kv.type()==KVTYPE.UUID) {
			add(kv.defaultvalue()+"<br><img height=48 width=48 src=\"http://texture-service.agni.lindenlab.com/"+kv.defaultvalue()+"/256x192.jpg/\">");
			add(simulated.getRawKV(dbo,
			                       kvname
			                      )+"<br><img height=48 width=48 src=\"http://texture-service.agni.lindenlab.com/"+simulated
					.getRawKV(dbo,kvname)+"/256x192.jpg/\">");
		} else {
			add(kv.defaultvalue());
			add(simulated.getRawKV(dbo,kv.fullname()));
		}
		if (st.hasPermission(kv.editpermission())) {
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
			rv.add(new Hidden("value",kv.defaultvalue()));
			rv.add(new Hidden("okreturnurl",st.getFullURL()));
			rv.add(new Button("Reset Value",true));
			add(rv);
		}
	}


}
