package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.*;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Currency;
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
import net.coagulate.GPHUD.State.Sources;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.*;
import java.util.*;

/**
 * A command, probably derived from Annotations.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Command {

	// ----- Internal Statics -----
	@Nonnull
	static Object assertNotNull(@Nullable final Object o,
								final String value,
								final String type) {
		if (o==null) {
			throw new UserInputLookupFailureException("Unable to resolve '"+value+"' to a "+type,true);
		}
		return o;
	}

	// ---------- INSTANCE ----------
	public abstract boolean isGenerated();

	public abstract String description();

	public abstract String notes();

	public abstract String requiresPermission();

	public abstract Context context();

	public abstract boolean permitHUD();

	public abstract boolean permitObject();

	public abstract boolean permitConsole();

	public abstract boolean permitWeb();

	public abstract boolean permitScripting();

	public abstract boolean permitExternal();

	@Nonnull
	public abstract List<Argument> getArguments();

	public int getArgumentCount() { return getArguments().size(); }

	@Nonnull
	public abstract String getFullName();

	@Nonnull
	public abstract String getName();

	/**
	 * Run this command given an array of string arguments.
	 * Converts them to named parameters and calls the SafeMap version
	 */
	@Nonnull
	public final Response run(@Nonnull final State state,
	                          @Nonnull final String[] args) {
		//for (int i=0;i<args.length;i++) { System.out.println("Arg "+i+" : "+args[i]); }
		final SafeMap map=new SafeMap();
		int arg=0;
		for (final Argument argument: getArguments()) {
			if (argument==null) {
				throw new SystemImplementationException("Argument metadata null on "+getFullName()+"() arg#"+(arg+1));
			}
			String v="";
			if (arg<args.length) {
				v=args[arg];
				//System.out.println("In here for "+arg+" = "+v);
			}
			map.put(argument.name(),v);
			//System.out.println("Command "+getFullName()+" mapped "+argument.getName()+"-"+arg+"/"+args.length+"->"+v);
			arg++;
		}
		return run(state,map);
	}

	/**
	 * Get the name of the arguments.
	 *
	 * @return list of argument names
	 */
	public final List<String> getArgumentNames() {
		final List<String> arguments=new ArrayList<>();
		for (final Argument a: getArguments()) {
			arguments.add(a.name());
		}
		return arguments;
	}

	public final Response run(@Nonnull final State state,
	                          @Nonnull final SafeMap parameterMap) {
		state.parameterDebugRaw =parameterMap;
		final Map<String,Object> arguments=new HashMap<>();
		for (final Argument arg: getArguments()) {
			String v=parameterMap.get(arg.name()).trim();
			if (v.isEmpty() || "-".equals(v)) { v=null; }
			if (v!=null && v.length()>getMaximumLength(arg)) {
				return new ErrorResponse(arg.name()+" is "+v.length()+" characters long and must be no more than "+getMaximumLength(arg)+".  Input has not been processed, please try again");
			}
			if (arg.type()==ArgumentType.BOOLEAN && v==null) { v="false"; }
			if (v==null) { arguments.put(arg.name(),null); }
			else {
				try {
					arguments.put(arg.name(),convertArgument(state,arg,v));
				}
				catch (final UserException conversionError) {
					return new ErrorResponse("Argument "+arg.name()+" failed : "+conversionError.getLocalizedMessage());
				}
			}
		}
		return run(state,arguments);
	}

	public final void simpleHtml(@Nonnull final State st,
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
	public final JSONObject getJSONTemplate(@Nonnull final State st) {
		final JSONObject json=new JSONObject();
		int arg=0;
		for (final Argument argument: getArguments()) {
			json.put("arg"+arg+"name",argument.name());
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
					for (final Region reg: Region.getRegions(st,false)) {
						final String label=reg.getName();
						json.put("arg"+arg+"button"+button,label);
						button++;
					}
					break;
				case COORDINATES:
					json.put("arg"+arg+"type","COORDINATES");
					break;
				case CURRENCY:
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
				case EFFECT:
					final Set<Effect> effects=Effect.getAll(st.getInstance());
					if (effects.size()>12) {
						json.put("arg"+arg+"type","TEXTBOX");
					}
					else {
						json.put("arg"+arg+"type","SELECT");
						button=0;
						for (final Effect e: effects) {
							json.put("arg"+arg+"button"+button,e.getName());
							button++;
						}
					}
					break;
				case CHARACTERGROUP:
					final List<CharacterGroup> groups=st.getInstance().getCharacterGroups();
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
				case INVENTORY:
					json.put("arg"+arg+"type","SELECT");
					button=0;
					for (final Attribute inventory:Inventory.getAll(st)) {
						json.put("arg"+arg+"button"+button,inventory.getName());
						button++;
					}
					break;
				case SET:
					json.put("arg"+arg+"type","SELECT");
					button=0;
					for (final Attribute set:CharacterSet.getAll(st)) {
						json.put("arg"+arg+"button"+button,set.getName());
						button++;
					}
					break;
				case ITEM:
					json.put("arg"+arg+"type","SELECT");
					button=0;
					for (final Item item:Item.getAll(st)) {
						json.put("arg"+arg+"button"+button,item.getName());
						button++;
					}
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

	public final void getHtmlTemplate(@Nonnull final State st) {
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
					t.add(new TextInput(arg.name()));
					break;
				case PASSWORD:
					t.add(new PasswordInput(arg.name()));
					break;
				case BOOLEAN:
					t.add(new CheckBox(arg.name()));
					break;
				case ATTRIBUTE_WRITABLE:
				case ATTRIBUTE:
					final DropDownList attributes=new DropDownList(arg.name());
					for (final Attribute a: st.getAttributes()) {
						if (arg.type()==ArgumentType.ATTRIBUTE || a.getSelfModify()) { attributes.add(a.getName()); }
					}
					t.add(attributes);
					break;
				case CHARACTER_FACTION:
					final DropDownList factionMembers=new DropDownList(arg.name());
					final CharacterGroup faction=CharacterGroup.getGroup(st,"Faction");
					if (faction==null) {
						throw new UserInputStateException("You are in no faction");
					}
					for (final Char c: faction.getMembers()) {
						factionMembers.add(c.getName());
					}
					t.add(factionMembers);
					break;
				case CHARACTER_NEAR:
					final DropDownList characters=new DropDownList(arg.name());
					for (final Char c: st.getCharacter().getNearbyCharacters(st)) {
						characters.add(c.getName());
					}
					t.add(characters);
					break;
				case EFFECT:
					final DropDownList effectList=new DropDownList(arg.name());
					for (final Effect effect: Effect.getAll(st.getInstance())) {
						effectList.add(effect.getName());
					}
					t.add(effectList);
					break;
				case EVENT:
					final DropDownList eventList=new DropDownList(arg.name());
					for (final Event event: Event.getAll(st.getInstance())) {
						eventList.add(event.getName());
					}
					t.add(eventList);
					break;
				case MODULE:
					final DropDownList moduleList=new DropDownList(arg.name());
					for (final Module aModule: Modules.getModules()) {
						moduleList.add(aModule.getName());
					}
					t.add(moduleList);
					break;
				case CURRENCY:
					final DropDownList currencyList=new DropDownList(arg.name());
					for (final Currency aCurrency: Currency.getAll(st)) {
						currencyList.add(aCurrency.getName());
					}
					t.add(currencyList);
					break;
				case REGION:
					final DropDownList regionList=new DropDownList(arg.name());
					for (final Region aRegion: Region.getRegions(st,false)) {
						regionList.add(aRegion.getName());
					}
					t.add(regionList);
					break;
				case ZONE:
					final DropDownList zoneList=new DropDownList(arg.name());
					for (final Zone aZone: Zone.getZones(st)) {
						zoneList.add(aZone.getName());
					}
					t.add(zoneList);
					break;
				case KVLIST:
					final DropDownList list=new DropDownList(arg.name());
					for (final String g: Modules.getKVList(st)) {
						list.add(g,g+" - "+st.getKVDefinition(g).description());
					}
					t.add(list);
					break;
				case CHARACTERGROUP:
					final DropDownList charGroupList=new DropDownList(arg.name());
					for (final CharacterGroup g: st.getInstance().getCharacterGroups()) {
						charGroupList.add(g.getNameSafe());
					}
					t.add(charGroupList);
					break;
				case PERMISSIONSGROUP:
					final DropDownList permGroupList=new DropDownList(arg.name());
					for (final PermissionsGroup g: PermissionsGroup.getPermissionsGroups(st)) {
						permGroupList.add(g.getNameSafe());
					}
					t.add(permGroupList);
					break;
				case PERMISSION:
					final DropDownList permList=new DropDownList(arg.name());
					for (final Module m: Modules.getModules()) {
						for (final String entry: m.getPermissions(st).keySet()) {
							permList.add(m.getName()+"."+entry);
						}
					}
					t.add(permList);
					break;
				case CHOICE:
					final DropDownList choiceList=new DropDownList(arg.name());
					for (final String s: arg.getChoices(st)) {
						choiceList.add(s);
					}
					t.add(choiceList);
					break;
				case INVENTORY:
					final DropDownList inventoryList=new DropDownList(arg.name());
					for (Attribute attribute:Inventory.getAll(st)) {
						inventoryList.add(attribute.getName());
					}
					t.add(inventoryList);
					break;
				case SET:
					final DropDownList setList=new DropDownList(arg.name());
					for (Attribute attribute:CharacterSet.getAll(st)) {
						setList.add(attribute.getName());
					}
					t.add(setList);
					break;
				case ITEM:
					final DropDownList itemList=new DropDownList(arg.name());
					for (String item:Item.getNames(st)) {
						itemList.add(item);
					}
					t.add(itemList);
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

	/**
	 * Run a command based on properly cast arguments.
	 *
	 * @param state        Session state
	 * @param parameterMap Arguments of appropriate type for receiving method (or throws exceptions).  State should be first argument!
	 *
	 * @return Command response
	 */
	@Nonnull
	public final Response run(@Nonnull final State state,
	                          @Nonnull final Map<String,Object> parameterMap) {
		state.parameterDebug =parameterMap;
		checkCallingInterface(state);
		// check permission
		if (!requiresPermission().isEmpty() && !state.hasPermission(requiresPermission())) {
			throw new UserAccessDeniedException("Permission is denied, you require '"+requiresPermission()+"'",true);
		}
		//check arguments
		for (final Argument a: getArguments()) {
			Object o=null;
			if (parameterMap.containsKey(a.name())) {
				o=parameterMap.get(a.name());
			}
			if (a.mandatory() && o==null) {
				return new ErrorResponse("Argument "+a.name()+" is mandatory on command "+getFullName()+" and nothing was passed");
			}
		}
		// check the "operational context" :)
		checkCallingContext(state);
		return execute(state,parameterMap);
	}

	// ----- Internal Instance -----
	protected abstract Response execute(State state,
	                                    Map<String,Object> arguments);

	@SuppressWarnings("fallthrough")
	protected final Object convertArgument(final State state,
	                                       final Argument argument,
	                                       String v) {
		final ArgumentType type=argument.type();
		switch (type) {
			case TEXT_INTERNAL_NAME:
				if (v.matches(".*[^a-zA-Z0-9].*")) {
					throw new UserInputValidationFilterException(argument.name()+" should only consist of alphanumeric characters (a-z 0-9) and you entered '"+v+"'");
				}
				// don't put anything here, follow up into the next thing
			case TEXT_CLEAN:
				if (v.matches(".*[^a-zA-Z0-9.'\\-, ].*")) {
					throw new UserInputValidationFilterException(argument.name()+" should only consist of simple characters (a-z 0-9 .'-,) and you entered '"+v+"'");
				}
				// don't put anything here, follow up into the next thing
			case TEXT_ONELINE:
			case TEXT_MULTILINE:
			case PASSWORD:
			case PERMISSION:
			case CHOICE:
			case KVLIST:
			case COORDINATES:
				//System.out.println("Adding arg "+v);
				return v;
			case BOOLEAN:
				if (("1".equals(v) || "on".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v) || "t".equalsIgnoreCase(v))) {
					return Boolean.TRUE;
				}
				else {
					return Boolean.FALSE;
				}
			case INTEGER:
				try {
					return Integer.valueOf(v);
				}
				catch (@Nonnull final NumberFormatException e) {
					throw new UserInputValidationParseException("Unable to convert '"+v+"' to a number for argument "+argument.name(),e);
				}
			case FLOAT:
				try {
					return Float.valueOf(v);
				}
				catch (@Nonnull final NumberFormatException e) {
					throw new UserInputValidationParseException("Unable to convert '"+v+"' to a number for argument "+argument.name(),e);
				}
			case MODULE:
				final Module m=Modules.get(null,v); // null to disable enablement check :)
				if (m==null) { return new UserInputLookupFailureException("Unable to resolve module "+v); }
				return m;
			//case FLOAT:
			case ATTRIBUTE_WRITABLE:
			case SET:
			case INVENTORY:
			case ATTRIBUTE:
				Attribute attr=null;
				for (final Attribute a: state.getAttributes()) {
					if (a.getName().equalsIgnoreCase(v)) {
						if (attr!=null) {
							throw new SystemConsistencyException("Duplicate attribute definition found for "+v);
						}
						if (type==ArgumentType.ATTRIBUTE || a.getSelfModify()) { attr=a; }
						if (type==ArgumentType.SET && a.getType()== Attribute.ATTRIBUTETYPE.SET) { attr=a; }
						if (type==ArgumentType.INVENTORY && a.getType()== Attribute.ATTRIBUTETYPE.INVENTORY) { attr=a; }
					}
				}
				if (attr==null) {
					throw new UserInputLookupFailureException("Unable to resolve '"+v+"' to an attribute");
				}
				return attr;
			case ITEM:
				return assertNotNull(Item.findNullable(state.getInstance(),v),v,"item");
			case CURRENCY:
				return assertNotNull(Currency.find(state,v),v,"currency");
			case PERMISSIONSGROUP:
				return assertNotNull(PermissionsGroup.resolveNullable(state,v),v,"permissions group");
			case CHARACTERGROUP:
				return assertNotNull(CharacterGroup.resolve(state,v),v,"character group");
			case CHARACTER_FACTION:
			case CHARACTER:
			case CHARACTER_PLAYABLE:
			case CHARACTER_NEAR:
				final Char targetChar;
				if (v.startsWith(">")) {
					v=v.substring(1);
					try {
						final User a=User.findUsername(v,false);
						targetChar=Char.getActive(a,state.getInstance());
					}
					catch (@Nonnull final NoDataException e) {
						throw new UserInputLookupFailureException("Unable to find character of avatar named '"+v+"'",e);
					}
				}
				else {
					targetChar=Char.resolve(state,v);
				}
				if (targetChar!=null) { return targetChar; }
				else {
					throw new UserInputLookupFailureException("Unable to find character named '"+v+"'");
				}
			case REGION:
				return assertNotNull(Region.findNullable(v,false),v,"region name");
			case EVENT:
				return assertNotNull(Event.find(state.getInstance(),v),v,"event name");
			case EFFECT:
				return assertNotNull(Effect.find(state.getInstance(),v),v,"effect name");
			case ZONE:
				return assertNotNull(Zone.findNullable(state.getInstance(),v),v,"zone name");
			case AVATAR:
			case AVATAR_NEAR:
				final User user=User.findUsernameNullable(v,false);
				if (user==null) { throw new UserInputLookupFailureException("Unable to find a known avatar named '"+v+"'"); }
				return assertNotNull(user,v,"avatar");
			default:
				throw new SystemImplementationException("Unhandled argument type "+type+" in converter for argument "+argument.name());
		}
	}

	private void checkCallingContext(final State state) {
		switch (context()) {
			case ANY:
				break;
			case CHARACTER:
				if (state.getInstanceNullable()==null) {
					throw new UserInputStateException("Character context required and you are not connected to an instance.");
				}
				if (state.getCharacterNullable()==null) {
					throw new UserInputStateException("Character context required, your request is lacking a character registration");
				}
				break;
			case AVATAR:
				if (state.getInstanceNullable()==null) {
					throw new UserInputStateException("Avatar context required and you are not connected to an instance.");
				}
				if (state.getAvatarNullable()==null) {
					throw new UserInputStateException("Avatar context required, your request is lacking an avatar registration");
				}
				break;
			default:
				throw new SystemImplementationException("Unhandled CONTEXT enum during pre-flight check in execute()");
		}
	}

	private void checkCallingInterface(final State state) {
		// check required interface
		if (state.source==Sources.USER) {
			if (!permitWeb()) {
				throw new UserAccessDeniedException(getFullName()+" command can not be accessed via the Web interface");
			}
		}
		if (state.source==Sources.SYSTEM) {
			if (!permitHUD()) {
				throw new UserAccessDeniedException(getFullName()+" command can not be accessed via the LSL System interface");
			}
		}
		if (state.source==Sources.CONSOLE) {
			if (!permitConsole()) {
				throw new UserAccessDeniedException(getFullName()+" command can not be accessed via the console");
			}
		}
		if (state.source==Sources.SCRIPTING) {
			if (!permitScripting()) {
				throw new UserAccessDeniedException(getFullName()+" command can not be access via the Scripting module");
			}
		}
		if (state.source==Sources.EXTERNAL) {
			if (!permitExternal()) {
				throw new UserAccessDeniedException(getFullName()+" command can not be accessed via the External API interface");
			}
		}

	}

	private int getMaximumLength(final Argument argument) {
		final ArgumentType type=argument.type();
		switch (type) {
			case TEXT_CLEAN:
			case TEXT_ONELINE:
			case TEXT_INTERNAL_NAME:
			case TEXT_MULTILINE:
				return argument.max();
			case PASSWORD:
			case CHOICE:
				return 1024;
			case BOOLEAN:
				return 8;
			case INTEGER:
			case FLOAT:
				return 32;
			case CHARACTER:
			case ATTRIBUTE_WRITABLE:
			case ATTRIBUTE:
			case SET:
			case INVENTORY:
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
			case EFFECT:
			case CURRENCY:
				return 64;
			case CHARACTERGROUP:
			case EVENT:
			case KVLIST:
			case ITEM:
				return 128;
			default:
				throw new SystemImplementationException("Argument "+argument.name()+" of type "+argument.type().name()+" fell through maximum length");
		}
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
		// ---------- INSTANCE ----------
		@Nonnull String description();

		@Nonnull String notes() default "";

		@Nonnull String requiresPermission() default "";

		@Nonnull Context context();

		boolean permitJSON() default true;

		boolean permitConsole() default true;

		boolean permitUserWeb() default true;

		boolean permitScripting() default true;

		boolean permitObject() default true;

		boolean permitExternal() default true;
	}

}
