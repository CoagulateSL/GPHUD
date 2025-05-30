package net.coagulate.GPHUD.Interfaces.User;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.HTML.Elements.Raw;
import net.coagulate.Core.HTML.Page;
import net.coagulate.Core.Tools.ExceptionTools;
import net.coagulate.GPHUD.Data.Cookie;
import net.coagulate.GPHUD.Data.Message;
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
import net.coagulate.SL.Config;
import net.coagulate.SL.Data.Session;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.HTTPPipelines.PlainTextMapper;
import net.coagulate.SL.SL;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.util.logging.Level.*;


/**
 * Handles User HTML connections.
 * <p>
 * I.e. the start of the HTML interface.
 *
 * @author iain
 */
public class Interface extends net.coagulate.GPHUD.Interfaces.Interface {
	
	@Override
	protected void executePage(final Method content) {
		try {
			//todo
			//noinspection deprecation
			Page.page().add(new Raw(renderHTML(state())));
			if (state().suppressOutput()) {
				return;
			}
			Page.page().template(new PlainTextMapper.PlainTextTemplate());
		} catch (@Nonnull final RedirectionException redir) {
			Page.page().responseCode(HttpStatus.SC_SEE_OTHER);
			String targeturl=redir.getURL();
			//System.out.println("PRE:"+targeturl);
			if (targeturl.startsWith("/")&&!targeturl.startsWith("/GPHUD")) {
				targeturl="/GPHUD"+targeturl;
			}
			//System.out.println("POST:"+targeturl);
			Page.page().addHeader("Location",targeturl);
		}
	}
	
	@Override
	protected void initialiseState(final HttpRequest request,
	                               final HttpContext context,
	                               final Map<String,String> parameters,
	                               final Map<String,String> cookies) {
		final State state=state();
		state.source=State.Sources.USER;
		state.parameterDebugRaw=new SafeMap();
		state.parameterDebugRaw.putAll(parameters);
	}
	
	@Nullable
	@Override
	protected Method lookupPageFromUri(final String uri) {
		state().setURL(uri);
		try {
			return getClass().getDeclaredMethod("renderHTML",State.class);
		} catch (final NoSuchMethodException e) {
			throw new SystemImplementationException("Internal reflection failed with method not found",e);
		}
	}
	
	// FIRST STEP of producing the page its self, this is all wrapped in a low level exception handler.
	@Nonnull
	public String renderHTML(@Nonnull final State st) {
		// This is basically the page template, the supporting structure that surrounds a title/menu/body
		
		// we compute the body first, so any changes it causes to the rest of the data (like logging out, menu changes etc) is reflected
		final String body=renderBodyProtected(st);
		if (st.suppressOutput()) {
			return "";
		}
		String p="";
		p+=headerHTML(st);
		p+=body;
		p+=footerHTML(st);
		return p;
	}
	
	// SECOND STAGE - called by the outline code, this is a wrapper around renderBody that "nicely" handles all deeper exceptions
	@Nonnull
	protected String renderBodyProtected(@Nonnull final State st) {
		// with all exception protections, in a 'sensible' way.
		try {
			// literally, just a wrapper
			return renderBody(st);
		} catch (@Nonnull final Exception t) {
			// Exception in processing the command
			if (t instanceof RedirectionException) {
				throw (RedirectionException)t;
			} // propagate to the top where it's handled
			try {
				t.printStackTrace();
				if (UserException.class.isAssignableFrom(t.getClass())) {
					String r="<h1>ERROR</h1><p>Sorry, your request could not be completed<br><pre>"+
					         t.getLocalizedMessage()+"</pre></p>";
					GPHUD.getLogger().log(INFO,"UserInterface/UserException "+t);
					if (Config.getDevelopment()) {
						r+="<hr><h1 align=center>DEV MODE</h1><hr><h1>User Mode Exception</h1>"+
						   ExceptionTools.dumpException(t);
						SL.report("GPHUD Web User Exception",t,st);
						GPHUD.getLogger().log(WARNING,"UserInterface/UserException",t);
					}
					return r;
				}
				SL.report("GPHUD Web Other Exception",t,st);
				GPHUD.getLogger().log(WARNING,"UserInterface/NonUserException",t);
				String r=
						"<h1>INTERNAL ERROR</h1><p>Sorry, your request could not be completed due to an internal error.</p>";
				if (Config.getDevelopment()) {
					r+="<hr><h1 align=center>DEV MODE</h1><hr><h1>NonUser Exception</h1>"+
					   ExceptionTools.dumpException(t);
				}
				return r;
			} catch (@Nonnull final Exception f) {
				GPHUD.getLogger().log(SEVERE,"Exception in exception handler",f);
				return "EXCEPTION IN EXCEPTION HANDLER, PANIC!"; // nice
			}
		}
	}
	
