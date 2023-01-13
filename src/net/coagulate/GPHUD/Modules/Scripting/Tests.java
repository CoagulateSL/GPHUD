package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSCompiler;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.GSParser;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.ParseException;
import net.coagulate.GPHUD.Tests.TestFramework;
import net.coagulate.SL.TestFrameworkPrototype;

import java.io.ByteArrayInputStream;

public class Tests {
	
	@TestFramework.Test(name="Test gsGetCharacter")
	public static TestFrameworkPrototype.TestOutput gsGetCharacter(final TestFramework f) throws ParseException {
		compileScript(f,
		              "Character testing=gsGetCharacter(\""+f.primaryHUD.character.getName()+
		              "\");  Integer ap=gsGetAbilityPoints(testing);");
		return new TestFrameworkPrototype.TestOutput(true,"Compiled okay hah!");
	}
	
	private static void compileScript(final TestFramework f,final String script) throws ParseException {
		final ByteArrayInputStream bais=new ByteArrayInputStream(script.getBytes());
		final GSCompiler compiler=new GSCompiler(new GSParser(bais).Start(),"Internal Test Script");
		compiler.compile(f.primaryHUD.state);
		new GSVM(compiler.toByteCode(f.primaryHUD.state)).execute(f.primaryHUD.state);
	}
}