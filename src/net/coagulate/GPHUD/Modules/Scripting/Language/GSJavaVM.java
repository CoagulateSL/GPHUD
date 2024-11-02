package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Data.ScriptRun;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCodeDataType;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/** A wrapper for the execution of scripts compiled as java code */

public class GSJavaVM extends GSVM {
	GSJavaCompiler compiler;
	List<GSVMJavaExecutionStep> steps=new ArrayList<>();
	public GSJavaVM(final Script init) {
	  throw new SystemImplementationException("GSJavaVM(Script) not implemented");
	}
	public GSJavaVM(final ScriptRun run,final State st) {
		throw new SystemImplementationException("GSJavaVM(ScriptRun,State) not implemented");
	}
	
	public GSJavaVM(@Nonnull final State st,@Nonnull final GSCompiler compiler) {
		this.compiler=(GSJavaCompiler)compiler;
		compiler.compile(st);
		//if (compiler.compiledState==null) { compiler.compiledState=st; }

	}
	
	@Override
	public void invokeOnExit(final String apiCommand) {
		throw new SystemImplementationException("invokeOnExit not implemented in GSJavaVM");
	}

	@Override
	public Response execute(final State st) { return execute(st,null); }
	
	public Response execute(final State st,final List<GSVMJavaExecutionStep> debug) {
		try {
			final Class c=GSJavaVMDynamicClassLoader.get().loadClass(compiler.fullClassName());
			final Object o=c.getDeclaredConstructor(GSVM.class,List.class).newInstance(this,debug);
			return ((Response)c.getMethod("execute").invoke(o));
		} catch (final ClassNotFoundException|
		               NoSuchMethodException|
		               InstantiationException|
		               IllegalAccessException e) {
			throw new GSInternalError("Java VM messed up",e);
		} catch (final InvocationTargetException e) {
			throw new GSInternalError("Java compiled script messed up",e.getCause());
		}
	}
	
	@Override
	public boolean suspended() {
		throw new SystemImplementationException("suspended not implemented in GSJavaVM");
	}
	
	@Override
	public void suspend(final State st,@Nonnull final Char respondant) {
		throw new SystemImplementationException("suspend not implemented in GSJavaVM");
	}
	
	@Override
	public String toHtml() {
		return "<B>Disassembly not supported on JVM</b>";
	}
	
	@Override
	public void setReturn(final ByteCodeDataType bcdt) {
		throw new SystemImplementationException("setReturn not implemented in GSJavaVM");
	}

	@Override
	public Response resume(final State st) {
		throw new SystemImplementationException("resume not implemented in GSJavaVM");
	}

	@Override
	public List<? extends GSVMExecutionStep> simulate(@Nonnull final State st) {
		setSimulation();
		execute(st,steps);
		return steps;
	}
}
