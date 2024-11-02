package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCode;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.*;
import net.coagulate.GPHUD.State;
import org.apache.commons.lang3.ArrayUtils;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Compile GS code to java bytecode, hopefully safely :P
 *
 * This is an interesting little project ; ultimately I need to be able to pause execution of the compiled class and restore it
 * i.e. the way that scriptruns are archived and restored needs to work ; this was much easier with a custom StackVM implementation
 * where I could just serialise the stack and state and restore that.
 *
 * Now that we're compiling to native java code we either need
 * 1) A persistence - save/restore method for java threads that can be invoked on one
 * or
 * 2) To write the code in a way that supports suspending/resuming without the java VM co-operating
 *
 * While 1 seems kinda nice and perhaps a better solution, there's no obvious way to serialise/deserialise a Java thread
 * and while there may be some third party solutions, the solution to (2) is just a hybridisation of how StackVM works
 *
 * That is, to implement (2) we will largely remove and flatten constructs like if/while and so on, and reimplement them
 * as a series of "branch" commands, with the code simply split up into blocks that can be executed
 *
 * E.G.
 * The statement
 * if (condition) { truth; } else { nottruth; }
 * can be rewritten as
 * if (condition) { goto T; } else { goto F; }
 * T:
 * truth
 * goto E
 * F:
 * false
 * goto E
 * E:
 * rest of program
 *
 * With these blocks numbered we can reduce the program to a "flat" set of code that simply executes a series of blocks
 * and we can suspend/resume at these block markers only.  Specifically
 * a=5
 * a=a+gsHudGetANumber(CALLER)
 * gsSayAsHud(a)
 * becomes
 * a=5
 * gsHudGetANumber(CALLER) -> resume at R:
 * (suspend self)
 * R:
 * a=a+getResult()
 * gsSayAsHud(a)
 *
 * Then all these blocks can just be arranged in a stream of cases in a switch(runBlock) type command.
 */

public class GSJavaCompiler extends GSCompiler {
	private StringBuilder          sb         =new StringBuilder();
	private int                    indent     =0;
	private       int                    usedBlockId=0;
	private final Map<String,SourceCode> sourceCodes=new HashMap<String,SourceCode>();
	private       byte[]                 bytesOut;
	
	public GSJavaCompiler(final GSStart gsscript,final String name,final int sourceVersion) {
		super(gsscript,name,sourceVersion);
	}
	
	public String fullClassName() { return "net.coagulate.GPHUD.Modules.Scripting.Scripts."+getCompiledState().getInstance().getName().replaceAll("[^A-Za-z0-9]","")+"."+className(); }
	private String className() {
		return scriptname()+((sourceVersion()<0)?"Simulation":"")+Math.abs(sourceVersion());
	}
	public List<ByteCode> compile(final State st) {
		if (compiledState==null) { compiledState=st; }
		final String instancename=st.getInstance().getName().replaceAll("[^A-Za-z0-9]","");
		final String classname=fullClassName();
		System.out.println("Entering compiler:");
		System.out.println("Instance name: "+instancename);
		System.out.println("Class name: "+classname);
		final String sourceCode=createSource(st,instancename);
		final JavaCompiler javac=ToolProvider.getSystemJavaCompiler();
		final GSJavaVMDynamicClassLoader classLoader=GSJavaVMDynamicClassLoader.get();
		sourceCodes.put(classname,new SourceCode(classname,sourceCode));
		final Collection<SourceCode> compilationUnits=sourceCodes.values();
		final CompiledCode[] code;
		
		code=new CompiledCode[compilationUnits.size()];
		System.out.println("code is of length "+code.length);
		final Iterator<SourceCode> iter=compilationUnits.iterator();
		for (int i=0;i<code.length;i++) {
			try {
				code[i]=new CompiledCode(iter.next().getClassName());
			} catch (final URISyntaxException e) {
				throw new GSInternalError("Odd URI error",e);
			}
		}
		final DiagnosticCollector<JavaFileObject> collector=new DiagnosticCollector<>();
		final ExtendedStandardJavaFileManager fileManager=
				new ExtendedStandardJavaFileManager(javac.getStandardFileManager(null,null,null),classLoader);
		final JavaCompiler.CompilationTask task=javac.getTask(null,fileManager,collector,null,null,compilationUnits);
		final boolean result=task.call();
		if (!result||collector.getDiagnostics().size()>0) {
			final StringBuffer exceptionMsg=new StringBuffer();
			exceptionMsg.append("Unable to compile the source");
			boolean hasWarnings=false;
			boolean hasErrors=false;
			for (final Diagnostic<? extends JavaFileObject> d: collector.getDiagnostics()) {
				switch (d.getKind()) {
					case NOTE:
					case MANDATORY_WARNING:
					case WARNING:
						hasWarnings=true;
						break;
					case OTHER:
					case ERROR:
					default:
						hasErrors=true;
						break;
				}
				exceptionMsg.append("<br>\n").append("[kind=").append(d.getKind());
				exceptionMsg.append(", ")
				            .append("line=")
				            .append(d.getLineNumber())
				            .append(":")
				            .append(d.getColumnNumber());
				exceptionMsg.append(", ")
				            .append("message=")
				            .append(d.getMessage(Locale.US))
				            .append("/")
				            .append(d.getCode())
				            .append("]");
			}
			if (hasErrors) {
				throw new GSInternalError(exceptionMsg.toString());
			}
		}
		
		final Map<String,Class<?>> classes=new HashMap<String,Class<?>>();
		for (final String className: sourceCodes.keySet()) {
			try {
				classes.put(className,classLoader.loadClass(className));
			} catch (final ClassNotFoundException|ClassCastException e) {
				throw new GSInternalError("Weird, didn't find our own class",e);
			}
		}
		try {
			this.bytesOut=classLoader.getClassBytes(classname);
		} catch (final ClassNotFoundException e) {
			throw new GSInternalError("Compilation target location error",e);
		}
		/*
		try {
			classes.get(classname).getMethod("execute",new Class[]{GSVM.class}).invoke(null,new Object[]{null});
		} catch (IllegalAccessException e) {
			throw new GSInternalError("Incorrect access on execute method",e);
		} catch (InvocationTargetException e) {
			throw new GSInternalError("Exception inside compiled script",e.getCause());
		} catch (NoSuchMethodException e) {
			throw new GSInternalError("No execute method?",e);
		}*/
		return null;
	}
	
