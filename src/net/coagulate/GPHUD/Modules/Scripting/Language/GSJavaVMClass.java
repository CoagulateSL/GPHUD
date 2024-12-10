package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCodeDataType;
import net.coagulate.GPHUD.State;

import java.util.List;

public abstract class GSJavaVMClass {
	protected final boolean                     debug;
	protected final List<GSVMJavaExecutionStep> steps;
	protected final GSVM                        vm;
	protected final State                       state;
	protected int    locality  =-1;
	
	protected void step(final String description) {
		steps.add(new GSVMJavaExecutionStep(description,vm.getVariables()));
	}
	
	protected void step(final ParseNode node) {
		steps.add(new GSVMJavaExecutionStep(node.tokens(),vm.getVariables()));
	}
	//// locality sets up these
	protected String sourceLine="UNKNOWN SOURCE";
	protected int    startRow  =-1;
	protected int    endRow    =-1;
	protected int    startCol  =-1;
	protected int    endCol    =-1;
	protected GSJavaVMClass(final GSVM vm,final State st,final List<GSVMJavaExecutionStep> debug) {
		this.vm=vm;
		this.state=st;
		if (debug==null) {
			this.debug=false;
			this.steps=null;
		} else {
			this.debug=true;
			this.steps=debug;
		}
	}

	protected void step() {
		step(locality,"");
	}

	protected void step(final int id,final String prefix) {
		getLocal(id);
		steps.add(new GSVMJavaExecutionStep(
				prefix+sourceLine+"     "+"(#"+id+" @ "+startRow+((startRow!=endRow)?"-"+endRow:"")+":"+startCol+
				((startCol!=endCol)?"-"+endCol:""),vm.getVariables()));
		clearLocal();
	}

	protected abstract void getLocal(final int locality);

	protected void clearLocal() {
		sourceLine="UNKNOWN SOURCE";
		startRow=-1;
		endRow=-1;
		startCol=-1;
		endCol=-1;
	}
	
	protected void stepIn(final int id) {
		step(locality,"ENTER: ");
	}
	
	protected void stepOut(final int id) {
		step(locality,"EXIT : ");
	}
	
	protected ByteCodeDataType pop() {
		return vm.pop();
	}
	
	protected void push(final ByteCodeDataType bcdt) {
		vm.stack.push(bcdt);
	}

	public Response execute() {
		try {return _execute();}
		catch (final Exception e) {
			throw new SystemImplementationException(e.getLocalizedMessage()+" at "+localityString(locality),e);
		}
	}
	
	protected abstract Response _execute();

	public String localityString(final int locality) {
		getLocal(locality);
		return GSJavaCompiler.base64decode(sourceLine)+" ["+startRow+((startRow!=endRow)?"-"+endRow:"")+":"+startCol+
		       ((startCol!=endCol)?"-"+endCol:"")+"]";
	}
}
