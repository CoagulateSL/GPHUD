package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Transport instance KV settings */
public class InstanceKVTransport extends Transporter {
	@Override
	public String description() {
		return "Instance level KVs";
	}
	
	@Nonnull
	@Override
	public List<String> getExportableElements(@Nonnull final State st) {
		final ArrayList<String> elements=new ArrayList<>();
		for (@Nonnull final KV x: Modules.getKVSet(st)) {
			if (x.appliesTo(st.getInstance())&&(!x.hidden())&&st.getRawKV(st.getInstance(),x.fullName())!=null) {
				elements.add(x.fullName());
			}
		}
		elements.sort(Comparator.naturalOrder());
		return elements;
	}
	
	@Override
	public void exportElement(@Nonnull final State st,
	                          @Nonnull final String element,
	                          @Nonnull final JSONObject exportTo) {
		exportTo.put("value",st.getRawKV(st.getInstance(),element));
	}
	
	@Override
	public void importElement(@Nonnull final State state,
	                          @Nonnull final ImportReport report,
	                          @Nonnull final String name,
	                          @Nonnull final JSONObject element,
	                          final boolean simulation) {
		final String currentValue=state.getRawKV(state.getInstance(),name);
		final String newValue=element.getString("value");
		importValue(state,
		            simulation,
		            report,
		            state.getInstance().getName(),
		            name,
		            currentValue,
		            newValue,
		            ()->state.setKV(state.getInstance(),name,newValue));
	}
}
