package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Data.Effect;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;

/** Transports effect configuration.  Not applications to characters though */
public class EffectTransport extends Transporter {
	@Override
	public String description() {
		return "Effects transport";
	}
	
	@Nonnull
	@Override
	public List<String> getExportableElements(@Nonnull final State st) {
		return Effect.getAll(st.getInstance()).stream().map(TableRow::getName).toList();
	}
	
	@Override
	protected void exportElement(@Nonnull final State st,
	                             @Nonnull final String element,
	                             @Nonnull final JSONObject exportTo) {
		final Effect effect=Effect.get(st,element);
		exportTo.put("metadata",effect.getMetaData());
		exportTo.put("kvstore",kvStore(st,effect));
	}
	
	@Override
	protected void importElement(@Nonnull final State state,
	                             @Nonnull final ImportReport report,
	                             @Nonnull final String name,
	                             @Nonnull final JSONObject element,
	                             final boolean simulation) {
		existCheck(state,simulation,report,Effect.getNullable(state,name),name,()->Effect.create(state,name,""));
		final Effect effect=Effect.getNullable(state,name);
		if (simulation&&effect==null) { // probably created it in simulation mode
			return;
		}
		if (effect==null) {
			throw new UserInputLookupFailureException("Could not find effect "+name);
		}
		importValue(state,
		            simulation,
		            report,
		            name,
		            "metadata",
		            effect.getMetaData(),
		            element.getString("metadata"),
		            x->effect.setMetaData((String)x));
		kvRestore(state,simulation,report,element.getJSONObject("kvstore"),name,effect);
	}
}