	protected List<ByteCode> _compile(final State st,final ParseNode node) {
		if (expectedChildren(node)>-1&&node.jjtGetNumChildren()!=expectedChildren(node)) {
			throw new SystemImplementationException(
					node.getClass().getSimpleName()+" had "+node.jjtGetNumChildren()+" children, expected "+
					expectedChildren(node)+" at line "+node.jjtGetFirstToken().beginLine+", column "+
					node.jjtGetFirstToken().beginColumn);
		}
		
		
		if (node instanceof GSStart||node instanceof GSExpression||node instanceof GSParameter||node instanceof GSTerm||
		    node instanceof GSStatement) { // expression
			// just breaks down into 1 of X executable subtypes
			// Start expands to a list of Statement (types)
			for (int i=0;i<node.jjtGetNumChildren();i++) {
				if (node instanceof GSStatement) {
					final String line=node.tokens().replaceAll("\"","\\\"");
					sb.append("if(debug){step(\"ENTER : ").append(line).append("\");}");
					newline();
				}
				_compile(st,node.child(i));
				if (node instanceof GSStatement) {
					final String line=node.tokens().replaceAll("\"","\\\"");
					sb.append("if(debug){step(\"EXIT  : ").append(line).append("\");}");
					newline();
				}
			}
			return null;
		}
		
		if (node instanceof GSInitialiser) {
			// 3 children node, type, name, value
			checkType(node,0,GSTypeSpecifier.class);
			checkType(node,1,GSIdentifier.class);
			checkType(node,2,GSExpression.class);
			final boolean typed=false;
			final String type=node.child(0).tokens();
			// INITIALISE the variable - reverse place name and null content.  Then we just implement "set variable".
			sb.append("vm.putVariable(\"");
			sb.append(node.child(1).tokens());  // identifier
			sb.append("\"");
			sb.append(",");
			_compile(st,node.child(2));
			sb.append(");");
			return null;
		}
		if (node instanceof GSAssignment) { // similar to end of GSInitialiser code
			checkType(node,0,GSIdentifierOrList.class);
			checkType(node,1,GSExpression.class);
			final ParseNode target=node.child(0).child(0);
			// this may be a NORMAL VARIABLE (GSIdentifier) or a SPECIFIC LIST ELEMENT (GSIdentifierWithIndex)
			if (target.getClass().equals(GSIdentifier.class)) {
				// assign/varname/value
				sb.append("vm.putVariable(\"");
				sb.append(node.child(0).tokens()); // identifier
				sb.append("\",");
				_compile(st,node.child(1));
				sb.append(");");
				return null;
			}
			if (target.getClass().equals(GSIdentifierWithIndex.class)) {
				//assignelement/varname/elementno/value
				sb.append("((BCList)getVariable(\"");
				sb.append(target.child(0).tokens());
				sb.append("\",false))");
				sb.append(".getContent().set(");
				sb.append(Integer.parseInt(target.child(1).tokens()));
				sb.append(",");
				sb.append(node.child(1).tokens());
				sb.append(";");
				return null;
			}
			throw new SystemImplementationException(
					"Compiler error: Unknown type of Assignment: "+target.getClass().getName());
		}
		
		if (node instanceof GSStringConstant) {
			// engage paranoia
			String string=node.tokens();
			string=string.substring(1,string.length()-1);
			final byte[] bytes=string.getBytes(StandardCharsets.UTF_8);
			sb.append("(");
			sb.append("new String(new byte[]{");
			boolean first=true;
			for (final byte b: bytes) {
				if (first) {
					first=false;
				} else {
					sb.append(", ");
				}
				sb.append("(byte)");
				sb.append(Byte.toUnsignedInt(b));
			}
			sb.append("})");
			return null;
		}
		
		if (node instanceof GSIntegerConstant) {
			sb.append("(new BCInteger(null,").append(node.tokens().trim()).append("))");
			return null;
		}
		
		if (node instanceof GSFloatConstant) {
			sb.append(node.tokens());
			return null;
		}
		
		if (node instanceof GSConditional) {
			// conditional branch
			checkType(node,0,GSExpression.class);
			checkType(node,1,GSStatement.class);
			// evaluate the condition, branch if zero, otherwise run the statement
			
			usedBlockId++;
			final int truthblock=usedBlockId;
			usedBlockId++;
			final int falseblock=usedBlockId;
			usedBlockId++;
			final int postblock=usedBlockId;
			
			sb.append("if (");
			_compile(st,node.child(0));
			sb.append(") { runstep=").append(truthblock).append("; } else { runstep=").append(falseblock).append("; }");
			newline();
			
			block(truthblock);
			_compile(st,node.child(1));
			runstep(postblock);
			
			block(falseblock);
			if (node.children()==3) {
				_compile(st,node.child(2));
			}
			
			block(postblock,false);
			return null;
		}
		
		if (node instanceof GSWhileLoop) {
			checkType(node,0,GSExpression.class);
			checkType(node,1,GSStatement.class);
			
			usedBlockId++;
			final int condition=usedBlockId;
			usedBlockId++;
			final int runcode=usedBlockId;
			usedBlockId++;
			final int endpoint=usedBlockId;
			
			block(condition,false);
			sb.append("if (");
			_compile(st,node.child(0));
			sb.append(") { runstep=").append(runcode).append("; } else { runstep=").append(endpoint).append("; }");
			newline();
			
			block(runcode,true);
			_compile(st,node.child(1));
			runstep(condition);
			
			block(endpoint,true);
			return null;
		}
		
		if (node instanceof GSFunctionCall) {/*
			// lots of random glue lives in here, but actually the function call at this level is easy enough, it has a name and some parameters
			checkType(node,0,GSFunctionName.class);
			if (node.jjtGetNumChildren()>1) {
				checkType(node,1,GSParameters.class);
			}
			// validate the function name
			final String functionname=node.child(0).tokens();
			if (priviledgedFunction(functionname)&&!st.hasPermission("Scripting.CompilePrivileged")) {
				throw new UserInputStateException("You can not call function "+functionname+
				                                  " due to not having the permission Scripting.CompilePrivileged");
			}
			// dump the paramters, in reverse order, (which starts with the paramter count), and finally the name and the invoking bytecode
			if (node.jjtGetNumChildren()>1) {
				compiled.addAll(compile(st,node.child(1)));
			} // assuming it has parameters
			else {
				compiled.add(new BCInteger(node,0));
			} // else zero parameters
			compiled.add(new BCString(node,(node.child(0).tokens())));
			addDebug(compiled,node);
			compiled.add(new BCInvoke(node));
			return compiled;*/
			// TODO :P
		}
		
		if (node instanceof GSParameters) {
			/*for (int i=node.jjtGetNumChildren()-1;i>=0;i--) {
				compiled.addAll(compile(st,node.child(i)));
			}
			compiled.add(new BCInteger(node,node.children()));
			return compiled;*/ // TODO
		}
		
		if (node instanceof GSLogicalOr) {
			op(st,node,"||",true);
			return null;
		}
		
		if (node instanceof GSLogicalAnd) {
			op(st,node,"&&",true);
			return null;
		}
		
		if (node instanceof GSInEquality) {
			op(st,node,"!=",false);
			return null;
		}
		
		if (node instanceof GSEquality) {
			funcop(st,node,"strictlyEquals",false);
			return null;
		}
		
		if (node instanceof GSLessThan) {
			op(st,node,"<",false);
			return null;
		}
		
		if (node instanceof GSGreaterThan) {
			op(st,node,">",false);
			return null;
		}
		
		if (node instanceof GSLessOrEqualThan) {
			op(st,node,"<=",false);
			return null;
		}
		
		if (node instanceof GSGreaterOrEqualThan) {
			op(st,node,">=",false);
			return null;
		}
		
		if (node instanceof GSAdd) {
			funcop(st,node,"add",false);
			return null;
		}
		
		if (node instanceof GSSubtract) {
			op(st,node,"-",false);
			return null;
		}
		
		if (node instanceof GSMultiply) {
			op(st,node,"*",false);
			return null;
		}
		
		if (node instanceof GSDivide) {
			op(st,node,"/",false);
			return null;
		}
		
		if (node instanceof GSLogicalNot) {
			sb.append("!(");
			_compile(st,node.child(0));
			sb.append(")");
			return null;
		}
		
		if (node instanceof GSUnaryMinus) {
			sb.append("(-");
			_compile(st,node.child(0));
			sb.append(")");
			return null;
		}
		
		if (node instanceof GSIdentifier) {
			sb.append("vm.getVariable(\"").append(node.tokens()).append("\",false)");
			return null;
		}
		
		if (node instanceof GSList) {
			/*
			// pop the list, in reverse order, then short the size, and then the command.
			for (int i=node.children()-1;i>=0;i--) {
				compiled.addAll(compile(st,node.child(i)));
			}
			compiled.add(new BCList(node,node.children()));
			return compiled;*/ // TODO
		}
		
		if (node instanceof GSListIndex) { // a list index in an evaluatable position
			checkType(node,0,GSIdentifier.class);
			checkType(node,1,GSExpression.class);
			// pop name, pop index
			sb.append("((GSList)vm.getVariable(\"").append(node.child(0).tokens()).append("\",false)).get(");
			_compile(st,node.child(1));
			sb.append(")");
			return null;
		}
		
		if (node instanceof GSReturn) { // a return statement with an optional value, otherwise we insert int 0
			/*
			if (node.children()>1) {
				throw new GSInternalError("Compilation error, 0 or 1 children expected");
			}
			addDebug(compiled,node);
			if (node.children()==1) {
				compiled.addAll(compile(st,node.child(0)));
			} else {
				compiled.add(new BCInteger(node,0));
			}
			compiled.add(new BCReturn(node));
			return; */ // todo
		}
		
		if (node instanceof GSDiscardExpression) { // a top level expression where the result is not assigned and should be binned from the stack
			if (node.children()!=1) {
				throw new GSInternalError("Compilation error, 1 children expected");
			}
			_compile(st,node.child(0));
			return null;
		}
		
		throw new SystemImplementationException(
				"Compilation not implemented for node type '"+node.getClass().getSimpleName()+"'");
	}
	
