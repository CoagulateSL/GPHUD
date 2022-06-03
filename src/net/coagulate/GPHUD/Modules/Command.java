package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.*;
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
import net.coagulate.GPHUD.State.Sources;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A command, probably derived from Annotations.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Command {
	
	/** Number of buttons on a dialog page */
	public static final int DIALOG_PAGE_LIMIT=12;
	
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
	
	/** Checks if this command is generated (per instance) or global
	 *
	 * @return True if this command is instance specific
	 */
	public abstract boolean isGenerated();
	
	/** Returns the description for this command.
	 *
	 * @return COmmand description
	 */
	public abstract String description();
	
	/** Returns usage notes for this command.
	 *
	 * @return Usage notes
	 */
	public abstract String notes();
	
	/** Returns required permission to invoke this command.
	 *
	 * @return Name of the permission
	 */
	public abstract String requiresPermission();
	
	/** Define what level of logon a user must have to access this command.
	 *
	 * @return The required context level to run this command.
	 */
	public abstract Context context();
	
	/** Define if this command can be called by HUD (e.g. menus)
	 *
	 * @return True if this command should be HUD invokable
	 */
	public abstract boolean permitHUD();
	
	/** Define if this command can be called by an object
	 *
	 * @return True if this command may be invoked by an object.
	 */
	public abstract boolean permitObject();
	
	/** Define if this command may be called from the console.
	 *
	 * @return True if the user may call this command from the console (/1 or **)
	 */
	public abstract boolean permitConsole();
	
	/** Define if this command may be accessed from the web.
	 *
	 * @return True if this command may be called through the web interface
	 */
	public abstract boolean permitWeb();
	
	/** Define if this command may be called from a script (gsAPI*)
	 *
	 * @return True if this command may be invoked from a script
	 */
	public abstract boolean permitScripting();
	
	/** Define if this command may be called from the external interface
	 *
	 * @return True to permit this command to be called from the external interface.
	 */
	public abstract boolean permitExternal();
	
	/** Return a list of arguments related to this command
	 *
	 * @return List of arguments
	 */
	@Nonnull
	public abstract List<Argument> getArguments();
	
	/** Returns the number of arguments for this command
	 *
	 * @return Argument count
	 */
	public int getArgumentCount() {return getArguments().size();}
	
	/** Returns the full command name with module
	 *
	 * @return FQ command name
	 */
	@Nonnull
	public abstract String getFullName();
	
	/**
	 *
	 * @return The name of this command without module prefix
	 */
	@Nonnull
	public abstract String getName();
	
	/**
	 * Run this command given an array of string arguments.
	 * Converts them to named parameters and calls the SafeMap version
	 * @param state State
	 * @param args Invoked arguments in order
	 * @return The Response from the command
	 * @see Command#run(State,SafeMap) Next in chain
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
	
	/** Run a command
	 *
	 * @param state State
	 * @param parameterMap Map of parameter name/value strings
	 * @see Command#run(State,Map) next in chain
	 * @return The command's response
	 */
	public final Response run(@Nonnull final State state,
							  @Nonnull final SafeMap parameterMap) {
		state.parameterDebugRaw=parameterMap;
		final Map<String,Object> arguments=new HashMap<>();
		for (final Argument arg: getArguments()) {
			String v=parameterMap.get(arg.name()).trim();
			if (v.isEmpty()||"-".equals(v)) {v=null;}
			if (v!=null&&v.length()>getMaximumLength(arg)) {
				return new ErrorResponse(arg.name()+" is "+v.length()+" characters long and must be no more than "+getMaximumLength(arg)+".  Input has not been processed, please try again");
			}
			if (arg.type()==ArgumentType.BOOLEAN&&v==null) {v="false";}
			if (v==null) {arguments.put(arg.name(),null);} else {
				try {
					arguments.put(arg.name(),convertArgument(state,arg,v));
				} catch (final UserException conversionError) {
					return new ErrorResponse("Argument "+arg.name()+" failed : "+conversionError.getLocalizedMessage());
				}
			}
		}
		return run(state,arguments);
	}
	
	/** Invoke or present a command for a html stream
	 *
	 * @param st State
	 * @param values Valuemap of string,string
	 */
	public final void simpleHtml(@Nonnull final State st,
								 @Nonnull final SafeMap values) {
		//System.out.println("HERE:"+getArgumentCount());
		if (getArgumentCount()==0||values.submit()) {
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
	
	/** Represent this command as a HUD JSON block
	 *
	 * @param st State
	 * @return JSON Object for the command
	 */
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
					@SuppressWarnings("LocalVariableUsedAndDeclaredInDifferentSwitchBranches") int button=0;
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
					if (options.size()>DIALOG_PAGE_LIMIT) {
						json.put("arg"+arg+"type","TEXTBOX");
					} else {
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
					if (effects.size()>DIALOG_PAGE_LIMIT) {
						json.put("arg"+arg+"type","TEXTBOX");
					} else {
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
					if (groups.size()>DIALOG_PAGE_LIMIT) {
						json.put("arg"+arg+"type","TEXTBOX");
					} else {
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
					for (final Attribute inventory: Inventory.getAll(st)) {
						json.put("arg"+arg+"button"+button,inventory.getName());
						button++;
					}
					break;
				case SET:
					json.put("arg"+arg+"type","SELECT");
					button=0;
					for (final Attribute set: CharacterSet.getAll(st)) {
						json.put("arg"+arg+"button"+button,set.getName());
						button++;
					}
					break;
				case ITEM:
					json.put("arg"+arg+"type","SELECT");
					button=0;
					for (final Item item: Item.getAll(st)) {
						json.put("arg"+arg+"button"+button,item.getName());
						button++;
					}
					break;
				case PERMISSIONSGROUP:
					json.put("arg"+arg+"type","SELECT");
					button=0;
					for (final PermissionsGroup pg: PermissionsGroup.getPermissionsGroups(st)) {
						json.put("arg"+arg+"button"+button,pg.getName());
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
	
	/** Return the HTML invoking template for this command
	 *
	 * @param st State
	 */
	public final void getHtmlTemplate(@Nonnull final State st) {
		final Form f=st.form();
		final Table t=new Table();
		f.add(t);
		for (final Argument arg: getArguments()) {
			t.openRow();
			//                t.add(p.getName());
			t.add(arg.description());
			switch (arg.type()) {
				case AVATAR,TEXT_CLEAN,TEXT_INTERNAL_NAME,TEXT_ONELINE,TEXT_MULTILINE,COORDINATES,CHARACTER,CHARACTER_PLAYABLE,FLOAT,INTEGER ->
						t.add(new TextInput(arg.name()));
				case PASSWORD -> t.add(new PasswordInput(arg.name()));
				case BOOLEAN -> t.add(new CheckBox(arg.name()));
				case ATTRIBUTE_WRITABLE,ATTRIBUTE -> {
					final DropDownList attributes=new DropDownList(arg.name());
					for (final Attribute a: st.getAttributes()) {
						if (arg.type()==ArgumentType.ATTRIBUTE||a.getSelfModify()) {
							attributes.add(a.getName());
						}
					}
					t.add(attributes);
				}
				case CHARACTER_FACTION -> {
					final DropDownList factionMembers=new DropDownList(arg.name());
					final CharacterGroup faction=CharacterGroup.getGroup(st,"Faction");
					if (faction==null) {
						throw new UserInputStateException("You are in no faction",true);
					}
					for (final Char c: faction.getMembers()) {
						factionMembers.add(c.getName());
					}
					t.add(factionMembers);
				}
				case CHARACTER_NEAR -> {
					final DropDownList characters=new DropDownList(arg.name());
					for (final Char c: st.getCharacter().getNearbyCharacters(st)) {
						characters.add(c.getName());
					}
					t.add(characters);
				}
				case EFFECT -> {
					final DropDownList effectList=new DropDownList(arg.name());
					for (final Effect effect: Effect.getAll(st.getInstance())) {
						effectList.add(effect.getName());
					}
					t.add(effectList);
				}
				case EVENT -> {
					final DropDownList eventList=new DropDownList(arg.name());
					for (final Event event: Event.getAll(st.getInstance())) {
						eventList.add(event.getName());
					}
					t.add(eventList);
				}
				case MODULE -> {
					final DropDownList moduleList=new DropDownList(arg.name());
					for (final Module aModule: Modules.getModules()) {
						moduleList.add(aModule.getName());
					}
					t.add(moduleList);
				}
				case CURRENCY -> {
					final DropDownList currencyList=new DropDownList(arg.name());
					for (final Currency aCurrency: Currency.getAll(st)) {
						currencyList.add(aCurrency.getName());
					}
					t.add(currencyList);
				}
				case REGION -> {
					final DropDownList regionList=new DropDownList(arg.name());
					for (final Region aRegion: Region.getRegions(st,false)) {
						regionList.add(aRegion.getName());
					}
					t.add(regionList);
				}
				case ZONE -> {
					final DropDownList zoneList=new DropDownList(arg.name());
					for (final Zone aZone: Zone.getZones(st)) {
						zoneList.add(aZone.getName());
					}
					t.add(zoneList);
				}
				case KVLIST -> {
					final DropDownList list=new DropDownList(arg.name());
					for (final String g: Modules.getKVList(st)) {
						list.add(g,g+" - "+st.getKVDefinition(g).description());
					}
					t.add(list);
				}
				case CHARACTERGROUP -> {
					final DropDownList charGroupList=new DropDownList(arg.name());
					for (final CharacterGroup g: st.getInstance().getCharacterGroups()) {
						charGroupList.add(g.getNameSafe());
					}
					t.add(charGroupList);
				}
				case PERMISSIONSGROUP -> {
					final DropDownList permGroupList=new DropDownList(arg.name());
					for (final PermissionsGroup g: PermissionsGroup.getPermissionsGroups(st)) {
						permGroupList.add(g.getNameSafe());
					}
					t.add(permGroupList);
				}
				case PERMISSION -> {
					final DropDownList permList=new DropDownList(arg.name());
					for (final Module m: Modules.getModules()) {
						for (final String entry: m.getPermissions(st).keySet()) {
							permList.add(m.getName()+"."+entry);
						}
					}
					t.add(permList);
				}
				case CHOICE -> {
					final DropDownList choiceList=new DropDownList(arg.name());
					for (final String s: arg.getChoices(st)) {
						choiceList.add(s);
					}
					t.add(choiceList);
				}
				case INVENTORY -> {
					final DropDownList inventoryList=new DropDownList(arg.name());
					for (final Attribute attribute: Inventory.getAll(st)) {
						inventoryList.add(attribute.getName());
					}
					t.add(inventoryList);
				}
				case SET -> {
					final DropDownList setList=new DropDownList(arg.name());
					for (final Attribute attribute: CharacterSet.getAll(st)) {
						setList.add(attribute.getName());
					}
					t.add(setList);
				}
				case ITEM -> {
					final DropDownList itemList=new DropDownList(arg.name());
					for (final String item: Item.getNames(st)) {
						itemList.add(item);
					}
					t.add(itemList);
				}
				default -> throw new SystemImplementationException("Unhandled ENUM TYPE in populateForm():"+arg.type());
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
	 * @return Command response
	 */
	@Nonnull
	public final Response run(@Nonnull final State state,
							  @Nonnull final Map<String,Object> parameterMap) {
		state.parameterDebug=parameterMap;
		checkCallingInterface(state);
		// check permission
		if (!requiresPermission().isEmpty()&&!state.hasPermission(requiresPermission())) {
			throw new UserAccessDeniedException("Permission is denied, you require '"+requiresPermission()+"'",true);
		}
		//check arguments
		for (final Argument a: getArguments()) {
			Object o=null;
			if (parameterMap.containsKey(a.name())) {
				o=parameterMap.get(a.name());
			}
			if (a.mandatory()&&o==null) {
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
				if (v.matches(".*[^a-zA-Z\\d].*")) {
					throw new UserInputValidationFilterException(argument.name()+" should only consist of alphanumeric characters (a-z 0-9) and you entered '"+v+"'",true);
				}
				// don't put anything here, follow up into the next thing
			case TEXT_CLEAN:
				if (v.matches(".*[^a-zA-Z\\d.'\\-, ].*")) {
					throw new UserInputValidationFilterException(argument.name()+" should only consist of simple characters (a-z 0-9 .'-,) and you entered '"+v+"'",true);
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
				if (("1".equals(v)||"on".equalsIgnoreCase(v)||"true".equalsIgnoreCase(v)||"t".equalsIgnoreCase(v))) {
					return Boolean.TRUE;
				} else {
					return Boolean.FALSE;
				}
			case INTEGER:
				try {
					return Integer.valueOf(v);
				} catch (@Nonnull final NumberFormatException e) {
					throw new UserInputValidationParseException("Unable to convert '"+v+"' to a number for argument "+argument.name(),e,true);
				}
			case FLOAT:
				try {
					return Float.valueOf(v);
				} catch (@Nonnull final NumberFormatException e) {
					throw new UserInputValidationParseException("Unable to convert '"+v+"' to a number for argument "+argument.name(),e,true);
				}
			case MODULE:
				final Module m=Modules.get(null,v); // null to disable enablement check :)
				if (m==null) {return new UserInputLookupFailureException("Unable to resolve module "+v);}
				return m;
			//case FLOAT:
			case ATTRIBUTE_WRITABLE:
			case SET:
			case INVENTORY:
			case ATTRIBUTE:
				Attribute attr=null;
				for (final Attribute a: state.getAttributes()) {
					if (a.getName().equalsIgnoreCase(v)) {
						//noinspection VariableNotUsedInsideIf
						if (attr!=null) {
							throw new SystemConsistencyException("Duplicate attribute definition found for "+v);
						}
						if (type==ArgumentType.ATTRIBUTE||a.getSelfModify()) {attr=a;}
						if (type==ArgumentType.SET&&a.getType()==Attribute.ATTRIBUTETYPE.SET) {attr=a;}
						if (type==ArgumentType.INVENTORY&&a.getType()==Attribute.ATTRIBUTETYPE.INVENTORY) {attr=a;}
					}
				}
				if (attr==null) {
					throw new UserInputLookupFailureException("Unable to resolve '"+v+"' to an attribute",true);
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
					} catch (@Nonnull final NoDataException e) {
						throw new UserInputLookupFailureException("Unable to find character of avatar named '"+v+"'",e,true);
					}
				} else {
					targetChar=Char.resolve(state,v);
				}
				if (targetChar==null) {
					throw new UserInputLookupFailureException("Unable to find character named '"+v+"'",true);
				} else {
					return targetChar;
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
				if (user==null) {
					throw new UserInputLookupFailureException("Unable to find a known avatar named '"+v+"'",true);
				}
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
					throw new UserInputStateException("Character context required and you are not connected to an instance.",true);
				}
				if (state.getCharacterNullable()==null) {
					throw new UserInputStateException("Character context required, your request is lacking a character registration",true);
				}
				break;
			case AVATAR:
				if (state.getInstanceNullable()==null) {
					throw new UserInputStateException("Avatar context required and you are not connected to an instance.",true);
				}
				if (state.getAvatarNullable()==null) {
					throw new UserInputStateException("Avatar context required, your request is lacking an avatar registration",true);
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
				throw new UserAccessDeniedException(getFullName()+" command can not be accessed via the Web interface",true);
			}
		}
		if (state.source==Sources.SYSTEM) {
			if (!permitHUD()) {
				throw new UserAccessDeniedException(getFullName()+" command can not be accessed via the LSL System interface",true);
			}
		}
		if (state.source==Sources.CONSOLE) {
			if (!permitConsole()) {
				throw new UserAccessDeniedException(getFullName()+" command can not be accessed via the console",true);
			}
		}
		if (state.source==Sources.SCRIPTING) {
			if (!permitScripting()) {
				throw new UserAccessDeniedException(getFullName()+" command can not be access via the Scripting module",true);
			}
		}
		if (state.source==Sources.EXTERNAL) {
			if (!permitExternal()) {
				throw new UserAccessDeniedException(getFullName()+" command can not be accessed via the External API interface",true);
			}
		}
		
	}
	
	@SuppressWarnings("MagicNumber")
	private int getMaximumLength(final Argument argument) {
		final ArgumentType type=argument.type();
		return switch (type) {
			case TEXT_CLEAN,TEXT_ONELINE,TEXT_INTERNAL_NAME,TEXT_MULTILINE -> argument.max();
			case PASSWORD,CHOICE -> 1024;
			case BOOLEAN -> 8;
			case INTEGER,FLOAT -> 32;
			case CHARACTER,ATTRIBUTE_WRITABLE,ATTRIBUTE,SET,INVENTORY,COORDINATES,ZONE,REGION,MODULE,PERMISSION,PERMISSIONSGROUP,AVATAR_NEAR,AVATAR,CHARACTER_FACTION,CHARACTER_NEAR,CHARACTER_PLAYABLE,EFFECT,CURRENCY ->
					64;
			case CHARACTERGROUP,EVENT,KVLIST,ITEM -> 128;
		};
	}
	
	
	/** Representation of all context levels a login may have */
	public enum Context {
		/** Not even logged in */ ANY,
		/** Must be fully logged in as a character */ CHARACTER,
		/** Must have an SL avatar known */ AVATAR
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
		
		/**
		 *
		 * @return Command description
		 */
		@Nonnull String description();
		
		/**
		 *
		 * @return Command notes
		 */
		@Nonnull String notes() default "";
		
		/**
		 *
		 * @return Command required permission
		 */
		@Nonnull String requiresPermission() default "";
		
		/**
		 *
		 * @return Command required context
		 */
		@Nonnull Context context();
		
		/** @return Permit this command from the System JSON interface */
		boolean permitJSON() default true;
		
		/** @return Permit this command from the console */
		boolean permitConsole() default true;
		
		/** @return Permit this command from the web interface */
		boolean permitUserWeb() default true;
		
		/** @return Permit this command from scripting */
		boolean permitScripting() default true;
		
		/** @return Permit this command from objects */
		boolean permitObject() default true;
		
		/** @return Permit this command from the external interface */
		boolean permitExternal() default true;
	}
	
}
