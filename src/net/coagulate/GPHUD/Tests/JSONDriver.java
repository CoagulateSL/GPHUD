package net.coagulate.GPHUD.Tests;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemInitialisationException;
import net.coagulate.Core.Exceptions.System.SystemRemoteFailureException;
import net.coagulate.Core.Tools.ByteTools;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.SL.Config;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.TestFrameworkPrototype;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerMapper;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A generic GPHUD JSON client, used to emulate a region server and HUD elements
 */
public abstract class JSONDriver extends Thread {
	private static final boolean           DEBUG_DUMP_JSON_RESPONSES=false;
	public final         UUID              objectKey                =UUID.randomUUID();
	public final         User              owner;
	/** The initating TestFramework */
	protected final      TestFramework     master;
	/** The name of this HTTP Client */
	protected final      String            name;
	/** The URL we can tell the server to use to contact us */
	private final        String            url;
	private final        Queue<JSONObject> responses                =new LinkedList<>();
	private              HttpServer        server;
	
	protected JSONDriver(final String name,final TestFramework caller,final User owner) throws IOException {
		final ServerSocket socket=new ServerSocket(0);
		/** Our claimed server socket port */
		final int port=socket.getLocalPort();
		socket.close();
		url="http://127.0.0.1:"+port+"/";
		master=caller;
		this.name=name;
		this.owner=owner;
		final SocketConfig reuse=SocketConfig.custom()
		                                     .setBacklogSize(100)
		                                     .setSoTimeout(60000)
		                                     .setTcpNoDelay(true)
		                                     .setSoReuseAddress(true)
		                                     .build();
		final ServerBootstrap bootstrap=ServerBootstrap.bootstrap()
		                                               .setListenerPort(port)
		                                               .setSocketConfig(reuse)
		                                               .setServerInfo("CoagulateSelfTestHTTP/1.1")
		                                               .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE)
		                                               .setHandlerMapper(new URLMapper());
		server=bootstrap.create();
		if (server==null) {
			throw new SystemInitialisationException("Server bootstrap is null?");
		}
		server.start();
	}
	
	public void assertResponseAndDiscard(final String test,final String key,final String value) {
		assertResponse(test,key,value);
		discardResponse();
	}
	
	public void assertResponse(final String test,final String key,final String value) {
		final JSONObject json=peek();
		if (json.getString(key).equals(value)) {
			master.result(true,test,"Response "+key+" matches "+value);
		} else {
			master.result(false,test,"Response "+key+" expected "+value+" but got "+json.getString(key));
		}
	}
	
	public void discardResponse() {
		responses.remove();
	}
	
	@Nonnull
	public JSONObject peek() {
		final JSONObject peeked=responses.peek();
		if (peeked==null) {
			throw new SystemConsistencyException("Peeked at a null JSON");
		}
		return peeked;
	}
	
	public TestFrameworkPrototype.TestOutput testResultPresenceAndDiscard(final String key,final boolean showValue) {
		final TestFrameworkPrototype.TestOutput result=testResultPresence(key,showValue);
		discardResponse();
		return result;
	}
	
	public TestFrameworkPrototype.TestOutput testResultPresence(final String key,final boolean showValue) {
		final JSONObject json=peek();
		if (!showValue) {
			return new TestFrameworkPrototype.TestOutput(json.has(key),
			                                             json.has(key)?
			                                             "Key "+key+" present at "+json.getString(key).length()+" len":
			                                             "Key "+key+" is not present");
		}
		return new TestFrameworkPrototype.TestOutput(json.has(key),
		                                             json.has(key)?"Key "+key+" content "+json.getString(key):
		                                             "Key "+key+" is not present");
	}
	
	public void shutdown() {
		final HttpServer ourserver=server;
		server=null;
		ourserver.shutdown(1,TimeUnit.SECONDS);
	}
	
	public void execute(final String command) throws IOException {
		execute(command,new JSONObject());
	}
	
	public void execute(final String command,final JSONObject payload) throws IOException {
		final Map<String,String> headers=new HashMap<>();
		headers.put("X-SecondLife-Owner-Name",owner.getUsername());
		headers.put("X-SecondLife-Owner-Key",owner.getUUID());
		addItemSpecificHeaders(headers);
		headers.put("X-SecondLife-Object-Name",name);
		headers.put("X-SecondLife-Shard","Production");
		headers.put("X-SecondLife-Local-Position","<128,128,100>");
		payload.put("command",command);
		payload.put("callback",getOurURL());
		payload.put("url",getOurURL());
		payload.put("developerkey",master.developerKey);
		payload.put("protocol",5);
		final JSONObject reply=new JSONObject(new Transmission(headers,
		                                                       payload,
		                                                       "http://127.0.0.1:"+Config.getPort()+
		                                                       "/GPHUD/system").sendAttempt());
		queue(reply);
	}
	
	protected abstract void addItemSpecificHeaders(final Map<String,String> headers);
	
	protected String getOurURL() {
		return url;
	}
	
	private void queue(final JSONObject json) {
		if (inputScraper(json)) {
			responses.add(json);
		}
		if (DEBUG_DUMP_JSON_RESPONSES) {
			master.logger.finest("JSON to "+name+": "+json.toString());
		}
	}
	
	/**
	 * Used for sub types to scrape inputs.
	 *
	 * @param json The input
	 * @return True to queue this, false to discard it
	 */
	protected boolean inputScraper(final JSONObject json) {
		return true;
	}
	
	private class URLHandler implements HttpRequestHandler {
		@Override
		public void handle(final HttpRequest request,
		                   final HttpResponse response,
		                   final HttpContext context) throws HttpException, IOException {
			if (request instanceof final HttpEntityEnclosingRequest r) {
				//final int inputSize=(int)r.getEntity().getContentLength();
				final JSONObject json=new JSONObject(ByteTools.convertStreamToString(r.getEntity().getContent()));
				queue(json);
			} else {
				master.error("Unexpected non entity post message from server",
				             new SystemRemoteFailureException("Oops!"));
			}
		}
	}
	
	@SuppressWarnings("ReturnOfInnerClass")
	private class URLMapper implements HttpRequestHandlerMapper {
		
		@Override
		public HttpRequestHandler lookup(final HttpRequest httpRequest) {
			return new URLHandler();
		}
	}
}