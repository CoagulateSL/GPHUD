package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;

import java.util.*;

public class GSVM {
	// GPHUD Scripting Virtual Machine ... smiley face

	public byte[] bytecode;
	public int PC=0;
	public int row=0;
	public int column=0;
	private int startPC=0;
	public Stack<ByteCodeDataType> stack=new Stack<>();
	Map<String,ByteCodeDataType> variables=new HashMap<>();

	private void initialiseVM() {
		stack.clear();
		PC=0;
		startPC=0;
		row=0;
		column=0;
		variables.clear();
		simulation=false;
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
			ByteCode instruction=ByteCode.load(this);
			line+=instruction.htmlDecode();
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

	public class ExecutionStep {
		public int programcounter;
		public String decode="";
		public Stack<ByteCodeDataType> resultingstack=new Stack<>();
		public Map<String,ByteCodeDataType> resultingvariables=new HashMap<>();
		public Throwable t=null;
	}

	public boolean simulation=false;
	public List<ExecutionStep> simulate() {
		List<ExecutionStep> simulationsteps=new ArrayList<>();
		initialiseVM();
		simulation=true;
		try {
			while (PC < bytecode.length) {
				ExecutionStep frame = new ExecutionStep();
				frame.programcounter = PC;
				startPC = PC;
				ByteCode instruction = ByteCode.load(this);
				frame.decode = instruction.htmlDecode();
				instruction.execute(this);
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
		int ret=(((int) this.bytecode[this.PC] & 0xff) << 24) + (((int) this.bytecode[this.PC + 1] & 0xff) << 16) + (((int) this.bytecode[this.PC + 2] & 0xff) << 8) + (((int) this.bytecode[this.PC + 3] & 0xff));
		this.PC+=4;
		return ret;
	}

	public int getShort()
	{
		int ret=((((int)this.bytecode[this.PC]&0xff)<<8)+(((int)this.bytecode[this.PC+1]&0xff)));
		this.PC+=2;
		return ret;
	}

}