	private String headerHTML(@Nonnull final State st) {
		String p="";
		final boolean external=st.getDebasedNoQueryURL().toLowerCase().startsWith("/published/");
		p+="<html><head><title>";
		p+="GPHUD";
		p+="</title>";
		if (!external) {
			p+=styleSheet();
			p+="<link rel=\"shortcut icon\" href=\"/resources/icon-gphud.png\">";
		}
		p+="</head><body>";
		if (!external) {
			p+="<table height=100% valign=top><tr><td colspan=3 align=center width=100%>";
			p+="<table style=\"margin: 0px; border:0px;\" width=100%><tr><td width=33% align=left>";
			p+="<h1 style=\"margin: 0px;\"><img src=\"/resources/banner-gphud.png\"></h1>";
			p+="</td><td width=34% align=center>";
			String middletarget="/resources/banner-gphud.png";
			int width=200;
			if (st.getInstanceNullable()!=null) {
				middletarget=st.getInstance().getLogoURL(st);
				width=st.getInstance().getLogoWidth(100);
			}
			p+="<h1 style=\"margin: 0px;\"><img src=\""+middletarget+"\" height=100px width="+width+"px></h1>";
			p+="</td><td width=33% align=right>";
			p+="<h1 style=\"margin: 0px;\"><a href=\"/\">"+SL.getBannerHREF()+"</a></h1>";
			p+="</td></tr></table>";
			//p+="<i>"+GPHUD.environment()+"</i>";
			p+="<hr>";
			p+="</td></tr>";
			p+="<tr height=100% valign=top>";
			p+="<td width=150px valign=top height=100%>";
			// calculate body first, since this sets up auth etc which the side menu will want to use to figure things out.
			
			try {
				p+=renderSideMenu(st);
			} catch (@Nonnull final Exception e) {
				// Exception in side menu code
				p+="<b><i>Crashed</i></b>";
				SL.report("GPHUD Side Menu crashed",e,st);
				st.logger().log(WARNING,"Side menu implementation crashed",e);
			}
			p+="</td><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td><td width=100% valign=top height=100%>";
			p+=messages(st);
		}
		return p;
	}
	
	@Nonnull
	private String footerHTML(@Nonnull final State st) {
		String p="";
		final boolean external=st.getDebasedNoQueryURL().toLowerCase().startsWith("/published/");
		if (!external) {
			p+="</td>";
			p+="</tr>";
			p+="</table>";
		}
		p+="</body></html>";
		return p;
	}
	
