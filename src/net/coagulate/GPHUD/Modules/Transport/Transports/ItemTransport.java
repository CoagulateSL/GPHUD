package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.GPHUD.Data.Item;
import net.coagulate.GPHUD.Data.ItemVerb;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemTransport extends Transporter {
	@Override
	public String description() {
		return "Item transport";
	}
	
	@Nonnull
	@Override
	public List<String> getExportableElements(@Nonnull final State st) {
		return Item.getAll(st.getInstance()).stream().map(TableRow::getName).toList();
	}
	
	@Override
	protected void exportElement(@Nonnull final State st,
	                             @Nonnull final String element,
	                             @Nonnull final JSONObject exportTo) {
		final Item item=Item.find(st,element);
		exportTo.put("description",item.description());
		exportTo.put("weight",item.weight());
		exportTo.put("tradable",item.tradable());
		exportTo.put("destroyable",item.destroyable());
		final JSONObject verbs=new JSONObject();
		ItemVerb.findAll(item).forEach(x->{
			final JSONObject verb=new JSONObject();
			verb.put("description",x.description());
			verb.put("payload",x.payload());
			verbs.put(x.getName(),verb);
		});
		exportTo.put("verbs",verbs);
	}
	
	@Override
	protected void importElement(@Nonnull final State state,
	                             @Nonnull final ImportReport report,
	                             @Nonnull final String name,
	                             @Nonnull final JSONObject element,
	                             final boolean simulation) {
		final String description=element.getString("description");
		final int weight=element.getInt("weight");
		final boolean tradable=element.getBoolean("tradable");
		final boolean destroyable=element.getBoolean("destroyable");
		final JSONObject verbs=element.getJSONObject("verbs");
		existCheck(state,
		           simulation,
		           report,
		           Item.findNullable(state.getInstance(),name),
		           name,
		           ()->Item.findOrCreate(state,name));
		final Item item=Item.findNullable(state.getInstance(),name);
		if (item==null&&simulation) {
			return;
		} // just created it?
		importValue(state,simulation,report,name,"weight",item.weight(),weight,()->item.weight(weight));
		importValue(state,simulation,report,name,"tradable",item.tradable(),tradable,()->item.tradable(tradable));
		importValue(state,
		            simulation,
		            report,
		            name,
		            "destroyable",
		            item.destroyable(),
		            destroyable,()->item.destroyable(destroyable));
		for (final String verbname: verbs.keySet()) {
			final JSONObject verbImport=verbs.getJSONObject(verbname);
			existCheck(state,
			           simulation,
			           report,
			           ItemVerb.findNullable(item,verbname),
			           name+"-"+verbname,
			           ()->ItemVerb.create(item,verbname));
			final ItemVerb verb=ItemVerb.findNullable(item,verbname);
			if (!(verb==null&&simulation)) {
				if (!verb.description().equals(verbImport.getString("description"))) {
					verb.description(verbImport.getString("description"));
				}
				if (!verb.payload().similar(verbImport.getJSONObject("payload"))) {
					verb.payload(verbImport.getJSONObject("payload"));
				}
			}
		}
	}
}
