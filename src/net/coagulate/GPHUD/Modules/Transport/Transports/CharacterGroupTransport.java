package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;

public class CharacterGroupTransport extends Transporter {
	@Override
	public String description() {
		return "Character Groups - does not copy members";
	}
	
	@Nonnull
	@Override
	public List<String> getExportableElements(@Nonnull final State st) {
		return CharacterGroup.getInstanceGroups(st.getInstance()).stream().map(TableRow::getName).toList();
	}
	
	@Override
	protected void exportElement(@Nonnull final State st,
	                             @Nonnull final String element,
	                             @Nonnull final JSONObject exportTo) {
		final CharacterGroup cg=CharacterGroup.resolve(st,element);
		exportTo.put("open",cg.isOpen());
		exportTo.put("type",cg.getTypeNotNull());
		exportTo.put("kvprecedence",cg.getKVPrecedence());
		exportTo.put("kvstore",kvStore(st,cg));
	}
	
	@Override
	protected void importElement(@Nonnull final State state,
	                             @Nonnull final ImportReport report,
	                             @Nonnull final String name,
	                             @Nonnull final JSONObject element,
	                             final boolean simulation) {
		final boolean open=element.getBoolean("open");
		final String subtype=element.getString("type");
		final int kvprecedence=element.getInt("kvprecedence");
		existCheck(state,
		           simulation,
		           report,
		           CharacterGroup.resolve(state,name),
		           name,
		           ()->CharacterGroup.create(state.getInstance(),name,open,subtype));
		final CharacterGroup cg=CharacterGroup.resolve(state,name);
		if (cg==null&&simulation) {
			return;
		}
		kvRestore(state,simulation,report,element.getJSONObject("kvstore"),name,cg);
		if (open!=cg.isOpen()) {
			report.info("CharacterGroup - changing open flag on "+name+" to "+open);
			if (!simulation) {
				final boolean oldopen=cg.isOpen();
				cg.setOpen(open);
				Audit.audit(state,
				            Audit.OPERATOR.AVATAR,
				            null,
				            null,
				            "CharacterGroup Import",
				            "Group Open",
				            Boolean.toString(oldopen),
				            Boolean.toString(open),
				            "Changed open status via character group import");
			}
		}
		if (kvprecedence!=cg.getKVPrecedence()) {
			report.info("CharacterGroup - changing kvprecedence on "+name+" to "+kvprecedence);
			if (!simulation) {
				final int oldprecedence=cg.getKVPrecedence();
				cg.setKVPrecedence(kvprecedence);
				Audit.audit(state,
				            Audit.OPERATOR.AVATAR,
				            null,
				            null,
				            "CharacterGroup Import",
				            "Group KV Precedence",
				            Integer.toString(oldprecedence),
				            Integer.toString(kvprecedence),
				            "Changed kv precedence via character group import");
			}
		}
		if (!cg.getTypeNotNull().equals(subtype)) {
			report.error("CharacterGroup - group sub type wants to change from '"+cg.getTypeNotNull()+"' to '"+subtype+
			             "' but changing subtypes is not allowed");
		}
	}
}