	// THIRD stage (process -> renderHTML (page layout) -> renderBodyProtected -> renderBody)
	@Nonnull
	public String renderBody(@Nonnull final State st) {
		Form f=null;
		final SafeMap values=getPostValues(st);
		st.postMap(values);
		URL content=Modules.getURL(st,st.getDebasedNoQueryURL());
		// call authenticator, it will return null if it managed something, otherwise it returns a login form which we'll render and exit
		if (content.requiresAuthentication()) {
			f=authenticationHook(st,values);
		}
		if (f!=null) {
			st.setForm(f);
			return f.asHtml(st,isRich());
		}
		// some kinda login information exists
		st.fleshOut();
		if (st.getAvatarNullable()!=null&&st.getAvatar().isSuspended()) {
			throw new UserAccessDeniedException(
					"Your access to GPHUD has been suspended.  If you feel this is in error please contact the system operator ");
		}
		content=Modules.getURL(st,st.getDebasedNoQueryURL());
		//System.out.println("Post auth URL is "+st.getDebasedURL()+" and form is "+f+" and content is "+content.getFullName()+" and interceptable is "+interceptable(st
		// .getDebasedNoQueryURL()));
		if (st.getInstanceNullable()==null&&interceptable(st.getDebasedNoQueryURL())) {
			content=Modules.getURL(st,"/GPHUD/switch/instance");
		} //f=new Form(); f.add(new TextHeader("Module "+content.getModule().getName()+" is inaccessible as no instance is currently selected")); }
		if (st.getInstanceNullable()!=null&&!content.getModule().isEnabled(st)) {
			f=new Form();
			f.add(new TextHeader(
					"Module "+content.getModule().getName()+" is not enabled in instance "+st.getInstanceString()));
		}
		//System.out.println("Post post-auth URL is "+st.getDebasedURL()+" and form is "+f+" and content is "+content.getFullName()+" and interceptable is "+interceptable(st
		// .getDebasedNoQueryURL()));
		f=new Form();
		st.setForm(f);
		if (!content.requiresPermission().isEmpty()) {
			if (!st.hasPermission(content.requiresPermission())) {
				st.logger()
				  .log(WARNING,
				       "Attempted access to "+st.getDebasedURL()+" which requires missing permission "+
				       content.requiresPermission());
				throw new UserAccessDeniedException(
						"Access to this page is denied, you require permission "+content.requiresPermission());
			}
		}
		content.run(st,values);
		for (final String value: values.keySet()) {
			f.readValue(value,values.get(value));
		}
		
		return f.asHtml(st,isRich());
	}
	
	// ---------- STATICS ----------
	// leave this here for now
	@Nonnull
	public static String styleSheet() {
		//"    width: 120px;\n" +
		return """
				<style>
				.tooltip {
				    position: relative;
				    display: inline-block;
				    border-bottom: 1px dotted black; /* If you want dots under the hover text */
				}
				
				.tooltip .tooltiptext {
				    visibility: hidden;
				    background-color: #e0e0e0;
				    color: black;
				    text-align: center;
				    padding: 5px 0;
				    border-radius: 6px;
				\s
				     top: -5px;
				    left: 105%;     position: absolute;
				    z-index: 1;
				white-space: nowrap;
				}
				
				/* Show the tooltip text when you mouse over the tooltip container */
				.tooltip:hover .tooltiptext {
				    visibility: visible;
				}
				</style>""";
	}
	
	@Nonnull
	String renderSideMenu(@Nonnull final State st) {
		final StringBuilder s=new StringBuilder();
		s.append(GPHUD.menuPanelEnvironment()).append("<hr width=150px>");
		if (st.getCharacterNullable()!=null||st.getAvatarNullable()!=null) {
			s.append("<b>Avatar:</b> ");
			//if (st.user!=null) s+="[<a href=\"/GPHUD/switch/avatar\">Switch</a>]"; // you can only switch avis if you're a logged in user, as that's what binds avis
			s.append("<br>");
			if (st.getAvatarNullable()==null) {
				s.append("<i>none</i><br>");
			} else {
				s.append(st.getAvatarNullable().getGPHUDLink()).append("<br>");
			}
			
			
			s.append("<b>Instance:</b> [<a href=\"/GPHUD/switch/instance\">Switch</a>]<br>");
			if (st.getInstanceNullable()==null) {
				s.append("<i>none</i><br>");
			} else {
				s.append(st.getInstance().asHtml(st,true)).append("<br>");
			}
			
			s.append("<b>Character:</b> [<a href=\"/GPHUD/switch/character\">Switch</a>]<br>");
			if (st.getCharacterNullable()==null) {
				s.append("<i>none</i><br>");
			} else {
				s.append(st.getCharacter().asHtml(st,true)).append("<br>");
			}
		} else {
			s.append("<i>Not logged in</i><hr width=150px><a href=\"/GPHUD/\">Index</a><br><br>");
			s.append("<a href=\"https://docs.sl.coagulate.net/\" target=\"_new\">Documentation</a><br>");
			s.append("<a href=\"/GPHUD/ChangeLog\">Change Log</a><br>");
			s.append("<hr width=150px>");
			return s.toString();
		}
		s.append("<br><a href=\"/GPHUD/logout\">Logout</a><br>");
		s.append("<hr width=150px>");
		s.append("<a href=\"/GPHUD/\">Index</a><br><br>");
		boolean dynamics=true;
		if (st.getAvatarNullable()==null) {
			s.append("<i>Select an avatar</i><br>");
			dynamics=false;
		}
		if (st.getInstanceNullable()==null) {
			s.append("<i>Select an instance</i><br>");
			dynamics=false;
		}
		if (dynamics) {
			s.append(dynamicSideMenus(st));
			s.append("<br>");
		}
		s.append("<a href=\"https://docs.sl.coagulate.net/\" target=\"_new\">Documentation</a><br>");
		s.append("<a href=\"/GPHUD/ChangeLog\">Change Log</a><br>");
		s.append("<hr width=150px>");
		String sectionHead="<b>PERMISSIONS:</b><br>";
		if (st.isSuperUser()) {
			s.append(sectionHead).append("<b style=\"color: blue;\">SUPER-ADMIN</b><br>");
			sectionHead="";
		}
		if (st.isInstanceOwner()) {
			s.append(sectionHead).append("<b style=\"color: blue;\">Instance Owner</b><br>");
			sectionHead="";
		}
		if (st.getPermissions()!=null) {
			for (final String permission: st.getPermissions()) {
				s.append(sectionHead).append("<font style=\"color: green;\">").append(permission).append("</font><br>");
				sectionHead="";
			}
		}
		return s.toString();
	}
	
