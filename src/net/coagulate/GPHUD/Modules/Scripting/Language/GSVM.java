package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ScriptRun;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class GSVM {
	// GPHUD Scripting Virtual Machine ... smiley face

	public final Stack<ByteCodeDataType> stack=new Stack<>();
	final Map<String,ByteCodeDataType> variables=new HashMap<>();
	final Map<Char,JSONObject> queue=new HashMap<>();
	final Map<String,ByteCodeDataType> introductions=new HashMap<>();
	@Nonnull
	public byte[] bytecode;
	public int PC;
	public int row;
	public int column;
	public boolean simulation;
	int IC;
	@Nullable
	String invokeonexit;
	boolean suspended;
	private int startPC;
	private int pid;
	private int suspensions;
	@Nullable
	private State invokerstate;

	public GSVM(@Nonnull final Byte[] code) {
		bytecode=new byte[code.length];
		{
			for (int i=0;i<code.length;i++) { bytecode[i]=code[i]; }
		}
	}

	public GSVM(@Nonnull final byte[] code) { bytecode=code; }

	public GSVM(@Nonnull final ScriptRun run,
	            @Nonnull final State st) {
		st.vm=this;
		// run the initialiser as prep
		initialiseVM(st);
		bytecode=run.getInitialiser();
		executeloop(st);
		// stack and variables should be restored
		bytecode=run.getByteCode();
		PC=((BCInteger) (variables.get(" PC"))).getContent();
		IC=((BCInteger) (variables.get(" IC"))).getContent();
		suspensions=((BCInteger) (variables.get(" SUSP"))).getContent();
		if (variables.containsKey((" ONEXIT"))) { invokeonexit=((BCString) variables.get(" ONEXIT")).getContent(); }
		// caller should now call resume() to return to the program.  caller may want to tickle the stack first though, if thats why we suspended.
	}

	// ---------- INSTANCE ----------
	// AN INSTANCE IS NOT THREAD SAFE :P  make many instances :P
	public boolean suspended() { return suspended; }

	public void invokeOnExit(final String commandname) {invokeonexit=commandname;}

	public ByteCodeDataType get(final String k) { return variables.get(k); }

	public void set(final String k,
	                @Nonnull final ByteCodeDataType v) {
		// does it already exist?
		final ByteCodeDataType existing=get(k);
		if (existing==null) {
			variables.put(k,v); // hopefully we're initialising :P
			return;
		}
		if (existing.getClass().equals(v.getClass())) {
			variables.put(k,v);
			return;
		}
		// auto polymorphism.
		if (existing.getClass().equals(BCString.class)) {
			variables.put(k,v.toBCString());
			return;
		}
		if (existing.getClass().equals(BCInteger.class)) {
			variables.put(k,v.toBCInteger());
			return;
		}
		throw new GSInvalidExpressionException("Can not assign value of type "+v.getClass().getSimpleName()+" to "+k+" which is of type "+existing.getClass().getSimpleName());
	}

	@Nonnull
	public String at() {
		return "at row "+row+", column "+column+", PC="+startPC+(startPC >= 0 && startPC<bytecode.length?", OP="+bytecode[startPC]+" ("+ByteCode.get(bytecode[startPC])+")":"");
	}

	@Nonnull
	public String toHtml() {
		PC=0;
		final StringBuilder line=new StringBuilder("<table>");
		while (PC<bytecode.length) {
			line.append("<tr><th>").append(PC).append("</th><td>");
			try {
				final ByteCode instruction=ByteCode.load(this);
				line.append(instruction.htmlDecode());
			}
			catch (@Nonnull final Exception e) {
				line.append("</td></tr><tr><td colspan=5>").append(e).append("</tD></tr></table>");
				return line.toString();
			}
			line.append("</td></tr>");
		}
		return line+"</td></tr></table>";
	}

	public ByteCodeDataType pop() { return stack.pop(); }

	public void push(final ByteCodeDataType add) { stack.push(add); }

	@Nonnull
	public BCString popString() {
		final ByteCodeDataType raw=pop();
		if (!raw.getClass().equals(BCString.class)) {
			throw new GSInvalidPopError("Expected BCString on stack, got "+raw.getClass().getSimpleName());
		}
		return (BCString) raw;
	}

	@Nonnull
	public BCInteger popInteger() {
		final ByteCodeDataType raw=pop();
		if (!raw.getClass().equals(BCInteger.class)) {
			throw new GSInvalidPopError("Expected BCInteger on stack, got "+raw.getClass().getSimpleName());
		}
		return (BCInteger) raw;
	}

	@Nonnull
	public BCList getList(final String name) {
		final ByteCodeDataType raw=get(name);
		if (raw==null) { throw new GSInvalidExpressionException("Variable "+name+" does not exist"); }
		if (!raw.getClass().equals(BCList.class)) {
			throw new GSInvalidExpressionException("Variable "+name+" is not a List");
		}
		return (BCList) raw;
	}

	@Nonnull
	public Response execute(@Nonnull final State st) {
		// like simulation but we dont keep the whole execution trace
		st.vm=this;
		initialiseVM(st);
		return executeloop(st);
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
		for (final Map.Entry<String,ByteCodeDataType> entry: variables.entrySet()) {
			ret.append("<tr><th>").append(entry.getKey()).append("</th>");
			final ByteCodeDataType value=entry.getValue();
			ret.append("<td>").append(value.getClass().getSimpleName()).append("</td><td>").append(value.explain()).append("</td></tr>");
		}
		ret.append("</table>");
		ret.append("<h3>Byte code</h3><br>");
		ret.append("<pre><table border=0><tr>");
		for (int i=0;i<bytecode.length;i++) {
			if ((i%25)==0) { ret.append("</tr><tr><th>").append(i).append("</th>"); }
			ret.append("<td>").append(bytecode[i]).append("</td>");
		}
		ret.append("</tr></table></pre>");

		ret.append("<h3>Code store</h3><br>");
		ret.append(toHtml());
		return ret.toString();
	}

	@Nonnull
	public Response dequeue(final State st,
	                        final Char target) {
		final boolean debug=false;
		final JSONObject totarget=getQueue(target);
		if (pid!=0) { totarget.put("processid",""+pid); }
		queue.remove(target);
		for (final Char k: queue.keySet()) {
			final JSONObject totransmit=getQueue(k);
			if (pid!=0) { totransmit.put("processid",""+pid); }
			new Transmission(k,totransmit).start();
		}
		return new JSONResponse(totarget);
	}

	public void queueSayAs(@Nonnull final Char ch,
	                       final String message) {
		final JSONObject out=getQueue(ch);
		String m="";
		if (out.has("say")) { m=out.getString("say")+"\n"; }
		m=m+message;
		out.put("say",m);
		out.put("sayas",ch.getName());
	}

	public void queueSay(@Nonnull final Char ch,
	                     final String message) {
		final JSONObject out=getQueue(ch);
		String m="";
		if (out.has("sayashud")) { m=out.getString("sayashud")+"\n"; }
		m=m+message;
		out.put("sayashud",m);
	}

	public void queueTeleport(final Char content,
	                          final String hudRepresentation) {
		final JSONObject queue=getQueue(content);
		queue.put("teleport",hudRepresentation);
	}

	public void queueOwnerSay(final Char ch,
	                          final String message) {
		final JSONObject out=getQueue(ch);
		String m="";
		if (out.has("message")) { m=out.getString("message")+"\n"; }
		m=m+message;
		out.put("message",m);
	}

	public void queueSelectCharacter(final Char ch,
	                                 final String description) {
		final JSONObject out=getQueue(ch);
		out.put("args",1);
		out.put("arg0name","response");
		out.put("arg0type","SENSORCHAR");
		out.put("arg0manual","surewhynot");
		out.put("arg0description",description);
		out.put("incommand","runtemplate");
		out.put("invoke","Scripting.CharacterResponse");
	}

	public void queueGetText(final Char ch,
	                         final String description) {
		final JSONObject out=getQueue(ch);
		out.put("args",1);
		out.put("arg0name","response");
		out.put("arg0type","TEXTBOX");
		out.put("arg0description",description);
		out.put("incommand","runtemplate");
		out.put("invoke","Scripting.StringResponse");
	}

	public void queueGetChoice(final Char ch,
	                           final String description,
	                           @Nonnull final List<String> options) {
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

	public void suspend(final State st,
	                    @Nonnull final Char respondant) {
		variables.put(" PC",new BCInteger(null,PC));
		variables.put(" IC",new BCInteger(null,IC));
		variables.put(" SUSP",new BCInteger(null,suspensions));
		if (invokeonexit!=null) { variables.put(" ONEXIT",new BCString(null,invokeonexit)); }
		suspensions++;
		if (suspensions>10) {
			throw new GSResourceLimitExceededException("Maximum number of VM suspensions reached - too many user input requests?");
		}
		// simulations dont suspend.  but do update the variables and fake a suspension count.  for completeness :P
		if (simulation) { return; }
		suspended=true;
		final List<ByteCode> initlist=new ArrayList<>(stack);
		for (final Map.Entry<String,ByteCodeDataType> entry: variables.entrySet()) {
			final ByteCodeDataType bcd=entry.getValue();
			if (!bcd.getClass().equals(BCList.class)) {
				initlist.add(bcd);
			}
			else {
				final BCList list=(BCList) bcd;
				initlist.addAll(list.getContent());
				initlist.add(list);
			}
			initlist.add(new BCString(null,entry.getKey()));
			initlist.add(new BCInitialise(null));
		}

		List<Byte> initbc=new ArrayList<>();
		for (final ByteCode bc: initlist) { bc.toByteCode(initbc); }
		// redo. now that forward references are completed
		initbc=new ArrayList<>();
		for (final ByteCode bc: initlist) { bc.toByteCode(initbc); }
		final Byte[] initialisertyped=initbc.toArray(new Byte[]{});
		final byte[] initialiser=new byte[initialisertyped.length];
		for (int i=0;i<initialisertyped.length;i++) { initialiser[i]=initialisertyped[i]; }

		final ScriptRun run=ScriptRun.create(bytecode,initialiser,respondant);
		pid=run.getId();
		//return dequeue(st,st.getCharacter(),run.getId());
	}

	@Nullable
	public State getInvokerState() { return invokerstate; }

	@Nonnull
	public Response resume(@Nonnull final State st) {
		invokerstate=st;
		return executeloop(st);
	}

	public void introduce(final String target,
	                      final ByteCodeDataType data) {
		introductions.put(target,data);
	}

	@Nonnull
	public List<ExecutionStep> simulate(@Nonnull final State st) {
		invokerstate=st;
		final List<ExecutionStep> simulationsteps=new ArrayList<>();
		initialiseVM(st);
		simulation=true;
		try {
			while (PC<bytecode.length) {
				increaseIC();
				final ExecutionStep frame=new ExecutionStep();
				frame.programcounter=PC;
				startPC=PC;
				final ByteCode instruction=ByteCode.load(this);
				frame.decode=instruction.htmlDecode();
				instruction.execute(st,this,true);
				for (int i=0;i<stack.size();i++) { frame.resultingstack.push(stack.elementAt(i).clone()); }
				for (final Map.Entry<String,ByteCodeDataType> entry: variables.entrySet()) {
					ByteCodeDataType clone=null;
					if (entry.getValue()!=null) { clone=entry.getValue().clone(); }
					frame.resultingvariables.put(entry.getKey(),clone);
				}
				frame.IC=IC;
				simulationsteps.add(frame);
			}
		}
		catch (@Nonnull final Throwable e) {
			final ExecutionStep step=new ExecutionStep();
			step.t=e;
			step.decode=at();
			simulationsteps.add(step);
		}
		return simulationsteps;
	}

	public int getInt() {
		final int a=bytecode[PC]&0xff;
		final int b=bytecode[PC+1]&0xff;
		final int c=bytecode[PC+2]&0xff;
		final int d=bytecode[PC+3]&0xff;
		final int ret=(a<<24)+(b<<16)+(c<<8)+d;
		//System.out.println("getInt: "+a+" "+b+" "+c+" "+d+" = "+ret);
		PC+=4;
		return ret;
	}

	public int getShort() {
		final int ret=((((int) bytecode[PC]&0xff)<<8)+(((int) bytecode[PC+1]&0xff)));
		PC+=2;
		return ret;
	}

	// ----- Internal Instance -----
	private void initialiseVM(@Nonnull final State st) {
		stack.clear();
		PC=0;
		IC=0;
		startPC=0;
		row=0;
		column=0;
		variables.clear();
		simulation=false;
		variables.put("CALLER",new BCCharacter(null,st.getCharacter()));
		variables.put("AVATAR",new BCAvatar(null,st.getAvatarNullable()));
		invokerstate=st;
		for (final Map.Entry<String,ByteCodeDataType> entry: introductions.entrySet()) {
			variables.put(entry.getKey(),entry.getValue());
		}
	}

	@Nonnull
	private Response executeloop(@Nonnull final State st) {
		ExecutionStep currentstep=new ExecutionStep();
		try {
			while (PC<bytecode.length && !suspended) {
				increaseIC();
				//noinspection UnusedAssignment
				currentstep=new ExecutionStep();
				startPC=PC;
				ByteCode.load(this).execute(st,this,false);
			}
		}
		catch (@Nonnull final Throwable t) {
			if (SystemException.class.isAssignableFrom(t.getClass())) {
				throw new GSInternalError("VM exception: "+t+" "+at(),t);
			}
			if (UserException.class.isAssignableFrom(t.getClass())) { throw new GSExecutionException("Script error: {"+t.getClass().getSimpleName()+"} "+t.getLocalizedMessage()+" "+at(),t); }
			if (t instanceof RuntimeException) { throw new GSInternalError("VM Runtime: {"+t.getClass().getSimpleName()+"} "+t.getLocalizedMessage()+" "+at(),t); }
			throw new GSInternalError("VM Uncaught: {"+t.getClass().getSimpleName()+"} "+t.getLocalizedMessage()+" "+at(),t);
		}
		st.vm=null;
		final JSONObject json=dequeue(st,st.getCharacter()).asJSON(st);
		if (invokeonexit!=null && !suspended) {
			json.put("incommand","runtemplate");
			json.put("args","0");
			json.put("invoke",invokeonexit);
		}
		return new JSONResponse(json);
	}

	private JSONObject getQueue(final Char c) {
		if (!queue.containsKey(c)) { queue.put(c,new JSONObject()); }
		return queue.get(c);
	}

	private void increaseIC() {
		IC++;
		if (IC>10000) {
			throw new GSResourceLimitExceededException("Instruction count exceeded, infinite loop (or complex script)?");
		}
	}

	public static class ExecutionStep {
		public final Stack<ByteCodeDataType> resultingstack=new Stack<>();
		public final Map<String,ByteCodeDataType> resultingvariables=new HashMap<>();
		public int programcounter;
		@Nullable
		public String decode="";
		@Nullable
		public Throwable t;
		public int IC;
	}
}
