package net.coagulate.GPHUD.Modules.Alias;

import net.coagulate.GPHUD.Data.Alias;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Custom module for aliases, has dynamically generated commands.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AliasModule extends ModuleAnnotation {
	public AliasModule(final String name,
					   final ModuleDefinition annotation) {
		super(name, annotation);
	}

	// ---------- INSTANCE ----------
	@Nullable
	@Override
	public Command getCommandNullable(@Nonnull final State st,
	                                  @Nonnull final String commandname) {
		final Alias alias=Alias.getAlias(st,commandname);
		if (alias==null) return null;
		final JSONObject template=alias.getTemplate();
		return new AliasCommand(st,commandname,template);
	}

	/**
	 * Constructs a command map based on the known aliases.
	 *
	 * @param st State, derives instance
	 *
	 * @return Map of String (command name) to command object
	 */
	@Nonnull
	@Override
	public Map<String,Command> getCommands(@Nonnull final State st) {
		final Map<String,Command> commands=new TreeMap<>();
		final Map<String,JSONObject> templates=Alias.getTemplates(st);
		for (final Map.Entry<String,JSONObject> entry: templates.entrySet()) {
			final String name=entry.getKey();
			try { commands.put(name,new AliasCommand(st,name,entry.getValue())); }
			catch (final Exception ignore) { // could be a lookup failure for an alias, so just don't add it
            }
		}
		return commands;
	}

	@Override
	protected void initialiseInstance(@Nonnull final State st) {
		// some useful defaults
		final JSONObject j=new JSONObject();
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