	@Override
	public Byte[] toByteCode(final State st) {
		compile(st);
		return ArrayUtils.toObject(bytesOut);
	}
	
	@Override
	public int version() {
		return 3;
	}
	
	private String createSource(final State st,final String instancename) {
		sb=new StringBuilder();
		indent=0;
		
		usedBlockId=0;
		sb.append("package net.coagulate.GPHUD.Modules.Scripting.Scripts.").append(instancename).append(";");
		newline();
		sb.append("import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;");
		newline();
		sb.append("import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.*;");
		newline();
		sb.append("import net.coagulate.GPHUD.State;");
		newline();
		sb.append("import net.coagulate.GPHUD.Modules.Scripting.Language.*;");
		newline();
		sb.append("import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;");
		newline();
		sb.append("import net.coagulate.GPHUD.Interfaces.Responses.Response;");
		newline();
		sb.append("import java.util.List;");
		newline();
		sb.append("public class ").append(className()).append(" extends GSJavaVMClass {");
		indent++;
		newline();
		sb.append("public ").append(className()).append("(GSVM vm,List<GSVMJavaExecutionStep> debug) { super(vm,debug); }");
		newline();
		sb.append("public Response execute() {");
		indent++;
		newline();
		sb.append("int runstep=0;");
		newline();
		sb.append("while (runstep>=0) {");
		indent++;
		newline();
		sb.append("if (debug) { step(\"Entering runstep \"+runstep); }");
		newline();
		sb.append("switch (runstep) {");
		indent++;
		newline();
		startBlock(0);
		_compile(st,startnode());
		newline();
		sb.append("runstep=-1;");
		newline();
		endBlock();
		sb.append("default: throw new GSInternalError(\"Could not find block \"+runstep+\" to execute\");");
		newline();
		indent--;
		newline();
		sb.append("}");
		newline();
		indent--;
		newline();
		sb.append("}");
		newline();
		sb.append("if(debug){step(\"SCRIPT EXIT\");}");
		sb.append("System.out.println(\"Execution completed\");");
		newline();
		sb.append("return new OKResponse(\"Script completed ok\");");
		newline();
		indent--;
		newline();
		sb.append("}");
		newline();
		indent--;
		newline();
		sb.append("}");
		newline();
		System.out.println("---------- SOURCE CODE GENERATED ----------");
		System.out.println(sb);
		System.out.println("-------------------------------------------");
		return sb.toString();
	}
	