	@Nonnull
	public String messages(@Nonnull final State st) {
		if (st.getCharacterNullable()==null) {
			return "";
		}
		final int messages=Message.count(st);
		if (messages>0) {
			return "<p>"+
			       new Link("<b>You have "+messages+" unread message"+(messages>1?"s":"")+", click here to read</b>",
			                "/GPHUD/messages/list").asHtml(st,true)+"</p>";
		}
		return "";
	}
	
	@Nonnull
	public SafeMap getPostValues(@Nonnull final State st) {
		if (st.parameterDebugRaw==null) {
			throw new SystemImplementationException("Parameter debug raw map is null (?)");
		}
		return st.parameterDebugRaw;
		/*
		final SafeMap values=new SafeMap();
		final HttpRequest req=st.req();
		// needs to have an entity to be a post
		if (req instanceof HttpEntityEnclosingRequest) {
			InputStream contentstream=null;
			try {
				// cast it
				final HttpEntityEnclosingRequest entityrequest=(HttpEntityEnclosingRequest) req;
				final HttpEntity entity=entityrequest.getEntity();
				// the content, as a stream (:/)
				contentstream=entity.getContent();
				// make a buffer, read, make a string, voila :P
				final String content= ByteTools.convertStreamToString(contentstream);
				// this should probably be done "properly"
				final String[] parts=content.split("&");
				for (final String part: parts) {
					final String[] keyvalue=part.split("=");
					final String key= URLDecoder.decode(keyvalue[0], StandardCharsets.UTF_8);
					String value="";
					if (keyvalue.length>1) { value=URLDecoder.decode(keyvalue[1], StandardCharsets.UTF_8); }
					if (keyvalue.length>2) {
						throw new SystemBadValueException("Unexpected parsing of line '"+part+"' - got "+keyvalue.length+" fields");
					}
					if (value!=null && !value.isEmpty()) { values.put(key,value); }
					//System.out.println("HTTP POST ["+key+"]=["+value+"]");
				}
			}
			catch (@Nonnull final IOException ex) {
				st.logger().log(SEVERE,"Unexpected IOException reading form post data?",ex);
			}
			catch (@Nonnull final UnsupportedOperationException ex) {
				st.logger().log(WARNING,"Unsupported Operation Exception reading form post data?",ex);
			}
			finally {
				try {
					if (contentstream!=null) { contentstream.close(); }
				}
				catch (@Nonnull final IOException ex) {
					st.logger().log(WARNING,"Unexpected IOException closing stream after primary exception?",ex);
				}
			}
		}
		return values;

		 */
	}
	
