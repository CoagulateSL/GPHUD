package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCLabel extends ByteCode {
	final int id;
	public BCLabel(final ParseNode n, final int id) {super(n);this.id=id;}
	public BCLabel(final ParseNode n, final int id, final int address) {super(n);this.id=id; this.address=address; }
	@Nonnull
	public String explain() { return "Label (:"+id+")"; }
	@Nullable
	Integer address;
	public int address() {
		if (address==null) { throw new SystemException("Jump address is null"); }
		return address;
	}
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		address=bytes.size();
	}

	@Override
	public void execute(final State st, final GSVM vm, final boolean simulation) {
		throw new GSInternalError("Can not execute the LABEL instruction, it is a pseudocode marker for compilation only");
	}
}
