package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.ExceptionTools;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Data.ScriptRun;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GSStackVM extends GSVM {
	// GPHUD Scripting Virtual Machine ... smiley face
	
	public final    Stack<ByteCodeDataType> stack=new Stack<>();
	@Nonnull public byte[]                  bytecode;
	/**
	 * As in what instruction we're executing ; R15 if you ARM
	 */
	public          int                     programCounter;
	public          int                     row;
	public          int                     column;
	public          String                  source;
	/**
	 * Total number of instructions executed, not to be confused with programCounter ; this is a resource limit counter
	 */
	int instructionCount;
	@Nullable String invokeonexit;
	boolean suspended;
	private int startPC;
	private int suspensions;
	
	public GSStackVM(@Nonnull final GSCompiler compiler) {
		final Byte[] code=compiler.toByteCode(compiler.getCompiledState());
		bytecode=new byte[code.length];
		{
			for (int i=0;i<code.length;i++) {
				bytecode[i]=code[i];
			}
		}
		introduce(" CANARY",new BCInteger(null,ThreadLocalRandom.current().nextInt()));
	}
	
	public GSStackVM(@Nonnull final Script script) {
		bytecode=script.getByteCode();
		introduce(" CODEBASE "+script.getName(),new BCInteger(null,0));
		introduce(" CANARY",new BCInteger(null,ThreadLocalRandom.current().nextInt()));
	}
	
	
	public GSStackVM(@Nonnull final ScriptRun run,@Nonnull final State st) {
		st.vm=this;
		// run the initialiser as prep
		initialiseVM(st,true);
		bytecode=run.getInitialiser();
		executeloop(st);
		// stack and variables should now be restored, configure for resuming the run.
		bytecode=run.getByteCode();
		programCounter=getInteger(" PC",false).getContent();
		instructionCount=getInteger(" IC",false).getContent();
		if (existsVariable(" SOURCE")) {
			source=getString(" SOURCE",false).getContent();
		}
		suspensions=getInteger(" SUSP",false).getContent();
		if (existsVariable(" ONEXIT")) {
			invokeonexit=getString(" ONEXIT",false).getContent();
		}
		// caller should now call resume() to return to the program.  caller may want to tickle the stack first though, if thats why we suspended.
	}
	
	// ----- Internal Instance -----
	private void initialiseVM(@Nonnull final State st,final boolean skipCanary) {
		//skip canary - used by the initialiser bytecode that restores the stack+variable map for a resumed suspension
		stack.clear();
		programCounter=0;
		instructionCount=0;
		startPC=0;
		row=0;
		column=0;
		clearVariables();
		
		if (!existsVariable("CALLER")) {
			putVariable("CALLER",new BCCharacter(null,st.getCharacter()));
		}
		if (!existsVariable("AVATAR")) {
			putVariable("AVATAR",new BCAvatar(null,st.getAvatarNullable()));
		}
		setInvokerState(st);
		// return compatible stack state
		if (!skipCanary) {
			push(new BCInteger(null,-1));
			push(new BCInteger(null,getCanary()));
		}
	}
	
	@Nonnull
	private Response executeloop(@Nonnull final State st) {
		GSStackVMExecutionStep currentstep=new GSStackVMExecutionStep();
		try {
			while (programCounter>=0&&programCounter<bytecode.length&&!suspended) {
				increaseIC();
				//noinspection UnusedAssignment
				currentstep=new GSStackVMExecutionStep();
				startPC=programCounter;
				ByteCode.load(this).execute(st,this,false);
			}
		} catch (@Nonnull final Throwable t) {
			if (SystemException.class.isAssignableFrom(t.getClass())) {
				throw new GSInternalError("VM exception: "+t+" "+at(),t);
			}
			if (UserException.class.isAssignableFrom(t.getClass())) {
				throw new GSExecutionException(
						"Script error: {"+t.getClass().getSimpleName()+"} "+t.getLocalizedMessage()+" "+at(),t);
			}
			if (t instanceof RuntimeException) {
				throw new GSInternalError(
						"VM Runtime: {"+t.getClass().getSimpleName()+"} "+t.getLocalizedMessage()+" "+at(),t);
			}
			throw new GSInternalError(
					"VM Uncaught: {"+t.getClass().getSimpleName()+"} "+t.getLocalizedMessage()+" "+at(),t);
		}
		st.vm=null;
		final JSONObject json=dequeue(st,st.getCharacterNullable()).asJSON(st);
		if (invokeonexit!=null&&!suspended) {
			json.put("incommand","runtemplate");
			json.put("args","0");
			json.put("invoke",invokeonexit);
		}
		return new JSONResponse(json);
	}
	
	public void push(final ByteCodeDataType add) {
		stack.push(add);
	}
	
	public int getCanary() {
		final ByteCodeDataType canary=getVariable(" CANARY",true);
		if (canary==null) {
			throw new GSInternalError("Canary not found? ("+source+" - "+row+":"+column+")");
		}
		return canary.toInteger();
	}
	
	private void increaseIC() {
		instructionCount++;
		if (instructionCount>20000) {
			throw new GSResourceLimitExceededException("Instruction count exceeded, infinite loop (or complex script)?");
		}
	}
	
	@Nonnull
	public String at() {
		return "at "+source+" row "+row+", column "+column+", PC="+startPC+
		       (startPC>=0&&startPC<bytecode.length?", OP="+bytecode[startPC]+" ("+ByteCode.get(bytecode[startPC])+")":
		        "");
	}
	
	
	// ---------- INSTANCE ----------
	// AN INSTANCE IS NOT THREAD SAFE :P  make many instances :P
	public boolean suspended() {
		return suspended;
	}
	
	public void invokeOnExit(final String commandname) {
		invokeonexit=commandname;
	}
	
	@Nonnull
	public Response execute(@Nonnull final State st) {
		// like simulation but we dont keep the whole execution trace
		st.vm=this;
		initialiseVM(st,false);
		return executeloop(st);
	}
	
	public void set(final String k,@Nonnull final ByteCodeDataType v) {
		// does it already exist?
		final ByteCodeDataType existing=getVariable(k,true);
		if (existing==null) {
			putVariable(k,v); // hopefully we're initialising :P
			return;
		}
		if (existing.getClass().equals(v.getClass())) {
			putVariable(k,v);
			return;
		}
		// auto polymorphism.
		if (existing.getClass().equals(BCString.class)) {
			putVariable(k,v.toBCString());
			return;
		}
		if (existing.getClass().equals(BCInteger.class)) {
			putVariable(k,v.toBCInteger());
			return;
		}
		if (existing.getClass().equals(BCFloat.class)) {
			putVariable(k,v.toBCFloat());
			return;
		}
		throw new GSInvalidExpressionException(
				"Can not assign value of type "+v.getClass().getSimpleName()+" to "+k+" which is of type "+
				existing.getClass().getSimpleName(),true);
	}
	
	@Nonnull
	public BCString popString() {
		final ByteCodeDataType raw=pop();
		if (!raw.getClass().equals(BCString.class)) {
			throw new GSInvalidPopError("Expected BCString on stack, got "+raw.getClass().getSimpleName());
		}
		return (BCString)raw;
	}
	
	public ByteCodeDataType pop() {
		if (stack.empty()) {
			throw new GSInternalError("Attempted to pop from the stack but it is empty!");
		}
		return stack.pop();
	}
	
	@Nonnull
	public BCInteger popInteger() {
		final ByteCodeDataType raw=pop();
		if (!raw.getClass().equals(BCInteger.class)) {
			throw new GSInvalidPopError("Expected BCInteger on stack, got "+raw.getClass().getSimpleName());
		}
		return (BCInteger)raw;
	}
	
	@Nonnull
	public BCList getList(final String name) {
		final ByteCodeDataType raw=getVariable(name,true);
		if (raw==null) {
			throw new GSInvalidExpressionException("Variable "+name+" does not exist");
		}
		if (!raw.getClass().equals(BCList.class)) {
			throw new GSInvalidExpressionException("Variable "+name+" is not a List");
		}
		return (BCList)raw;
	}
	
	@Nonnull
	public String dumpStateToHtml() {
		final StringBuilder ret=new StringBuilder();
		ret.append("<h3>Stack</h3><br><table>");
		for (int i=0;i<stack.size();i++) {
			ret.append("<tr><th>")
			   .append(i)
			   .append("</th><td>")
			   .append(stack.get(i).getClass().getSimpleName())
			   .append("</td><td>")
			   .append(stack.get(i).explain())
			   .append("</td></tr>");
		}
		ret.append("</table>");
		ret.append("<h3>Variable store</h3><br><table>");
		for (final Map.Entry<String,ByteCodeDataType> entry: variables().entrySet()) {
			ret.append("<tr><th>").append(entry.getKey()).append("</th>");
			final ByteCodeDataType value=entry.getValue();
			ret.append("<td>")
			   .append(value.getClass().getSimpleName())
			   .append("</td><td>")
			   .append(value.explain())
			   .append("</td></tr>");
		}
		ret.append("</table>");
		ret.append("<h3>Byte code</h3><br>");
		ret.append("<pre><table border=0><tr>");
		for (int i=0;i<bytecode.length;i++) {
			if ((i%25)==0) {
				ret.append("</tr><tr><th>").append(i).append("</th>");
			}
			ret.append("<td>").append(bytecode[i]).append("</td>");
		}
		ret.append("</tr></table></pre>");
		
		ret.append("<h3>Code store</h3><br>");
		ret.append(toHtml());
		return ret.toString();
	}
	
	@Nonnull
	public String toHtml() {
		programCounter=0;
		final StringBuilder line=new StringBuilder("<table>");
		while (programCounter<bytecode.length) {
			line.append("<tr><th>").append(programCounter).append("</th><td>");
			try {
				final ByteCode instruction=ByteCode.load(this);
				line.append(instruction.htmlDecode());
			} catch (@Nonnull final Exception e) {
				line.append("</td></tr><tr><td colspan=5>").append(e).append("</tD></tr></table>");
				return line.toString();
			}
			line.append("</td></tr>");
		}
		return line+"</td></tr></table>";
	}
	
	public void suspend(final State st,@Nonnull final Char respondant) {
		putVariable(" PC",new BCInteger(null,programCounter));
		putVariable(" IC",new BCInteger(null,instructionCount));
		putVariable(" SUSP",new BCInteger(null,suspensions));
		if (source!=null) {
			putVariable(" SOURCE",new BCString(null,source));
		}
		if (invokeonexit!=null) {
			putVariable(" ONEXIT",new BCString(null,invokeonexit));
		}
		suspensions++;
		if (suspensions>10) {
			throw new GSResourceLimitExceededException(
					"Maximum number of VM suspensions reached - too many user input requests?");
		}
		putVariable(" SUSPENSIONS",new BCInteger(null,suspensions));
		// simulations dont suspend.  but do update the variables and fake a suspension count.  for completeness :P
		if (simulation()) {
			return;
		}
		suspended=true;
		final List<ByteCode> initlist=new ArrayList<>(stack);
		for (final Map.Entry<String,ByteCodeDataType> entry: variables().entrySet()) {
			final ByteCodeDataType bcd=entry.getValue();
			if (bcd.getClass().equals(BCList.class)) {
				final BCList list=(BCList)bcd;
				final List<ByteCodeDataType> theList=list.getContent();
				for (int i=theList.size()-1;i>=0;i--) {
					initlist.add(theList.get(i));
				}
				initlist.add(list);
			} else {
				initlist.add(bcd);
			}
			initlist.add(new BCString(null,entry.getKey()));
			initlist.add(new BCInitialise(null));
		}
		
		List<Byte> initbc=new ArrayList<>();
		for (final ByteCode bc: initlist) {
			bc.toByteCode(initbc);
		}
		// redo. now that forward references are completed
		initbc=new ArrayList<>();
		for (final ByteCode bc: initlist) {
			bc.toByteCode(initbc);
		}
		final Byte[] initialisertyped=initbc.toArray(new Byte[] {});
		final byte[] initialiser=new byte[initialisertyped.length];
		for (int i=0;i<initialisertyped.length;i++) {
			initialiser[i]=initialisertyped[i];
		}
		
		final ScriptRun run=ScriptRun.create(bytecode,initialiser,respondant,2);
		pid=run.getId();
		//return dequeue(st,st.getCharacter(),run.getId());
	}
	
	@Override
	public void setReturn(final ByteCodeDataType bcdt) {
	
	}
	
	@Nonnull
	public Response resume(@Nonnull final State st) {
		setInvokerState(st);
		return executeloop(st);
	}
	
	@Nonnull
	public List<? extends GSVMExecutionStep> simulate(@Nonnull final State st) {
		setInvokerState(st);
		final List<GSVMExecutionStep> simulationsteps=new ArrayList<>();
		initialiseVM(st,false);
		setSimulation();
		try {
			while (programCounter>=0&&programCounter<bytecode.length) {
				increaseIC();
				final GSStackVMExecutionStep frame=new GSStackVMExecutionStep();
				frame.programCounter=programCounter;
				startPC=programCounter;
				final ByteCode instruction=ByteCode.load(this);
				frame.decode=instruction.htmlDecode();
				instruction.execute(st,this,true);
				for (int i=0;i<stack.size();i++) {
					frame.resultingstack.push(stack.elementAt(i).clone());
				}
				for (final Map.Entry<String,ByteCodeDataType> entry: variables().entrySet()) {
					ByteCodeDataType clone=null;
					if (entry.getValue()!=null) {
						clone=entry.getValue().clone();
					}
					frame.resultingvariables.put(entry.getKey(),clone);
				}
				frame.instructionCount=instructionCount;
				simulationsteps.add(frame);
			}
		} catch (@Nonnull final Throwable e) {
			final GSStackVMExecutionStep step=new GSStackVMExecutionStep();
			step.t=e;
			step.decode=at();
			simulationsteps.add(step);
		}
		return simulationsteps;
	}
	
	public float getFloat() {
		final int getInt=getInt();
		//System.out.println("getInt: "+a+" "+b+" "+c+" "+d+" = "+ret);
		return Float.intBitsToFloat(getInt);
	}
	
	public int getInt() {
		final int a=bytecode[programCounter]&0xff;
		final int b=bytecode[programCounter+1]&0xff;
		final int c=bytecode[programCounter+2]&0xff;
		final int d=bytecode[programCounter+3]&0xff;
		final int ret=(a<<24)+(b<<16)+(c<<8)+d;
		//System.out.println("getInt: "+a+" "+b+" "+c+" "+d+" = "+ret);
		programCounter+=4;
		return ret;
	}
	
	public int getShort() {
		final int ret=(((bytecode[programCounter]&0xff)<<8)+((bytecode[programCounter+1]&0xff)));
		programCounter+=2;
		return ret;
	}
	
	public static class GSStackVMExecutionStep extends GSVMExecutionStep {
		public final     Stack<ByteCodeDataType>      resultingstack    =new Stack<>();
		public final     Map<String,ByteCodeDataType> resultingvariables=new TreeMap<>();
		public           int                          programCounter;
		@Nullable public String                       decode            ="";
		@Nullable public Throwable                    t;
		public           int                          instructionCount;
		
		@Nonnull
		@Override
		public String formatStep() {
			final StringBuilder output=new StringBuilder();
			output.append("<tr><td>")
			      .append(instructionCount)
			      .append("</td><th>")
			      .append(programCounter)
			      .append("</th><td>")
			      .append(decode)
			      .append("</td><td><table>");
			for (int i=0;i<resultingstack.size();i++) {
				output.append("<tr><th>")
				      .append(i)
				      .append("</th><td>")
				      .append(resultingstack.get(i).htmlDecode())
				      .append("</td></tr>");
			}
			output.append("</table></td><td><table>");
			for (final Map.Entry<String,ByteCodeDataType> entry: resultingvariables.entrySet()) {
				String decode="???";
				final boolean italics=entry.getKey().startsWith(" ");
				if (entry.getValue()!=null) {
					decode=entry.getValue().htmlDecode();
				}
				output.append("<tr><th>")
				      .append(italics?"<i>":"")
				      .append(entry.getKey())
				      .append(italics?"</i>":"")
				      .append("</th><td>")
				      .append(italics?"<i>":"")
				      .append(decode.replaceAll("<td>","<td>"+(italics?"<i>":"")))
				      .append(italics?"</i>":"")
				      .append("</td></tr>");
			}
			output.append("</table></td></tr>");
			if (t!=null) {
				output.append("<tr><td colspan=100>").append(ExceptionTools.toHTML(t)).append("</td></tr>");
			}
			return output.toString();
		}
	}
}
