package net.coagulate.GPHUD.Interfaces.User;

import net.coagulate.Core.Tools.ExceptionTools;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Cookies;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.PasswordInput;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.Link;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.Text;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.*;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.Session;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.*;

import static java.util.logging.Level.*;


/**
 * Handles User HTML connections.
 * <p>
 * I.e. the start of the HTML interface.
 *
 * @author iain
 */
public class Interface extends net.coagulate.GPHUD.Interface {

	// leave this here for now
	@Nonnull
	public static String styleSheet() {
		return "" +
				"<style>\n" +
				".tooltip {\n" +
				"    position: relative;\n" +
				"    display: inline-block;\n" +
				"    border-bottom: 1px dotted black; /* If you want dots under the hoverable text */\n" +
				"}\n" +
				"\n" +
				".tooltip .tooltiptext {\n" +
				"    visibility: hidden;\n" +
//"    width: 120px;\n" +
				"    background-color: #e0e0e0;\n" +
				"    color: black;\n" +
				"    text-align: center;\n" +
				"    padding: 5px 0;\n" +
				"    border-radius: 6px;\n" +
				" \n" +
				"     top: -5px;\n" +
				"    left: 105%; " +
				"    position: absolute;\n" +
				"    z-index: 1;\n" +
				"white-space: nowrap;\n" +
				"}\n" +
				"\n" +
				"/* Show the tooltip text when you mouse over the tooltip container */\n" +
				".tooltip:hover .tooltiptext {\n" +
				"    visibility: visible;\n" +
				"}\n" +
				"</style>";
	}

	/**
	 * URLs we do not redirect to instance/char selection.  Like logout.
	 */
	private boolean interceptable(@Nullable String url) {
		if ("/logout".equalsIgnoreCase(url)) { return false; }
		if ("/Help".equalsIgnoreCase(url)) { return false; }
		if (url!=null && url.toLowerCase().startsWith("/published/")) { return false; }
		return true;
	}

	/**
	 * this is where the request comes in after generic processing.
	 * We basically just encapsulate all requests in an Exception handler that will spew errors as HTML errors (rather than JSON errors).
	 * These are rather useless in production, but in DEV we dump the stack traces too.
	 *
	 * @param st
	 */
	@Override
	public void process(@Nonnull State st) {
		st.source = State.Sources.USER;
		//for (Header h:headers) { System.out.println(h.getName()+"="+h.getValue()); }

		// Exception catcher, basically.  with redirection support
		try {
			st.resp.setStatusCode(HttpStatus.SC_OK);
			st.resp.setEntity(new StringEntity(renderHTML(st), ContentType.TEXT_HTML));
		} catch (RedirectionException redir) {
			st.resp.setStatusCode(303);
			String targeturl = redir.getURL();
			//System.out.println("PRE:"+targeturl);
			if (targeturl.startsWith("/") && !targeturl.startsWith("/GPHUD")) { targeturl = "/GPHUD" + targeturl; }
			//System.out.println("POST:"+targeturl);
			st.resp.addHeader("Location", targeturl);
		} catch (Exception e) {
			try {
				SL.report("GPHUD UserInterface exception", e, st);
				GPHUD.getLogger().log(SEVERE, "UserInterface exception : " + e.getLocalizedMessage(), e);
				// stash the exception for the ErrorPage
				st.exception = e;
				st.resp.setStatusCode(HttpStatus.SC_OK);
				// an unmapped page, "ErrorPage"
				st.resp.setEntity(new StringEntity("Error.", ContentType.TEXT_HTML));
			} catch (Exception ex) {
				SL.report("GPHUD UserInterface exception in exception handler", ex, st);
				GPHUD.getLogger().log(SEVERE, "Exception in exception handler - " + ex.getLocalizedMessage(), ex);
			}
		}
	}

	@Nonnull
	public String messages(@Nonnull State st) {
		if (st.getCharacterNullable() == null) { return ""; }
		int messages = st.getCharacter().messages();
		if (messages > 0) {
			return "<p>" + new Link("<b>You have " + messages + " unread message" + (messages > 1 ? "s" : "") + ", click here to read</b>", "/GPHUD/messages/list").asHtml(st, true) + "</p>";
		}
		return "";
	}

