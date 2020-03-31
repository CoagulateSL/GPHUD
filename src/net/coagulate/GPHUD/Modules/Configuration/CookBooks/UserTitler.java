package net.coagulate.GPHUD.Modules.Configuration.CookBooks;

import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class UserTitler extends CookBook {
	// ---------- STATICS ----------
	@URL.URLs(url="/configuration/cookbooks/user-titler")
	public static void createForm(@Nonnull final State st,
	                              @Nonnull final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("User Configurable Titler Cookbook"));
		final boolean act=false;
		f.add(new Paragraph("This cookbook will enable the user to append their own textual data to the Titler text, it will perform the following steps:"));
		final Table t=new Table();
		f.add(t);
		run(st,t,false);
		if (values.containsKey("ACTIVATE COOKBOOK") && (st.hasPermission("instance.cookbooks"))) {
			f.add("");
			f.add(new TextSubHeader("EXECUTING COOKBOOK"));
			final Table runt=new Table();
			f.add(runt);
			run(st,runt,true);
		}
		else {
			confirmButton(st,f);
		}
	}

	// ----- Internal Statics -----
	private static void run(@Nonnull final State st,
	                        @Nonnull final Table t,
	                        final boolean act) {
		t.add(new HeaderRow().add("Action").add("Verification").add("Description"));
		charAttribute(st,act,t,"TitlerText","true","TEXT","","FALSE","FALSE","");
		final String newvalue;//=st.getRawKV(st.getInstance(), "GPHUDClient.TitlerText");
		newvalue="--NAME----NEWLINE----TITLERTEXT--";
		setKV(st,act,t,st.getInstance(),"GPHUDClient.TitlerText",newvalue);
		final JSONObject mappings=new JSONObject();
		mappings.put("attribute","TitlerText");
		mappings.put("value-desc","Enter new titler text");
		createAlias(st,act,t,"SetTitlerText","characters.set",mappings);
		menu(st,act,t,"Titler Text","Alias.SetTitlerText");
	}

}
