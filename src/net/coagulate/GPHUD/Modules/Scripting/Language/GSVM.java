package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Data.ScriptRun;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCodeDataType;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The abstract superclass of all compiled code runners
 * GSStackVM based languages (versions 1 and 2) are implemented by GSStackVM, which supports relative and absolute addressing
 * GSJavaVM is a native bytecode implementation and our role here is mostly to provide a supporting environment for the generated code
 */

public abstract class GSVM {
	
	final     Map<Char,JSONObject> queue=new HashMap<>();
	private final     Map<String,ByteCodeDataType> variables    =new TreeMap<>();
	private final     Map<String,ByteCodeDataType> introductions=new HashMap<>();
	protected int                  pid;
	/** Some stash of the invokers state */
	@Nullable private State                        invokerstate;
	/** Simulating behaviour ; dont update data, dont make gsAPI or HUD calls */
	private boolean SIMULATION=false;

	/** Create a GSVM of the appropriate type for the compiled version of this script */
	public static GSVM create(final Script init) {
		final int language=init.getByteCodeVersion();
		if (language==1||language==2) {
			return new GSStackVM(init);
		}
		if (language==3) {
			return new GSJavaVM(init);
		}
		throw new SystemImplementationException("Unrecognised language byte code version '"+language+"'");
	}
	
	public static GSVM create(final int vmVersion,final Byte[] rawcode) {
		if (vmVersion==2) { return new GSStackVM(rawcode); }
		throw new SystemImplementationException("Unknown virtual machine version "+vmVersion);
	}
	
	public static GSVM create(final int vmVersion,final ScriptRun run,final State  st) {
		if (vmVersion==2) { return new GSStackVM(run,st); }
		throw new SystemImplementationException("Unknown virtual machine version "+vmVersion);
	}
	
	/** Command the HUD should be prompted to invoke when the script ends */
	public abstract void invokeOnExit(final String apiCommand);
	
	/** Execute the script in this context */
	public abstract Response execute(final State st);
	
	/** Set when a VM exist due to a suspension (awaiting user IO etc) rather than fully completing the script */
	public abstract boolean suspended();
	
	/** Suspends the VMs state */
	public abstract void suspend(final State st,@Nonnull final Char respondant);
	
	/** Add a variable into the script */
	public void introduce(final String target,final ByteCodeDataType data) {
		introductions.put(target,data);
	}
	
	/** Returns the simulation flag. */
	public final boolean simulation() {
		return SIMULATION;
	}
	
	/** Set this VM as a simulation ; make no calls with side effects or player IO */
	public final void setSimulation() {
		SIMULATION=true;
	}
	
	public abstract String toHtml();
	
	public abstract void setReturn(final ByteCodeDataType bcdt);
	
	public abstract Response resume(final State st);
	
	/** Simulate an execution and return its execution steps */
	public abstract List<GSVMExecutionStep> simulate(@Nonnull final State st);

	public State getInvokerState() { return invokerstate; }

	/** Stash the state used by the person who originally invoked this script */
	protected void setInvokerState(final State st) {
		invokerstate=st;
	}
	
	public BCInteger getInteger(final String name,final boolean nullifNotFound) {
		try {
			return (BCInteger)getVariable(name,nullifNotFound);
		} catch (final ClassCastException e) {
			throw new GSCastException("Variable '"+name+"' was expected to be a BCInteger but was "+
			                          getVariable(name,nullifNotFound).getClass().getSimpleName());
		}
	}
	
	public ByteCodeDataType getVariable(final String name,final boolean nullIfNotFound) {
		final ByteCodeDataType bcdt=variables.get(name);
		if (nullIfNotFound) {
			return bcdt;
		}
		if (bcdt==null) {
			throw new GSUnknownIdentifier("Variable '"+name+"' is undefined.");
		}
		return bcdt;
	}
	
	public BCString getString(final String name,final boolean nullifNotFound) {
		try {
			return (BCString)getVariable(name,nullifNotFound);
		} catch (final ClassCastException e) {
			throw new GSCastException("Variable '"+name+"' was expected to be a BCString but was "+
			                          getVariable(name,nullifNotFound).getClass().getSimpleName());
		}
	}
	
	public void clearVariables() {
		variables.clear();
		variables.putAll(introductions);
	}
	
	public boolean existsVariable(final String name) {
		return getVariable(name,true)!=null;
	}
	
	public void putVariable(final String name,final ByteCodeDataType data) {
		variables.put(name,data);
	}
	
	
	// Actions the script might queue up
	
	public Map<String,ByteCodeDataType> variables() {
		return variables;
	}

	public void queueOwnerSay(final Char ch,final String message) {
		final JSONObject out=getQueue(ch);
		JSONResponse.ownerSay(out,message,ch.getProtocol());
	}
	
	private JSONObject getQueue(final Char c) {
		if (!queue.containsKey(c)) {
			queue.put(c,new JSONObject());
		}
		return queue.get(c);
	}
	
	public void queueSelectCharacter(final Char ch,final String description,final boolean allowManualSelection) {
		final JSONObject out=getQueue(ch);
		out.put("args",1);
		out.put("arg0name","response");
		out.put("arg0type","SENSORCHAR");
		if (allowManualSelection) {
			out.put("arg0manual","yes");
		}
		out.put("arg0description",description);
		out.put("incommand","runtemplate");
		out.put("invoke","Scripting.CharacterResponse");
	}
	
	public void queueGetText(final Char ch,final String description) {
		final JSONObject out=getQueue(ch);
		out.put("args",1);
		out.put("arg0name","response");
		out.put("arg0type","TEXTBOX");
		out.put("arg0description",description);
		out.put("incommand","runtemplate");
		out.put("invoke","Scripting.StringResponse");
	}
	
	public void queueSayAs(@Nonnull final Char ch,final String message) {
		final JSONObject out=getQueue(ch);
		JSONResponse.sayAs(out,ch.getName(),message,ch.getProtocol());
	}
	
	public void queueSay(@Nonnull final Char ch,final String message) {
		final JSONObject out=getQueue(ch);
		JSONResponse.sayAsHud(out,message,ch.getProtocol());
	}
	
	public void queueTeleport(final Char ch,final String hudRepresentation) {
		final JSONObject queue=getQueue(ch);
		queue.put("teleport",hudRepresentation);
	}
	
	public void queueGetChoice(final Char ch,final String description,@Nonnull final List<String> options) {
		final JSONObject out=getQueue(ch);
		out.put("args",1);
		out.put("arg0name","response");
		out.put("arg0type","SELECT");
		out.put("arg0description",description);
		for (int i=0;i<options.size();i++) {
			out.put("arg"+0+"button"+i,options.get(i));
		}
		out.put("incommand","runtemplate");
		out.put("invoke","Scripting.StringResponse");
	}
	
	@Nonnull
	public Response dequeue(final State st,final Char target) {
		final boolean debug=false;
		JSONObject totarget=new JSONObject();
		if (target!=null) {
			totarget=getQueue(target);
			if (pid!=0) {
				totarget.put("processid",String.valueOf(pid));
			}
			queue.remove(target);
		}
		for (final Char k: queue.keySet()) {
			final JSONObject totransmit=getQueue(k);
			if (pid!=0) {
				totransmit.put("processid",String.valueOf(pid));
			}
			new Transmission(k,totransmit).start();
		}
		return new JSONResponse(totarget);
	}
	
	/** Define a step in execution ; used most by the stack VM */
	public abstract static class GSVMExecutionStep {
		public abstract String formatStep();
	}
}
