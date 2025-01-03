package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Event;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Data.Zone;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;

public class EventTransport extends Transporter {
	@Override
	public String description() {
		return "Event Transport - does not copy schedules";
	}
	
	@Nonnull
	@Override
	public List<String> getExportableElements(@Nonnull final State st) {
		return Event.getAll(st.getInstance()).stream().map(x->x.getName()).toList();
	}
	
	@Override
	protected void exportElement(@Nonnull final State st,
	                             @Nonnull final String element,
	                             @Nonnull final JSONObject exportTo) {
		final Event event=Event.find(st.getInstance(),element);
		exportTo.put("kvstore",kvStore(st,event));
		final JSONArray zones=new JSONArray();
		zones.putAll(event.getZones().stream().map(TableRow::getName).toList());
		exportTo.put("zones",zones);
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
		           Event.find(state.getInstance(),name),
		           name,
		           ()->Event.create(state.getInstance(),name));
		final Event event=Event.find(state.getInstance(),name);
		kvRestore(state,simulation,report,element.getJSONObject("kvstore"),name,event);
		element.getJSONArray("zones").forEach((x)->{
			final Zone zone=Zone.findNullable(state.getInstance(),(String)x);
			if (zone==null) {
				report.warn("Event - Zone '"+x+"' could not be found and was not restored to event '"+name+"'");
			} else {
				boolean found=false;
				for (final Zone compare: event.getZones()) {
					if (zone.getId()==compare.getId()) {
						found=true;
						break;
					}
				}
				if (!found) {
					report.info("Event - Adding zone '"+x+"' to event '"+name+"'");
					if (!simulation) {
						event.addZone(zone);
						Audit.audit(state,
						            Audit.OPERATOR.AVATAR,
						            null,
						            null,
						            "Impor Event",
						            "Added Zone",
						            null,
						            (String)x,
						            "Added zone to event via import");
					}
				}
			}
		});
	}
}
