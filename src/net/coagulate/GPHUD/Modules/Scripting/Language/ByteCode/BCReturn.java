package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCReturn extends ByteCode {

	public BCReturn(final ParseNode n) {
		super(n);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String explain() {
		return "Return (Pop return value, pop stack canary, pop return PC, push return value)";
	}

	@Override
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Return.get());
	}

	@Override
	public void execute(final State st,
	                    @Nonnull final GSVM vm,
	                    final boolean simulation) {
		// this function is funky.  We expect to find the following on the stack:
		// 1) A return value, whatever the last evaluation left behind
		// 2) A canary, should match the vm's random canary
		// 3) An absolute PC to restore.
		// We need to put the return value back after removing the canary/restore so that the caller can use this.
		// If the absolute PC is -1 then this terminates the VM.
		ByteCodeDataType data=vm.pop();
		int canary=vm.popInteger().getContent();
		if (canary!=vm.getCanary()) {
			throw new GSInternalError("Return canary failed, got "+canary+" and expected "+vm.getCanary());
		}
		int newpc=vm.popInteger().getContent();
		vm.push(data);
		vm.PC=newpc;
	}
}