	// FIRST STEP of producing the page its self, this is all wrapped in a low level exception handler.
	@Nonnull
	public String renderHTML(@Nonnull State st) {
		// This is basically the page template, the supporting structure that surrounds a title/menu/body

		// we compute the body first, so any changes it causes to the rest of the data (like logging out, menu changes etc) is reflected
		String body = renderBodyProtected(st);
		String p = "";
		final boolean external=st.getDebasedNoQueryURL().toLowerCase().startsWith("/published/");
		p += "<html><head><title>";
		p += "GPHUD";
		p += "</title>";
		if (!external) {
			p += styleSheet();
			p += "<link rel=\"shortcut icon\" href=\"/resources/icon-gphud.png\">";
		}
		p += "</head><body>";
		if (!external) {
			p += "<table height=100% valign=top><tr><td colspan=3 align=center width=100%>";
			p += "<table style=\"margin: 0px; border:0px;\" width=100%><tr><td width=33% align=left>";
			p += "<h1 style=\"margin: 0px;\"><img src=\"/resources/banner-gphud.png\"></h1>";
			p += "</td><td width=34% align=center>";
			String middletarget = "/resources/banner-gphud.png";
			if (st.getInstanceNullable() != null) {
				middletarget = st.getInstance().getLogoURL(st);
			}
			p += "<h1 style=\"margin: 0px;\"><img src=\"" + middletarget + "\" height=100px></h1>";
			p += "</td><td width=33% align=right>";
			p += "<h1 style=\"margin: 0px;\"><a href=\"/\">" + SL.getBannerHREF() + "</a></h1>";
			p += "</td></tr></table>";
			//p+="<i>"+GPHUD.environment()+"</i>";
			p += "<hr>";
			p += "</td></tr>";
			p += "<tr height=100% valign=top>";
			p += "<td width=150px valign=top height=100%>";
			// calculate body first, since this sets up auth etc which the side menu will want to use to figure things out.

			try {
				p += renderSideMenu(st);
			} catch (Exception e) {
				// Exception in side menu code
				p += "<b><i>Crashed</i></b>";
				SL.report("GPHUD Side Menu crashed", e, st);
				st.logger().log(WARNING, "Side menu implementation crashed", e);
			}
			p += "</td><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td><td width=100% valign=top height=100%>";
			p += messages(st);
		}
		p += body;
		if (!external) {
			p += "</td>";
			p += "</tr>";
			p += "</table>";
		}
		p += "</body></html>";
		return p;
	}

	// SECOND STAGE - called by the outline code, this is a wrapper around renderBody that "nicely" handles all deeper exceptions
	@Nonnull
	protected String renderBodyProtected(@Nonnull State st) {
		// with all exception protections, in a 'sensible' way.
		try {
			// literally, just a wrapper
			return renderBody(st);
		} catch (Exception t) {
			// Exception in processing the command
			if (t instanceof RedirectionException) { throw (RedirectionException) t; } // propagate to the top where it's handled
			try {
				t.printStackTrace();
				if (t instanceof UserException) {
					String r = "<h1>ERROR</h1><p>Sorry, your request could not be completed<br><pre>" + t.getLocalizedMessage() + "</pre></p>";
					GPHUD.getLogger().log(INFO, "UserInterface/UserException " + t.toString());
					if (GPHUD.DEV) {
						r += "<hr><h1 align=center>DEV MODE</h1><hr><h1>User Mode Exception</h1>" + ExceptionTools.dumpException(t) + "<Br><br>" + st.toHTML();
						SL.report("GPHUD Web User Exception", t, st);
						GPHUD.getLogger().log(WARNING, "UserInterface/UserException", t);
					}
					return r;
				}
				SL.report("GPHUD Web Other Exception", t, st);
				GPHUD.getLogger().log(WARNING, "UserInterface/NonUserException", t);
				String r = "<h1>INTERNAL ERROR</h1><p>Sorry, your request could not be completed due to an internal error.</p>";
				if (GPHUD.DEV) {
					r += "<hr><h1 align=center>DEV MODE</h1><hr><h1>NonUser Exception</h1>" + ExceptionTools.dumpException(t) + "<Br><br>" + st.toHTML();
				}
				return r;
			} catch (Exception f) {
				GPHUD.getLogger().log(SEVERE, "Exception in exception handler", f);
				return "EXCEPTION IN EXCEPTION HANDLER, PANIC!"; // nice
			}
		}
	}

