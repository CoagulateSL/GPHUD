package net.coagulate.GPHUD.Modules.Effects;

import net.coagulate.GPHUD.Data.Effect;
import net.coagulate.GPHUD.Interfaces.Outputs.HeaderRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Configuration {

	// ---------- STATICS ----------
	@URL.URLs(url="/configuration/Effects")
	public static void configPage(@Nonnull final State st,
	                              final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("Defined Effects"));
		f.noForm();
		final Table at=new Table();
		f.add(at);
		at.add(new HeaderRow().add("Name").add("Metadata"));
		for (final Effect effect: Effect.getAll(st.getInstance())) {
			at.openRow();
			at.add(effect);
			String metaData=effect.getMetaData();
			if (metaData.isEmpty()) { metaData="<i>None Set</i>"; }
			at.add(metaData);
			if (st.hasPermission("Effects.Delete")) {
				at.add(new Form(st,true,"./Effects/Delete","Delete","name",effect.getName()));
			}
		}
		if (st.hasPermission("Effects.Create")) {
			f.add(new Form(st,true,"./Effects/Create","Create Effect"));
		}
	}

}
