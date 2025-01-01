package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.GPHUD.Data.Alias;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;

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
		exportTo.put("template",Alias.getAlias(st,element).getTemplate());
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
		importValue(state,
		            simulation,
		            report,
		            name,
		            "template",
		            Alias.getAlias(state,name).getTemplate(),
		            element.getJSONObject("template"),
		            ()->Alias.getAlias(state,name).setTemplate(element.getJSONObject("template")),
		            (x,y)->((JSONObject)x).similar(y));
	}
}
