package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.SL.Data.User;

import java.util.List;

public class BCAvatar extends ByteCode {
	private User content=null;
	public BCAvatar(){}
	public BCAvatar(User content) { this.content=content; }
	public String explain() { return "BCAvatar("+content+")"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.BCAvatar.get());
		if (content==null) { bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff);return; }
		addInt(bytes,content.getId());
	}

}
