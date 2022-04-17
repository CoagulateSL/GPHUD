package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.Core.Tools.ExceptionTools;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.CheckBox;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.Paragraph;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCode;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCodeDataType;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSCompiler;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.GSParser;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.GSStart;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.ParseException;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.coagulate.GPHUD.Modules.Scripting.ScriptingConfig.STAGE.RESULTS;
import static net.coagulate.GPHUD.Modules.Scripting.ScriptingConfig.STAGE.SIMULATION;

public class ScriptingConfig {
	// ---------- STATICS ----------
	@URL.URLs(url="/configuration/scripting",
			  requiresPermission = "Scripting.*")
	public static void configPage(@Nonnull final State st,
	                              final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("Scripting Module"));
		f.add(new Paragraph("List of scripts"));
		f.add(Script.getTable(st,st.hasPermission("scripting.delete")));
		f.noForm();
		f.add(new Form(st,true,"/GPHUD/configuration/scripting/create","Create new script"));
	}

	@URL.URLs(url="/configuration/scripting/delete",
			  requiresPermission="Scripting.Create")
	public static void deleteScript(@Nonnull final State st,
									@Nonnull final SafeMap values) {
		final Form f = st.form();
        final String scriptName = values.get("scriptname");
		if (!(scriptName.isEmpty() || values.get("confirm").isEmpty())) {
			Script.find(st,scriptName).delete();
			Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,null,"Delete",values.get("scriptname"),"","","Deleted script");
			throw new RedirectionException("/GPHUD/configuration/scripting");
		}
		f.add(new TextHeader("Delete script"));
		f.add("Name of script:").add(new TextInput("scriptname")).br();
		f.add(new CheckBox("confirm")).add("Confirm deletion?  This action can not be undone.").br().br();
		f.add(new Button("Delete"));
	}

	@URL.URLs(url="/configuration/scripting/create",
	          requiresPermission="Scripting.Create")
	public static void createScript(@Nonnull final State st,
	                                @Nonnull final SafeMap values) {
		final Form f=st.form();
		if (!values.get("scriptname").isEmpty()) {
			Script.create(st,values.get("scriptname"));
			Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,null,"Create",values.get("scriptname"),"","","Created script");
			throw new RedirectionException("/GPHUD/configuration/scripting");
		}
		f.add(new TextHeader("Create new script"));
		f.add("Name of script:").add(new TextInput("scriptname")).br();
		f.add(new Button("Create"));
	}

	@URL.URLs(url="/configuration/scripting/edit/*",
	          requiresPermission="Scripting.Create")
	public static void editScript(@Nonnull final State st,
	                              @Nonnull final SafeMap values) {
		final Form f=st.form();
		final String[] split=st.getDebasedURL().split("/");
		final String id=split[split.length-1];
		final Script script=Script.get(Integer.parseInt(id));
		script.validate(st);
		if (!values.get("scriptalias").isEmpty()) {
			script.alias(values.get("scriptalias"));
		}
		if (!values.get("scriptsource").isEmpty()) {
			script.setSource(values.get("scriptsource"));
			f.p("Script source saved OK!");
			final String source=script.getSource();
			final int version=script.getSourceVersion();
			// try compile
			try {
				final ByteArrayInputStream bais=new ByteArrayInputStream(source.getBytes());
				final GSParser parser=new GSParser(bais);
				parser.enable_tracing();
				GSStart gsscript=null;
				try {
					gsscript=parser.Start();
				}
				catch (@Nonnull final Throwable e) { // catch throwable bad, but "lexical error" is an ERROR type... which we're not meant to catch.   but have to.  great.
					if (e instanceof ParseException) {
						final ParseException pe=(ParseException) e;
						final String tokenimage;
						tokenimage="Last token: "+pe.currentToken.image+"<br>";
						f.p("<b>Parse failed:</b> "+e+"<br>"+tokenimage);
					}
					else {
						f.p("<b>Parse failed:</b> "+e);
					}
					Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,null,"Save",script.getName(),"","ParseFail","Saved script, with parse failures");
				}
				if (gsscript!=null) {
					final GSCompiler compiler=new GSCompiler(gsscript, script.getName());
					compiler.compile(st);
					script.setBytecode(compiler.toByteCode(st),version,GSCompiler.COMPILER_VERSION);
					f.p("Script compiled and saved OK!");
					Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,null,"Save",script.getName(),"","OK!","Saved script and compiled OK!");
				}
			}
			catch (@Nonnull final NullPointerException ex) {
				throw new GSInternalError("Null pointer exception in compiler",ex);
			} catch (@Nonnull final Throwable t) {
				f.p("Compilation failed; " + t);
				Audit.audit(true, st, Audit.OPERATOR.AVATAR, null, null, "Save", script.getName(), "", "CompileFail", "Saved script, with compilation failures");
			}
		}
		f.add(new TextHeader("Edit script " + script.getName()));
		final Table versions = new Table();
		versions.add("Source code version").add(script.getSourceVersion() + "");
		versions.openRow();
		if (script.getSourceVersion() == script.getByteCodeVersion()) {
			versions.add("Byte code version").add(script.getByteCodeVersion() + "");
		} else {
			versions.add("<font color=red>Byte code version</font>")
					.add("<font color=red>" + script.getByteCodeVersion() + "</font>")
					.add("<font color=red>Compiled version is behind source version, please attempt to SAVE SOURCE</font>");
		}
		versions.openRow().add("Script alias").add(new TextInput("scriptalias", script.alias()));
		f.add(versions);
		f.br();
		f.add("<textarea name=scriptsource rows=25 cols=80>" + script.getSource() + "</textarea>");
		f.br();
		f.add(new Button("Save Source", "Save Source"));
		f.br().br().add("<hr>").br();
		f.add("Debugging information: ");
		f.add(new Button("View Parse Tree", "View Parse Tree"));
		f.add(new Button("View Compiler Output", "View Compiler Output"));
		f.add(new Button("View Raw ByteCode","View Raw ByteCode"));
		f.add(new Button("View Disassembly","View Disassembly"));
		f.add(new Button("View Simulation","View Simulation"));
		f.add(new Button("View Results","View Results"));
		f.add(new Button("View ALL","View ALL"));
		//if (GPHUD.DEV) { f.add(new Button("DEBUG", "DEBUG")); }
		f.br();
		if ("View Parse Tree".equals(values.get("View Parse Tree")) || "View ALL".equals(values.get("View ALL"))) {
			f.add("<hr>").br().add(new TextSubHeader("Parse Tree Output"));
			f.add(debug(st, script.getSource(), STAGE.PARSER));
		}
		if ("View Compiler Output".equals(values.get("View Compiler Output")) || "View ALL".equals(values.get("View ALL"))) {
			f.add("<hr>").br().add(new TextSubHeader("Compiler Output"));
			f.add(debug(st, script.getSource(), STAGE.COMPILER));
		}
		if ("View Raw ByteCode".equals(values.get("View Raw ByteCode")) || "View ALL".equals(values.get("View ALL"))) {
			f.add("<hr>").br().add(new TextSubHeader("Raw ByteCode"));
			f.add(debug(st, script.getSource(), STAGE.BYTECODE));
		}
		if ("View Disassembly".equals(values.get("View Disassembly")) || "View ALL".equals(values.get("View ALL"))) {
			f.add("<hr>").br().add(new TextSubHeader("Disassembly"));
			f.add(debug(st, script.getSource(), STAGE.DISASSEMBLY));
		}
		if ("View Simulation".equals(values.get("View Simulation")) || "View ALL".equals(values.get("View ALL"))) {
			f.add("<hr>").br().add(new TextSubHeader("Simulation"));
			f.add(debug(st, script.getSource(), SIMULATION));
		}
		if ("View Results".equals(values.get("View Results")) || "View ALL".equals(values.get("View ALL"))) {
			f.add("<hr>").br().add(new TextSubHeader("Simulation"));
			f.add(debug(st, script.getSource(), RESULTS));
		}
		//if (GPHUD.DEV && values.get("DEBUG").equals("DEBUG")) { Scripts.test(); }
	}

	// ----- Internal Statics -----
	@Nonnull
	private static String debug(@Nonnull final State st,
	                            @Nonnull final String script,
	                            final STAGE stage) {
		final ByteArrayInputStream bais=new ByteArrayInputStream(script.getBytes());
		final GSParser parser=new GSParser(bais);
		parser.enable_tracing();
		final GSStart gsscript;
		try {
			gsscript=parser.Start();
			if (stage==STAGE.PARSER) { return gsscript.toHtml(); }
		}
		catch (@Nonnull final Throwable e) { // catch throwable bad, but "lexical error" is an ERROR type... which we're not meant to catch.   but have to.  great.
			if (e instanceof ParseException) {
				final ParseException pe=(ParseException) e;
				final String tokenimage;
				tokenimage="Last token: "+pe.currentToken.image+"<br>";
				return "<b>Parse failed:</b> "+e+"<br>"+tokenimage;
			}
			return "<b>Parse failed:</b> "+e;
		}

		final GSCompiler compiler;
		try {
			compiler=new GSCompiler(gsscript,"SIMULATION");
			if (stage==STAGE.COMPILER) {
				final List<ByteCode> bytecode=compiler.compile(st);
				final StringBuilder code=new StringBuilder("<pre><table border=0>");
				for (final ByteCode bc: bytecode) {
					final ParseNode bcnode=bc.node();
					code.append("<tr><td>")
					    .append(bcnode!=null?bcnode.tokens():"")
					    .append("</td><td>")
					    .append(bc.explain().replaceFirst(" \\(","</td><td><i>("))
					    .append("</i></td><td>");
					final ArrayList<Byte> bcl=new ArrayList<>();
					bc.toByteCode(bcl);
					for (final Byte b: bcl) {
						code.append(b).append(" ");
					}
					code.append("</td></tr>");
				}
				code.append("</table></pre>");
				return code.toString();
			}
		}
		catch (@Nonnull final NullPointerException ex) {
			throw new GSInternalError("Null pointer exception in compiler",ex);
		}
		catch (@Nonnull final Throwable e) { // catch throwable bad, but "lexical error" is an ERROR type... which we're not meant to catch.   but have to.  great.
			return "<b>Compilation failed:</b> "+e;
		}

		final Byte[] rawcode=compiler.toByteCode(st);
		if (stage==STAGE.BYTECODE) {
			final StringBuilder bcstring=new StringBuilder("<pre><table border=0><tr>");
			for (int i=0;i<rawcode.length;i++) {
				if ((i%25)==0) { bcstring.append("</tr><tr><th>").append(i).append("</th>"); }
				bcstring.append("<td>").append(rawcode[i]).append("</td>");
			}
			bcstring.append("</tr></table></pre>");
			return bcstring.toString();
		}

		final GSVM gsvm=new GSVM(rawcode);
		if (stage==STAGE.DISASSEMBLY) {
			return gsvm.toHtml();
		}

		try {
			if (stage==SIMULATION || stage==RESULTS) {
				final long start=System.currentTimeMillis();
				final List<GSVM.ExecutionStep> steps=gsvm.simulate(st);
				final long end=System.currentTimeMillis();
				final StringBuilder output=new StringBuilder("<p><i>Run time: "+(end-start)+" ms</i></p><table border=1><td>IC</td><th>PC</th><th>OpCode</th><th>OpArgs</th"+"><th>Stack</th><th>Variables</th></tr>");
				if (stage==SIMULATION) {
					// no more than 100 steps now
					int index=steps.size()-100;
					if (index<0) { index=0; }
					for (;index<steps.size();index++) {
						output.append(formatStep(steps.get(index)));
					}
				}
				else {
					if (steps.size()>1) { output.append(formatStep(steps.get(steps.size()-2))); }
					if (!steps.isEmpty()) { output.append(formatStep(steps.get(steps.size()-1))); }
				}
				output.append("</table>");
				return output.toString();
			}

		}
		catch (@Nonnull final ArrayIndexOutOfBoundsException ex) {
			throw new GSInternalError("Array index out of bounds in compiler/simulator",ex);
		}
		catch (@Nonnull final Throwable e) { // catch throwable bad, but "lexical error" is an ERROR type... which we're not meant to catch.   but have to.  great.
			return "<b>Simulation failed:</b> "+e;
		}
		return "Did nothing (?)";
	}

	@Nonnull
	private static String formatStep(@Nonnull final GSVM.ExecutionStep step) {
		final StringBuilder output=new StringBuilder();
		output.append("<tr><td>").append(step.IC).append("</td><th>").append(step.programcounter).append("</th><td>").append(step.decode).append("</td><td><table>");
		for (int i=0;i<step.resultingstack.size();i++) {
			output.append("<tr><th>").append(i).append("</th><td>").append(step.resultingstack.get(i).htmlDecode()).append("</td></tr>");
		}
		output.append("</table></td><td><table>");
		for (final Map.Entry<String,ByteCodeDataType> entry: step.resultingvariables.entrySet()) {
            String decode = "???";
            final boolean italics = entry.getKey().startsWith(" ");
			if (entry.getValue()!=null) {
				decode=entry.getValue().htmlDecode();
			}
			output.append("<tr><th>").
					append(italics?"<i>":"").
					append(entry.getKey()).
					append(italics?"</i>":"").
					append("</th><td>").
					append(italics?"<i>":"").
					append(decode.replaceAll("<td>","<td>"+(italics?"<i>":""))).
					append(italics?"</i>":"").
					append("</td></tr>");
		}
		output.append("</table></td></tr>");
		if (step.t!=null) {
			output.append("<tr><td colspan=100>").append(ExceptionTools.toHTML(step.t)).append("</td></tr>");
		}
		return output.toString();
	}

	public enum STAGE {
		PARSER,
		COMPILER,
		BYTECODE,
		DISASSEMBLY,
		SIMULATION,
		RESULTS
	}
}
