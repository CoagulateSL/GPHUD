package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.*;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.State;

import java.util.Map;
import java.util.TreeMap;

/**
 * Module for GPHUDClient, does dynamic KV for conveyances.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class GPHUDClientModule extends ModuleAnnotation {

	Map<String, KV> base = new TreeMap<>();

	public GPHUDClientModule(String name, ModuleDefinition def) throws SystemException, UserException {
		super(name, def);
	}

	@Commands(context = Command.Context.CHARACTER, description = "Set your Titler's Altitude (height above avatar)")
	public static Response setAltitude(State st,
	                                   @Arguments(description = "Offset, in meters", mandatory = true, max = 3, type = Argument.ArgumentType.FLOAT)
			                                   Float offset) {
		st.setKV(st.getCharacter(), "GPHUDClient.TitlerAltitude", offset + "");
		return new OKResponse("Updated your Titler altitude to " + offset);
	}

	@Override
	public void registerKV(KV a) throws UserException {
		base.put(a.name().toLowerCase(), a);
	}

	@Override
	public Map<String, KV> getKVDefinitions(State st) {
		Map<String, KV> kv = new TreeMap<>();
		kv.putAll(base); // anotation defined KVs for us.
		// note we must NOT use modules.getallKVs() (ok thats not its name) because it will come back through here and thus infinite loop.
		// instead we'll enumerate the modules other than ourselves, and then deal with ourselves.
		for (Module m : Modules.getModules()) {
			if (m.isEnabled(st)) {
				if (m != this) {
					keyConveyances(kv, m.getKVDefinitions(st));
				}
			}
		}
		keyConveyances(kv, base);
		return kv;
	}

	@Override
	public KV getKVDefinition(State st, String qualifiedname) throws SystemException {
		// avoid infinite loops as we look up definitions and try get our attributes to make more defintiions etc
		if (base.containsKey(qualifiedname.toLowerCase())) { return base.get(qualifiedname.toLowerCase()); }
		return getKVDefinitions(st).get(qualifiedname.toLowerCase());
	}

	private void keyConveyances(Map<String, KV> filterinto, Map<String, KV> filterfrom) {
		for (String name : filterfrom.keySet()) {
			KV kv = filterfrom.get(name);
			String conveyas = kv.conveyas();
			if (conveyas != null && !conveyas.isEmpty()) {
				String newname = "conveyance-" + conveyas;
				//System.out.println("CREATING CONVEYANCE KV "+newname);
				filterinto.put(newname, new ConveyanceKV(newname));
			}
		}
	}

}