	@Nonnull
	String dynamicSubMenus(State st, SideMenu menu) {
		// dereference the menu into a module
		Module owner = null;
		for (Module m : Modules.getModules()) { if (m.getSideMenu(st) == menu) { owner = m; } }
		if (owner == null) { return ">> NULL?<br>"; }

		StringBuilder ret = new StringBuilder();
		Map<Integer, Set<SideSubMenu>> priorities = new TreeMap<>();
		// collect sidemenus, by priority
		for (SideSubMenu s : owner.getSideSubMenus(st)) {
			//System.out.println("Testing "+s.name());
			Integer priority = s.priority();
			Set<SideSubMenu> set = new HashSet<>();
			if (priorities.containsKey(priority)) { set = priorities.get(priority); }
			set.add(s);
			priorities.put(s.priority(), set);
		}
		// enumerate the priorities
		for (Set<SideSubMenu> sideSubMenus : priorities.values()) {
			// enumerate the SideMenus
			for (SideSubMenu s : sideSubMenus) {
				String u = s.getURL();
				ret.append("&nbsp;&nbsp;&nbsp;&gt;&nbsp;&nbsp;&nbsp;<a href=\"/GPHUD").append(u).append("\">").append(s.name()).append("</a><br>");
			}
		}
		return ret.toString();
	}

	@Nonnull
	String dynamicSideMenus(@Nonnull State st) throws UserException, SystemException {
		StringBuilder r = new StringBuilder();
		Map<Integer, Set<SideMenu>> priorities = new TreeMap<>();
		// collect sidemenus, by priority
		for (Module m : Modules.getModules()) {
			if (m.isEnabled(st)) {
				SideMenu s = m.getSideMenu(st);
				if (s != null) {
					boolean permitted = true;
					if (!s.requiresPermission().isEmpty()) {
						if (!st.hasPermission(s.requiresPermission())) {
							permitted = false;
						}
					}
					if (permitted) {
						Integer priority = s.priority();
						Set<SideMenu> set = new HashSet<>();
						if (priorities.containsKey(priority)) { set = priorities.get(priority); }
						set.add(s);
						priorities.put(s.priority(), set);
					}
				}
			}
		}
		// enumerate the priorities
		for (Set<SideMenu> sideMenus : priorities.values()) {
			// enumerate the SideMenus
			for (SideMenu menu : sideMenus) {
				String url = menu.url();
				String name = menu.name();
				r.append("<a href=\"/GPHUD").append(url).append("\">").append(name).append("</a><br>");
				if (st.getDebasedURL().startsWith(url)) {
					r.append(dynamicSubMenus(st, menu));
				}
			}
		}


		return r.toString();
	}

