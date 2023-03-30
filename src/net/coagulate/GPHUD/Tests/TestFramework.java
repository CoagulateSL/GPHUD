package net.coagulate.GPHUD.Tests;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Tools.ClassTools;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SelfTest;
import net.coagulate.SL.TestFrameworkPrototype;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

public class TestFramework extends TestFrameworkPrototype {
	
	public static final String              instanceName   ="_TestInstance";
	public final        User                developer;
	public final        String              developerKey;
	public              VirtualHUD          primaryHUD     =null;
	public              VirtualHUD          secondaryHUD   =null;
	public              VirtualRegionServer primaryRegion  =null;
	public              VirtualRegionServer secondaryRegion=null;
	public              Instance            instance       =null;
	
	
	public TestFramework() {
		logger=GPHUD.getLogger("SelfTest");
		developer=User.get(1);
		if (developer==null) {
			throw new SystemConsistencyException("User ID 1 does not exist?.");
		}
		developerKey=developer.getDeveloperKey();
		if (developerKey==null) {
			throw new SystemConsistencyException(
					"User ID 1 does not have a developer key but must for correct operation of GPHUD (?).  Either that or fix this test.");
		}
	}
	
	public SelfTest.PassFailRecord execute() {
		try {
			preSetupTests();
			setup();
			tests();
		} catch (final IOException|RuntimeException odd) {
			logger.log(Level.SEVERE,"GPHUD testing threw an uncaught runtime exception",odd);
			results.record(new TestResult(false,"EXCEPTION","Uncaught Exception: "+odd));
		}
		try {
			teardown();
		} catch (final RuntimeException odd) {
			logger.log(Level.SEVERE,"GPHUD testing teardown threw an unexpected runtime exception",odd);
		}
		return results;
	}
	
	private void preSetupTests() {
		runTest("Check an OK thing",()->new TestOutput(true,"ok!"));
	}
	
	private void setup() throws IOException {
		primaryRegion=new VirtualRegionServer("Primary Region","_TestRegion1",this,developer,1);
		secondaryRegion=new VirtualRegionServer("Secondary Region","_TestRegion2",this,developer,2);
		primaryHUD=new VirtualHUD("Primary HUD",this,developer,primaryRegion);
		secondaryHUD=new VirtualHUD("Secondary HUD",this,User.findUsername("Kate Burner",false),secondaryRegion);
		// Sorry if you're debugging why your test instance complained here
		
		
		// start up the region servers
		
		primaryRegion.connect();
		primaryRegion.assertResponseAndDiscard("Check for failure to connect",
		                                       "error",
		                                       ">>> ERROR : Region not registered, only pre-registration commands may be run");
		primaryRegion.createInstance(instanceName);
		
		primaryRegion.assertResponseAndDiscard("Check for reboot response to instance creation",
		                                       "rebootserver",
		                                       "rebootserver");
		instance=Instance.find(instanceName);
		secondaryRegion.joinInstance(instanceName);
		secondaryRegion.assertResponseAndDiscard("Check for reboot response to instance joining",
		                                         "rebootserver",
		                                         "rebootserver");
		primaryRegion.connect();
		primaryRegion.assertResponseAndDiscard("Check for connection success","incommand","registered");
		secondaryRegion.connect();
		secondaryRegion.assertResponseAndDiscard("Check for connection success","incommand","registered");
		// give the server a second to run this
		try {
			//noinspection MagicNumber
			Thread.sleep(1000L);
		} catch (final InterruptedException ignore) {
		}
		primaryRegion.assertResponseAndDiscard("Check for connection callback success","incommand","registering");
		secondaryRegion.assertResponseAndDiscard("Check for connection callback success","incommand","registering");
		runTest("Assert region has logo set",
		        ()->new TestOutput(!primaryRegion.logo.isEmpty(),"Region logo is "+primaryRegion.logo));
		runTest("Assert region has logo set",
		        ()->new TestOutput(!secondaryRegion.logo.isEmpty(),"Region logo is "+primaryRegion.logo));
		
		// start up the HUDs
		
		primaryHUD.connect();
		runTest("Assert HUD has a character ID",
		        ()->new TestOutput(primaryHUD.charKey>0,"Recorded user key is "+primaryHUD.charKey));
		secondaryHUD.connect();
		runTest("Assert HUD has a character ID",
		        ()->new TestOutput(secondaryHUD.charKey>0,"Recorded user key is "+secondaryHUD.charKey));
		runTest("Assert different characters",
		        ()->new TestOutput(primaryHUD.charKey!=secondaryHUD.charKey,
		                           "User keys are different is "+(primaryHUD.charKey!=secondaryHUD.charKey)));
		
	}
	
	private void tests() {
		for (final Method method: ClassTools.getAnnotatedMethods(Test.class)) {
			runTest(method.getAnnotation(Test.class).name(),()->{
				try {
					return (TestOutput)method.invoke(null,this);
				} catch (final IllegalAccessException|InvocationTargetException e) {
					throw new SystemConsistencyException("Unexpected invocation error",e);
				}
			});
		}
	}
	
	private void teardown() {
		if (primaryHUD!=null) {
			primaryHUD.shutdown();
			primaryHUD=null;
		}
		if (secondaryHUD!=null) {
			secondaryHUD.shutdown();
			secondaryHUD=null;
		}
		if (primaryRegion!=null) {
			primaryRegion.shutdown();
			primaryRegion=null;
		}
		if (secondaryRegion!=null) {
			secondaryRegion.shutdown();
			secondaryRegion=null;
		}
		try {
			Instance.find(instanceName).delete();
		} catch (final Exception ignore) {
		}
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.METHOD)
	public @interface Test {
		// ---------- INSTANCE ----------
		@Nonnull String name();
	}
	
}