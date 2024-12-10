package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Data.ScriptRun;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCAvatar;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCodeDataType;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/** A wrapper for the execution of scripts compiled as java code */

public class GSJavaVM extends GSVM {
	GSJavaCompiler              compiler;
	final Class c;
	
	public GSJavaVM(final Script script) {
		try {
			c=GSJavaVMDynamicClassLoader.get().findClass(fullClassName(script.getInstance(),script));
		} catch (final ClassNotFoundException e) {
			throw new SystemImplementationException("Failed to loadclass "+fullClassName(script.getInstance(),script),e);
		}
	}

	//TODO probably refactor out this stuff
	private String fullClassName(final Instance i,final Script s) {
		return "net.coagulate.GPHUD.Modules.Scripting.Scripts."+i.getName().replaceAll("[^A-Za-z0-9]","")+"."+className(s);
	}
	
	private String className(final Script s) {
		return s.getName()+((s.getByteCodeVersion()<0)?"Simulation":"")+Math.abs(s.getByteCodeVersion());
	}
	
	public GSJavaVM(final ScriptRun run,final State st) {
		c=null;
		throw new SystemImplementationException("GSJavaVM(ScriptRun,State) not implemented");
	}
	
	public GSJavaVM(@Nonnull final State st,@Nonnull final GSCompiler compiler) {
		this.compiler=(GSJavaCompiler)compiler;
		try {
			c=GSJavaVMDynamicClassLoader.get().loadClass(((GSJavaCompiler)compiler).fullClassName());
		} catch (final ClassNotFoundException e) {
			throw new SystemImplementationException("Failed to load class from compiler "+((GSJavaCompiler)compiler).fullClassName(),e);
		}
		compiler.compile(st);
		//if (compiler.compiledState==null) { compiler.compiledState=st; }
		
	}
	
	@Override
	public void invokeOnExit(final String apiCommand) {
		throw new SystemImplementationException("invokeOnExit not implemented in GSJavaVM");
	}
	
	@Override
	public Response execute(final State st) {
		return execute(st,null);
	}
	
	@Override
	public void _simulate(@Nonnull final State st,@Nonnull final List<GSVMExecutionStep> steps) {
		setSimulation();
		execute(st,steps);
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
	
	public Response execute(final State st,final List<GSVMExecutionStep> debug) {
		try {
			clearVariables();
			variables().putAll(introductions());
			if (!existsVariable("CALLER")) {
				putVariable("CALLER",new BCCharacter(null,st.getCharacter()));
			}
			if (!existsVariable("AVATAR")) {
				putVariable("AVATAR",new BCAvatar(null,st.getAvatarNullable()));
			}
			final Object o=c.getDeclaredConstructor(GSVM.class,State.class,List.class).newInstance(this,st,debug);
			return ((Response)c.getMethod("execute").invoke(o));
		} catch (final NoSuchMethodException|InstantiationException|IllegalAccessException e) {
			throw new GSInternalError("Java VM messed up",e);
		} catch (final InvocationTargetException e) {
			if (UserException.class.isAssignableFrom(e.getCause().getClass())) {
				throw (UserException)(e.getCause());
			}
			throw new GSInternalError("Java compiled script messed up",e.getCause());
		}
	}
}
