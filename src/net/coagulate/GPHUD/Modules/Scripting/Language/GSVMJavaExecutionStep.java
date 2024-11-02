package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCodeDataType;

import java.util.HashMap;
import java.util.Map;

public class GSVMJavaExecutionStep extends GSVM.GSVMExecutionStep {
	
	private final Map<String,ByteCodeDataType> variables;
	private final String                       sourceline;
	
	public GSVMJavaExecutionStep(final String sourceline,final Map<String,ByteCodeDataType> variables) {
		this.sourceline=sourceline;
		this.variables=new HashMap<>();
		this.variables.putAll(variables);
	}
	
	@Override
	public String formatStep() {
		final StringBuilder s=new StringBuilder();
		s.append("<tr>");
		s.append("<td></td>");
		s.append("<td>").append(sourceline).append("</td><td></td><td></td><td></td><td>");
		
		s.append("<table>");
		for (final Map.Entry<String,ByteCodeDataType> entry: variables.entrySet()) {
			s.append("<tr><th>").append(entry.getKey()).append("</th>");
			s.append("<th>").append(entry.getValue().htmlDecode()).append("</th></tr>");
		}
		s.append("</table>");
		s.append("</td></tr>");
		return s.toString();
		
	}
}
