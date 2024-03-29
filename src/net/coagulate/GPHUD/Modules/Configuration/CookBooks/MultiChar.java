package net.coagulate.GPHUD.Modules.Configuration.CookBooks;

import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class MultiChar extends CookBook {
	// ---------- STATICS ----------
	@URL.URLs(url="/configuration/cookbooks/multi-char")
	public static void createForm(@Nonnull final State st,@Nonnull final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("Multiple Character Cookbook"));
		final boolean act=false;
		f.add(new Paragraph(
				"This cookbook will enable the user to create multiple characters and not assume avatar naming."));
		f.add(new Paragraph(
				"Note that further options are available, such as only allowing creation/switch in the OOC zone, if one exists."));
		f.add(new Paragraph(
				"The default setup here will enable chracter creation/switching anywhere, by making the following changes:"));
		final Table t=new Table();
		f.add(t);
		run(st,t,false);
		if (values.containsKey("ACTIVATE COOKBOOK")&&(st.hasPermission("instance.cookbooks"))) {
			f.add("");
			f.add(new TextSubHeader("EXECUTING COOKBOOK"));
			final Table runt=new Table();
			f.add(runt);
			run(st,runt,true);
		} else {
			confirmButton(st,f);
		}
	}
	
	// ----- Internal Statics -----
	private static void run(@Nonnull final State st,@Nonnull final Table t,final boolean act) {
		t.add(new HeaderRow().add("Action").add("Verification").add("Description"));
		setKV(st,act,t,st.getInstance(),"Instance.AutoNameCharacter","false");
		setKV(st,act,t,st.getInstance(),"Instance.CharacterSwitchEnabled","true");
		setKV(st,act,t,st.getInstance(),"Instance.MaxCharacters","5");
	}
	
}
