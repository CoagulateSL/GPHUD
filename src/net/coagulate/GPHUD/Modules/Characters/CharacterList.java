package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.CharacterSummary;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Show all characters within the instance.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class CharacterList {

	@URLs(url = "/characters/list*", requiresPermission = "Characters.ViewAll")
	public static void list(@Nonnull State st, SafeMap values) throws UserException, SystemException {
		List<CharacterSummary> list = st.getInstance().getCharacterSummary(st);
		if (list.isEmpty()) {
			st.form.add("No characters found");
			return;
		}
		Table t = new Table();
		st.form.add(t);
		t.border(true);
		t.add(list.get(0).headers(st));
		for (CharacterSummary s : list) { /*if (!s.retired)*/ { t.add(s.asRow(st)); } }
	}
}
