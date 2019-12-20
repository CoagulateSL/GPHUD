package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.*;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.TreeMap;

/**
 * Module for GPHUDClient, does dynamic KV for conveyances.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class GPHUDClientModule extends ModuleAnnotation {

	final Map<String,KV> base=new TreeMap<>();

	public GPHUDClientModule(final String name,
	                         final ModuleDefinition def) {
		super(name,def);
	}

	@Nonnull
	@Commands(context=Command.Context.CHARACTER, description="Set your Titler's Altitude (height above avatar)")
	public static Response setAltitude(@Nonnull final State st,
	                                   @Arguments(description="Offset, in meters", max=3, type=Argument.ArgumentType.FLOAT) final Float offset) {
		st.setKV(st.getCharacter(),"GPHUDClient.TitlerAltitude",offset+"");
		return new OKResponse("Updated your Titler altitude to "+offset);
	}

	@Override
	public void registerKV(@Nonnull final KV a) {
		base.put(a.name().toLowerCase(),a);
	}

	@Nonnull
	@Override
	public Map<String,KV> getKVDefinitions(final State st) {
		final Map<String,KV> kv=new TreeMap<>(base); // anotation defined KVs for us.
		// note we must NOT use modules.getallKVs() (ok thats not its name) because it will come back through here and thus infinite loop.
		// instead we'll enumerate the modules other than ourselves, and then deal with ourselves.
		for (final Module m: Modules.getModules()) {
			if (m.isEnabled(st)) {
				if (m!=this) {
					keyConveyances(kv,m.getKVDefinitions(st));
				}
			}
		}
		keyConveyances(kv,base);
		return kv;
	}

	@Override
	public KV getKVDefinition(final State st,
	                          @Nonnull final String qualifiedname) {
		// avoid infinite loops as we look up definitions and try get our attributes to make more defintiions etc
		if (base.containsKey(qualifiedname.toLowerCase())) { return base.get(qualifiedname.toLowerCase()); }
		return getKVDefinitions(st).get(qualifiedname.toLowerCase());
	}

	private void keyConveyances(@Nonnull final Map<String,KV> filterinto,
	                            @Nonnull final Map<String,KV> filterfrom) {
		for (final KV kv: filterfrom.values()) {
			final String conveyas=kv.conveyas();
			if (!conveyas.isEmpty()) {
				final String newname="conveyance-"+conveyas;
				//System.out.println("CREATING CONVEYANCE KV "+newname);
				filterinto.put(newname,new ConveyanceKV(newname));
			}
		}
	}

}
