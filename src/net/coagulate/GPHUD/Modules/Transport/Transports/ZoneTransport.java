package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Data.Zone;
import net.coagulate.GPHUD.Data.ZoneArea;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;

public class ZoneTransport extends Transporter {
	@Override
	public String description() {
		return "Zone transport - may not migrate areas if region names differ";
	}
	
	@Nonnull
	@Override
	public List<String> getExportableElements(@Nonnull final State st) {
		return Zone.getZones(st).stream().map(TableRow::getName).toList();
	}
	
	@Override
	protected void exportElement(@Nonnull final State st,
	                             @Nonnull final String element,
	                             @Nonnull final JSONObject exportTo) {
		final Zone zone=Zone.find(st.getInstance(),element);
		exportTo.put("kvstore",kvStore(st,zone));
		final JSONArray zones=new JSONArray();
		exportTo.put("zones",zones);
		for (final ZoneArea area: zone.getZoneAreas()) {
			final JSONObject z=new JSONObject();
			zones.put(z);
			z.put("region",area.getRegion(true).getName());
			final String[] vectors=area.getVectors();
			z.put("coord1",vectors[0]);
			z.put("coord2",vectors[1]);
		}
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
		           Zone.findNullable(state.getInstance(),name),
		           name,
		           ()->Zone.create(state.getInstance(),name));
		final Zone zone=Zone.findNullable(state.getInstance(),name);
		if (zone==null&&simulation) {
			return;
		}
		kvRestore(state,simulation,report,element.getJSONObject("kvstore"),name,zone);
		final JSONArray areas=element.getJSONArray("zones");
		for (int i=0;i<areas.length();i++) {
			final JSONObject area=areas.getJSONObject(i);
			final String regionName=area.getString("region");
			final String coord1=area.getString("coord1");
			final String coord2=area.getString("coord2");
			final Region region=Region.findNullable(regionName,true);
			if (region!=null) {
				if (region.getInstance()==state.getInstance()) {
					boolean exists=false;
					for (final ZoneArea compare: zone.getZoneAreas()) {
						if (compare.compareCoord(1,coord1)&&compare.compareCoord(2,coord2)) {
							exists=true;
						}
					}
					if (!exists) {
						if (simulation) {
							report.info("Zone - "+name+" - added zone area");
						} else {
							report.info("Zone - "+name+" - added zone area");
							zone.addArea(region,coord1,coord2);
						}
					}
				}
			}
		}
		
	}
}
