package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCBranchIfZero extends ByteCode {

	private final BCLabel target;
	public BCBranchIfZero(BCLabel target) { this.target=target; }
	public String explain() { return "BCBranchIfZero#"+target.id+" (Pop one, branch if zero)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.BCBranchIfZero.get());
		if (target.address==null) { bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); }
		else { addInt(bytes,target.address); }
	}
}
