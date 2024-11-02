package net.coagulate.GPHUD.Modules.Scripting.Language;

import java.util.HashMap;
import java.util.Map;

public class GSJavaVMDynamicClassLoader extends ClassLoader {
	
	private static final Map<String,GSJavaCompiler.CompiledCode> customCompiledCode=new HashMap<>();
	private static final Map<String,Class>          classes =new HashMap<>();
	private static final GSJavaVMDynamicClassLoader instance=
			new GSJavaVMDynamicClassLoader(ClassLoader.getSystemClassLoader());
	
	private GSJavaVMDynamicClassLoader(final ClassLoader parent) {
		super(parent);
	}
	
	public static GSJavaVMDynamicClassLoader get()
	{
		return instance;
	}
	
	public void addCode(final GSJavaCompiler.CompiledCode cc) {
		customCompiledCode.put(cc.getName(),cc);
	}

	public boolean defined(final String name) { return classes.containsKey(name); }
	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException {
		if (!classes.containsKey(name)) {
			final GSJavaCompiler.CompiledCode cc=customCompiledCode.get(name);
			if (cc==null) {
				return super.findClass(name);
			}
			final byte[] byteCode=cc.getByteCode();
			classes.put(name,defineClass(name,byteCode,0,byteCode.length));
		}
		return classes.get(name);
	}
	
	public byte[] getClassBytes(final String name) throws ClassNotFoundException {
		final GSJavaCompiler.CompiledCode cc=customCompiledCode.get(name);
		if (cc==null) {
			return null;
		}
		final byte[] byteCode=cc.getByteCode();
		return byteCode;
	}
}
