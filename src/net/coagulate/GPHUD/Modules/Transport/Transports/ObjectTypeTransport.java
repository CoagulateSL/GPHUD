package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.GPHUD.Data.ObjType;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;

public class ObjectTypeTransport extends Transporter {
	@Override
	public String description() {
		return "Object Type Transport";
	}
	
	@Nonnull
	@Override
	public List<String> getExportableElements(@Nonnull final State st) {
		return ObjType.getObjectTypes(st).stream().toList();
	}
	
	@Override
	protected void exportElement(@Nonnull final State st,
	                             @Nonnull final String element,
	                             @Nonnull final JSONObject exportTo) {
		exportTo.put("behaviour",ObjType.get(st,element).getBehaviour());
	}
	
	@Override
	protected void importElement(@Nonnull final State state,
	                             @Nonnull final ImportReport report,
	                             @Nonnull final String name,
	                             @Nonnull final JSONObject element,
	                             final boolean simulation) {
		final JSONObject behaviour=element.getJSONObject("behaviour");
		existCheck(state,simulation,report,ObjType.get(state,name),name,()->ObjType.create(state,name,behaviour));
		final ObjType ot=ObjType.get(state,name);
		if (simulation&&ot==null) {
			return;
		}
		importValue(state,
		            simulation,
		            report,
		            name,
		            "behaviour",
		            ot.getBehaviour(),
		            behaviour,
		            ()->ot.setBehaviour(behaviour),
		            (x,y)->((JSONObject)x).similar(y));
	}
}