	private void block(final int id) {
		endBlock();
		startBlock(id);
	}
	
	private void block(final int id,final boolean breaks) {
		endBlock(breaks);
		startBlock(id);
	}
	
	private void startBlock(final int id) {
		sb.append("case ").append(id).append(":");
		indent++;
		newline();
		//sb.append("System.out.println(\"Class \"+this.getClass().getName()+\" entering runstep "+id+"\\n\");");
	}
	
	private void endBlock() {
		endBlock(true);
	}
	
	private void endBlock(final boolean br) {
		if (br) {
			sb.append("break;");
		}
		indent--;
		newline();
	}
	
	private void op(final State st,final ParseNode node,final String op,final boolean linebreak) {
		_compile(st,node.child(0));
		for (int i=1;i<node.children();i++) {
			sb.append(" ").append(op);
			if (linebreak) {
				newline();
			} else {
				sb.append(" ");
			}
			_compile(st,node.child(i));
		}
		
	}
	
	private void funcop(final State st,final ParseNode node,final String op,final boolean linebreak) {
		_compile(st,node.child(0));
		for (int i=1;i<node.children();i++) {
			sb.append(".").append(op).append("(");
			_compile(st,node.child(i));
			sb.append(")");
			if (linebreak) {
				newline();
			} else {
				sb.append(" ");
			}
		}
		
	}
	
