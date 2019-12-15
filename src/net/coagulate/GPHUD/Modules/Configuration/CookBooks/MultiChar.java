package net.coagulate.GPHUD.Modules.Configuration.CookBooks;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
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
	@URL.URLs(url = "/configuration/cookbooks/multi-char")
	public static void createForm(@Nonnull State st, @Nonnull SafeMap values) throws UserException, SystemException {
		Form f = st.form();
		f.add(new TextHeader("Multiple Character Cookbook"));
		boolean act = false;
		f.add(new Paragraph("This cookbook will enable the user to create multiple characters and not assume avatar naming."));
		f.add(new Paragraph("Note that further options are available, such as only allowing creation/switch in the OOC zone, if one exists."));
		f.add(new Paragraph("The default setup here will enable chracter creation/switching anywhere, by making the following changes:"));
		Table t = new Table();
		f.add(t);
		run(st, t, false);
		if (values.containsKey("ACTIVATE COOKBOOK") && (st.hasPermission("instance.cookbooks"))) {
			f.add("");
			f.add(new TextSubHeader("EXECUTING COOKBOOK"));
			Table runt = new Table();
			f.add(runt);
			run(st, runt, true);
		} else {
			confirmButton(st, f);
		}
	}

	private static void run(@Nonnull State st, @Nonnull Table t, boolean act) {
		t.add(new HeaderRow().add("Action").add("Verification").add("Description"));
		setKV(st, act, t, st.getInstance(), "Instance.AutoNameCharacter", "false");
		setKV(st, act, t, st.getInstance(), "Instance.CharacterSwitchEnabled", "true");
		setKV(st, act, t, st.getInstance(), "Instance.MaxCharacters", "5");
	}

}
