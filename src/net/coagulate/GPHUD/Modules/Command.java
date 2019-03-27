package net.coagulate.GPHUD.Modules;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static java.util.logging.Level.WARNING;
import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Data.Event;
import net.coagulate.GPHUD.Data.PermissionsGroup;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Data.Zone;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.CheckBox;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Inputs.PasswordInput;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.AVATAR_NEAR;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.BOOLEAN;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.CHARACTERGROUP;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.CHARACTER_FACTION;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.CHARACTER_NEAR;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.CHARACTER_PLAYABLE;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.CHOICE;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.COORDINATES;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.EVENT;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.INTEGER;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.KVLIST;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.MODULE;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.PASSWORD;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.PERMISSION;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.PERMISSIONSGROUP;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.REGION;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.TEXT_MULTILINE;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.TEXT_ONELINE;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.ZONE;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

/** A command, probably derived from Annotations.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Command {

    /** Defines an exposed command.
     * That is, something the user can call through web, SL or other user interfaces.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target(ElementType.METHOD)
    public @interface Commands {
        String description();
        String requiresPermission() default "";
        Context context();
        boolean permitJSON() default true;
        boolean permitConsole() default true;
        boolean permitHUDWeb() default true;
        boolean permitUserWeb() default true;
    }

    public static enum Context { ANY, CHARACTER, AVATAR};
    
    public abstract Method getMethod();
    public abstract boolean isGenerated(); 
    public abstract String description();
    public abstract String requiresPermission();
    public abstract Context context();
    public abstract boolean permitJSON();
    public abstract boolean permitConsole();
    public abstract boolean permitHUDWeb();
    public abstract boolean permitUserWeb();
    public abstract List<Argument> getArguments();
    public abstract int getArgumentCount();
    public abstract String getFullName();
    public abstract String getName();
        
    static Object assertNotNull(Object o, String value, String type) throws UserException {
        if (o == null) {
            throw new UserException("Unable to resolve '" + value + "' to a " + type);
        }
        return o;
    }
    
    public Response run(State st, String[] args) throws SystemException, UserException {
        List<Object> typedargs = new ArrayList<>();
        int arg = 0;
        typedargs.add(st);
        for (Argument argument : getInvokingArguments()) {
            if (argument == null) {
                throw new SystemException("Argument metadata null on " +getFullName() + "() arg#" + (arg + 1) + " " + argument.getName());
            }            
            ArgumentType type = argument.type();
            String v = args[arg];
            arg++;
            if ((v == null || v.equals("")) && type != ArgumentType.BOOLEAN) {
                typedargs.add(null);
            } else {
                int maxlen=-1;
                switch (type) {
                    case TEXT_ONELINE: maxlen=argument.max(); break;
                    case TEXT_MULTILINE: maxlen=argument.max(); break;
                    case PASSWORD: maxlen=1024; break;
                    case BOOLEAN: maxlen=8; break;
                    case INTEGER: maxlen=32; break;
                    case FLOAT: maxlen=32; break; 
                    case CHOICE: maxlen=1024; break;
                    case CHARACTER: maxlen=64; break;
                    case CHARACTER_PLAYABLE: maxlen=64; break;
                    case CHARACTER_NEAR: maxlen=64; break;
                    case CHARACTER_FACTION: maxlen=64; break;
                    case AVATAR: maxlen=64; break;
                    case AVATAR_NEAR: maxlen=64; break;
                    case PERMISSIONSGROUP:  maxlen=64; break;
                    case PERMISSION:  maxlen=64; break;
                    case CHARACTERGROUP:  maxlen=128; break;
                    case KVLIST: maxlen=128; break;
                    case MODULE: maxlen=64; break;
                    case REGION: maxlen=64; break;
                    case ZONE: maxlen=64; break;
                    case COORDINATES: maxlen=64; break;
                    case EVENT: maxlen=128; break;
                    case ATTRIBUTE: maxlen=64; break;
                    case ATTRIBUTE_WRITABLE: maxlen=64; break;
                    default:
                        throw new AssertionError(type.name());
                    
                }
                if (maxlen<1) { st.logger().warning("Command "+this.getClass().getSimpleName()+" argument "+argument.getName()+" does not specify a max, assuming 65k..."); }
                else {
                    if (v!=null && v.length()>maxlen) { throw new UserException(argument.getName()+" is "+v.length()+" characters long and must be no more than "+maxlen+".  Input has not been processed, please try again"); }
                }
                switch (type) {
                    case TEXT_ONELINE:
                    case TEXT_MULTILINE:
                    case PASSWORD:
                    case PERMISSION:
                    case CHOICE:
                    case KVLIST:
                    case COORDINATES:
                        //System.out.println("Adding arg "+v);
                        typedargs.add(new String(v));
                        break;
                    case BOOLEAN:
                        if (v != null && (v.equals("1") || v.equalsIgnoreCase("on") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("t"))) {
                            typedargs.add(Boolean.TRUE);
                        } else {
                            typedargs.add(Boolean.FALSE);
                        }
                        break;
                    case INTEGER:
                        try {
                            typedargs.add(new Integer(v));
                        } catch (NumberFormatException e) {
                            return new ErrorResponse("Unable to convert '" + v + "' to a number for argument " + argument.getName());
                        }
                        break;
                    case MODULE:
                        Module m=Modules.get(null, v);
                        if (m==null) { return new ErrorResponse("Unable to resolve module "+m); }
                        typedargs.add(m);
                        break;
                //case FLOAT:
                    case ATTRIBUTE_WRITABLE:
                    case ATTRIBUTE:
                        Attribute attr=null;
                        for (Attribute a:st.getAttributes()) {
                            if (a.getName().equalsIgnoreCase(v)) {
                                if (attr!=null) { throw new SystemException("Duplicate attribute definition found for "+v); }
                                if (type==ArgumentType.ATTRIBUTE || a.getSelfModify()) { attr=a; }
                            }
                        }
                        if (attr==null) { throw new UserException("Unable to resolve '"+v+"' to an attribute"); }
                        typedargs.add(attr);
                        break;
                    case CHARACTER_FACTION:
                    case CHARACTER:
                    case CHARACTER_PLAYABLE:
                        Char targetchar=Char.resolve(st, v);
                        typedargs.add(assertNotNull(targetchar, v, "character"));
                        if (argument.getName().equalsIgnoreCase("target")) { st.setTarget(targetchar); }
                        break;
                    case PERMISSIONSGROUP:
                        typedargs.add(assertNotNull(PermissionsGroup.resolve(st, v), v, "permissions group"));
                        break;
                    case CHARACTERGROUP:
                        typedargs.add(assertNotNull(CharacterGroup.resolve(st, v), v, "character group"));
                        break;
                    case CHARACTER_NEAR:
                        Char targchar=null;
                        if (v.startsWith(">")) {
                            v=v.substring(1);
                            try {
                                User a=User.findMandatory(v); 
                                targchar=Char.getActive(a);
                            } catch (NoDataException e) { throw new UserException("Unable to find character or avatar named '"+v+"'"); }
                        } else {
                            targchar=Char.resolve(st, v);
                        }
                        if (targchar!=null) { typedargs.add(targchar);  }
                        break;
                    case REGION:
                        typedargs.add(assertNotNull(Region.find(v),v,"region name"));
                        break;
                    case EVENT:
                        typedargs.add(assertNotNull(Event.find(st.getInstance(),v),v,"event name"));
                        break;
                    case ZONE:
                        typedargs.add(assertNotNull(Zone.find(st.getInstance(),v),v,"zone name"));
                        break;
                    case AVATAR:
                    case AVATAR_NEAR:
                        typedargs.add(assertNotNull(User.findMandatory(v), v, "avatar"));
                        break;
                    default:
                        throw new SystemException("Unhandled ENUM TYPE in executor:" + type);
                }
            }
        }
        return run(st, typedargs.toArray(new Object[]{}));
    }
    
    /** Run a command based on properly cast arguments.
     * 
     * @param st Session state
     * @param args Arguments of appropriate type for receiving method (or exceptions)
     * @return Command response
     * @throws SystemException 
     */
    Response run(State st, Object[] args) throws SystemException {
        boolean debug=false;
        try {
            // check permission
            if (!requiresPermission().isEmpty() && !st.hasPermission(requiresPermission())) {
                return new ErrorResponse("Permission is denied, you require '" + requiresPermission() + "'");
            }
            // check required interface
            boolean ok=false;
            if (st.handler.equals(net.coagulate.GPHUD.Interfaces.HUD.Interface.class)) {
                if (!this.permitHUDWeb()) { return new ErrorResponse("This command can not be accessed via the HUD Web interface"); }
                ok=true;
            }
            if (st.handler.equals(net.coagulate.GPHUD.Interfaces.User.Interface.class)) {
                if (!this.permitUserWeb()) { return new ErrorResponse("This command can not be accessed via the Web interface"); }
                ok=true;
            }
            if (st.handler.equals(net.coagulate.GPHUD.Interfaces.System.Interface.class)) {
                if (!this.permitJSON()) { return new ErrorResponse("This command can not be accessed via the LSL System interface"); }
                ok=true;
            }
            if (st.handler.equals(net.coagulate.GPHUD.Interfaces.HUD.Interface.class)) {
                if (!this.permitHUDWeb()) { return new ErrorResponse("This command can not be accessed via the HUD Web interface"); }
                ok=true;
            }
            
            //check arguments
            int i = 0;
            if (args.length != getInvokingArgumentCount()+1) {
                return new ErrorResponse("Incorrect number of arguments, "+getFullName()+" aka "+getMethod().getName()+" requires " + (getInvokingArgumentCount()+1) + " and we got " + args.length);
            }
            String suspiciousname="";
            for (Argument a:getInvokingArguments()) {
                if (a.getName().startsWith("arg") && a.getName().length()==4) { suspiciousname=".  ***WARNING*** this argument name starts with 'arg' and may indicate javac was NOT invoked with the -parameter!!!"; }
                if (i > 0) {
                    Object o = args[i];
                    // I don't really like this, but...
                    if (o instanceof String) {
                        args[i] = ((String) o).trim();
                        o = args[i];
                    }
                    if (a.mandatory()) {
                        if (o == null) {
                            return new ErrorResponse("Argument " + a.getName() + " is mandatory and null was passed"+suspiciousname);
                        }
                        if (o instanceof String) {
                            String s = (String) o;
                            if (s.isEmpty()) {
                                return new ErrorResponse("Argument " + a.getName() + " is mandatory and a blank string was passed"+suspiciousname);
                            }
                            if (s.equals("-")) {
                                return new ErrorResponse("Argument " + a.getName() + " is mandatory and a dash '-' was passed"+suspiciousname);
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
                    if (st.getInstance() == null) {
                        return new ErrorResponse("Character context required and you are not connected to an instance.");
                    }
                    if (st.getCharacter() == null) {
                        return new ErrorResponse("Character context required, your request is lacking a character registration");
                    }
                    break;
                case AVATAR:
                    if (st.getInstance() == null) {
                        return new ErrorResponse("Avatar context required and you are not connected to an instance.");
                    }
                    if (st.avatar() == null) {
                        return new ErrorResponse("Avatar context required, your request is lacking an avatar registration");
                    }
                    break;
                default:
                    throw new SystemException("Unhandled CONTEXT enum during pre-flight check in execute()");
            }
            if (debug) {
                System.out.println("DEBUG");
                System.out.println("Go for invoke on "+getMethod());
                for (Object o:args) {System.out.println(o); }
            }
            Response response = (Response) (getMethod().invoke(null, args));
            return response;
        } catch (IllegalAccessException ex) {
            throw new SystemException("Command programming error in " + getName() + " - run() access modifier is incorrect", ex);
        } catch (IllegalArgumentException ex) {
            st.logger().log(WARNING,"Execute command " + this.getName() + " failed", ex);
            return new ErrorResponse("Illegal argument in " + getName());
        } catch (InvocationTargetException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof UserException) {
                return new ErrorResponse(getName() + " errored: \n--- " + ex.getCause().getLocalizedMessage());
            }
            if (ex.getCause()!=null && ex.getCause() instanceof SystemException) {
                throw ((SystemException)ex.getCause());
            }
            throw new SystemException("Exception " + ex.toString() + " from call to " + getName(), ex);
        }
    }

    /** Get the name of the arguments.
     * 
     * @param st
     * @param commandname
     * @return
     * @throws UserException 
     */
    public List<String> getArgumentNames(State st) throws UserException {
        List<String> arguments = new ArrayList<>();
        for (Argument a : getArguments()) {
            arguments.add(a.getName());
        }
        return arguments;
    }
    public List<Argument> getInvokingArguments() { return getArguments(); }
    public  Response run(State st,SafeMap parametermap) throws UserException, SystemException {
        //System.out.println("Run in method "+this.getClass().getCanonicalName());
        List<String> arguments=new ArrayList<>();
        for (Argument arg:getInvokingArguments()) {
            if (parametermap.containsKey(arg.getName())) {
                //System.out.println("Added argument "+arg.getName()); 
                arguments.add(parametermap.get(arg.getName()));
            } else {
                //System.out.println("Skipped argument "+arg.getName()); 
                arguments.add(null);
            }
        }
        return run(st,arguments.toArray(new String[]{}));
    }
    
    
    public void simpleHtml(State st, SafeMap values) throws UserException, SystemException {
        //System.out.println("HERE:"+getArgumentCount());
        if (getArgumentCount()==0 || values.submit()) {
            Response response=run(st,values);
            // IF this is an OK response
            if (response instanceof OKResponse) { 
                // and we have cached a 'return url'
                if (!values.get("okreturnurl").isEmpty()) {
                    // go there
                    throw new RedirectionException(values.get("okreturnurl"));
                }
            }
            st.form.add(response);
        }
        getHtmlTemplate(st);
    }
    JSONObject getJSONTemplate(State st) throws UserException, SystemException {
        JSONObject json = new JSONObject();
        int arg = 0;
        for (Argument argument:getArguments()) {
            json.put("arg" + arg + "name", argument.getName());
            json.put("arg" + arg + "description", argument.description());
            switch (argument.type()) {
                case BOOLEAN:
                    json.put("arg"+arg+"type","SELECT");
                    json.put("arg"+arg+"button0","True");
                    json.put("arg"+arg+"button1","False");
                    break;
                case CHOICE:
                    json.put("arg" + arg + "type", "SELECT");
                    int button = 0;
                    for (String label : argument.getChoices(st)) {
                        json.put("arg" + arg + "button" + button, label);
                        button++;
                    }
                    break;
                case REGION:
                    json.put("arg" + arg + "type", "SELECT");
                    button = 0;
                    for (Region reg : st.getInstance().getRegions()) {
                        String label=reg.getName();
                        json.put("arg" + arg + "button" + button, label);
                        button++;
                    }
                    break;                    
                case COORDINATES:
                    json.put("arg" + arg + "type", "COORDINATES");
                    break;
                case EVENT:
                case MODULE:
                case KVLIST:
                case TEXT_ONELINE:
                case TEXT_MULTILINE:
                case PASSWORD:
                case INTEGER:
                case FLOAT:
                case ATTRIBUTE:
                case ATTRIBUTE_WRITABLE:
                case CHARACTER:
                case ZONE:
                    json.put("arg" + arg + "type", "TEXTBOX");
                    break;
                case CHARACTER_PLAYABLE:
                    Set<Char> options = Char.getCharacters(st.getInstance(),st.getAvatar());
                    if (options.size()>12) {
                        json.put("arg"+arg+"type","TEXTBOX");
                    } else {
                        json.put("arg"+arg+"type","SELECT");
                        button=0;
                        for (Char c:options) {
                            json.put("arg"+arg+"button"+button,c.getName());
                            button++;
                        }
                    }
                    break;
                case CHARACTER_FACTION:
                    json.put("arg" + arg + "type", "SENSOR");
                    json.put("arg" + arg + "manual", "true");
                    break;
                case CHARACTER_NEAR:
                    json.put("arg" + arg + "type", "SENSORCHAR");
                    break;
                case AVATAR_NEAR:
                    json.put("arg" + arg + "type", "SENSOR");
                    break;
                default:
                    throw new SystemException("Unhandled ENUM TYPE in getJSONTemplate():" + argument.type());
            }
            arg++;
        }
        json.put("args", arg);
        json.put("invoke", getFullName());
        json.put("incommand", "runtemplate");
        return json;
    }

    void getHtmlTemplate(State st) throws UserException, SystemException {
        Form f = st.form;
        Table t = new Table();
        f.add(t);
        for (Argument arg:getArguments()) {
            t.openRow();
            //                t.add(p.getName());
            t.add(arg.description());
            switch (arg.type()) {
                case AVATAR:
                case TEXT_ONELINE:
                case TEXT_MULTILINE:
                case COORDINATES:
                case CHARACTER:
                case CHARACTER_PLAYABLE: // FIXME this can be done properly
                    t.add(new TextInput(arg.getName()));
                    break;
                case PASSWORD:
                    t.add(new PasswordInput(arg.getName()));
                    break;
                case INTEGER:
                    t.add(new TextInput(arg.getName()));
                    break;
                case FLOAT:
                    t.add(new TextInput(arg.getName()));
                    break;
                case BOOLEAN:
                    t.add(new CheckBox(arg.getName()));
                    break;
                case ATTRIBUTE_WRITABLE:    
                case ATTRIBUTE:
                    DropDownList attributes = new DropDownList(arg.getName());
                    for (Attribute a : st.getAttributes()) {
                        if (arg.type()==ArgumentType.ATTRIBUTE || a.getSelfModify()) { attributes.add(a.getName()); }
                    }
                    t.add(attributes);
                    break;
                case CHARACTER_FACTION:
                    DropDownList factionmembers = new DropDownList(arg.getName());
                    CharacterGroup faction = st.getCharacter().getGroup("Faction");
                    if (faction == null) {
                        throw new UserException("You are in no faction");
                    }
                    for (Char c : faction.getMembers()) {
                        factionmembers.add(c.getName());
                    }
                    t.add(factionmembers);
                    break;
                case CHARACTER_NEAR:
                    DropDownList characters = new DropDownList(arg.getName());
                    for (Char c : st.getCharacter().getNearbyCharacters(st)) {
                        characters.add(c.getName());
                    }
                    t.add(characters);
                    break;
                case EVENT:
                    DropDownList eventlist = new DropDownList(arg.getName());
                    for (Event event : Event.getAll(st.getInstance())) {
                        eventlist.add(event.getName());
                    }
                    t.add(eventlist);
                    break;
                case MODULE:
                    DropDownList modulelist = new DropDownList(arg.getName());
                    for (Module amodule : Modules.getModules()) {
                        modulelist.add(amodule.getName());
                    }
                    t.add(modulelist);
                    break;
                case REGION:
                    DropDownList regionlist = new DropDownList(arg.getName());
                    for (Region aregion : st.getInstance().getRegions()) {
                        regionlist.add(aregion.getName());
                    }
                    t.add(regionlist);
                    break;                    
                case ZONE:
                    DropDownList zonelist = new DropDownList(arg.getName());
                    for (Zone azone:st.getInstance().getZones()) {
                        zonelist.add(azone.getName());
                    }
                    t.add(zonelist);
                    break;                    
                case KVLIST:
                    DropDownList list = new DropDownList(arg.getName());
                    for (String g : Modules.getKVList(st)) {
                        list.add(g,g+" - "+st.getKVDefinition(g).description());
                    }
                    t.add(list);
                    break;
                case CHARACTERGROUP:
                    DropDownList chargrouplist = new DropDownList(arg.getName());
                    for (CharacterGroup g : st.getInstance().getCharacterGroups()) {
                        chargrouplist.add(g.getNameSafe());
                    }
                    t.add(chargrouplist);
                    break;
                case PERMISSIONSGROUP:
                    DropDownList permgrouplist = new DropDownList(arg.getName());
                    for (PermissionsGroup g : st.getInstance().getPermissionsGroups()) {
                        permgrouplist.add(g.getNameSafe());
                    }
                    t.add(permgrouplist);
                    break;
                case PERMISSION:
                    DropDownList permlist = new DropDownList(arg.getName());
                    for (Module m : Modules.getModules()) {
                        for (String entry : m.getPermissions(st).keySet()) {
                            permlist.add(m.getName() + "." + entry);
                        }
                    }
                    t.add(permlist);
                    break;
                case CHOICE:
                    DropDownList choicelist = new DropDownList(arg.getName());
                    for (String s : arg.getChoices(st)) {
                        choicelist.add(s);
                    }
                    t.add(choicelist);
                    break;
                default:
                    throw new SystemException("Unhandled ENUM TYPE in populateForm():" + arg.type());
            }
        }
        if (getArgumentCount() > 0) {
            t.openRow();
            t.add("");
            t.add(new Button("Submit"));
        }
    }

    public String getFullMethodName() {
        return getMethod().getDeclaringClass().getName()+"."+getMethod().getName()+"()";
    }

    public int getInvokingArgumentCount() {
        return getArgumentCount();
    }
    protected static void checkPublicStatic(Method m) throws SystemException {
        if (!Modifier.isStatic(m.getModifiers())) {
            throw new SystemException("Method " + m.getDeclaringClass().getName() + "/" + m.getName() + " must be static");
        }
        if (!Modifier.isPublic(m.getModifiers())) {
            throw new SystemException("Method " + m.getDeclaringClass().getName() + "/" + m.getName() + " must be public");
        }
    }

}
