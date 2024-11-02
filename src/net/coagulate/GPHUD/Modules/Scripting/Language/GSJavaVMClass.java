package net.coagulate.GPHUD.Modules.Scripting.Language;

import java.util.List;

public abstract class GSJavaVMClass {
	protected final boolean                     debug;
	protected final List<GSVMJavaExecutionStep> steps;
	protected final GSVM                        vm;
	
	protected GSJavaVMClass(final GSVM vm,final List<GSVMJavaExecutionStep> debug) {
		this.vm=vm;
		if (debug==null) {
			this.debug=false;
			this.steps=null;
		} else {
			this.debug=true;
			this.steps=debug;
		}
	}
	
	protected void step(final String description) {
		steps.add(new GSVMJavaExecutionStep(description,vm.getVariables()));
	}
	
	protected void step(final ParseNode node) {
		steps.add(new GSVMJavaExecutionStep(node.tokens(),vm.getVariables()));
	}
	
}
