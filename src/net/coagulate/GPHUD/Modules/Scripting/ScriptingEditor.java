package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCodeDataType;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSCompiler;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.GSParser;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.GSStart;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.ParseException;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

public class ScriptingEditor {
	@URL.URLs(url="/configuration/scripting/edit/*", requiresPermission="Scripting.Create")
	public static void editScript(@Nonnull final State st,@Nonnull final SafeMap values) {
		try {
			final Form f=st.form();
			final String[] split=st.getDebasedURL().split("/");
			final String id=split[split.length-1];
			final Script script=Script.get(Integer.parseInt(id));
			script.validate(st);
			if (!values.get("scriptalias").isEmpty()) {
				if (!script.alias().equals(values.get("scriptalias"))) {
					script.alias(values.get("scriptalias"));
				}
			}
			String compilerVersion=values.get("Compiler");
			if (!values.get("scriptsource").isEmpty()) {
				if (script.setSource(values.get("scriptsource"))) {
					f.p("Script source saved OK!");
				}
			}
			final String source=script.getSource();
			final int version=script.getSourceVersion();
			
			// try for catching everything that might go wrong
			String parserOutput=null;
			String compilerOutput=null;
			float parseTime=-1;
			float compileTime=-1;
			GSParser parser=null;
			GSCompiler compiler=null;
			boolean parsedOk=false;
			boolean compiledOk=false;
			try {
				// setup the parser
				parserOutput=null;
				final ByteArrayInputStream bais=new ByteArrayInputStream(source.getBytes());
				parser=new GSParser(bais);
				parser.enable_tracing();
				GSStart gsscript=null;
				// run the parser
				try {
					final long parseStart=System.nanoTime();
					gsscript=parser.Start();
					final long parseEnd=System.nanoTime();
					parseTime=(parseEnd-parseStart)/1000000.0f;
					f.p("Parser finished in "+parseTime+"ms");
					parserOutput=gsscript.toHtml();
					parsedOk=true;
				} catch (@Nonnull final Throwable e) {
					// catch throwable bad, but "lexical error" is an ERROR type... which we're not meant to catch.   but have to.  great.
					if (e instanceof final ParseException pe) {
						final String tokenimage;
						tokenimage="Last token: "+pe.currentToken.image+"<br>";
						f.p("<b>Parse failed:</b> "+e+"<br>"+tokenimage);
					} else {
						f.p("<b>Parse failed:</b> "+e);
					}
					Audit.audit(true,
					            st,
					            Audit.OPERATOR.AVATAR,
					            null,
					            null,
					            "Save",
					            script.getName(),
					            "",
					            "ParseFail",
					            "Saved script, with parse failures");
				}
				
				// if the parser passed, then compile
				if (gsscript!=null) {
					compiler=GSCompiler.create(compilerVersion,gsscript,script);
					final long compileStart=System.nanoTime();
					compiler.compile(st);
					final long compileEnd=System.nanoTime();
					compileTime=(compileEnd-compileStart)/1000000.0f;
					f.p("Compiler finished in "+compileTime+"ms");
					script.setBytecode(compiler.toByteCode(st),version,compiler.version());
					f.p("Script compiled and saved OK!");
					Audit.audit(true,
					            st,
					            Audit.OPERATOR.AVATAR,
					            null,
					            null,
					            "Save",
					            script.getName(),
					            "",
					            "OK!",
					            "Saved script and compiled OK!");
					compiledOk=true;
				}
			} catch (@Nonnull final NullPointerException ex) {
				throw new GSInternalError("Null pointer exception in compiler",ex);
			} catch (@Nonnull final Throwable t) {
				f.p("Compilation failed; "+t);
				Audit.audit(true,
				            st,
				            Audit.OPERATOR.AVATAR,
				            null,
				            null,
				            "Save",
				            script.getName(),
				            "",
				            "CompileFail",
				            "Saved script, with compilation failures");
			}
			if (compiler!=null) {
				compilerOutput=compiler.diagnosticOutput(st);
			}
			f.add(new TextHeader("Edit script "+script.getName()));
			final Table versions=new Table();
			versions.add("Source code version").add(String.valueOf(script.getSourceVersion()));
			versions.openRow();
			if (script.getSourceVersion()==script.getByteCodeVersion()) {
				versions.add("Byte code version").add(String.valueOf(script.getByteCodeVersion()));
			} else {
				versions.add("<font color=red>Byte code version</font>")
				        .add("<font color=red>"+script.getByteCodeVersion()+"</font>")
				        .add("<font color=red>Compiled version is behind source version, please attempt to SAVE SOURCE</font>");
			}
			versions.openRow().add("Script alias").add(new TextInput("scriptalias",script.alias()));
			f.add(versions);
			f.br();
			f.add("<textarea name=scriptsource rows=25 cols=80>"+script.getSource()+"</textarea>");
			f.br();
			if (compilerVersion==null||compilerVersion.isBlank()) {
				compilerVersion="V2-GSStackVM/Relative";
			}
			f.add(new DropDownList("Compiler").add("V2-GSStackVM/Relative")
			                                  .add("V3-GSJavaVM")
			                                  .setValue(compilerVersion));
			f.add(new Button("Save Source","Save Source"));
			f.br().br().add("<hr>").br();
			f.add("Debugging information: ");
			f.add(new Button("View Simulation","View Simulation"));
			f.add(new Button("View Results","View Results"));
			//if (GPHUD.DEV) { f.add(new Button("DEBUG", "DEBUG")); }
			f.br();
			if (parserOutput!=null) {
				f.add("<hr>")
				 .br()
				 .add("<a onClick='parserOutput.hidden=0;'>View parser output"+(parseTime>0?" ("+parseTime+"ms)":"")+
				      "</a><br>");
				f.add("<div id=parserOutput name=parserOutput hidden=1>").add(parserOutput).add("</div><br>");
			}
			if (compilerOutput!=null) {
				f.add("<hr>")
				 .br()
				 .add("<a onClick='compilerOutput.hidden=0;'>View compiler output"+
				      (compileTime>0?" ("+compileTime+"ms)":" (Compilation failed)")+"</a><br>");
				f.add("<div id=compilerOutput name=compilerOutput hidden=1>").add(compilerOutput).add("</div><br>");
			}
			if (compiledOk) {
				f.add("<hr>").br().add("<a onClick='byteCodeOutput.hidden=0;'>View bytecode</a><br>");
				f.add("<div id=byteCodeOutput name=byteCodeOutput hidden=1>")
				 .add(formatByteCode(compiler,st))
				 .add("</div><br>");
			}
			if (compiledOk) {
				f.add("<hr>").br().add("<a onClick='disassemblyOutput.hidden=0;'>View disassembly</a><br>");
				f.add("<div id=disassemblyOutput name=disassemblyOutput hidden=1>")
				 .add(GSVM.create(st,compiler).toHtml())
				 .add("</div><br>");
			}
			if ("View Simulation".equals(values.get("View Simulation"))||"View ALL".equals(values.get("View ALL"))) {
				f.add("<hr>").br().add(new TextSubHeader("Simulation"));
				f.add(debug(st,compilerVersion,script,true));
			}
			if ("View Results".equals(values.get("View Results"))||"View ALL".equals(values.get("View ALL"))) {
				f.add("<hr>").br().add(new TextSubHeader("Simulation"));
				f.add(debug(st,compilerVersion,script,false));
			}
			//if (GPHUD.DEV && values.get("DEBUG").equals("DEBUG")) { Scripts.test(); }
		} catch (final NoDataException e) {
			throw new UserInputLookupFailureException("Script no longer exists",e,true);
		}
	}
	
