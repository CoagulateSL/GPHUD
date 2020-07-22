package net.coagulate.GPHUD.Modules.Characters;


import net.coagulate.SL.SL;

public class BackgroundGroupInviter extends Thread {

	private final net.coagulate.GPHUD.State st;

	public BackgroundGroupInviter(final net.coagulate.GPHUD.State st) {this.st=st;}

	// ---------- INSTANCE ----------
	public void run() {
		try {
			SL.groupInvite(st.getAvatar().getUUID(),"34ead140-555f-42f9-2b54-bb887554b70f","00000000-0000-0000-0000-000000000000");
		}
		catch (final Throwable t) {
			SL.report("Failed to group invite an instance owner "+st.getAvatar().getName(),t,st);
		}
	}

}
