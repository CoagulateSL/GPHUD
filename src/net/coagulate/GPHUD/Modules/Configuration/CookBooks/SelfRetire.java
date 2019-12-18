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
public class SelfRetire extends CookBook {
	@URL.URLs(url="/configuration/cookbooks/self-retire")
	public static void createForm(@Nonnull final State st,
	                              @Nonnull final SafeMap values)
	{
		final Form f=st.form();
		f.add(new TextHeader("Self Retirement Cookbook"));
		final boolean act=false;
		f.add(new Paragraph("This cookbook will enable the user to retire their own character through the menu."));
		f.add(new Paragraph(
				"Note that further options are available, such as only allowing retiring in the OOC zone, if one exists."));
		f.add(new Paragraph(
				"The default setup here will enable self retiring anywhere, by making the following changes:"));
		final Table t=new Table();
		f.add(t);
		run(st,t,false);
		if (values.containsKey("ACTIVATE COOKBOOK") && (st.hasPermission("instance.cookbooks"))) {
			f.add("");
			f.add(new TextSubHeader("EXECUTING COOKBOOK"));
			final Table runt=new Table();
			f.add(runt);
			run(st,runt,true);
		} else {
			confirmButton(st,f);
		}
	}

	private static void run(@Nonnull final State st,
	                        @Nonnull final Table t,
	                        final boolean act)
	{
		t.add(new HeaderRow().add("Action").add("Verification").add("Description"));
		setKV(st,act,t,st.getInstance(),"Instance.AllowSelfRetire","true");
		createMenu(st,
		           act,
		           t,
		           "RetireMe",
		           "Are you SURE you wish to retire your character?  This action CAN NOT be undone."
		          );
		menu(st,act,t,"RetireMe","NO","GPHUDClient.NOOP");
		menu(st,act,t,"RetireMe","retire","Characters.RetireMe");
		menu(st,act,t,"RetireMe","NO ","GPHUDClient.NOOP");
		menu(st,act,t,"Retire Char","Menus.RetireMe");
	}

}
