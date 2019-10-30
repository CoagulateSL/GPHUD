package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCBranchIfZero extends ByteCode {

	private final BCLabel target;
	public BCBranchIfZero(BCLabel target) { this.target=target; }
	public BCBranchIfZero(int pc) { target=new BCLabel(-1,pc); }
	public String explain() { return "BranchIfZero#"+target.id+" (Pop one, branch if zero)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.BranchIfZero.get());
		if (target.address==null) { bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); }
		else { addInt(bytes,target.address); }
	}
	@Override public String htmlDecode() { return "BranchIfZero</td><td>"+target.address; }
}
