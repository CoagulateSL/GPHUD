package net.coagulate.GPHUD.Modules.Transport.Transports;

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
		exportTo.put("type",cg.getType());
		exportTo.put("kvprecedence",cg.getKVPrecedence());
		exportTo.put("kvstore",kvStore(st,cg));
	}
	
	@Override
	protected void importElement(@Nonnull final State state,
	                             @Nonnull final ImportReport report,
	                             @Nonnull final String name,
	                             @Nonnull final JSONObject element,
	                             final boolean simulation) {
	}
}

