package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.Core.Tools.Cache;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.*;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.CacheConfig;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Module for GPHUDClient, does dynamic KV for conveyances.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class GPHUDClientModule extends ModuleAnnotation {
	
	final Map<String,KV> base=new TreeMap<>();
	
	public GPHUDClientModule(final String name,final ModuleDefinition annotation) {
		super(name,annotation);
	}
	
	// ---------- STATICS ----------
	@Nonnull
	@Commands(context=Command.Context.CHARACTER,
	          description="Set your Titler's Altitude (height above avatar)",
	          permitExternal=false)
	public static Response setAltitude(@Nonnull final State st,
	                                   @Arguments(name="offset",
	                                              description="Offset, in meters",
	                                              max=3,
	                                              type=Argument.ArgumentType.FLOAT) final Float offset) {
		st.setKV(st.getCharacter(),"GPHUDClient.TitlerAltitude",String.valueOf(offset));
		return new OKResponse("Updated your Titler altitude to "+offset);
	}
	
	private static final Cache<Instance,Map<String,KV>> kvDefinitions=Cache.getCache("GPHUD/GPHUDClientModuleKVDefinitions",
	                                                                                 CacheConfig.SHORT);

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public Map<String,KV> getKVDefinitions(final State st) {
		return kvDefinitions.get(st.getInstance(),()->{
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
		});
	}
	@Override
	public KV getKVDefinition(final State st,@Nonnull final String qualifiedname) {
		// avoid infinite loops as we look up definitions and try get our attributes to make more defintiions etc
		if (base.containsKey(qualifiedname.toLowerCase())) {
			return base.get(qualifiedname.toLowerCase());
		}
		return getKVDefinitions(st).get(qualifiedname.toLowerCase());
	}
	
	@Override
	public void registerKV(@Nonnull final KV a) {
		base.put(a.name().toLowerCase(),a);
	}
	
	// ----- Internal Instance -----
	private void keyConveyances(@Nonnull final Map<String,KV> filterinto,@Nonnull final Map<String,KV> filterfrom) {
		final Map<String,KV> copy=new HashMap<>(filterfrom); // lame concurrency fix?
		for (final KV kv: copy.values()) {
			final String conveyas=kv.conveyAs();
			if (!conveyas.isEmpty()) {
				final String newname="conveyance-"+conveyas;
				//System.out.println("CREATING CONVEYANCE KV "+newname);
				filterinto.put(newname,new ConveyanceKV(newname));
			}
		}
	}
	
}
