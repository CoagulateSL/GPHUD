package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.System.SystemExecutionException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserExecutionException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A command, probably derived from Annotations.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class CommandAnnotation extends Command {
	Module owner;
	Commands meta;
	Method method;
	List<Argument> arguments;
	private boolean generated=true;

	protected CommandAnnotation() {}

	public CommandAnnotation(final Module owner,
	                         @Nonnull final Method c) {
		this.owner=owner;
		method=c;
		meta=c.getAnnotation(Commands.class);
		validate(null);
		populateArguments();
		generated=false;
	}

	// ----- Internal Statics -----
	protected static void checkPublicStatic(@Nonnull final Method m) {
		if (!Modifier.isStatic(m.getModifiers())) {
			throw new SystemImplementationException("Method "+m.getDeclaringClass().getName()+"/"+m.getName()+" must be static");
		}
		if (!Modifier.isPublic(m.getModifiers())) {
			throw new SystemImplementationException("Method "+m.getDeclaringClass().getName()+"/"+m.getName()+" must be public");
		}
	}

	// ---------- INSTANCE ----------
	@Nonnull
	public Method getMethod() { return method; }

	public boolean isGenerated() { return generated; }

	@Nonnull
	public String description() { return meta.description(); }

	@Override
	public String notes() { return meta.notes(); }

	@Nonnull
	public String requiresPermission() { return meta.requiresPermission(); }

	@Nonnull
	public Context context() { return meta.context(); }

	public boolean permitHUD() { return meta.permitJSON(); }

	public boolean permitObject() { return meta.permitObject(); }

	public boolean permitConsole() { return meta.permitConsole(); }

	public boolean permitWeb() { return meta.permitUserWeb(); }

	public boolean permitScripting() { return meta.permitScripting(); }

	public boolean permitExternal() { return meta.permitExternal(); }

	@Nonnull
	public List<Argument> getArguments() { return arguments; }

	public int getArgumentCount() { return getArguments().size(); }

	@Nonnull
	public String getFullName() { return owner.getName()+"."+getName(); }

	@Nonnull
	public String getName() { return method.getName(); }
	// ----- Internal Instance -----

	@Nonnull
	public List<Argument> getInvokingArguments() { return getArguments(); }

	@Nonnull
	public String getFullMethodName() {
		return method.getDeclaringClass().getName()+"."+method.getName()+"()";
	}

	public int getInvokingArgumentCount() {
		return getArgumentCount();
	}

	@Override
	protected Response execute(final State state,
	                           final Map<String,Object> arguments) {
		try {
			state.parameterDebug =arguments;
			final List<Object> parameters=new ArrayList<>();
			parameters.add(state);
			for (final Argument arg: getArguments()) {
				parameters.add(arguments.getOrDefault(arg.name(),null));
			}
			state.parameterDebugFinal =parameters;
			final Object result=getMethod().invoke(this,parameters.toArray());
			return (Response) (result);
		}
		catch (final IllegalArgumentException e) {
			throw new SystemImplementationException("An illegal argument leaked to "+getFullName()+" at "+getMethod().getClass().getName()+"."+getMethod().getName(),e);
		}
		catch (final IllegalAccessException e) {
			throw new SystemImplementationException("Access to target method "+getMethod().getName()+" in "+getMethod().getDeclaringClass().getSimpleName()+" denied by JVM",e);
		}
		catch (final InvocationTargetException e) {
			final Throwable content=e.getCause();
			if (content==null) { throw new SystemImplementationException("Null invocation target exception cause",e); }
			if (UserException.class.isAssignableFrom(content.getClass())) {
				throw new UserExecutionException(content.getLocalizedMessage(),content);
			}
			throw new SystemExecutionException("Annotated command "+getFullName()+" from "+getMethod().getName()+" in class "+getMethod().getDeclaringClass().getSimpleName()+" exceptioned: "+content.getLocalizedMessage(),content);
		}
	}
	void validate(final State st) {
		if (!requiresPermission().isEmpty()) {
			Modules.validatePermission(st,requiresPermission());
		}
		Module.checkPublicStatic(method);
		if (method.getParameterCount()==0) {
			throw new SystemImplementationException("Method "+getFullName()+"() takes zero arguments but must take 'State' as its first argument");
		}
		//noinspection ConstantConditions
		if ((method.getParameters()[0]).getClass().getCanonicalName().equalsIgnoreCase(State.class.getCanonicalName())) {
			throw new SystemImplementationException("Method "+getFullName()+" must take State as its first argument");
		}
		for (int i=1;i<method.getParameters().length;i++) {
			final Parameter p=method.getParameters()[i];
			final Arguments arg=p.getAnnotation(Arguments.class);
			if (arg==null) {
				throw new SystemImplementationException("Method "+getFullName()+"() argument "+(i+1)+" ("+p.getName()+") has no @Argument metadata");
			}
			if (arg.type()==ArgumentType.CHOICE) {
				// validate the choice method
				final String choicemethod=arg.choiceMethod();
				try {
					ArgumentAnnotation.getMethod(choicemethod);
				}
				catch (@Nonnull final Exception e) {
					throw new SystemImplementationException("Failed to instansiate choice method "+getFullName()+" / "+choicemethod,e);
				}
			}
		}

	}

	private void populateArguments() {
		arguments=new ArrayList<>();
		boolean skipfirst=true; // first should be STATE
		for (final Parameter p: method.getParameters()) {
			if (skipfirst) { skipfirst=false; }
			else { arguments.add(new ArgumentAnnotation(this,p)); }
		}
	}


}