	// override me if you want to disable authentication or something :P
	// return "null" to proceed with normal stuff (modify the context, store auth results here).
	// return a Form if you want to intercept the connection to authenticate it
	//
	// generally our job is to set up the avatar/instance/character stuff
	@Nullable
	public Form authenticationHook(@Nonnull final State st,@Nonnull final SafeMap values) {
		// FIRSTLY, pick up any existing session data
		String cookie=extractGPHUDCookie(st);
		final String coagulateslcookie=extractClusterCookie(st);
		extractURLCookieAndRedirect(st);
		final Cookie cookies=Cookie.loadOrNull(cookie); // can i have cookie?
		if (cookies!=null) {
			cookies.setStateFromCookies(st);
		} // already native logged in, load state from cookie
		if (cookies==null&&coagulateslcookie!=null&&!coagulateslcookie.isEmpty()) {
			setupStateFromCluster(st,coagulateslcookie);
		} // cluster login
		// are we authenticated now?
		if (st.avatar!=null||st.getCharacterNullable()!=null) {
			return null;
		}
		if (cookieAuthenticationOnly()) {
			final Form failed=new Form();
			if (cookie!=null&&!cookie.isEmpty()) {
				failed.add("Sorry, your session has expired, please start a new session somehow");
			} else {
				failed.add("Sorry, login failed, cookie not received at this time.");
			}
			return failed;
		}
		
		final Form login=new Form();
		final Text topline=new Text("");
		login.add(topline);
		login.add("<h3>Welcome to GPHUD</h3><p>Please provide authentication:</p>");
		final Table t=new Table();
		t.add("Username:").add(new TextInput("username")).closeRow();
		t.add("Password:").add(new PasswordInput("password")).closeRow();
		t.add(new Button("Submit"));
		login.add(t);
		st.setForm(login);
		final String username=values.get("username");
		final String password=values.get("password");
		String failed="";
		if ("Submit".equals(values.get("Submit"))&&!(username.isEmpty())&&!(password.isEmpty())) {
			final User target=User.findUsernameNullable(username,false); // users may use their old user name...
			if (target==null) {
				failed="Incorrect credentials.";
				st.logger().log(WARNING,"Attempt to login as '"+username+"' failed, no such user.");
			} else {
				if (target.checkPassword(password)) {
					cookie=Cookie.generate(target,null,null,true);
					st.username=username;
					st.avatar=target;
					st.cookieString=cookie;
					try {
						st.cookie(new Cookie(cookie));
					} catch (@Nonnull final UserException ex) {
						st.logger().log(SEVERE,"Cookie load gave exception, right after it was generated?",ex);
					}
					Page.page().addHeader("Set-Cookie","gphud="+cookie+"; Path=/");
					st.logger().log(INFO,"Logged in from "+st.getClientIP());
					return null; //return characterSelectionHook(st, values);
				} else {
					st.logger().log(WARNING,"Attempt to login as '"+username+"' failed, wrong password.");
					failed="Incorrect credentials.";
				}
			}
		}
		login.add(failed);
		return login;
	}
	
	public boolean isRich() {
		return true;
	}
	
	/**
	 * URLs we do not redirect to instance/char selection.  Like logout.
	 */
	private boolean interceptable(@Nullable final String url) {
		if ("/logout".equalsIgnoreCase(url)) {
			return false;
		}
		if ("/Help".equalsIgnoreCase(url)) {
			return false;
		}
		if ("/ChangeLog".equalsIgnoreCase(url)) {
			return false;
		}
		return url==null||!url.toLowerCase().startsWith("/published/");
	}
	
