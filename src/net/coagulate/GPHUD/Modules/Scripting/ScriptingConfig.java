package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.Core.Tools.ExceptionTools;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Scripts;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
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
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.GSParser;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.GSStart;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.ParseException;
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
	@URL.URLs(url = "/configuration/scripting")
	public static void configPage(@Nonnull State st, SafeMap values) {
		Form f = st.form();
		f.add(new TextHeader("Scripting Module"));
		f.add(new Paragraph("List of scripts"));
		f.add(Scripts.getTable(st.getInstance()));
		f.noForm();
		f.add(new Form(st, true, "/GPHUD/configuration/scripting/create", "Create new script"));
	}

	@URL.URLs(url = "/configuration/scripting/create", requiresPermission = "Scripting.Create")
	public static void createScript(@Nonnull State st, @Nonnull SafeMap values) {
		Form f = st.form();
		if (!values.get("scriptname").isEmpty()) {
			Scripts.create(st, values.get("scriptname"));
			Audit.audit(true,st, Audit.OPERATOR.AVATAR,null,null,"Create",values.get("scriptname"),"","","Created script");
			throw new RedirectionException("/GPHUD/configuration/scripting");
		}
		f.add(new TextHeader("Create new script"));
		f.add("Name of script:").add(new TextInput("scriptname")).br();
		f.add(new Button("Create"));
	}

	@URL.URLs(url = "/configuration/scripting/edit/*", requiresPermission = "Scripting.Create")
	public static void editScript(@Nonnull State st, @Nonnull SafeMap values) {
		Form f = st.form();
		String[] split = st.getDebasedURL().split("/");
		//System.out.println(split.length);
		String id = split[split.length - 1];
		Scripts script = Scripts.get(Integer.parseInt(id));
		script.validate(st);
		if (!values.get("scriptsource").isEmpty()) {
			script.setSource(values.get("scriptsource"));
			f.p("Script source saved OK!");
			String source=script.getSource();
			int version=script.getSourceVersion();
			// try compile
			try {
				ByteArrayInputStream bais = new ByteArrayInputStream(source.getBytes());
				GSParser parser = new GSParser(bais);
				parser.enable_tracing();
				GSStart gsscript = null;
				try {
					gsscript = parser.Start();
				} catch (Throwable e) { // catch throwable bad, but "lexical error" is an ERROR type... which we're not meant to catch.   but have to.  great.
					if (e instanceof ParseException) {
						ParseException pe = (ParseException) e;
						String tokenimage = "";
						tokenimage = "Last token: " + pe.currentToken.image + "<br>";
						f.p("<b>Parse failed:</b> " + e.toString() + "<br>" + tokenimage);
					} else {
						f.p("<b>Parse failed:</b> " + e.toString());
					}
					Audit.audit(true,st, Audit.OPERATOR.AVATAR,null,null,"Save",script.getName(),"","ParseFail","Saved script, with parse failures");
				}
				if (gsscript!=null) {
					GSCompiler compiler = new GSCompiler(gsscript);
					compiler.compile();
					script.setBytecode(compiler.toByteCode(), version);
					f.p("Script compiled and saved OK!");
					Audit.audit(true,st, Audit.OPERATOR.AVATAR,null,null,"Save",script.getName(),"","OK!","Saved script and compiled OK!");
				}
			} catch (NullPointerException ex) {
				throw new SystemException("Null pointer exception in compiler",ex);
			} catch (Throwable t) {
				f.p("Compilation failed; "+t.toString());
				Audit.audit(true,st, Audit.OPERATOR.AVATAR,null,null,"Save",script.getName(),"","CompileFail","Saved script, with compilation failures");
			}
		}
		f.add(new TextHeader("Edit script " + script.getName()));
		Table versions = new Table();
		versions.add("Source code version").add(script.getSourceVersion() + "");
		versions.openRow();
		if (script.getSourceVersion() != script.getByteCodeVersion()) {
			versions.add("<font color=red>Byte code version</font>").add("<font color=red>" + script.getByteCodeVersion() + "</font>").add("<font color=red>Compiled version is behind source version, please attempt to SAVE SOURCE</font>");
		} else {
			versions.add("Byte code version").add(script.getByteCodeVersion() + "");
		}
		f.add(versions);
		f.br();
		f.add("<textarea name=scriptsource rows=25 cols=80>" + script.getSource() + "</textarea>");
		f.br();
		f.add(new Button("Save Source", "Save Source"));
		f.br().br().add("<hr>").br();
		f.add("Debugging information: ");
		f.add(new Button("View Parse Tree", "View Parse Tree"));
		f.add(new Button("View Compiler Output", "View Compiler Output"));
		f.add(new Button("View Raw ByteCode", "View Raw ByteCode"));
		f.add(new Button("View Disassembly", "View Disassembly"));
		f.add(new Button("View Simulation", "View Simulation"));
		f.add(new Button("View Results", "View Results"));
		f.add(new Button("View ALL", "View ALL"));
		if (GPHUD.DEV) { f.add(new Button("DEBUG", "DEBUG")); }
		f.br();
		if (values.get("View Parse Tree").equals("View Parse Tree") || values.get("View ALL").equals("View ALL")) {
			f.add("<hr>").br().add(new TextSubHeader("Parse Tree Output"));
			f.add(debug(st,script.getSource(), STAGE.PARSER));
		}
		if (values.get("View Compiler Output").equals("View Compiler Output") || values.get("View ALL").equals("View ALL")) {
			f.add("<hr>").br().add(new TextSubHeader("Compiler Output"));
			f.add(debug(st,script.getSource(), STAGE.COMPILER));
		}
		if (values.get("View Raw ByteCode").equals("View Raw ByteCode") || values.get("View ALL").equals("View ALL")) {
			f.add("<hr>").br().add(new TextSubHeader("Raw ByteCode"));
			f.add(debug(st,script.getSource(), STAGE.BYTECODE));
		}
		if (values.get("View Disassembly").equals("View Disassembly") || values.get("View ALL").equals("View ALL")) {
			f.add("<hr>").br().add(new TextSubHeader("Disassembly"));
			f.add(debug(st,script.getSource(), STAGE.DISASSEMBLY));
		}
		if (values.get("View Simulation").equals("View Simulation") || values.get("View ALL").equals("View ALL")) {
			f.add("<hr>").br().add(new TextSubHeader("Simulation"));
			f.add(debug(st,script.getSource(), SIMULATION));
		}
		if (values.get("View Results").equals("View Results") || values.get("View ALL").equals("View ALL")) {
			f.add("<hr>").br().add(new TextSubHeader("Simulation"));
			f.add(debug(st,script.getSource(), STAGE.RESULTS));
		}
		if (GPHUD.DEV && values.get("DEBUG").equals("DEBUG")) { Scripts.test(); }
	}
	public enum STAGE {PARSER,COMPILER,BYTECODE,DISASSEMBLY,SIMULATION,RESULTS}

	@Nonnull
	private static String debug(@Nonnull State st, @Nonnull String script, STAGE stage) {
		ByteArrayInputStream bais = new ByteArrayInputStream(script.getBytes());
		GSParser parser = new GSParser(bais);
		parser.enable_tracing();
		GSStart gsscript = null;
		try {
			gsscript = parser.Start();
			if (stage==STAGE.PARSER) { return gsscript.toHtml(); }
		} catch (Throwable e) { // catch throwable bad, but "lexical error" is an ERROR type... which we're not meant to catch.   but have to.  great.
			if (e instanceof ParseException) {
				ParseException pe = (ParseException) e;
				String tokenimage = "";
				tokenimage = "Last token: " + pe.currentToken.image + "<br>";
				return "<b>Parse failed:</b> " + e.toString() + "<br>" + tokenimage;
			}
			return "<b>Parse failed:</b> " + e.toString();
		}

		GSCompiler compiler;
		if (gsscript != null) {
			try {
				compiler = new GSCompiler(gsscript);
				if (stage==STAGE.COMPILER) {
					List<ByteCode> bytecode = compiler.compile();
					StringBuilder code = new StringBuilder("<pre><table border=0>");
					for (ByteCode bc : bytecode) {
						code.append("<tr><td>").append(bc.node() != null ? bc.node().tokens() : "").append("</td><td>").append(bc.explain().replaceFirst(" \\(", "</td><td><i>(")).append("</i></td><td>");
						ArrayList<Byte> bcl = new ArrayList<>();
						bc.toByteCode(bcl);
						for (Byte b : bcl) {
							code.append(b).append(" ");
						}
						code.append("</td></tr>");
					}
					code.append("</table></pre>");
					return code.toString();
				}
			} catch (NullPointerException ex) {
				throw new SystemException("Null pointer exception in compiler",ex);
			} catch (Throwable e) { // catch throwable bad, but "lexical error" is an ERROR type... which we're not meant to catch.   but have to.  great.
				return "<b>Compilation failed:</b> " + e.toString();
			}

			Byte[] rawcode= compiler.toByteCode();
			if (stage==STAGE.BYTECODE) {
				StringBuilder bcstring = new StringBuilder("<pre><table border=0><tr>");
				for (int i = 0; i < rawcode.length; i++) {
					if ((i % 25) == 0) { bcstring.append("</tr><tr><th>").append(i).append("</th>"); }
					bcstring.append("<td>").append(rawcode[i]).append("</td>");
				}
				bcstring.append("</tr></table></pre>");
				return bcstring.toString();
			}

			GSVM gsvm=new GSVM(rawcode);
			if (stage==STAGE.DISASSEMBLY) {
				return gsvm.toHtml();
			}

			try {
				if (stage== SIMULATION || stage==RESULTS) {
					long start=System.currentTimeMillis();
					List<GSVM.ExecutionStep> steps = gsvm.simulate(st);
					long end=System.currentTimeMillis();
					StringBuilder output= new StringBuilder("<p><i>Run time: " + (end - start) + " ms</i></p><table border=1><td>IC</td><th>PC</th><th>OpCode</th><th>OpArgs</th><th>Stack</th><th>Variables</th></tr>");
					if (stage==SIMULATION) {
							// no more than 100 steps now
						int index=steps.size()-100;
						if (index<0) { index=0; }
						for (;index<steps.size();index++) {
							output.append(formatStep(steps.get(index)));
						}
					} else {
						if (steps.size()>1) { output.append(formatStep(steps.get(steps.size() - 2))); }
						if (steps.size()>0) { output.append(formatStep(steps.get(steps.size() - 1))); }
					}
					output.append("</table>");
					return output.toString();
				}

			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new SystemException("Array index out of bounds in compiler/simulator",ex);
			} catch (Throwable e) { // catch throwable bad, but "lexical error" is an ERROR type... which we're not meant to catch.   but have to.  great.
				return "<b>Simulation failed:</b> " + e.toString();
			}
		}
		return "Did nothing (?)";
	}

	@Nonnull
	private static String formatStep(@Nonnull GSVM.ExecutionStep step) {
		StringBuilder output = new StringBuilder();
		output.append("<tr><td>").append(step.IC).append("</td><th>").append(step.programcounter).append("</th><td>").append(step.decode).append("</td><td><table>");
		for (int i = 0; i < step.resultingstack.size(); i++) {
			output.append("<tr><th>").append(i).append("</th><td>").append(step.resultingstack.get(i).htmlDecode()).append("</td></tr>");
		}
		output.append("</table></td><td><table>");
		for (Map.Entry<String, ByteCodeDataType> entry : step.resultingvariables.entrySet()) {
			String decode = "???";
			if (entry.getValue() != null) {
				decode = entry.getValue().htmlDecode();
			}
			output.append("<tr><th>").append(entry.getKey()).append("</th><td>").append(decode).append("</td></tr>");
		}
		output.append("</table></td></tr>");
		if (step.t != null) {
			output.append("<tr><td colspan=100>").append(ExceptionTools.toHTML(step.t)).append("</td></tr>");
		}
		return output.toString();
	}
}
