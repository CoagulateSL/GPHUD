package net.coagulate.GPHUD.Modules.Zoning;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Data.Zone;
import net.coagulate.GPHUD.Data.ZoneArea;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Command units for zone control.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class ZoneCommands {
	private ZoneCommands() {}

	@Nonnull
	@Commands(context=Context.AVATAR, description="Create a new zone", requiresPermission="Zoning.Config")
	public static Response create(@Nonnull final State st,
	                              @Arguments(description="Name of the zone", type=ArgumentType.TEXT_ONELINE, max=64) final String name) {
		Zone.create(st.getInstance(),name);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Create","Zone","",name,"New zone created");
		return new OKResponse("Zone '"+name+"' created.");

	}

	@Nonnull
	@Commands(context=Context.AVATAR,description="Delete a zone",requiresPermission="Zoning.Config")
	public static Response delete(@Nonnull final State st,
	                              @Arguments(description="Name of the zone to delete",type=ArgumentType.ZONE) final Zone zone) {
		zone.validate(st);
		final String zonename=zone.getName();
		final Set<Region> toupdate=new HashSet<>();
		for (final ZoneArea area:zone.getZoneAreas()) {
			try { toupdate.add(area.getRegion(false)); }
			catch (final UserException|SystemException ignore) {}
		}
		zone.delete(st);
		for (final Region region:toupdate) { region.pushZoning(); }
		return new OKResponse("Deleted zone "+zonename);
	}

	@Nonnull
	@Commands(context=Context.AVATAR, description="Add a volume to a zone", requiresPermission="Zoning.Config")
	public static Response addVolume(@Nonnull final State st,
	                                 @Nonnull @Arguments(type=ArgumentType.ZONE, description="Zone we are adding the volume to") final Zone zone,
	                                 @Nonnull @Arguments(type=ArgumentType.REGION, description="Region for the volume") final Region region,
	                                 @Arguments(type=ArgumentType.COORDINATES, description="Co-ordinates for one corner of the volume cube") final String cornerOne,
	                                 @Arguments(type=ArgumentType.COORDINATES, description="Co-ordinates for the opposing corner of the volume cube.")
	                                 final String cornerTwo) {
		zone.addArea(region,cornerOne,cornerTwo);
		region.pushZoning();
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Add Volume",zone.getName(),null,cornerOne+" - "+cornerTwo,"Added new volume to zone");
		return new OKResponse("Added new volume to zone "+zone.getName());

	}

	@Nonnull
	@Commands(context=Context.CHARACTER, permitScripting=false, description="Trigger a zone change event", permitConsole=false, permitUserWeb=false)
	public static Response zoneTransition(@Nonnull final State st,
	                                      @Nullable @Arguments(description="Name of zone we transitioned into", type=ArgumentType.ZONE,mandatory=false) final Zone zone) {
		// check some things make sense
		// note zone may be null, legally, and fairly often probably.
		if (zone!=null) { zone.validate(st); }
		final Zone oldzone=st.getCharacter().getZone();
		st.getCharacter().setZone(zone);
		st.zone=zone;
		final String entrymessage=st.getKV("Zoning.EntryMessage").value();
		final JSONObject response=new JSONObject();
		if (entrymessage!=null && !entrymessage.isEmpty()) { response.put("message",entrymessage); }
		if (st.hasModule("events")) { EventsMaintenance.zoneTransition(st,response,oldzone,zone); }
		return new JSONResponse(response);
	}

	@Nonnull
	@Template(name="ZONE", description="Current zone")
	public static String getZone(@Nonnull final State st,
	                             final String key) {
		if (!st.hasModule("Zoning")) { return ""; }
		if (st.zone==null) { return ""; }
		return st.zone.getName();
	}

	@Nonnull
	@Commands(context=Context.AVATAR, description="Delete a zone area", requiresPermission="Zoning.Config")
	public static Response deleteVolume(@Nonnull final State st,
	                                    @Arguments(type=ArgumentType.INTEGER, description="Internal ID for the zone volume") final Integer zoneareaid) {
		final ZoneArea za=ZoneArea.get(zoneareaid);
		try { za.validate(st); }
		catch (final NoDataException e) { return new ErrorResponse("There is no zone containing a volume with ID "+zoneareaid); }
		final Zone zone=za.getZone();
		zone.validate(st);
		final String[] vectors=za.getVectors();
		final Region region=za.getRegion(true);
		za.delete();
		String position="NoPosition";
		if (vectors!=null) { position=vectors[0]+" - "+vectors[1]; }
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Delete Area",zone.getName(),position,null,"Area removed from zone");
		region.pushZoning();
		return new OKResponse("Zone area removed from zone "+zone.getName());
	}
}