	@Nonnull
	String renderSideMenu(@Nonnull State st) throws UserException, SystemException {
		StringBuilder s = new StringBuilder();
		s.append(GPHUD.menuPanelEnvironment()).append("<hr width=150px>");
		boolean loggedin = true;
		if (st.getCharacterNullable() != null || st.getAvatar() != null) {
			s.append("<b>Avatar:</b> ");
			//if (st.user!=null) s+="[<a href=\"/GPHUD/switch/avatar\">Switch</a>]"; // you can only switch avis if you're a logged in user, as thats what binds avis
			s.append("<br>");
			if (st.avatar() != null) {
				s.append(st.avatar().getGPHUDLink()).append("<br>");
			} else { s.append("<i>none</i><br>"); }


			s.append("<b>Instance:</b> [<a href=\"/GPHUD/switch/instance\">Switch</a>]<br>");
			if (st.getInstanceNullable() != null) {
				s.append(st.getInstance().asHtml(st, true)).append("<br>");
			} else { s.append("<i>none</i><br>"); }

			s.append("<b>Character:</b> [<a href=\"/GPHUD/switch/character\">Switch</a>]<br>");
			if (st.getCharacterNullable() != null) {
				s.append(st.getCharacter().asHtml(st, true)).append("<br>");
			} else { s.append("<i>none</i><br>"); }
		} else {
			s.append("<i>Not logged in</i><hr width=150px><a href=\"/GPHUD/\">Index</a><br><br>");
			s.append("<a href=\"/GPHUD/Help\">Documentation</a><br>");
			s.append("<hr width=150px>");
			return s.toString();
		}
		if (loggedin) {
			s.append("<br><a href=\"/GPHUD/logout\">Logout</a><br>");
		}
		s.append("<hr width=150px>");
		s.append("<a href=\"/GPHUD/\">Index</a><br><br>");
		boolean dynamics = true;
		if (st.avatar() == null) {
			s.append("<i>Select an avatar</i><br>");
			dynamics = false;
		}
		if (st.getInstanceNullable() == null) {
			s.append("<i>Select an instance</i><br>");
			dynamics = false;
		}
		if (dynamics) {
			s.append(dynamicSideMenus(st));
			s.append("<br>");
		}
		s.append("<a href=\"/GPHUD/Help\">Documentation</a><br>");
		s.append("<hr width=150px>");
		String sectionhead = "<b>PERMISSIONS:</b><br>";
		if (st.isSuperUser()) {
			s.append(sectionhead).append("<b style=\"color: blue;\">SUPER-ADMIN</b><br>");
			sectionhead = "";
		}
		if (st.isInstanceOwner()) {
			s.append(sectionhead).append("<b style=\"color: blue;\">Instance Owner</b><br>");
			sectionhead = "";
		}
		if (st.getPermissions() != null) {
			for (String permission : st.getPermissions()) {
				s.append(sectionhead).append("<font style=\"color: green;\">").append(permission).append("</font><br>");
				sectionhead = "";
			}
		}
		return s.toString();
	}

	public boolean isRich() { return true; }

	// THIRD stage (process -> renderHTML (page layout) -> renderBodyProtected -> renderBody)
	@Nonnull
	public String renderBody(@Nonnull State st) throws SystemException, UserException {
		Form f = null;
		SafeMap values = getPostValues(st);
		st.postmap=values;
		URL content = Modules.getURL(st, st.getDebasedNoQueryURL());
		// call authenticator, it will return null if it managed something, otherwise it returns a login form which we'll render and exit
		if (content.requiresAuthentication()) { f = authenticationHook(st, values); }
		if (f!=null) { st.form=f; return f.asHtml(st,isRich()); }
		// some kinda login information exists
		st.fleshOut();
		content = Modules.getURL(st, st.getDebasedNoQueryURL());
		//System.out.println("Post auth URL is "+st.getDebasedURL()+" and form is "+f+" and content is "+content.getFullName()+" and interceptable is "+interceptable(st.getDebasedNoQueryURL()));
		if (st.getInstanceNullable()==null && interceptable(st.getDebasedNoQueryURL())) { content=Modules.getURL(st,"/GPHUD/switch/instance"); } //f=new Form(); f.add(new TextHeader("Module "+content.getModule().getName()+" is inaccessible as no instance is currently selected")); }
		if (st.getInstanceNullable()!=null && !content.getModule().isEnabled(st)) { f=new Form(); f.add(new TextHeader("Module "+content.getModule().getName()+" is not enabled in instance "+st.getInstanceString())); }
		//System.out.println("Post post-auth URL is "+st.getDebasedURL()+" and form is "+f+" and content is "+content.getFullName()+" and interceptable is "+interceptable(st.getDebasedNoQueryURL()));
		f = new Form();
		st.form = f;
		if (!content.requiresPermission().isEmpty()) {
			if (!st.hasPermission(content.requiresPermission())) {
				st.logger().log(WARNING, "Attempted access to " + st.getDebasedURL() + " which requires missing permission " + content.requiresPermission());
				throw new UserException("Access to this page is denied, you require permission " + content.requiresPermission());
			}
		}
		content.run(st, values);
		for (String value : values.keySet()) { f.readValue(value, values.get(value)); }

		return f.asHtml(st, isRich());
	}

