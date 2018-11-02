package net.coagulate.GPHUD.Modules.Alias;

import java.util.Map;
import java.util.TreeMap;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Alias;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/**  Custom module for aliases, has dynamically generated commands.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AliasModule extends ModuleAnnotation {
    public AliasModule(String name,ModuleDefinition definition) {
        super(name,definition);
    }
    /** Constructs a command map based on the known aliases.
     * 
     * @param st State, derives instance
     * @return Map of String (command name) to command object
     */
    @Override
    public Map<String, Command> getCommands(State st) {
        Map<String,Command> commands=new TreeMap<>();
        Map<String,JSONObject> templates=Alias.getTemplates(st);
        for (String name:templates.keySet()) {
            commands.put(name,new AliasCommand(st,name,templates.get(name)));
        }
        return commands;
    }

    @Override
    public Command getCommand(State st,String commandname) {
        Alias alias = Alias.getAlias(st, commandname);
        if (alias==null) { throw new UserException("Unknown command alias "+commandname); }
        JSONObject template = alias.getTemplate();
        return new AliasCommand(st,commandname,template);
    }

    @Override
    protected void initialiseInstance(State st) {
        // some useful defaults
        JSONObject j=new JSONObject();
        j.put("invoke","Roller.Roll");
        j.put("sides","100");
        j.put("bias","--LEVEL--");
        j.put("dice","1");
        Alias.create(st,"roll",j);
        j.put("sides","6");
        j.put("bias","0");
        j.put("reason","Damage roll");
        Alias.create(st,"damage",j);
    }

    
}
