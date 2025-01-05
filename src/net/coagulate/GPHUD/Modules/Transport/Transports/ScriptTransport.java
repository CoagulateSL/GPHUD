package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSCompiler;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.GSParser;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.TokenMgrError;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.util.List;

/** Transport scripts */
public class ScriptTransport extends Transporter {
	@Override
	public String description() {
		return "Script Transport";
	}
	
	@Nonnull
	@Override
	public List<String> getExportableElements(@Nonnull final State st) {
		return Script.getScripts(st).stream().map(TableRow::getName).toList();
	}
	
	@Override
	protected void exportElement(@Nonnull final State st,
	                             @Nonnull final String element,
	                             @Nonnull final JSONObject exportTo) {
		final Script script=Script.find(st,element);
		exportTo.put("source",script.getSource());
		exportTo.put("sourceversion",script.getSourceVersion());
		exportTo.put("alias",script.alias());
	}
	
	@Override
	protected void importElement(@Nonnull final State state,
	                             @Nonnull final ImportReport report,
	                             @Nonnull final String name,
	                             @Nonnull final JSONObject element,
	                             final boolean simulation) {
		existCheck(state,simulation,report,Script.findNullable(state,name),name,()->Script.create(state,name));
		final Script s=Script.findNullable(state,name);
		if (s==null&&simulation) {
			return;
		}
		if (s==null) {
			throw new UserInputLookupFailureException("Can not find recently created script "+name);
		}
		final String source=element.getString("source");
		final int version=element.getInt("sourceversion");
		final String alias=element.getString("alias");
		importValue(state,simulation,report,name,"source",s.getSource(),source,()->{
			s.setSource(source);
			final ByteArrayInputStream bais=new ByteArrayInputStream(source.getBytes());
			final GSParser parser=new GSParser(bais);
			//noinspection ErrorNotRethrown
			try {
				final GSCompiler compiler=new GSCompiler(parser.Start(),name);
				s.setBytecode(compiler.toByteCode(state),version,GSCompiler.COMPILER_VERSION);
				report.info("Script - recompiled script "+name);
			} catch (final Exception|TokenMgrError e) {
				report.error("Script - Failed to compile imported script "+name+" - "+e);
			}
		});
		importValue(state,simulation,report,name,"alias",s.alias(),alias,()->s.alias(alias));
	}
}
