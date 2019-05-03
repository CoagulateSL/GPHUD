package net.coagulate.GPHUD.Interfaces.HUD;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Interfaces.Outputs.Link;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/**
 * A Interface object in the HUD's web panel, no frills.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Interface extends net.coagulate.GPHUD.Interfaces.User.Interface {

	/**
	 * Without any surroundings
	 *
	 * @return
	 */
	@Override
	public String renderHTML(State st) {
		boolean debug = false;
		String uri = st.getDebasedURL();
		if (!uri.startsWith("/hud/")) {
			throw new SystemException("Unexpected call");
		}
		uri = uri.substring("/hud/".length());
		String command = "";
		for (String piece : uri.split("/")) {
			if (!"".equals(command)) {
				command = command + ".";
			}
			command += piece;
		}
		uri = st.getDebasedURL();
		if (uri.startsWith("/hud/show/")) {
			uri = uri.replaceAll("/hud/show/", "/hud/");
		}
		//st.uri=st.uri.replaceAll("\\?gphud=.*","");
		command = command.replaceAll("\\?gphud=.*", "");
		uri = uri.replaceAll("\\?seed=.*", "");
		command = command.replaceAll("\\?seed=.*", "");
		boolean sendshow = false;
		if (command.startsWith("show.")) {
			command = command.substring("show.".length());
			sendshow = true;
			if (debug) {
				System.out.println("SENDSHOW TRUE");
			}
		}
		st.command = command;
		st.sendshow = sendshow;

		st.source = State.Sources.HUD;
		// This is basically the page template, the supporting structure that surrounds a title/menu/body
		String p = "";
		//System.out.println(st.uri);
		p += "<html><head><title>";
		p += "GPHUD";
		p += "</title></head><body>";
		p += "<b><u>You must click in this window to activate it before you can select any buttons etc</u></b><br>";
		// calculate body first, since this sets up auth etc which the side menu will want to use to figure things out.
		String body = renderBodyProtected(st);
		p += messages(st);
		p += body;
		p += "<br><a href=\"/GPHUD/hud/gphudclient/menu\">Main Menu</a>  <a href=\"/GPHUD/hud/gphudclient.close\">Close</a>";
		p += "</body></html>";
		if (st.sendshow) {
			if (debug) {
				System.out.println("SENDING");
				System.out.println(st.getInstance());
				System.out.println(st.avatar());
				System.out.println(st.getCharacter());
			}
			JSONObject j = new JSONResponse(new JSONObject().put("incommand", "open").put("debug", "SendShow set in Interface for uri " + st.getDebasedURL())).asJSON(st);
			Transmission t = new Transmission(st.getCharacter(), j);
			t.start();
		}
		return p;
	}

	@Override
	public boolean isRich() {
		return false;
	}

	@Override
	protected boolean cookieAuthenticationOnly() {
		return true;
	}

	public Form authenticationHook(State st, SafeMap values) throws SystemException {
		Form f = super.authenticationHook(st, values);
		if (f == null) {
			if (st.getCharacter() == null) {
				f = new Form();
				f.add("Failed to get a character from the session!");
				return f;
			}
			return null;
		}
		return f;
	}

	public String messages(State st) {
		if (st.getCharacter() == null) {
			return "";
		}
		int messages = st.getCharacter().messages();
		if (messages > 0) {
			return "<p>" + new Link("<b>You have " + messages + " unread message" + (messages > 1 ? "s" : "") + ", click here to read</b>", "/hud/listmessages").asHtml(st, true) + "</p>";
		}
		return "";
	}

	public String renderBody(State st) throws UserException, SystemException {
		boolean debug = false;
		Form f;
		SafeMap values = getPostValues(st);
		f = authenticationHook(st, values);
		if (st.getInstanceNullable() == null && st.getCharacter() != null) {
			st.setInstance(st.getCharacter().getInstance());
		}
		if (f == null) {
			if (debug) {
				System.out.println(st.getDebasedURL());
			}
			if (debug) {
				System.out.println(st.command);
			}
			f = new Form();
			st.form = f;
			URL content = null;
			content = Modules.getURL(st, st.getDebasedURL(), false);

			if (content != null) {
				if (debug) {
					System.out.println("Run as content");
				}
				f = new Form();
				st.form = f;
				content.run(st, values);
			} else {
				if (debug) {
					System.out.println("Run as command");
				}
				//System.out.println(content);
				Modules.simpleHtml(st, st.command, values);
			}
		} else {
			st.form = f;
		}

		return f.asHtml(st, isRich());
	}
}
