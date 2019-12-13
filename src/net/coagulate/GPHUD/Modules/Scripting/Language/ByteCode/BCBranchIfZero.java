package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCBranchIfZero extends ByteCode {

	@Nullable
	private final BCLabel target;
	public BCBranchIfZero(ParseNode n, @Nullable BCLabel target) { super(n); this.target=target; }
	public BCBranchIfZero(ParseNode n,int pc) { super(n); target=new BCLabel(node(),-1,pc); }
	@Nonnull
	public String explain() { return "BranchIfZero#"+target.id+" (Pop one, branch if zero)"; }
	public void toByteCode(@Nonnull List<Byte> bytes) {
		bytes.add(InstructionSet.BranchIfZero.get());
		if (target.address==null) { bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); }
		else { addInt(bytes,target.address); }
	}
	@Nullable
	@Override public String htmlDecode() { return "BranchIfZero</td><td>"+target.address; }

	@Override
	public void execute(State st, @Nonnull GSVM vm, boolean simulation) {
		// pop an int
		BCInteger conditional=vm.popInteger();
		// set PC if zero
		if (conditional.getContent()==0) { vm.PC=target.address; }
	}
}