	@Nonnull
	public SafeMap getPostValues(@Nonnull State st) {
		SafeMap values = new SafeMap();
		Form f = st.form;
		HttpRequest req = st.req;
		// needs to have an entity to be a post
		if (req instanceof HttpEntityEnclosingRequest) {
			InputStream contentstream = null;
			try {
				// cast it
				HttpEntityEnclosingRequest entityrequest = (HttpEntityEnclosingRequest) req;
				HttpEntity entity = entityrequest.getEntity();
				// the content, as a stream (:/)
				contentstream = entity.getContent();

				// make a buffer, read, make a string, voila :P
				int available = contentstream.available();
				if (available == 0) { return values; } //not actually a post
				byte[] array = new byte[available];
				contentstream.read(array);
				String content = new String(array);
				// parse the string into post variables
				//System.out.println(content);
				// this should probably be done "properly"
				String[] parts = content.split("&");
				for (String part : parts) {
					String[] keyvalue = part.split("=");
					String key = URLDecoder.decode(keyvalue[0], "UTF-8");
					String value = "";
					if (keyvalue.length > 1) { value = URLDecoder.decode(keyvalue[1], "UTF-8"); }
					if (keyvalue.length > 2) {
						throw new SystemException("Unexpected parsing of line '" + part + "' - got " + keyvalue.length + " fields");
					}
					if (value != null && !value.isEmpty()) { values.put(key, value); }
					//System.out.println("HTTP POST ["+key+"]=["+value+"]");
				}
			} catch (IOException ex) {
				st.logger().log(SEVERE, "Unexpected IOException reading form post data?", ex);
			} catch (UnsupportedOperationException ex) {
				st.logger().log(WARNING, "Unsupported Operation Exception reading form post data?", ex);
			} finally {
				try {
					if (contentstream != null) { contentstream.close(); }
				} catch (IOException ex) {
					st.logger().log(WARNING, "Unexpected IOException closing stream after primary exception?", ex);
				}
			}
		}
		return values;
	}

	protected boolean cookieAuthenticationOnly() { return false; }


	// this should probably be done better, i dont think we have to "split" on ; as i think the API will decompose that for us if we ask nicely
	@Nullable
	public String extractGPHUDCookie(@Nonnull State st) {
		for (Header h : st.req.getHeaders("Cookie")) {
			for (String piece : h.getValue().split(";")) {
				piece = piece.trim();
				if (piece.startsWith("gphud=")) {
					return piece.substring(6);
				}
			}
		}
		return null;
	}
	@Nullable
	public String extractClusterCookie(@Nonnull State st){
		for (Header h : st.req.getHeaders("Cookie")) {
			for (String piece : h.getValue().split(";")) {
				piece = piece.trim();
				if (piece.startsWith("coagulateslsessionid=")) {
					return piece.substring("coagulateslsessionid=".length());
				}
			}
		}
		return null;
	}


