package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;
import net.coagulate.GPHUD.State;

import java.util.*;

public class GSVM {
	// GPHUD Scripting Virtual Machine ... smiley face

	// AN INSTANCE IS NOT THREAD SAFE :P  make many instances :P

	public byte[] bytecode;
	public int PC=0;
	public int row=0;
	public int column=0;
	private int startPC=0;
	public Stack<ByteCodeDataType> stack=new Stack<>();
	Map<String,ByteCodeDataType> variables=new HashMap<>();

	private void initialiseVM(State st) {
		stack.clear();
		PC=0;
		startPC=0;
		row=0;
		column=0;
		variables.clear();
		simulation=false;
		variables.put("CALLER",new BCCharacter(null,st.getCharacter()));
		variables.put("AVATAR",new BCAvatar(null,st.getAvatar()));
	}

	public ByteCodeDataType get(String k) { return variables.get(k); }
	public void set(String k,ByteCodeDataType v) {
		// does it already exist?
		ByteCodeDataType existing=get(k);
		if (existing==null) {
			variables.put(k, v); // hopefully we're initialising :P
			return;
		}
		if (existing.getClass().equals(v.getClass())) { variables.put(k,v); return; }
		// auto polymorphism.
		if (existing.getClass().equals(BCString.class)) { variables.put(k,v.toBCString()); return; }
		if (existing.getClass().equals(BCInteger.class)) { variables.put(k,v.toBCInteger()); return; }
		throw new GSInvalidExpressionException("Can not assign value of type "+v.getClass().getSimpleName()+" to "+k+" which is of type "+existing.getClass().getSimpleName());
	}
	public String at() {
		return "at row "+row+", column "+column+", PC="+startPC+", OP="+bytecode[startPC]+" ("+ByteCode.get(bytecode[startPC])+")";
	}

	public GSVM(Byte[] code) {
		bytecode = new byte[code.length];
		{
			for (int i = 0; i < code.length; i++) { bytecode[i] = code[i]; }
		}
	}

	public GSVM(byte[] code) { bytecode = code; }

	public String toHtml() {
		PC=0;
		String line = "<table>";
		while (PC < bytecode.length) {
			line += "<tr><th>" + PC + "</th><td>";
			try {
				ByteCode instruction = ByteCode.load(this);
				line += instruction.htmlDecode();
			}
			catch (Exception e) { line+="</td></tr><tr><td colspan=5>"+e.toString()+"</tD></tr></table>"; return line; }
			line += "</td></tr>";
		}
		return line + "</td></tr></table>";
	}

	public ByteCodeDataType pop() { return stack.pop(); }

	public void push(ByteCodeDataType add) { stack.push(add); }

	public BCString popString() {
		ByteCodeDataType raw = pop();
		if (!raw.getClass().equals(BCString.class)) {
			throw new GSInvalidPopError("Expected BCString on stack, got "+raw.getClass().getSimpleName());
		}
		return (BCString)raw;
	}
	public BCInteger popInteger() {
		ByteCodeDataType raw = pop();
		if (!raw.getClass().equals(BCInteger.class)) {
			throw new GSInvalidPopError("Expected BCInteger on stack, got "+raw.getClass().getSimpleName());
		}
		return (BCInteger)raw;
	}

	public BCList getList(String name) {
		ByteCodeDataType raw=get(name);
		if (raw==null) { throw new GSInvalidExpressionException("Variable "+name+" does not exist"); }
		if (!raw.getClass().equals(BCList.class)) {
			throw new GSInvalidExpressionException("Variable "+name+" is not a List");
		}
		return (BCList)raw;
	}

	public void execute(State st) {
		// like simulation but we dont keep the whole execution trace
		st.vm=this;
		initialiseVM(st);
		ExecutionStep currentstep=new ExecutionStep();
		try {
			while (PC<bytecode.length) {
				currentstep=new ExecutionStep();
				startPC=PC;
				ByteCode.load(this).execute(st,this,false);
			}
		} catch (Throwable t) {
			if (t instanceof SystemException || t instanceof GSInternalError) {
				throw new SystemException("VM exception: "+t.toString()+" "+at(),t);
			}
			if (t instanceof GSException || t instanceof UserException) { throw new UserException("Script error: "+t.toString()+" "+at(),t); }
			if (t instanceof RuntimeException) { throw new SystemException("VM Runtime: "+t.toString()+" "+at(),t); }
			throw new SystemException("VM Uncaught: "+t.toString()+" "+at(),t);
		}
		st.vm=null;
	}

	public String dumpStateToHtml() {
		String ret="";
		ret+="<h3>Stack</h3><br><table>";
		for (int i=0;i<stack.size();i++) {
			ret+="<tr><th>"+i+"</th><td>"+stack.get(i).getClass().getSimpleName()+"</td><td>"+stack.get(i).explain()+"</td></tr>";
		}
		ret+="</table>";
		ret+="<h3>Variable store</h3><br><table>";
		for (String k:variables.keySet()) {
			ret+="<tr><th>"+k+"</th>";
			ByteCodeDataType value = variables.get(k);
			ret+="<td>"+value.getClass().getSimpleName()+"</td><td>"+value.explain()+"</td></tr>";
		}
		ret+="</table>";
		ret+="<h3>Byte code</h3><br>";
		ret += "<pre><table border=0><tr>";
		for (int i = 0; i < bytecode.length; i++) {
			if ((i % 25) == 0) { ret += "</tr><tr><th>" + i + "</th>"; }
			ret += "<td>" + bytecode[i] + "</td>";
		}
		ret += "</tr></table></pre>";

		ret+="<h3>Code store</h3><br>";
		ret+=toHtml();
		return ret;
	}

	public class ExecutionStep {
		public int programcounter;
		public String decode="";
		public Stack<ByteCodeDataType> resultingstack=new Stack<>();
		public Map<String,ByteCodeDataType> resultingvariables=new HashMap<>();
		public Throwable t=null;
	}

	public boolean simulation=false;
	public List<ExecutionStep> simulate(State st) {
		List<ExecutionStep> simulationsteps=new ArrayList<>();
		initialiseVM(st);
		simulation=true;
		try {
			while (PC < bytecode.length) {
				ExecutionStep frame = new ExecutionStep();
				frame.programcounter = PC;
				startPC = PC;
				ByteCode instruction = ByteCode.load(this);
				frame.decode = instruction.htmlDecode();
				instruction.execute(st, this,true);
				for (int i=0;i<stack.size();i++) { frame.resultingstack.push(stack.elementAt(i).clone()); }
				for (String k:variables.keySet()) {
					ByteCodeDataType clone = null;
					if (variables.get(k)!=null) { clone=variables.get(k).clone(); }
					frame.resultingvariables.put(k,clone);
				}
				simulationsteps.add(frame);
			}
		} catch (Throwable e) {
			ExecutionStep step=new ExecutionStep();
			step.t=e;
			step.decode=at();
			simulationsteps.add(step);
		}
		return simulationsteps;
	}

	public int getInt() {
		int a=bytecode[PC] & 0xff;
		int b=bytecode[PC+1] & 0xff;
		int c=bytecode[PC+2] & 0xff;
		int d=bytecode[PC+3]&0xff & 0xff;
		int ret=(a<<24)+(b<<16)+(c<<8)+d;
		System.out.println("getInt: "+a+" "+b+" "+c+" "+d+" = "+ret);
		PC+=4;
		return ret;
	}

	public int getShort()
	{
		int ret=((((int)this.bytecode[this.PC]&0xff)<<8)+(((int)this.bytecode[this.PC+1]&0xff)));
		this.PC+=2;
		return ret;
	}

}
