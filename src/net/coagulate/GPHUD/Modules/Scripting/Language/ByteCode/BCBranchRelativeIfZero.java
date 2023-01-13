package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCBranchRelativeIfZero extends ByteCode {
	
	@Nullable private final BCLabel source;
	@Nullable private final BCLabel target;
	
	public BCBranchRelativeIfZero(final ParseNode n,@Nullable final BCLabel source,@Nullable final BCLabel target) {
		super(n);
		this.source=source;
		this.target=target;
	}
	
	public BCBranchRelativeIfZero(final ParseNode n,final int relativePC) {
		super(n);
		source=null;
		target=new BCLabel(node(),-1,relativePC);
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public String explain() {
		return "BranchRelativeIfZero#"+target().id+" (Pop one, branch relative if zero)";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.BranchRelativeIfZero.get());
		if (target().address==null||source().address==null) {
			bytes.add((byte)0xff);
			bytes.add((byte)0xff);
			bytes.add((byte)0xff);
			bytes.add((byte)0xff);
		} else {
			addInt(bytes,target().address()-source().address()-5);
		}
		// 5 is the instruction length for BranchRelativeIfZero - 1 byte opcode, 4 bytes relative offset
	}
	
	@Nullable
	@Override
	public String htmlDecode() {
		return "BranchRelativeIfZero</td><td>"+target().address();
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSVM vm,final boolean simulation) {
		// pop an int
		final BCInteger conditional=vm.popInteger();
		// set PC if zero
		if (conditional.getContent()==0) {
			vm.programCounter+=target().address();
		}
	}
	
	// ----- Internal Instance -----
	@Nonnull
	private BCLabel source() {
		if (source==null) {
			throw new GSInternalError("Source is null");
		}
		return source;
	}
	
	@Nonnull
	private BCLabel target() {
		if (target==null) {
			throw new GSInternalError("Target is null");
		}
		return target;
	}
}
