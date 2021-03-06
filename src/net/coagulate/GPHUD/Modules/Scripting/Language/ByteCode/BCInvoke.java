package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidFunctionCall;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
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
		final Method function=GSFunctions.get(functionname);
		final int argcount=vm.popInteger().getContent();
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
			throw new GSInternalError("Method access to "+function.getName()+" in "+function.getDeclaringClass()+" is not permitted.  Check access qualifier is public.");
		}
		catch (@Nonnull final InvocationTargetException e) {
			final Throwable t=e.getCause();
			if (t!=null) {
				if (UserException.class.isAssignableFrom(t.getClass())) { throw ((UserException) t); }
				if (SystemException.class.isAssignableFrom(t.getClass())) { throw ((SystemException) t); }
				if (RuntimeException.class.isAssignableFrom(t.getClass())) {
					throw new GSInvalidFunctionCall("Function "+function.getName()+" errored: "+t,t);
				}
				throw new GSInternalError("Unhandled exception in GPHUDScript invoke bytecode calling "+function.getName()+" in "+function.getDeclaringClass().getSimpleName(),
				                          t
				);
			}
			throw new GSInternalError("No cause to invocation target exception from "+function.getDeclaringClass().getSimpleName()+"/"+function.getName(),e);
		}
		final ByteCodeDataType ret=(ByteCodeDataType) rawret;
		ret.stack(vm);
	}
}
