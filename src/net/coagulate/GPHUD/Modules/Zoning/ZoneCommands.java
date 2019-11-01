package net.coagulate.GPHUD.Modules.Zoning;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Data.Zone;
import net.coagulate.GPHUD.Data.ZoneArea;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Events.EventsMaintenance;
import net.coagulate.GPHUD.Modules.Templater.Template;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/**
 * Command units for zone control.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class ZoneCommands {

	@Commands(context = Context.AVATAR, description = "Create a new zone", requiresPermission = "Zoning.Config")
	public static Response create(State st,
	                              @Arguments(description = "Name of the zone", type = ArgumentType.TEXT_ONELINE, max = 64)
			                              String name) {
		Zone.create(st.getInstance(), name);
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "Create", "Zone", "", name, "New zone created");
		return new OKResponse("Zone '" + name + "' created.");

	}

	@Commands(context = Context.AVATAR, description = "Add a volume to a zone", requiresPermission = "Zoning.Config")
	public static Response addVolume(State st,
	                                 @Arguments(type = ArgumentType.ZONE, description = "Zone we are adding the volume to")
			                                 Zone zone,
	                                 @Arguments(type = ArgumentType.REGION, description = "Region for the volume")
			                                 Region region,
	                                 @Arguments(type = ArgumentType.COORDINATES, description = "Co-ordinates for one corner of the volume cube")
			                                 String cornerOne,
	                                 @Arguments(type = ArgumentType.COORDINATES, description = "Co-ordinates for the opposing corner of the volume cube.")
			                                 String cornerTwo) {
		zone.addArea(region, cornerOne, cornerTwo);
		region.pushZoning();
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "Add Volume", zone.getName(), null, cornerOne + " - " + cornerTwo, "Added new volume to zone");
		return new OKResponse("Added new volume to zone " + zone.getName());

	}

	@Commands(context = Context.CHARACTER, permitScripting = false,description = "Trigger a zone change event", permitConsole = false, permitHUDWeb = false, permitUserWeb = false)
	public static Response zoneTransition(State st,
	                                      @Arguments(description = "Name of zone we transitioned into", type = ArgumentType.ZONE)
			                                      Zone zone) {
		// check some things make sense
		// note zone may be null, legally, and fairly often probably.
		if (zone != null) { zone.validate(st); }
		Zone oldzone = st.getCharacter().getZone();
		st.getCharacter().setZone(zone);
		st.zone = zone;
		String entrymessage = st.getKV("Zoning.EntryMessage").value();
		JSONObject response = new JSONObject();
		if (entrymessage != null && !entrymessage.isEmpty()) { response.put("message", entrymessage); }
		if (st.hasModule("events")) { EventsMaintenance.zoneTransition(st, response, oldzone, zone); }
		return new JSONResponse(response);
	}

	@Template(name = "ZONE", description = "Current zone")
	public static String getZone(State st, String key) {
		if (st.zone == null) { return ""; }
		return st.zone.getName();
	}

	@Commands(context = Context.AVATAR, description = "Delete a zone area", requiresPermission = "Zoning.Config")
	public static Response deleteVolume(State st,
	                                    @Arguments(type = ArgumentType.INTEGER, description = "Internal ID for the zone volume")
			                                    Integer zoneareaid) {
		ZoneArea za = ZoneArea.get(zoneareaid);
		Zone zone = za.getZone();
		zone.validate(st);
		String[] vectors = za.getVectors();
		Region region = za.getRegion();
		za.delete();
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "Delete Area", zone.getName(), vectors[0] + " - " + vectors[1], null, "Area removed from zone");
		region.pushZoning();
		return new OKResponse("Zone area removed from zone " + zone.getName());
	}
}
