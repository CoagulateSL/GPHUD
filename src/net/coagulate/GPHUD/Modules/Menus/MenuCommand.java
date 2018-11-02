package net.coagulate.GPHUD.Modules.Menus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import org.json.JSONObject;

/** Templated command implementation, aka a menu command.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class MenuCommand extends Command {

    JSONObject definition;
    Command targetcommand;
    String description="Pick a menu item item item. :P";
    String name;
    public MenuCommand(State st,String name,JSONObject newdef) throws UserException, SystemException {
        super();
        definition=newdef;
        this.name=name;
        targetcommand=this;
    }

    @Override
    public Context context() { return Context.CHARACTER; }

    @Override
    public String description() { return description; }

    @Override
    public List<String> getArgumentNames(State st) throws UserException {
        List<String> args = new ArrayList<>();
        args.add("choice");
        return args;
    }

    @Override
    public int getArgumentCount() { return 1; }

    @Override
    public List<Argument> getArguments() {
        List<Argument> args = new ArrayList<>();
        args.add(new MenuArgument(this,definition));
        return args;
    }

    @Override
    public String getFullName() { return "Menus."+getName(); }

    public String getName() { return name; }

    @Override
    public boolean permitConsole() { return false; }

    @Override
    public boolean permitHUDWeb() {return false; }

    @Override
    public boolean permitJSON() { return true; }

    @Override
    public boolean permitUserWeb() { return false; }

    @Override
    public String getFullMethodName() { return this.getClass()+".run()"; }

    @Override
    public String requiresPermission() { return ""; }

    @Override
    public Response run(State st, SafeMap parametermap) throws UserException, SystemException {
        String selected=parametermap.get("choice");
        int choice=0;
        for (int i=1;i<=12;i++) { if (definition.optString("button"+i,"").equals(selected)) { choice=i; } }
        return Modules.getJSONTemplateResponse(st, definition.getString("command"+choice));        
    }

    @Override
    public Method getMethod() {
        try {
            Method m=this.getClass().getDeclaredMethod("run", State.class,SafeMap.class);
            return m;
        } catch (NoSuchMethodException|SecurityException ex) {
            throw new SystemException("Issue locating RUN command for MenuCommand, this makes no sense :)");
        }
    }

    @Override
    public List<Argument> getInvokingArguments() {
        return getArguments();
    }

    @Override
    public int getInvokingArgumentCount() {
        return getArgumentCount();
    }

    @Override
    public boolean isGenerated() {
        return true;
    }
}
