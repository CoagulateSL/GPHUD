package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Data.Alias;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;

/** Transport for aliases */

public class AliasTransport extends Transporter {
	@Override
	public String description() {
		return "Alias definitions";
	}
	
	@Nonnull
	@Override
	public List<String> getExportableElements(@Nonnull final State st) {
		return Alias.getAliasMap(st).keySet().stream().toList();
	}
	
	@Override
	protected void exportElement(@Nonnull final State st,
	                             @Nonnull final String element,
	                             @Nonnull final JSONObject exportTo) {
		final Alias alias=Alias.getAlias(st,element);
		if (alias==null) {
			throw new UserInputLookupFailureException("Could not find marked alias "+element);
		}
		exportTo.put("template",alias.getTemplate());
	}
	
	@Override
	protected void importElement(@Nonnull final State state,
	                             @Nonnull final ImportReport report,
	                             @Nonnull final String name,
	                             @Nonnull final JSONObject element,
	                             final boolean simulation) {
		existCheck(state,
		           simulation,
		           report,
		           Alias.getAlias(state,name),
		           name,
		           ()->Alias.create(state,name,element.getJSONObject("template")));
		final Alias alias=Alias.getAlias(state,name);
		if (alias==null&&simulation) {
			return;
		}
		if (alias==null) {
			throw new UserInputLookupFailureException("Failed to find alias '"+name+"' after creating it?");
		}
		importValue(state,
		            simulation,
		            report,
		            name,
		            "template",
		            alias.getTemplate(),
		            element.getJSONObject("template"),
		            ()->alias.setTemplate(element.getJSONObject("template")),
		            (x,y)->((JSONObject)x).similar(y));
	}
}
