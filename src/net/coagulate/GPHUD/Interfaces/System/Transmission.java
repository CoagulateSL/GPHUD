package net.coagulate.GPHUD.Interfaces.System;

import net.coagulate.Core.Tools.MailTools;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Cookies;
import net.coagulate.GPHUD.Data.Objects;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.GPHUD;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

import static java.util.logging.Level.*;

/**
 * Implements a callback transmission.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Transmission extends Thread {
	public static final boolean debugspawn = false;
	@Nullable
	final String url;
	@Nullable
	JSONObject json = null;
	@Nullable
	JSONObject jsonresponse = null;
	@Nullable
	Objects object=null;
	int delay = 0;
	@Nullable
	protected Char character = null;
	@Nullable
	Region region = null;
	boolean succeeded=false;
	public boolean failed() { return !succeeded; }
	@Nullable
	StackTraceElement[] caller=null;
	public Transmission(Char character, @Nonnull JSONObject json, String oldurl) {
		if (debugspawn) {
			System.out.println("Transmission to character " + character + " on url " + oldurl + " with json " + json.toString());
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		this.character=character;
		this.url = oldurl;
		this.json = json;
	}

	public Transmission (Objects obj, @Nonnull JSONObject json) {
		if (debugspawn) {
			System.out.println("Transmission to object " + obj + " with json " + json.toString());
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		this.object=obj;
		this.url=object.getURL();
		this.json=json;
	}

	public Transmission(@Nonnull Char character, @Nonnull JSONObject json) {
		if (debugspawn) {
			System.out.println("Transmission to character " + character + " with json " + json.toString());
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		this.character = character;
		this.url = character.getURL();
		this.json = json;
	}

	public Transmission(@Nonnull Char character, @Nonnull JSONObject json, int i) {
		if (debugspawn) {
			System.out.println("Transmission to character " + character + " with json " + json.toString());
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		this.character = character;
		this.url = character.getURL();
		this.delay = i;
		this.json = json;
	}

	public Transmission(@Nonnull Region region, JSONObject message) {
		if (debugspawn) {
			System.out.println("Transmission to region " + region + " with json " + json.toString());
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		this.region = region;
		this.url = region.getURL();
		json = message;
	}

	public Transmission(@Nonnull Region region, JSONObject message, int i) {
		if (debugspawn) {
			System.out.println("Transmission to region " + region + " with json " + json.toString());
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		this.region = region;
		this.url = region.getURL();
		this.delay = i;
		json = message;
	}

	public Transmission(Region r, JSONObject message, String oldurl) {
		if (debugspawn) {
			System.out.println("Transmission to region " + region + " on url " + oldurl + " with json " + json.toString());
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		this.url = oldurl;
		json = message;
	}

	public Transmission(Char aChar, JSONObject ping, String url, int i) {
		if (debugspawn) {
			System.out.println("DELAYED Transmission to character " + aChar + " on url " + url + " with delay " + i + " and json " + json.toString());
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		this.json = ping;
		this.url = url;
		this.delay = i;
	}


	@Nullable
	public JSONObject getResponse() { return jsonresponse; }

	// can call .start() to background run this, or .run() to async run inline/inthread
	@Override
	public void run() {
		try { runUnwrapped(); }
		catch (Exception e) {
			Throwable step=e;
			int sanity=100;
			while (step.getCause()!=null && sanity>=0) {
				step=step.getCause();
				sanity--;
				if (sanity==0) { GPHUD.getLogger("Transmission").log(SEVERE,"Excess exception stepping in Transmission exception reporter"); }
			}
			if (step.getCause()==null) {
				SystemException se = new SystemException("Transmission caller stack trace");
				se.setStackTrace(caller);
				step.initCause(se);
			}
			GPHUD.getLogger("Transmission").log(Level.WARNING,"Transmission threw exception from inner wrapper",e);

		}
	}
	public void runUnwrapped() {
		boolean debug = false;
		if (delay > 0) {
			try { Thread.sleep(delay * 1000); } catch (InterruptedException e) {}
		}
		int retries = 5;
		if (character != null) { character.appendConveyance(new net.coagulate.GPHUD.State(character), json); }
		String response = null;
		if (url == null || json == null || url.isEmpty()) { return; }
		while (response == null && retries > 0) {
			try {
				response = sendAttempt();
			} catch (FileNotFoundException e) {
				GPHUD.getLogger().log(FINE, "404 on url, revoked connection while sending " + json.toString());
				GPHUD.purgeURL(url);
				return;
			} catch (MalformedURLException ex) {
				GPHUD.getLogger().log(WARNING, "MALFORMED URL: " + url + ", revoked connection while sending " + json.toString());
				GPHUD.purgeURL(url);
				return;
			} catch (IOException e) {
				retries--;
				GPHUD.getLogger().log(INFO, "IOException " + e.getMessage() + " retries=" + retries + " left");
				try { Thread.sleep(5 * 1000); } catch (InterruptedException ee) {}
			}
		}
		if (response == null) {
			GPHUD.getLogger().log(WARNING, "Failed all retransmission attempts for " + json.toString());
		}
		if (response != null && !response.isEmpty()) {
			try {
				JSONObject j = new JSONObject(response);
				jsonresponse = j;
				String incommand = j.optString("incommand", "");
				if ("pong".equals(incommand)) {
					if (j.has("callback")) { Char.refreshURL(j.getString("callback")); }
					if (j.has("callback")) { Region.refreshURL(j.getString("callback")); }
					if (j.has("cookie")) { Cookies.refreshCookie(j.getString("cookie")); }
				}
			} catch (Exception e) {
				GPHUD.getLogger().log(WARNING, "Exception in response parser",e);
				StringBuilder body= new StringBuilder(url + "\n<br>\n");
				body.append("Character:").append(character == null ? "null" : character.getNameSafe()).append("\n<br>\n");
				for (StackTraceElement ele:caller) {
					body.append("Caller: ").append(ele.getClassName()).append("/").append(ele.getMethodName()).append(":").append(ele.getLineNumber()).append("\n<br>\n");
				}
				body.append(response);
				try { MailTools.mail("Failed response", body.toString()); } catch (MessagingException ee){
					GPHUD.getLogger().log(SEVERE,"Mail exception in response parser exception handler",ee);
				}
			}
		}
		succeeded=true;
	}

	@Nonnull
	private String sendAttempt() throws IOException {
		boolean debug = false;
		URLConnection transmission = new URL(url).openConnection();
		transmission.setDoOutput(true);
		transmission.setAllowUserInteraction(false);
		transmission.setDoInput(true);
		transmission.setConnectTimeout(5000);
		transmission.setReadTimeout(35000);
		transmission.connect();

		OutputStreamWriter out = new OutputStreamWriter(transmission.getOutputStream());
		out.write(json.toString() + "\n");
		out.flush();
		out.close();
		BufferedReader rd = new BufferedReader(new InputStreamReader(transmission.getInputStream()));
		String line;
		StringBuilder response = new StringBuilder();
		while ((line = rd.readLine()) != null) {
			response.append(line).append("\n");
		}
		return response.toString();
	}
}