	private static String formatByteCode(final GSCompiler compiler,final State st) {
		final Byte[] rawcode=compiler.toByteCode(st);
		final StringBuilder bcstring=new StringBuilder("<pre><table border=0><tr>");
		for (int i=0;i<rawcode.length;i++) {
			if ((i%25)==0) {
				bcstring.append("</tr><tr><th>").append(i).append("</th>");
			}
			bcstring.append("<td>").append(rawcode[i]).append("</td>");
		}
		bcstring.append("</tr></table></pre>");
		return bcstring.toString();
	}
	
	// ----- Internal Statics -----
	@Nonnull
	private static String debug(@Nonnull final State st,
	                            @Nonnull final String compilerVersion,
	                            @Nonnull final Script script,
	                            final boolean fullDebug) {
		
		String error="";
		long start=0;
		GSVM gsvm=null;
		try {
			gsvm=GSVM.create(script);
			start=System.currentTimeMillis();
			if (fullDebug) {
				gsvm.simulate(st);
			} else {
				gsvm.execute(st);
			}
			// no more than 100 steps now
		} catch (@Nonnull final ArrayIndexOutOfBoundsException ex) {
			throw new GSInternalError("Array index out of bounds in compiler/simulator",ex);
		} catch (@Nonnull final UserException e) {
			error="<b>Simulation failed:</b> "+e;
		}
		final long end=System.currentTimeMillis();
		List<? extends GSVM.GSVMExecutionStep> steps=null;
		final StringBuilder output=new StringBuilder();
		if (gsvm!=null) { steps=gsvm.getSteps(); }
		output.append("<p><i>Run time: ")
		      .append(end-start)
		      .append(" ms</i></p>");
		if (steps!=null && !steps.isEmpty()) {
			output.append("<table border=1><td>IC</td><th>PC</th><th>OpCode</th><th>OpArgs</th")
			      .append("><th>Stack</th><th>Variables</th></tr>");
			int index=steps.size()-100;
			if (index<0) {
				index=0;
			}
			for (;index<steps.size();index++) {
				output.append(steps.get(index).formatStep());
			}
			output.append("</table>");
		}
		
		output.append("<b>Final State</b><br><table><tr><th>Variable</th><th>Value</th></tr>");
		for (final Map.Entry<String,ByteCodeDataType> variable:gsvm.getVariables().entrySet()) {
			output.append("<tr><th>").append(variable.getKey()).append("</th>");
			output.append("<td>").append(variable.getValue().htmlDecode()).append("</td></tr>");
		}
		output.append("</table>");
		return error+"<br>"+output;
		
		//return error;
	}
	
}