	@Nonnull
	String dynamicSideMenus(@Nonnull final State st) {
		final StringBuilder r=new StringBuilder();
		final Map<Integer,Set<SideMenu>> priorities=new TreeMap<>();
		// collect sidemenus, by priority
		for (final Module m: Modules.getModules()) {
			if (m.isEnabled(st)) {
				final SideMenu s=m.getSideMenu(st);
				if (s!=null) {
					boolean permitted=true;
					if (!s.requiresPermission().isEmpty()) {
						if (!st.hasPermission(s.requiresPermission())) {
							permitted=false;
						}
					}
					if (permitted) {
						final Integer priority=s.priority();
						Set<SideMenu> set=new HashSet<>();
						if (priorities.containsKey(priority)) {
							set=priorities.get(priority);
						}
						set.add(s);
						priorities.put(s.priority(),set);
					}
				}
			}
		}
		// enumerate the priorities
		for (final Set<SideMenu> sideMenus: priorities.values()) {
			// enumerate the SideMenus
			for (final SideMenu menu: sideMenus) {
				final String url=menu.url();
				final String name=menu.name();
				r.append("<a href=\"/GPHUD").append(url).append("\">").append(name).append("</a><br>");
				if (st.getDebasedURL().startsWith(url)) {
					r.append(dynamicSubMenus(st,menu));
				}
			}
		}
		
		
		return r.toString();
	}
	
	// this should probably be done better, i dont think we have to "split" on ; as i think the API will decompose that for us if we ask nicely
	@Nullable
	public String extractGPHUDCookie(@Nonnull final State st) {
		for (final Header h: st.req().getHeaders("Cookie")) {
			for (String piece: h.getValue().split(";")) {
				piece=piece.trim();
				if (piece.startsWith("gphud=")) {
					return piece.substring(6);
				}
			}
		}
		return null;
	}
	
	@Nullable
	public String extractClusterCookie(@Nonnull final State st) {
		for (final Header h: st.req().getHeaders("Cookie")) {
			for (String piece: h.getValue().split(";")) {
				piece=piece.trim();
				if (piece.startsWith("coagulateslsessionid=")) {
					return piece.substring("coagulateslsessionid=".length());
				}
			}
		}
		return null;
	}
	
	private void setupStateFromCluster(@Nonnull final State st,final String coagulateSLCookie) {
		final Session slSession=Session.get(coagulateSLCookie);
		if (slSession!=null) {
			final User av=slSession.user();
			if (av!=null) {
				st.setAvatar(av);
				final String cookie=Cookie.generate(av,null,null,true);
				st.cookieString=cookie;
				try {
					st.cookie(new Cookie(cookie));
				} catch (@Nonnull final UserException ex) {
					st.logger().log(SEVERE,"Cookie load gave exception, right after it was generated?",ex);
				}
				Page.page().addHeader("Set-Cookie","gphud="+cookie+"; Path=/");
				st.logger().log(INFO,"SL Stack SSO as "+av);
			}
		}
	}
	
	protected boolean cookieAuthenticationOnly() {
		return false;
	}
	
	// ----- Internal Instance -----
	@Nonnull
	String dynamicSubMenus(final State st,final SideMenu menu) {
		// dereference the menu into a module
		Module owner=null;
		for (final Module m: Modules.getModules()) {
			if (m.getSideMenu(st)==menu) {
				owner=m;
			}
		}
		if (owner==null) {
			return ">> NULL?<br>";
		}
		
		final StringBuilder ret=new StringBuilder();
		final Map<Integer,Set<SideSubMenu>> priorities=new TreeMap<>();
		// collect sidemenus, by priority
		final Set<SideSubMenu> sidesubmenus=owner.getSideSubMenus(st);
		if (sidesubmenus!=null) {
			for (final SideSubMenu s: sidesubmenus) {
				//System.out.println("Testing "+s.name());
				final Integer priority=s.priority();
				Set<SideSubMenu> set=new HashSet<>();
				if (priorities.containsKey(priority)) {
					set=priorities.get(priority);
				}
				set.add(s);
				priorities.put(s.priority(),set);
			}
		}
		// enumerate the priorities
		for (final Set<SideSubMenu> sideSubMenus: priorities.values()) {
			// enumerate the SideMenus
			for (final SideSubMenu s: sideSubMenus) {
				final String u=s.getURL();
				ret.append("&nbsp;&nbsp;&nbsp;&gt;&nbsp;&nbsp;&nbsp;<a href=\"/GPHUD")
				   .append(u)
				   .append("\">")
				   .append(s.name())
				   .append("</a><br>");
			}
		}
		return ret.toString();
	}
	
