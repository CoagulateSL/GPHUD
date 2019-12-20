package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.User.UserInputTooLongException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.Inputs.*;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.logging.Level.WARNING;

/**
 * A command, probably derived from Annotations.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Command {

	@Nonnull
	static Object assertNotNull(@Nullable final Object o,
	                            final String value,
	                            final String type) {
		if (o==null) {
			throw new UserInputLookupFailureException("Unable to resolve '"+value+"' to a "+type);
		}
		return o;
	}

	protected static void checkPublicStatic(@Nonnull final Method m) {
		if (!Modifier.isStatic(m.getModifiers())) {
			throw new SystemImplementationException("Method "+m.getDeclaringClass().getName()+"/"+m.getName()+" must be static");
		}
		if (!Modifier.isPublic(m.getModifiers())) {
			throw new SystemImplementationException("Method "+m.getDeclaringClass().getName()+"/"+m.getName()+" must be public");
		}
	}

	@Nonnull
	public abstract Method getMethod();

	public abstract boolean isGenerated();

	public abstract String description();

	public abstract String requiresPermission();

	public abstract Context context();

	public abstract boolean permitJSON();

	public abstract boolean permitObject();

	public abstract boolean permitConsole();

	public abstract boolean permitUserWeb();

	public abstract boolean permitScripting();

	@Nonnull
	public abstract List<Argument> getArguments();

	public abstract int getArgumentCount();

	@Nonnull
	public abstract String getFullName();

	@Nonnull
	public abstract String getName();

	@Nonnull
	@SuppressWarnings("fallthrough")
	public Response run(@Nonnull final State st,
	                    @Nonnull final String[] args) {
		final boolean debug=false;
		final List<Object> typedargs=new ArrayList<>();
		int arg=0;
		typedargs.add(st);
		for (final Argument argument: getInvokingArguments()) {
			if (argument==null) {
				throw new SystemImplementationException("Argument metadata null on "+getFullName()+"() arg#"+(arg+1));
			}
			final ArgumentType type=argument.type();
			String v;
			if (args.length>arg) { v=args[arg]; }
			else { v=""; }
			arg++;
			if ((v==null || "".equals(v)) && type!=ArgumentType.BOOLEAN) {
				typedargs.add(null);
			}
			else {
				final int maxlen;
				switch (type) {
					case TEXT_CLEAN:
					case TEXT_ONELINE:
					case TEXT_INTERNAL_NAME:
					case TEXT_MULTILINE:
						maxlen=argument.max();
						break;
					case PASSWORD:
					case CHOICE:
						maxlen=1024;
						break;
					case BOOLEAN:
						maxlen=8;
						break;
					case INTEGER:
					case FLOAT:
						maxlen=32;
						break;
					case CHARACTER:
					case ATTRIBUTE_WRITABLE:
					case ATTRIBUTE:
					case COORDINATES:
					case ZONE:
					case REGION:
					case MODULE:
					case PERMISSION:
					case PERMISSIONSGROUP:
					case AVATAR_NEAR:
					case AVATAR:
					case CHARACTER_FACTION:
					case CHARACTER_NEAR:
					case CHARACTER_PLAYABLE:
						maxlen=64;
						break;
					case CHARACTERGROUP:
					case EVENT:
					case KVLIST:
						maxlen=128;
						break;
					default:
						throw new AssertionError(type.name());

				}
				if (maxlen<1) {
					st.logger().warning("Command "+getClass().getSimpleName()+" argument "+argument.getName()+" does not specify a max, assuming 65k...");
				}
				else {
					if (v!=null && v.length()>maxlen) {
						throw new UserInputTooLongException(argument.getName()+" is "+v.length()+" characters long and must be no more than "+maxlen+".  Input has not been "+"processed, please try again");
					}
				}
				switch (type) {
					case TEXT_INTERNAL_NAME:
						if (v.matches(".*[^a-zA-Z0-9].*")) {
							return new ErrorResponse(argument.getName()+" should only consist of alphanumeric characters (a-z 0-9) and you entered '"+v+"'");
						}
						// dont put anything here, follow up into the next thing
					case TEXT_CLEAN:
						if (v.matches(".*[^a-zA-Z0-9.'\\-, ].*")) {
							return new ErrorResponse(argument.getName()+" should only consist of typable characters (a-z 0-9 .'-,) and you entered '"+v+"'");
						}
						// dont put anything here, follow up into the next thing
					case TEXT_ONELINE:
					case TEXT_MULTILINE:
					case PASSWORD:
					case PERMISSION:
					case CHOICE:
					case KVLIST:
					case COORDINATES:
						//System.out.println("Adding arg "+v);
						typedargs.add(v);
						break;
					case BOOLEAN:
						if (("1".equals(v) || "on".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v) || "t".equalsIgnoreCase(v))) {
							typedargs.add(Boolean.TRUE);
						}
						else {
							typedargs.add(Boolean.FALSE);
						}
						break;
					case INTEGER:
						try {
							typedargs.add(Integer.valueOf(v));
						}
						catch (@Nonnull final NumberFormatException e) {
							return new ErrorResponse("Unable to convert '"+v+"' to a number for argument "+argument.getName());
						}
						break;
					case FLOAT:
						try {
							typedargs.add(Float.valueOf(v));
						}
						catch (@Nonnull final NumberFormatException e) {
							return new ErrorResponse("Unable to convert '"+v+"' to a number for argument "+argument.getName());
						}
						break;
					case MODULE:
						final Module m=Modules.get(null,v);
						if (m==null) { return new ErrorResponse("Unable to resolve module "+v); }
						typedargs.add(m);
						break;
					//case FLOAT:
					case ATTRIBUTE_WRITABLE:
					case ATTRIBUTE:
						Attribute attr=null;
						for (final Attribute a: st.getAttributes()) {
							if (a.getName().equalsIgnoreCase(v)) {
								if (attr!=null) {
									throw new SystemConsistencyException("Duplicate attribute definition found for "+v);
								}
								if (type==ArgumentType.ATTRIBUTE || a.getSelfModify()) { attr=a; }
							}
						}
						if (attr==null) {
							throw new UserInputLookupFailureException("Unable to resolve '"+v+"' to an attribute");
						}
						typedargs.add(attr);
						break;
                        /*Char targetchar=Char.resolve(st, v);
                        typedargs.add(assertNotNull(targetchar, v, "character"));
                        if (argument.getName().equalsIgnoreCase("target")) { st.setTarget(targetchar); }
                        break;*/
					case PERMISSIONSGROUP:
						typedargs.add(assertNotNull(PermissionsGroup.resolve(st,v),v,"permissions group"));
						break;
					case CHARACTERGROUP:
						typedargs.add(assertNotNull(CharacterGroup.resolve(st,v),v,"character group"));
						break;
					case CHARACTER_FACTION:
					case CHARACTER:
					case CHARACTER_PLAYABLE:
					case CHARACTER_NEAR:
						final Char targchar;
						if (v.startsWith(">")) {
							v=v.substring(1);
							try {
								final User a=User.findMandatory(v);
								targchar=Char.getActive(a,st.getInstance());
							}
							catch (@Nonnull final NoDataException e) {
								return new ErrorResponse("Unable to find character of avatar named '"+v+"'");
							}
						}
						else {
							targchar=Char.resolve(st,v);
						}
						if (targchar!=null) { typedargs.add(targchar); }
						else {
							return new ErrorResponse("Unable to find character named '"+v+"'");
						}
						break;
					case REGION:
						typedargs.add(assertNotNull(Region.findNullable(v,false),v,"region name"));
						break;
					case EVENT:
						typedargs.add(assertNotNull(Event.find(st.getInstance(),v),v,"event name"));
						break;
					case ZONE:
						typedargs.add(assertNotNull(Zone.find(st.getInstance(),v),v,"zone name"));
						break;
					case AVATAR:
					case AVATAR_NEAR:
						final User user=User.findOptional(v);
						if (user==null) { return new ErrorResponse("Unable to find a known avatar named '"+v+"'"); }
						typedargs.add(assertNotNull(user,v,"avatar"));
						break;
					default:
						throw new SystemImplementationException("Unhandled ENUM TYPE in executor:"+type);
				}
			}
		}
		return run(st,typedargs.toArray(new Object[]{}));
	}

	/**
	 * Run a command based on properly cast arguments.
	 *
	 * @param st   Session state
	 * @param args Arguments of appropriate type for receiving method (or exceptions)
	 *
	 * @return Command response
	 */
	@Nonnull
	Response run(@Nonnull final State st,
	             @Nonnull final Object[] args) {
		final boolean debug=false;
		try {
			// check permission
			if (!requiresPermission().isEmpty() && !st.hasPermission(requiresPermission())) {
				return new ErrorResponse("Permission is denied, you require '"+requiresPermission()+"'");
			}
			// check required interface
			if (st.source==State.Sources.USER) {
				if (!permitUserWeb()) {
					return new ErrorResponse("This command can not be accessed via the Web interface");
				}
			}
			if (st.source==State.Sources.SYSTEM) {
				if (!permitJSON()) {
					return new ErrorResponse("This command can not be accessed via the LSL System interface");
				}
			}
			if (st.source==State.Sources.CONSOLE) {
				if (!permitConsole()) {
					return new ErrorResponse("This command can not be accessed via the console");
				}
			}
			if (st.source==State.Sources.SCRIPTING) {
				if (!permitScripting()) {
					return new ErrorResponse("This command can not be access via the Scripting module");
				}
			}

			//check arguments
			int i=0;
			if (args.length!=getInvokingArgumentCount()+1) {
				return new ErrorResponse("Incorrect number of arguments, "+getFullName()+" aka "+getMethod().getName()+" requires "+(getInvokingArgumentCount()+1)+" and we "+"got "+args.length);
			}
			String suspiciousname="";
			for (final Argument a: getInvokingArguments()) {
				if (a.getName().startsWith("arg") && a.getName().length()==4) {
					suspiciousname=".  ***WARNING*** this argument name starts with 'arg' and may indicate javac was NOT invoked with the -parameter!!!";
				}
				if (i>0) {
					Object o=args[i];
					// I don't really like this, but...
					if (o instanceof String) {
						args[i]=((String) o).trim();
						o=args[i];
					}
					if (a.mandatory()) {
						if (o==null) {
							return new ErrorResponse("Argument "+a.getName()+" is mandatory and null was passed"+suspiciousname);
						}
						if (o instanceof String) {
							final String s=(String) o;
							if (s.isEmpty()) {
								return new ErrorResponse("Argument "+a.getName()+" is mandatory and a blank string was passed"+suspiciousname);
							}
							if ("-".equals(s)) {
								return new ErrorResponse("Argument "+a.getName()+" is mandatory and a dash '-' was passed"+suspiciousname);
							}
						}
					}
				}
				i++;
			}
			// check the "operational context" :)
			switch (context()) {
				case ANY:
					break;
				case CHARACTER:
					if (st.getInstanceNullable()==null) {
						return new ErrorResponse("Character context required and you are not connected to an instance.");
					}
					if (st.getCharacterNullable()==null) {
						return new ErrorResponse("Character context required, your request is lacking a character registration");
					}
					break;
				case AVATAR:
					if (st.getInstanceNullable()==null) {
						return new ErrorResponse("Avatar context required and you are not connected to an instance.");
					}
					if (st.getAvatarNullable()==null) {
						return new ErrorResponse("Avatar context required, your request is lacking an avatar registration");
					}
					break;
				default:
					throw new SystemImplementationException("Unhandled CONTEXT enum during pre-flight check in execute()");
			}
			return (Response) (getMethod().invoke(this,args));
		}
		catch (@Nonnull final IllegalAccessException ex) {
			throw new SystemImplementationException("Command programming error in "+getName()+" - run() access modifier is incorrect",ex);
		}
		catch (@Nonnull final IllegalArgumentException ex) {
			SL.report("Command "+getName()+" failed",ex,st);
			st.logger().log(WARNING,"Execute command "+getName()+" failed",ex);
			return new ErrorResponse("Illegal argument in "+getName());
		}
		catch (@Nonnull final InvocationTargetException ex) {
			if (ex.getCause()!=null && UserException.class.isAssignableFrom(ex.getCause().getClass())) {
				return new ErrorResponse(getName()+" errored: \n--- "+ex.getCause().getLocalizedMessage());
			}
			if (ex.getCause()!=null && SystemException.class.isAssignableFrom(ex.getCause().getClass())) {
				throw ((SystemException) ex.getCause());
			}
			throw new SystemImplementationException("Exception "+ex+" from call to "+getName(),ex);
		}
	}

	/**
	 * Get the name of the arguments.
	 *
	 * @param st state
	 *
	 * @return list of argument names
	 */
	public List<String> getArgumentNames(final State st) {
		final List<String> arguments=new ArrayList<>();
		for (final Argument a: getArguments()) {
			arguments.add(a.getName());
		}
		return arguments;
	}

	public List<Argument> getInvokingArguments() { return getArguments(); }

	public Response run(@Nonnull final State st,
	                    @Nonnull final SafeMap parametermap) {
		//System.out.println("Run in method "+this.getClass().getCanonicalName());
		final List<String> arguments=new ArrayList<>();
		for (final Argument arg: getInvokingArguments()) {
			if (parametermap.containsKey(arg.getName())) {
				//System.out.println("Added argument "+arg.getName());
				arguments.add(parametermap.get(arg.getName()));
			}
			else {
				//System.out.println("Skipped argument "+arg.getName());
				arguments.add(null);
			}
		}
		return run(st,arguments.toArray(new String[]{}));
	}

	public void simpleHtml(@Nonnull final State st,
	                       @Nonnull final SafeMap values) {
		//System.out.println("HERE:"+getArgumentCount());
		if (getArgumentCount()==0 || values.submit()) {
			final Response response=run(st,values);
			// IF this is an OK response
			if (response instanceof OKResponse) {
				// and we have cached a 'return url'
				if (!values.get("okreturnurl").isEmpty()) {
					// go there
					throw new RedirectionException(values.get("okreturnurl"));
				}
			}
			st.form().add(response);
		}
		getHtmlTemplate(st);
	}

	@Nonnull
	JSONObject getJSONTemplate(@Nonnull final State st) {
		final JSONObject json=new JSONObject();
		int arg=0;
		for (final Argument argument: getArguments()) {
			json.put("arg"+arg+"name",argument.getName());
			json.put("arg"+arg+"description",argument.description());
			switch (argument.type()) {
				case BOOLEAN:
					json.put("arg"+arg+"type","SELECT");
					json.put("arg"+arg+"button0","True");
					json.put("arg"+arg+"button1","False");
					break;
				case CHOICE:
					json.put("arg"+arg+"type","SELECT");
					int button=0;
					for (final String label: argument.getChoices(st)) {
						json.put("arg"+arg+"button"+button,label);
						button++;
					}
					break;
				case REGION:
					json.put("arg"+arg+"type","SELECT");
					button=0;
					for (final Region reg: st.getInstance().getRegions(false)) {
						final String label=reg.getName();
						json.put("arg"+arg+"button"+button,label);
						button++;
					}
					break;
				case COORDINATES:
					json.put("arg"+arg+"type","COORDINATES");
					break;
				case EVENT:
				case MODULE:
				case KVLIST:
				case TEXT_INTERNAL_NAME:
				case TEXT_CLEAN:
				case TEXT_ONELINE:
				case TEXT_MULTILINE:
				case PASSWORD:
				case INTEGER:
				case FLOAT:
				case ATTRIBUTE:
				case ATTRIBUTE_WRITABLE:
				case CHARACTER:
				case AVATAR:
				case ZONE:
					json.put("arg"+arg+"type","TEXTBOX");
					break;
				case CHARACTER_PLAYABLE:
					final Set<Char> options=Char.getCharacters(st.getInstance(),st.getAvatar());
					if (options.size()>12) {
						json.put("arg"+arg+"type","TEXTBOX");
					}
					else {
						json.put("arg"+arg+"type","SELECT");
						button=0;
						for (final Char c: options) {
							json.put("arg"+arg+"button"+button,c.getName());
							button++;
						}
					}
					break;
				case CHARACTERGROUP:
					final Set<CharacterGroup> groups=st.getInstance().getCharacterGroups();
					if (groups.size()>12) {
						json.put("arg"+arg+"type","TEXTBOX");
					}
					else {
						json.put("arg"+arg+"type","SELECT");
						button=0;
						for (final CharacterGroup g: groups) {
							json.put("arg"+arg+"button"+button,g.getName());
							button++;
						}
					}
					break;
				case CHARACTER_FACTION:
					json.put("arg"+arg+"type","SENSORCHAR");
					json.put("arg"+arg+"manual","true");
					break;
				case CHARACTER_NEAR:
					json.put("arg"+arg+"type","SENSORCHAR");
					break;
				case AVATAR_NEAR:
					json.put("arg"+arg+"type","SENSOR");
					break;
				default:
					throw new SystemImplementationException("Unhandled ENUM TYPE in getJSONTemplate():"+argument.type());
			}
			arg++;
		}
		json.put("args",arg);
		json.put("invoke",getFullName());
		json.put("incommand","runtemplate");
		return json;
	}

	void getHtmlTemplate(@Nonnull final State st) {
		final Form f=st.form();
		final Table t=new Table();
		f.add(t);
		for (final Argument arg: getArguments()) {
			t.openRow();
			//                t.add(p.getName());
			t.add(arg.description());
			switch (arg.type()) {
				case AVATAR:
				case TEXT_CLEAN:
				case TEXT_INTERNAL_NAME:
				case TEXT_ONELINE:
				case TEXT_MULTILINE:
				case COORDINATES:
				case CHARACTER:
				case CHARACTER_PLAYABLE: // FIXME this can be done properly
				case FLOAT:
				case INTEGER:
					t.add(new TextInput(arg.getName()));
					break;
				case PASSWORD:
					t.add(new PasswordInput(arg.getName()));
					break;
				case BOOLEAN:
					t.add(new CheckBox(arg.getName()));
					break;
				case ATTRIBUTE_WRITABLE:
				case ATTRIBUTE:
					final DropDownList attributes=new DropDownList(arg.getName());
					for (final Attribute a: st.getAttributes()) {
						if (arg.type()==ArgumentType.ATTRIBUTE || a.getSelfModify()) { attributes.add(a.getName()); }
					}
					t.add(attributes);
					break;
				case CHARACTER_FACTION:
					final DropDownList factionmembers=new DropDownList(arg.getName());
					final CharacterGroup faction=st.getCharacter().getGroup("Faction");
					if (faction==null) {
						throw new UserInputStateException("You are in no faction");
					}
					for (final Char c: faction.getMembers()) {
						factionmembers.add(c.getName());
					}
					t.add(factionmembers);
					break;
				case CHARACTER_NEAR:
					final DropDownList characters=new DropDownList(arg.getName());
					for (final Char c: st.getCharacter().getNearbyCharacters(st)) {
						characters.add(c.getName());
					}
					t.add(characters);
					break;
				case EVENT:
					final DropDownList eventlist=new DropDownList(arg.getName());
					for (final Event event: Event.getAll(st.getInstance())) {
						eventlist.add(event.getName());
					}
					t.add(eventlist);
					break;
				case MODULE:
					final DropDownList modulelist=new DropDownList(arg.getName());
					for (final Module amodule: Modules.getModules()) {
						modulelist.add(amodule.getName());
					}
					t.add(modulelist);
					break;
				case REGION:
					final DropDownList regionlist=new DropDownList(arg.getName());
					for (final Region aregion: st.getInstance().getRegions(false)) {
						regionlist.add(aregion.getName());
					}
					t.add(regionlist);
					break;
				case ZONE:
					final DropDownList zonelist=new DropDownList(arg.getName());
					for (final Zone azone: st.getInstance().getZones()) {
						zonelist.add(azone.getName());
					}
					t.add(zonelist);
					break;
				case KVLIST:
					final DropDownList list=new DropDownList(arg.getName());
					for (final String g: Modules.getKVList(st)) {
						list.add(g,g+" - "+st.getKVDefinition(g).description());
					}
					t.add(list);
					break;
				case CHARACTERGROUP:
					final DropDownList chargrouplist=new DropDownList(arg.getName());
					for (final CharacterGroup g: st.getInstance().getCharacterGroups()) {
						chargrouplist.add(g.getNameSafe());
					}
					t.add(chargrouplist);
					break;
				case PERMISSIONSGROUP:
					final DropDownList permgrouplist=new DropDownList(arg.getName());
					for (final PermissionsGroup g: st.getInstance().getPermissionsGroups()) {
						permgrouplist.add(g.getNameSafe());
					}
					t.add(permgrouplist);
					break;
				case PERMISSION:
					final DropDownList permlist=new DropDownList(arg.getName());
					for (final Module m: Modules.getModules()) {
						for (final String entry: m.getPermissions(st).keySet()) {
							permlist.add(m.getName()+"."+entry);
						}
					}
					t.add(permlist);
					break;
				case CHOICE:
					final DropDownList choicelist=new DropDownList(arg.getName());
					for (final String s: arg.getChoices(st)) {
						choicelist.add(s);
					}
					t.add(choicelist);
					break;
				default:
					throw new SystemImplementationException("Unhandled ENUM TYPE in populateForm():"+arg.type());
			}
		}
		if (getArgumentCount()>0) {
			t.openRow();
			t.add("");
			t.add(new Button("Submit"));
		}
	}

	@Nonnull
	public String getFullMethodName() {
		return getMethod().getDeclaringClass().getName()+"."+getMethod().getName()+"()";
	}

	public int getInvokingArgumentCount() {
		return getArgumentCount();
	}


	public enum Context {
		ANY,
		CHARACTER,
		AVATAR
	}

	/**
	 * Defines an exposed command.
	 * That is, something the user can call through web, SL or other user interfaces.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.METHOD)
	public @interface Commands {
		@Nonnull String description();

		@Nonnull String requiresPermission() default "";

		@Nonnull Context context();

		boolean permitJSON() default true;

		boolean permitConsole() default true;

		boolean permitUserWeb() default true;

		boolean permitScripting() default true;

		boolean permitObject() default true;
	}

}
