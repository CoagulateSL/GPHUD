package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.Core.Tools.ExceptionTools;
import net.coagulate.GPHUD.Data.Scripts;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.Paragraph;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCode;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSCompiler;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.GSParser;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.GSStart;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.ParseException;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class ScriptingConfig {
	@URL.URLs(url = "/configuration/scripting")
	public static void configPage(State st, SafeMap values) {
		Form f = st.form;
		f.add(new TextHeader("Scripting Module"));
		f.add(new Paragraph("List of scripts"));
		f.add(Scripts.getTable(st.getInstance()));
		f.noForm();
		f.add(new Form(st, true, "/GPHUD/configuration/scripting/create", "Create new script"));
	}

	@URL.URLs(url = "/configuration/scripting/create", requiresPermission = "Scripting.Create")
	public static void createScript(State st, SafeMap values) {
		Form f = st.form;
		if (!values.get("scriptname").isEmpty()) {
			Scripts.create(st, values.get("scriptname"));
			throw new RedirectionException("/GPHUD/configuration/scripting");
		}
		f.add(new TextHeader("Create new script"));
		f.add("Name of script:").add(new TextInput("scriptname")).br();
		f.add(new Button("Create"));
	}

	@URL.URLs(url = "/configuration/scripting/edit/*", requiresPermission = "Scripting.Create")
	public static void editScript(State st, SafeMap values) {
		Form f = st.form;
		String split[] = st.getDebasedURL().split("/");
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
				}
				if (gsscript!=null) {
					GSCompiler compiler = new GSCompiler(gsscript);
					compiler.compile();
					script.setBytecode(compiler.toByteCode(), version);
					f.p("Script compiled and saved OK!");
				}
			} catch (Throwable t) {
				f.p("Compilation failed; "+t.toString());
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
		f.add(new Button("View ALL", "View ALL"));
		f.br();
		if (values.get("View Parse Tree").equals("View Parse Tree") || values.get("View ALL").equals("View ALL")) {
			f.add("<hr>").br().add(new TextSubHeader("Parse Tree Output"));
			f.add(debug(script.getSource(), STAGE.PARSER));
		}
		if (values.get("View Compiler Output").equals("View Compiler Output") || values.get("View ALL").equals("View ALL")) {
			f.add("<hr>").br().add(new TextSubHeader("Compiler Output"));
			f.add(debug(script.getSource(), STAGE.COMPILER));
		}
		if (values.get("View Raw ByteCode").equals("View Raw ByteCode") || values.get("View ALL").equals("View ALL")) {
			f.add("<hr>").br().add(new TextSubHeader("Raw ByteCode"));
			f.add(debug(script.getSource(), STAGE.BYTECODE));
		}
		if (values.get("View Disassembly").equals("View Disassembly") || values.get("View ALL").equals("View ALL")) {
			f.add("<hr>").br().add(new TextSubHeader("Disassembly"));
			f.add(debug(script.getSource(), STAGE.DISASSEMBLY));
		}
		if (values.get("View Simulation").equals("View Simulation") || values.get("View ALL").equals("View ALL")) {
			f.add("<hr>").br().add(new TextSubHeader("Simulation"));
			f.add(debug(script.getSource(), STAGE.SIMULATION));
		}
	}
	public enum STAGE {PARSER,COMPILER,BYTECODE,DISASSEMBLY,SIMULATION};
	private static String debug(String script, STAGE stage) {
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
					String code = "<pre><table border=0>";
					for (ByteCode bc : bytecode) {
						code += "<tr><td>"+(bc.node()!=null?bc.node().tokens():"")+"</td><td>" + bc.explain().replaceFirst(" \\(", "</td><td><i>(") + "</i></td><td>";
						ArrayList<Byte> bcl = new ArrayList<>();
						bc.toByteCode(bcl);
						for (Byte b : bcl) {
							code += b + " ";
						}
						code += "</td></tr>";
					}
					code += "</table></pre>";
					return code;
				}
			} catch (Throwable e) { // catch throwable bad, but "lexical error" is an ERROR type... which we're not meant to catch.   but have to.  great.
				return "<b>Compilation failed:</b> " + e.toString();
			}

			Byte[] rawcode= compiler.toByteCode();
			if (stage==STAGE.BYTECODE) {
				String bcstring = "<pre><table border=0><tr>";
				for (int i = 0; i < rawcode.length; i++) {
					if ((i % 25) == 0) { bcstring += "</tr><tr><th>" + i + "</th>"; }
					bcstring += "<td>" + rawcode[i] + "</td>";
				}
				bcstring += "</tr></table></pre>";
				return bcstring;
			}

			GSVM gsvm=new GSVM(rawcode);
			if (stage==STAGE.DISASSEMBLY) {
				return gsvm.toHtml();
			}

			try {
				if (stage==STAGE.SIMULATION) {
					List<GSVM.ExecutionStep> steps = gsvm.simulate();
					String output="<table border=1><th>PC</th><th>OpCode</th><th>OpArgs</th><th>Stack</th><th>Variables</th></tr>";
					for (GSVM.ExecutionStep step:steps) {
						output+="<tr><th>"+step.programcounter+"</th><td>"+step.decode+"</td><td><table>";
						for (int i=0;i<step.resultingstack.size();i++) {
							output+="<tr><th>"+i+"</th><td>"+
									step.resultingstack.get(i).htmlDecode()+"</td></tr>";
						}
						output+="</table></td><td><table>";
						for (String k:step.resultingvariables.keySet()) {
							String decode="???";
							if (step.resultingvariables.get(k)!=null) {
								decode = step.resultingvariables.get(k).htmlDecode();
							}
							output+="<tr><th>"+k+"</th><td>"+
									decode+"</td></tr>";
						}
						output+="</table></td></tr>";
						if (step.t!=null) { output+="<tr><td colspan=100>"+
								ExceptionTools.toHTML(step.t)+"</td></tr>"; }
					}
					output+="</table>";
					return output;
				}

			} catch (Throwable e) { // catch throwable bad, but "lexical error" is an ERROR type... which we're not meant to catch.   but have to.  great.
				return "<b>Simulation failed:</b> " + e.toString();
			}
		}
		return "Did nothing (?)";
	}
}