	// override me if you want to disable authentication or something :P
	// return "null" to proceed with normal stuff (modify the context, store auth results here).
	// return a Form if you want to intercept the connection to authenticate it
	//
	// generally our job is to set up the avatar/instance/character stuff
	@Nullable
	public Form authenticationHook(@Nonnull State st, @Nonnull SafeMap values) throws SystemException {
		boolean debug = false;
		// FIRSTLY, pick up any existing session data
		String cookie = extractGPHUDCookie(st);
		String coagulateslcookie = extractClusterCookie(st);
		extractURLCookieAndRedirect(st);
		Cookies cookies = Cookies.loadOrNull(cookie); // can i have cookie?
		if (cookies!=null) { cookies.setStateFromCookies(st); } // already native logged in, load state from cookie
		if (cookies == null && coagulateslcookie != null && !coagulateslcookie.isEmpty()) { setupStateFromCluster(st,coagulateslcookie); } // cluster login
		// are we authenticated now?
		if (st.avatar!=null || st.getCharacterNullable()!=null) { return null; }
		if (cookieAuthenticationOnly()) {
			Form failed = new Form();
			if (cookie != null && !"".equals(cookie)) {
				failed.add("Sorry, your session has expired, please start a new session somehow");
			} else {
				failed.add("Sorry, login failed, cookie not received at this time.");
			}
			return failed;
		}

		Form login = new Form();
		Text topline = new Text("");
		login.add(topline);
		login.add("<h3>Welcome to GPHUD</h3><p>Please provide authentication:</p>");
		Table t = new Table();
		t.add("Username:").add(new TextInput("username")).closeRow();
		t.add("Password:").add(new PasswordInput("password")).closeRow();
		t.add(new Button("Submit"));
		login.add(t);
		st.form = login;
		String username = values.get("username");
		String password = values.get("password");
		String failed = "";
		if ("Submit".equals(values.get("Submit")) && !(username.isEmpty()) && !(password.isEmpty())) {
			User target = User.findOptional(username);
			if (target == null) {
				failed = "Incorrect credentials.";
				st.logger().log(WARNING, "Attempt to login as '" + username + "' failed, no such user.");
			} else {
				if (target.checkPassword(password)) {
					cookie = Cookies.generate(target, null, null, true);
					st.username = username;
					st.avatar = target;
					st.cookiestring = cookie;
					try {
						st.cookie = new Cookies(cookie);
					} catch (SystemException ex) {
						st.logger().log(SEVERE, "Cookie load gave exception, right after it was generated?", ex);
					}
					st.resp.addHeader("Set-Cookie", "gphud=" + cookie + "; Path=/");
					st.logger().log(INFO, "Logged in from " + st.address.getHostAddress());
					return characterSelectionHook(st, values);
				} else {
					st.logger().log(WARNING, "Attempt to login as '" + username + "' failed, wrong password.");
					failed = "Incorrect credentials.";
				}
			}
		}
		login.add(failed);
		return login;
	}

	private void setupStateFromCluster(@Nonnull State st, String coagulateslcookie) {
		Session slsession = Session.get(coagulateslcookie);
		if (slsession != null) {
			User av = slsession.user();
			if (av != null) {
				st.setAvatar(av);
				String cookie = Cookies.generate(av, null, null, true);
				st.cookiestring = cookie;
				try { st.cookie = new Cookies(cookie); }
				catch (SystemException ex) { st.logger().log(SEVERE, "Cookie load gave exception, right after it was generated?", ex); }
				st.resp.addHeader("Set-Cookie", "gphud=" + cookie + "; Path=/");
				st.logger().log(INFO, "SL Cluster Services SSO as " + av);
			}
		}
	}

	private void extractURLCookieAndRedirect(@Nonnull State st) {
		String array[] = st.getDebasedURL().split("\\?"); // URLs passed always takes precedence
		for (String piece : array) {
			if (piece.startsWith("gphud=")) {
				String cookie = piece.substring("gphud=".length());
				st.resp.addHeader("Set-Cookie", "gphud=" + cookie + "; Path=/");
				st.setURL(st.getFullURL().replaceAll("\\?gphud=.*", ""));
				throw new RedirectionException(st.getDebasedURL());
			}
		}
	}

	// A login must select an avatar from its list of avatars, if it has more than one...
	@Nullable
	private Form characterSelectionHook(@Nonnull State st, @Nonnull Map<String, String> values) {
		if (1 == 1) { return null; }
		if (st.getCharacter() != null) { return null; } // already have one from cookie etc
		Set<Char> characters = Char.getCharacters(st.getInstance(), st.getAvatar());
		//if (characters.isEmpty()) { Form f=new Form(); f.add("You have no active characters at any instances, please visit an instance to commence."); return f; }
		// technically you should be able to do stuff as an avatar alone, but...
		if (characters.isEmpty()) { return null; }
		if (characters.size() == 1) {
			st.setCharacter(characters.iterator().next());
			st.cookie.setCharacter(st.getCharacter());
			return null;
		}
		Form selectavatars = new Form();
		selectavatars.add(new TextHeader("Select a character"));
		Map<Button, Char> buttons = new HashMap<>();
		for (Char e : characters) {
			Button b = new Button(e.getName());
			buttons.put(b, e);
			selectavatars.add(b);
			selectavatars.add("<br>");
		}
		st.form = selectavatars;
		for (Char e : characters) {
			if (values.get(e.getName()) != null && !values.get(e.getName()).isEmpty()) {
				st.setCharacter(e);
				st.cookie.setCharacter(st.getCharacter());
				return null;
			}
		}
		return selectavatars;
	}

}
