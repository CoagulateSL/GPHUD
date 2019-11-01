package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import java.util.concurrent.ThreadLocalRandom;

public class Randomness {

	@GSFunctions.GSFunction(description = "Produces a random whole number in the range provided",
					notes = "",
					parameters = "(Integer minimum,Integer maximum) - smallest and largest numbers that can be returned by this function.",
					returns = "Integer - random number in the range (inclusive) minimum-maximum")
	public static BCInteger gsRand(State st, GSVM vm, BCInteger minimum,BCInteger maximum) {
		return new BCInteger(null,ThreadLocalRandom.current().nextInt(minimum.toInteger(), maximum.toInteger() + 1));
	}
}
