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
 *
 *
 *
 * All the above was written before i deconstructed expressions into stacks again, cos I need to be able to suspend the
 * vm if someone calls something like
 * String reply=gsGetText(CALLER,"Enter a first name)+" "+gsGetText(CALLER,"Enter a last name");
 * here we need to be able to suspend the VM after one of the getText calls and resume the VM later in the middle of
 * evaluating this expression.  If I just convert the expression on one line we have the same problem of suspend/resume
 * needing a checkpoint ; i.e. a runstep case break in the current build.
 *
 * So now maths is done with a stack just like the other VM.
 */

public class GSJavaCompiler extends GSCompiler {
	private       StringBuilder          sb         =new StringBuilder();
	private       int                    indent     =0;
	private       int                    usedBlockId=0;
	private final Map<String,SourceCode> sourceCodes=new HashMap<String,SourceCode>();
	private       byte[]                 bytesOut;
	private final Map<String,String> dataTypes=new HashMap<>();
	Map<String,Integer> localisers=new HashMap<>();
	private       String                 sourceCode =null;
	private GSException   storedError=null;
	
	public GSJavaCompiler(final GSStart gsscript,final String name,final int sourceVersion) {
		super(gsscript,name,sourceVersion);
	}
	private final StringBuilder localiser=new StringBuilder();
	
	private String className() {
		return scriptname()+((sourceVersion()<0)?"Simulation":"")+Math.abs(sourceVersion());
	}
	private       int localiserid=0;
	private final int discardid  =1;

	protected static String base64encode(final String plaintext) {
		return Base64.getEncoder().encodeToString(plaintext.getBytes());
	}

	protected static String base64decode(final String base64) {
		return new String(Base64.getDecoder().decode(base64));
	}

	private void addDebug() {
		sb.append("if(debug){step();}");
		newline();
	}

	private void addDebugEnter(final int id) {
		sb.append("if(debug){stepIn("+id+");}");
		newline();
	}
	
	public String fullClassName() {
		return "net.coagulate.GPHUD.Modules.Scripting.Scripts."+
		       getCompiledState().getInstance().getName().replaceAll("[^A-Za-z0-9]","")+"."+className();
	}
	
	private void addDebugExit(final int id) {
		sb.append("if(debug){stepOut("+id+");}");
		newline();
	}
	
	private void funcOp(final String funcname,final State st,final ParseNode node) {
		if (node.children()==1) {
			_compile(st,node.child(0));
			return;
		}
		for(int i=node.children()-1;i>=0;i--) {
			_compile(st,node.child(i));
			newline();
		}
		for(int i=node.children()-1;i>0;i--) {
			sb.append("push(pop()."+funcname+"(pop()));");
			newline();
		}
	}
	
	private String createSource(final State st,final String instancename) {
		sb=new StringBuilder();
		indent=0;
		
		usedBlockId=0;
		sb.append("package net.coagulate.GPHUD.Modules.Scripting.Scripts.").append(instancename).append(";");
		newline();
		sb.append("""
				          import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;
				          import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.*;
				          import net.coagulate.GPHUD.State;
				          import net.coagulate.GPHUD.Modules.Scripting.Language.*;
				          import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
				          import net.coagulate.GPHUD.Interfaces.Responses.Response;
				          import java.util.List;
				          import java.util.Base64;
				          public class\s""")
		  .append(className())
		  .append("""
				           extends GSJavaVMClass {
				          \tpublic\s""")
		  .append(className())
		  .append("""
				          (GSVM vm,State st,List<GSVMJavaExecutionStep> debug) { super(vm,st,debug); }
				                  // ----- BEGIN GENERATED CODE -----
				          """);
		indent=2;
		newline();
		startBlock(0);
		_compile(st,startnode());
		newline();
		sb.append("runstep=-1;");
		endBlock();
		newline();
		sb.append("""
				     \n\tprivate int runstep=0;
				     \tprotected Response _execute() {
				     \t\twhile (runstep>=0) {
				     \t\t\tswitch (runstep) {
				     """);
		for (int i=0;i<=usedBlockId;i++) {
			sb.append("\t\t\t\tcase "+i+": runStep"+i+"(); break;");
			newline();
		}
		sb.append("""
				     \t\t\t\tdefault: throw new GSInternalError("Runstep "+runstep+" unknown");
				     \t\t\t}
				     \t\t}
				     \t\treturn new OKResponse("Script completed ok");
				     \t}""");
		appendLocaliser();
		sb.append("}\n");
		return sb.toString();
	}
	
	private void startBlock(final int id) {
		sb.append("private void runStep").append(id).append("() {");
		indent++;
		newline();
	}
	
	@Override
	public int version() {
		return 3;
	}
	
	private void endBlock() {
		sb.append("}");
		indent--;
		newline();
	}
	
	private void block(final int id) {
		endBlock();
		startBlock(id);
	}
	
	private int localise(final ParseNode node) {
		final String encode=base64encode(node.tokens());
		if (localisers.containsKey(encode)) { return localisers.get(encode); }
		localiserid++;
		sb.append("locality="+localiserid+";"); newline();
		localiser.append("\t\t\tcase ").append(localiserid).append(": ");
		localiser.append("sourceLine=\"").append(base64encode(node.tokens())).append("\"; ");
		localiser.append("startRow=").append(node.jjtGetFirstToken().beginLine).append("; ");
		localiser.append("endRow=").append(node.jjtGetLastToken().endLine).append("; ");
		localiser.append("startCol=").append(node.jjtGetFirstToken().beginColumn).append("; ");
		localiser.append("endCol=").append(node.jjtGetLastToken().endColumn).append("; ");
		localiser.append("break;\n");
		localisers.put(encode,localiserid);
		return localiserid;
	}
	
	private void appendLocaliser() {
		sb.append("  protected void getLocal(final int locality) {\n");
		sb.append("    switch (locality) {\n");
		sb.append(localiser);
		sb.append("    }\n");
		sb.append("  }\n");
	}

	@Override
	public String diagnosticOutput(final State st) {
		if (sourceCode==null) {
			return "";
		}
		final String[] lines=sourceCode.split("\n");
		final StringBuilder s=new StringBuilder();
		s.append("<pre>");
		int lineno=1;
		for (final String line: lines) {
			s.append(lineno).append(": ").append(line).append("\n");
			lineno++;
		}
		s.append("</pre>");
		return s.toString();
	}

	public void compile(final State st) {
		if (compiledState==null) {
			compiledState=st;
		}
		final String instancename=st.getInstance().getName().replaceAll("[^A-Za-z0-9]","");
		final String classname=fullClassName();
		sourceCode=createSource(st,instancename);
		if (storedError!=null) {
			throw storedError;
		}
		final JavaCompiler javac=ToolProvider.getSystemJavaCompiler();
		final GSJavaVMDynamicClassLoader classLoader=GSJavaVMDynamicClassLoader.get();
		sourceCodes.put(classname,new SourceCode(classname,sourceCode));
		final Collection<SourceCode> compilationUnits=sourceCodes.values();
		final CompiledCode[] code;
		
		code=new CompiledCode[compilationUnits.size()];
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
			final StringBuilder exceptionMsg=new StringBuilder();
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
				final int locid=localise(node);
				if (!(node instanceof GSStart)) {
					addDebugEnter(locid);
					sb.append("// "+node.tokens());
					newline();
				}
				_compile(st,node.child(i));
				if (!(node instanceof GSStart)) {
					addDebugExit(locid);
				}
				/*TODO PROBABLY TRASH THIS
				   if (node instanceof GSStatement) {
					usedBlockId++;
					runstep(usedBlockId);
					block(usedBlockId);
				}*/
			}
			return null;
		}
		
		if (node instanceof GSInitialiser) {
			// 3 children node, type, name, value
			checkType(node,0,GSTypeSpecifier.class);
			checkType(node,1,GSIdentifier.class);
			checkType(node,2,GSExpression.class);
			boolean typed=false;
			final String type=node.child(0).tokens();
			if ("String".equals(type)) {
				dataTypes.put(node.child(1).tokens(),"toBCString");
				typed=true;
			}
			if ("Response".equals(type)) {
				dataTypes.put(node.child(1).tokens(),"toBCResponse");
				typed=true;
			}
			if ("Integer".equals(type)) {
				dataTypes.put(node.child(1).tokens(),"toBCInteger");
				typed=true;
			}
			if ("Float".equals(type)) {
				dataTypes.put(node.child(1).tokens(),"toBCFloat");
				typed=true;
			}
			if ("Avatar".equals(type)) {
				dataTypes.put(node.child(1).tokens(),"toBCAvatar");
				typed=true;
			}
			if ("Group".equals(type)) {
				dataTypes.put(node.child(1).tokens(),"toBCGroup");
				typed=true;
			}
			if ("Character".equals(type)) {
				dataTypes.put(node.child(1).tokens(),"toBCCharacter");
				typed=true;
			}
			if ("List".equals(type)) {
				dataTypes.put(node.child(1).tokens(),"toBCList");
				typed=true;
			}
			if (!typed) {
				throw new SystemImplementationException(
						"Unable to initialise variable of type "+type+" (not implemented)");
			}
			// INITIALISE the variable - stuff the value on the stack and pop it
			_compile(st,node.child(2));
			sb.append("vm.putVariable(\"");
			sb.append(node.child(1).tokens());  // identifier
			sb.append("\"");
			sb.append(",");
			sb.append("pop());");
			newline();
			return null;
		}
		if (node instanceof GSAssignment) { // similar to end of GSInitialiser code
			checkType(node,0,GSIdentifierOrList.class);
			checkType(node,1,GSExpression.class);
			final ParseNode target=node.child(0).child(0);
			// this may be a NORMAL VARIABLE (GSIdentifier) or a SPECIFIC LIST ELEMENT (GSIdentifierWithIndex)
			if (target.getClass().equals(GSIdentifier.class)) {
				final String type=dataTypes.get(node.child(0).tokens());
				if (type==null&&storedError==null) {
					storedError=
							new GSInvalidExpressionException("Variable "+node.child(0).tokens()+" is not initialised");
				}
				// assign/varname/value
				_compile(st,node.child(1));
				sb.append("vm.putVariable(\"");
				sb.append(node.child(0).tokens()); // identifier
				sb.append("\",pop()");
				if (type!=null) {
					sb.append(".").append(type).append("()");
				}
				sb.append(");");
				newline();
				return null;
			}
			if (target.getClass().equals(GSIdentifierWithIndex.class)) {
				//assignelement/varname/elementno/value
				_compile(st,node.child(1));
				sb.append("((BCList)vm.getVariable(\"");
				sb.append(target.child(0).tokens());
				sb.append("\",false))");
				sb.append(".getContent().set(");
				sb.append(Integer.parseInt(target.child(1).tokens()));
				sb.append(",pop()");
				sb.append(");");
				newline();
				return null;
			}
			throw new SystemImplementationException(
					"Compiler error: Unknown type of Assignment: "+target.getClass().getName());
		}
		
		if (node instanceof GSStringConstant) {
			// engage paranoia
			String string=node.tokens();
			string=string.substring(1,string.length()-1);
			sb.append("push(").append("new BCString(null,new String(Base64.getDecoder().decode(\"");
			sb.append(base64encode(string));
			sb.append("\"))));");
			newline();
			return null;
		}
		
		if (node instanceof GSIntegerConstant) {
			sb.append("push(new BCInteger(null,").append(node.tokens().trim()).append("));");
			newline();
			return null;
		}
		
		if (node instanceof GSFloatConstant) {
			sb.append("push(new BCFloat(null,").append(node.tokens().trim()).append("f));");
			newline();
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
			
			_compile(st,node.child(0));
			sb.append("if (pop().toBoolean()) { runstep=")
			  .append(truthblock)
			  .append("; } else { runstep=")
			  .append(falseblock)
			  .append("; }");
			newline();
			
			block(truthblock);
			_compile(st,node.child(1));
			runstep(postblock);
			
			block(falseblock);
			if (node.children()==3) {
				_compile(st,node.child(2));
			}
			runstep(postblock);
			block(postblock);
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
			runstep(condition);
			block(condition);
			_compile(st,node.child(0));
			sb.append("if (pop().toBoolean()) { runstep=")
			  .append(runcode)
			  .append("; } else { runstep=")
			  .append(endpoint)
			  .append("; }");
			newline();
			
			block(runcode);
			_compile(st,node.child(1));
			runstep(condition);
			
			block(endpoint);
			return null;
		}
		
		if (node instanceof GSFunctionCall) {
			/*
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
			final Method function=GSFunctions.getNullable(functionname);
			if (function!=null) {
				// THIS IS A WELL DEFINED GS INTERNAL FUNCTION ; set it up and call it natively with java, nothing really that fancy here
				// internal function's signature is always a State followed by a GSVM followed by parameters.
				final Class<?>[] parameterTypes=function.getParameterTypes();
				final ParseNode parameters=((ParseNode)(node.jjtGetChild(1)));
				if (parameterTypes.length<2) {
					throw new GSInternalError(
							"Calling GSFunction "+functionname+" which does not require at least 2 parameters?");
				}
				sb.append(function.getDeclaringClass().getCanonicalName())
				  .append(".")
				  .append(functionname)
				  .append("(state,vm");
				if (parameters.jjtGetNumChildren()!=(parameterTypes.length-2)) {
					throw new GSInternalError("Parameter count mismatch - calling function "+functionname+" with "+
					                          node.jjtGetNumChildren()+" arguments, but we want "+
					                          (parameterTypes.length-2));
				}
				for (int i=2;i<parameterTypes.length;i++) {
					sb.append(",");
					_compile(st,parameters.child(i-2));
				}
				sb.append(")");
				return null;
			} else {
				// This is NOT a well defined GS function and is either a script to script call or just is botched.
				throw new GSInternalError("Not implemented - func call to script or unknown");
			}/*
			if (node.jjtGetNumChildren()>1) {
				compiled.addAll(compile(st,node.child(1)));
			} // assuming it has parameters
			else {
				compiled.add(new BCInteger(node,0));
			} // else zero parameters
*/
			return null;
		}
		
		if (node instanceof GSParameters) {
			/*for (int i=node.jjtGetNumChildren()-1;i>=0;i--) {
				compiled.addAll(compile(st,node.child(i)));
			}
			compiled.add(new BCInteger(node,node.children()));
			return compiled;*/ // TODO
			return null;
		}
		
		if (node instanceof GSLogicalOr) {
			funcOp("logicalOr",st,node);
			return null;
		}
		
		if (node instanceof GSLogicalAnd) {
			funcOp("logicalAnd",st,node);
			return null;
		}
		
		if (node instanceof GSInEquality) {
			funcOp("valueEquals",st,node);
			if (node.children()==2) {
				sb.append("push(pop().not());");
				newline();
			}
			return null;
		}
		
		if (node instanceof GSEquality) {
			funcOp("valueEquals",st,node);
			return null;
		}
		
		if (node instanceof GSLessThan) {
			funcOp("lessThan",st,node);
			return null;
		}
		
		if (node instanceof GSGreaterThan) {
			funcOp("greaterThan",st,node);
			return null;
		}
		
		if (node instanceof GSLessOrEqualThan) {
			funcOp("lessThanOrEqual",st,node);
			return null;
		}
		
		if (node instanceof GSGreaterOrEqualThan) {
			funcOp("greaterThanOrEqual",st,node);
			return null;
		}
		
		if (node instanceof GSAdd) {
			funcOp("add",st,node);
			return null;
		}
		
		if (node instanceof GSSubtract) {
			funcOp("subtract",st,node);
			return null;
		}
		
		if (node instanceof GSMultiply) {
			funcOp("multiply",st,node);
			return null;
		}
		
		if (node instanceof GSDivide) {
			funcOp("divide",st,node);
			return null;
		}
		
		if (node instanceof GSLogicalNot) {
			_compile(st,node.child(0));
			sb.append("push(pop().not());");
			newline();
			if (node.children()>2) { throw new GSInternalError("NOT had more than 2 children in java compiler"); }
			return null;
		}
		
		if (node instanceof GSUnaryMinus) {
			_compile(st,node.child(0));
			sb.append("push(pop().unaryMinus());");
			newline();
			if (node.children()>2) { throw new GSInternalError("Unary minus had more than 2 children in java compiler"); }
			return null;
		}
		
		if (node instanceof GSIdentifier) {
			sb.append("push(vm.getVariable(\"").append(node.tokens()).append("\",false));");
			newline();
			return null;
		}
		
		if (node instanceof GSList) {
			for (int i=node.children()-1;i>=0;i--) {
				_compile(st,node.child(i));
			}
			sb.append("push(new BCList(null)");
			for (int i=0;i<node.children();i++) {
				sb.append(".append(pop())");
			}
			sb.append(");");
			newline();
			return null;
		}
		
		if (node instanceof GSListIndex) { // a list index in an evaluatable position
			checkType(node,0,GSIdentifier.class);
			checkType(node,1,GSExpression.class);
			// pop name, pop index
			_compile(st,node.child(1));
			sb.append("push(((BCList)vm.getVariable(\"")
			  .append(node.child(0).tokens())
			  .append("\",false)).getElement(pop().toInteger()));");
			newline();
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
			newline();
			sb.append("pop(); // discard");
			newline();
			return null;
		}
		
		throw new SystemImplementationException(
				"Compilation not implemented for node type '"+node.getClass().getSimpleName()+"'");
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
			if (tba==null) {
				tba=baos.toByteArray();
			}
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
		protected ExtendedStandardJavaFileManager(final JavaFileManager fileManager,
		                                          final GSJavaVMDynamicClassLoader cl) {
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
	
	@Override
	public Byte[] toByteCode(final State st) {
		try { compile(st); }
		catch (final Exception e) {
			System.out.println(sb.toString());
			throw(e);
		}
		return ArrayUtils.toObject(bytesOut);
	}
}

