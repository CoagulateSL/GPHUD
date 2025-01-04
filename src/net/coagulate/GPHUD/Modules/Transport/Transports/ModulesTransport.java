package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;

public class ModulesTransport extends Transporter {
	@Override
	public String description() {
		return "Modules - enables modules to match this configuration";
	}
	
	@Nonnull
	@Override
	public List<String> getExportableElements(@Nonnull final State st) {
		return List.of("Modules");
	}
	
	@Override
	protected void exportElement(@Nonnull final State st,
	                             @Nonnull final String element,
	                             @Nonnull final JSONObject exportTo) {
		for (final Module m: Modules.getModules()) {
			if (m.isEnabled(st)) {
				exportTo.put(m.getName(),true);
			}
		}
	}
	
	@Override
	protected void importElement(@Nonnull final State state,
	                             @Nonnull final ImportReport report,
	                             @Nonnull final String name2,
	                             @Nonnull final JSONObject element,
	                             final boolean simulation) {
		for (final String enableme: element.keySet()) {
			for (final Module m: Modules.getModules()) {
				if (enableme.equals(m.getName())) {
					System.out.println("HERE1");
					if (element.getBoolean(enableme)) {
						System.out.println("HERE2");
						if (!m.isEnabled(state)) {
							System.out.println("HERE3");
							report.info("Module - Enabling module "+m.getName());
							if (!simulation) {
								state.setKV(state.getInstance(),m.getName()+".Enabled","true");
								Audit.audit(state,
								            Audit.OPERATOR.AVATAR,
								            null,
								            null,
								            "Import",
								            "Enable Module",
								            null,
								            m.getName(),
								            "Module enabled by importer");
							}
						}
					}
				}
			}
		}
	}
}
