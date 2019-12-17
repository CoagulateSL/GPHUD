package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Data.Message;
import net.coagulate.GPHUD.Interfaces.Outputs.Paragraph;
import net.coagulate.GPHUD.Interfaces.Outputs.TextError;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * Deal with messages via a web interface.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Messages {
	@URLs(url = "/hud/listmessages")
	public static void messagesListHUD(@Nonnull final State st, @Nonnull final SafeMap values) throws SystemException, UserException {
		messagesList(st, values);
	}

	@URLs(url = "/messages/list")
	public static void messagesList(@Nonnull final State st, @Nonnull final SafeMap values) throws SystemException, UserException {
		final Message m = st.getCharacter().getMessage();
		final Form f = st.form();
		if (m == null) {
			f.add(new TextError("You have no messages."));
			return;
		}
		m.setActive();
		final JSONObject j = new JSONObject(m.getJSON());
		final String message = j.optString("message", "");
		if ("factioninvite".equalsIgnoreCase(message)) {
			displayFactionInvite(st, values, j);
			return;
		}
		throw new SystemConsistencyException("Malformed message " + m.getId() + ", contains no message");

	}

	public static void displayFactionInvite(@Nonnull final State st, @Nonnull final SafeMap values, @Nonnull final JSONObject j) throws UserException, SystemException {
		final Form f = st.form();
		final Char from = Char.get(j.getInt("from"));
		final CharacterGroup to = CharacterGroup.get(j.getInt("to"));
		f.add(new TextSubHeader("Invite"));
		f.add("You have been invited to join the " + to.getName() + " by " + from.getName());
		f.add(new Paragraph());
		Modules.simpleHtml(st, "gphudclient.acceptrejectmessage", values);

	}
}
