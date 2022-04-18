package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions;
import net.coagulate.GPHUD.Modules.Scripting.Language.*;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class BCInvoke extends ByteCode {
	public BCInvoke(final ParseNode n) {
		super(n);
	}

	// ---------- INSTANCE ----------
	// Invoke a function.  Pop name, arg count, N*arguments
	@Nonnull
	public String explain() { return "Invoke (pop function name, pop arg count, pop arguments, push result)"; }

	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Invoke.get());
	}

	@Override
	public void execute(final State st,
	                    @Nonnull final GSVM vm,
	                    final boolean simulation) {
		final String functionname=vm.popString().getContent();
		final Method function=GSFunctions.getNullable(functionname);
		final int argcount=vm.popInteger().getContent();
		if (function==null) {
			if (argcount!=0) { throw new GSInvalidFunctionCall("Calls to other scripts do not currently take parameters"); }
			executeCall(st,vm,simulation,functionname);
			return;
		}
		final ByteCodeDataType[] args=new ByteCodeDataType[argcount];
		for (int i=0;i<argcount;i++) { args[i]=vm.pop(); }
		// MAGIC GOES HERE
		// check the arguments, two forms are allowed, both have State and GSVM as the initial arguments
		final Class<?>[] parameters=function.getParameterTypes();
		if (parameters.length<2) {
			throw new GSInternalError("Function call "+functionname+" does not have at least 2 arguments");
		}
		if (!parameters[0].equals(State.class)) {
			throw new GSInternalError("First parameter to function "+functionname+" must be the State");
		}
		if (!parameters[1].equals(GSVM.class)) {
			throw new GSInternalError("Second parameter to function "+functionname+" must be the GSVM");
		}
		// any arguments?
		if (argcount!=0 || parameters.length!=2) {
			if (parameters.length==3 && parameters[2].equals(ByteCodeDataType[].class)) {
				// yes and the 3rd parameter catches them all (varargs style)
				final Object[] pass={st,vm,args};
				invoke(st,vm,function,pass);
				return;
			}
			// otherwise we have multiple arguments at both ends, does it match?
			if (argcount!=(parameters.length-2)) {
				throw new GSInvalidFunctionCall("Call to "+functionname+" has incorrect number of arguments - it requires "+(parameters.length-2)+" and was supplied "+argcount,true);
			}
			// okay, number of arguments matches, what about the individual types
			final Object[] pass=new Object[parameters.length];
			pass[0]=st;
			pass[1]=vm;
			for (int i=0;i<argcount;i++) {
				if (!parameters[i+2].equals(args[i].getClass())) {
					throw new GSInvalidFunctionCall("Call to "+functionname+", parameter "+i+" is expected to be of type "+parameters[i+2].getSimpleName()+" but was "+"supplied"+" "+args[i]
							.getClass()
							.getSimpleName(),true);
				}
				pass[i+2]=args[i];
			}
			invoke(st,vm,function,pass);
			return;
		}
		// no arguments
		invoke(st,vm,function,new Object[]{st,vm});
	}

	// ----- Internal Instance -----
	private void invoke(final State st,
	                    @Nonnull final GSVM vm,
	                    @Nonnull final Method function,
	                    final Object[] parameters) {
		final Object rawret;
		try {
			rawret=function.invoke(null,parameters);
		}
		catch (@Nonnull final IllegalAccessException e) {
			throw new GSInternalError("Method access to " + function.getName() + " in " + function.getDeclaringClass() + " is not permitted.  Check access qualifier is public.", e);
		}
		catch (@Nonnull final InvocationTargetException e) {
			final Throwable t=e.getCause();
			if (t!=null) {
				if (UserException.class.isAssignableFrom(t.getClass())) { //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
					throw ((UserException) t);
				}
				if (SystemException.class.isAssignableFrom(t.getClass())) { //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
					throw ((SystemException) t);
				}
				if (RuntimeException.class.isAssignableFrom(t.getClass())) {
					//noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
					throw new GSInvalidFunctionCall("Function " + function.getName() + " errored: " + t, t);
				}
				//noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
				throw new GSInternalError("Unhandled exception in GPHUDScript invoke bytecode calling " + function.getName() + " in " + function.getDeclaringClass().getSimpleName(),
						t
				);
			}
			throw new GSInternalError("No cause to invocation target exception from "+function.getDeclaringClass().getSimpleName()+"/"+function.getName(),e);
		}
		final ByteCodeDataType ret=(ByteCodeDataType) rawret;
		ret.stack(vm);
	}

	private void executeCall(final State st,
							 @Nonnull final GSVM vm,
							 final boolean simulation,
							 @Nonnull final String scriptname) {
		// well then.  We need the function in memory, set up the return function and jump to it, also dealing with debug info and stuff.  ehh
		// 1) Load function
		if (vm.get(" CODEBASE "+scriptname)==null) { // function is not loaded
			final Script script = Script.findNullable(st, scriptname);
			if (script == null) {
				throw new GSUnknownIdentifier("Can not find function or script '" + scriptname + "'", true);
			}
			if (script.getCompilerVersion() < 1) {
				throw new GSInvalidFunctionCall("Script " + scriptname + " must be recompiled (it was compiled by a compiler that used absolute addressing in branch instructions which inhibits relocation / inclusion).");
			}
			vm.set(" CODEBASE " + scriptname, new BCInteger(null, vm.bytecode.length)); // stash the start place
			// merge the new script onto the end of the existing bytecode.
			final byte[] append = script.getByteCode();
			final byte[] merge = new byte[vm.bytecode.length + append.length];
			System.arraycopy(vm.bytecode, 0, merge, 0, vm.bytecode.length);
			System.arraycopy(append, 0, merge, vm.bytecode.length, append.length);
			vm.bytecode = merge;
		}
		final int targetPC = vm.get(" CODEBASE " + scriptname).toBCInteger().getContent();
		// 2) Set up return stack
		vm.stack.push(new BCInteger(null, vm.programCounter)); // stash the return PC
		vm.stack.push(new BCInteger(null, vm.getCanary())); // stash the canary.
		// 3) Call function
		vm.programCounter = targetPC;
		// 4) Restore debug - can't do this here, we lose control in (3) - compiler must reassert this :)
	}
}
