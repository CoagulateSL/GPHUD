package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Transport attribute definitions */
public class AttributeTransport extends Transporter {
	@Override
	public String description() {
		return "Attribute definitions";
	}
	
	@Nonnull
	@Override
	public List<String> getExportableElements(@Nonnull final State st) {
		final List<String> l=new ArrayList<>();
		final Set<Attribute> attrs=Attribute.getAttributes(st);
		for (final Attribute a: attrs) {
			l.add(a.getName());
		}
		return l;
	}
	
	@Override
	protected void exportElement(@Nonnull final State st,
	                             @Nonnull final String element,
	                             @Nonnull final JSONObject exportTo) {
		final Attribute attr=Attribute.findNullable(st,element);
		if (attr==null) {
			throw new UserInputLookupFailureException("Could not find Attribute "+element);
		}
		exportTo.put("selfmodify",attr.getSelfModify());
		exportTo.put("attributetype",attr.getType());
		exportTo.put("grouptype",attr.getSubType());
		exportTo.put("usesabilitypoints",attr.usesAbilityPoints());
		exportTo.put("required",attr.getRequired());
		exportTo.put("defaultvalue",attr.getDefaultValue());
		exportTo.put("templatable",attr.templatable());
	}
	
	@Override
	protected void importElement(@Nonnull final State state,
	                             @Nonnull final ImportReport report,
	                             @Nonnull final String name,
	                             @Nonnull final JSONObject element,
	                             final boolean simulation) {
		final boolean selfmodify=element.getBoolean("selfmodify");
		final Attribute.ATTRIBUTETYPE attributetype=Attribute.ATTRIBUTETYPE.valueOf(element.getString("attributetype"));
		final String grouptype=element.optString("grouptype",null);
		final boolean usesabilitypoints=element.getBoolean("usesabilitypoints");
		final boolean required=element.getBoolean("required");
		final String defaultvalue=element.optString("defaultvalue",null);
		final boolean templatable=element.getBoolean("templatable");
		
		final Attribute curattr=Attribute.findNullable(state,name);
		if (curattr==null) {
			if (simulation) {
				report.info("Attribute - Would create attribute "+name);
				return;
			}
			report.info("Attribute - Create attribute "+name);
			Attribute.create(state,name,selfmodify,attributetype,grouptype,usesabilitypoints,required,defaultvalue);
			Audit.audit(state,
			            Audit.OPERATOR.AVATAR,
			            null,
			            null,
			            "Import Created",
			            name,
			            null,
			            name,
			            "Created attribute via import");
		}
		
		final Attribute attr=Attribute.find(state.getInstance(),name);
		importValue(state,
		            simulation,
		            report,
		            name,
		            "Self Modify",
		            attr.getSelfModify(),
		            selfmodify,
		            ()->attr.setSelfModify(selfmodify));
		if (attr.getType()!=attributetype) {
			report.error("Attribute - Can not change attribute type from "+attr.getType()+" to "+attributetype+
			             " - not supported");
		}
		if ((attr.getSubType()==null&&grouptype!=null)||(attr.getSubType()!=null&&grouptype==null)||
		    (grouptype!=null&&(!grouptype.equals(attr.getSubType())))) {
			report.error("Attribute - Can not change attribute sub type from "+attr.getSubType()+" to "+grouptype+
			             " - not supported");
		}
		importValue(state,
		            simulation,
		            report,
		            name,
		            "Uses Ability Points",
		            attr.usesAbilityPoints(),
		            usesabilitypoints,
		            ()->attr.setUsesAbilityPoints(usesabilitypoints));
		importValue(state,simulation,report,name,"Required",attr.getRequired(),required,()->attr.setRequired(required));
		importValue(state,
		            simulation,
		            report,
		            name,
		            "Default Value",
		            attr.getDefaultValue(),
		            defaultvalue,
		            ()->attr.setDefaultValue(defaultvalue));
		importValue(state,
		            simulation,
		            report,
		            name,
		            "Templatable",
		            attr.templatable(),
		            templatable,
		            ()->attr.templatable(state,templatable));
	}
}