	private void extractURLCookieAndRedirect(@Nonnull final State st) {
		final String[] array=st.getDebasedURL().split("\\?"); // URLs passed always takes precedence
		for (final String piece: array) {
			if (piece.startsWith("gphud=")) {
				final String cookie=piece.substring("gphud=".length());
				Page.page().addHeader("Set-Cookie","gphud="+cookie+"; Path=/");
				st.setURL(st.getFullURL().replaceAll("\\?gphud=.*",""));
				throw new RedirectionException(st.getDebasedURL());
			}
		}
	}
	
	@Override
	protected void renderUserError(final HttpRequest request,
	                               final HttpContext context,
	                               final HttpResponse response,
	                               final UserException userException) {
		SL.report("GPWeb User: "+userException.getLocalizedMessage(),userException,state());
		final State st=state();
		String page=headerHTML(st);
		page+="<p><h1>Error</h1></p>";
		page+="<p>There was an error processing your request.</p>";
		page+=
				"<p>Please review your inputs and the error message, this type of error is likely more related to user data, configuration, or similar, rather than being a system error.</p>";
		page+="<p><b>ERROR:</b> "+userException.getLocalizedMessage();
		page+=footerHTML(st);
		response.setEntity(new StringEntity(page,ContentType.TEXT_HTML));
		response.setStatusCode(200);
	}
	
	@Override
	protected void renderSystemError(final HttpRequest request,
	                                 final HttpContext context,
	                                 final HttpResponse response,
	                                 final SystemException systemException) {
		SL.report("GPWeb SysEx: "+systemException.getLocalizedMessage(),systemException,state());
		final State st=state();
		String page=headerHTML(st);
		page+="<p><h1>Internal Error</h1></p>";
		page+="<p>Sorry, an internal error has occurred.</p>";
		page+="<p>The system administrator has been mailed about this issue.</p>";
		page+=footerHTML(st);
		response.setEntity(new StringEntity(page,ContentType.TEXT_HTML));
		response.setStatusCode(200);
	}
	
	/*// A login must select an avatar from its list of avatars, if it has more than one...
	@Nullable
	private Form characterSelectionHook(@Nonnull final State st,
	                                    @Nonnull final Map<String,String> values) {
		//if (1 == 1) { return null; }
		final Set<Char> characters=Char.getCharacters(st.getInstance(),st.getAvatar());
		//if (characters.isEmpty()) { Form f=new Form(); f.add("You have no active characters at any instances, please visit an instance to commence."); return f; }
		// technically you should be able to do stuff as an avatar alone, but...
		if (characters.isEmpty()) { return null; }
		if (characters.size()==1) {
			st.setCharacter(characters.iterator().next());
			st.cookie().setCharacter(st.getCharacter());
			return null;
		}
		final Form selectavatars=new Form();
		selectavatars.add(new TextHeader("Select a character"));
		//final Map<Button, Char> buttons = new HashMap<>();
		for (final Char e: characters) {
			final Button b=new Button(e.getName());
			//buttons.put(b, e);
			selectavatars.add(b);
			selectavatars.add("<br>");
		}
		st.setForm(selectavatars);
		for (final Char e: characters) {
			if (values.get(e.getName())!=null && !values.get(e.getName()).isEmpty()) {
				st.setCharacter(e);
				st.cookie().setCharacter(st.getCharacter());
				return null;
			}
		}
		return selectavatars;
	}*/
	@Override
	protected void renderUnhandledError(final HttpRequest request,
	                                    final HttpContext context,
	                                    final HttpResponse response,
	                                    final Throwable t) {
		SL.report("GPWeb UnkEx: "+t.getLocalizedMessage(),t,state());
		final State st=state();
		String page=headerHTML(st);
		page+="<p><h1>Unhandled Internal Error</h1></p>";
		page+="<p>Sorry, an unhandled internal error has occurred.</p>";
		page+="<p>The system administrator has been mailed about this issue.</p>";
		page+=footerHTML(st);
		response.setEntity(new StringEntity(page,ContentType.TEXT_HTML));
		response.setStatusCode(200);
	}
}
