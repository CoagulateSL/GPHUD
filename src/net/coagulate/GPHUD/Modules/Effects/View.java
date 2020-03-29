package net.coagulate.GPHUD.Modules.Effects;

import net.coagulate.GPHUD.Data.Effect;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Configuration.GenericConfiguration;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class View {
	@URL.URLs(url="/configuration/Effects/View/*")
	public static void viewEffect(@Nonnull final State st,
	                              final SafeMap values) {
		final String[] split=st.getDebasedURL().split("/");
		final String id=split[split.length-1];
		final Effect e=Effect.get(Integer.parseInt(id));
		e.validate(st);
		viewEffect(st,values,e);
	}

	public static void viewEffect(@Nonnull final State st,
	                              final SafeMap values,
	                              @Nonnull final Effect e) {
		e.validate(st);
		final Form f=st.form();
		f.noForm();
		f.add(new TextHeader("Effect: "+e.getName()));

		f.add(new TextSubHeader("KV influences"));
		GenericConfiguration.page(st,values,e,st);
	}
}