	private void runstep(final int postblock) {
		sb.append("runstep=").append(postblock).append(";");
		newline();
	}
	
	private void newline() {
		sb.append("\n");
		for (int i=0;i<indent;i++) {
			sb.append("\t");
		}
	}
	
	public class CompiledCode extends SimpleJavaFileObject {
		byte[] tba=null;
		private final ByteArrayOutputStream baos=new ByteArrayOutputStream();
		private final String                className;
		
		public CompiledCode(final String className) throws URISyntaxException {
			super(new URI(className),Kind.CLASS);
			this.className=className;
		}
		
		public String getClassName() {
			return className;
		}
		
		@Override
		public OutputStream openOutputStream() throws IOException {
			return baos;
		}

		public byte[] getByteCode() {
			if (tba==null) { tba=baos.toByteArray(); }
			return tba;
		}
	}
	
	public class SourceCode extends SimpleJavaFileObject {
		private       String contents=null;
		private final String className;
		
		public SourceCode(final String className,final String contents) {
			super(URI.create("string:///"+className.replace('.','/')+Kind.SOURCE.extension),Kind.SOURCE);
			this.contents=contents;
			this.className=className;
		}
		
		public String getClassName() {
			return className;
		}
		
		public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws IOException {
			return contents;
		}
	}
	
	public class ExtendedStandardJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
		
		private final List<CompiledCode>         compiledCode=new ArrayList<CompiledCode>();
		private final GSJavaVMDynamicClassLoader cl;
		
		/**
		 * Creates a new instance of ForwardingJavaFileManager.
		 *
		 * @param fileManager delegate to this file manager
		 * @param cl
		 */
		protected ExtendedStandardJavaFileManager(final JavaFileManager fileManager,final GSJavaVMDynamicClassLoader cl) {
			super(fileManager);
			this.cl=cl;
		}
		
		@Override
		public ClassLoader getClassLoader(final JavaFileManager.Location location) {
			return cl;
		}
		
		@Override
		public JavaFileObject getJavaFileForOutput(final JavaFileManager.Location location,
		                                           final String className,
		                                           final JavaFileObject.Kind kind,
		                                           final FileObject sibling) throws IOException {
			
			try {
				final CompiledCode innerClass=new CompiledCode(className);
				compiledCode.add(innerClass);
				cl.addCode(innerClass);
				return innerClass;
			} catch (final Exception e) {
				throw new RuntimeException("Error while creating in-memory output file for "+className,e);
			}
		}
	}
}
