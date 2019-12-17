package net.coagulate.GPHUD.Interfaces.System;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.Core.Exceptions.SystemException;
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
	@Nonnull
	JSONObject json=new JSONObject();
	@Nonnull
	JSONObject jsonresponse=new JSONObject();
	@Nullable
	Objects object;
	int delay;
	@Nullable
	protected Char character;
	@Nullable
	Region region;
	boolean succeeded;
	public boolean failed() { return !succeeded; }
	@Nullable
	final
	StackTraceElement[] caller;
	public Transmission(@Nullable final Char character, @Nonnull final JSONObject json, @Nullable final String oldurl) {
		if (debugspawn) {
			System.out.println("Transmission to character " + character + " on url " + oldurl + " with json " + json);
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		this.character=character;
		url = oldurl;
		this.json = json;
	}

	public Transmission (@Nonnull final Objects obj, @Nonnull final JSONObject json) {
		if (debugspawn) {
			System.out.println("Transmission to object " + obj + " with json " + json);
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		object =obj;
		url =object.getURL();
		this.json=json;
	}

	public Transmission(@Nonnull final Char character, @Nonnull final JSONObject json) {
		if (debugspawn) {
			System.out.println("Transmission to character " + character + " with json " + json);
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		this.character = character;
		url = character.getURL();
		this.json = json;
	}

	public Transmission(@Nonnull final Char character, @Nonnull final JSONObject json, final int i) {
		if (debugspawn) {
			System.out.println("Transmission to character " + character + " with json " + json);
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		this.character = character;
		url = character.getURL();
		delay = i;
		this.json = json;
	}

	public Transmission(@Nonnull final Region region, @Nonnull final JSONObject message) {
		if (debugspawn) {
			System.out.println("Transmission to region " + region + " with json " + json);
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		this.region = region;
		url = region.getURL();
		json = message;
	}

	public Transmission(@Nonnull final Region region, @Nonnull final JSONObject message, final int i) {
		if (debugspawn) {
			System.out.println("Transmission to region " + region + " with json " + json);
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		this.region = region;
		url = region.getURL();
		delay = i;
		json = message;
	}

	public Transmission(final Region r, @Nonnull final JSONObject message, @Nullable final String oldurl) {
		if (debugspawn) {
			System.out.println("Transmission to region " + region + " on url " + oldurl + " with json " + json);
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		url = oldurl;
		json = message;
	}

	public Transmission(final Char aChar, @Nonnull final JSONObject ping, @Nullable final String url, final int i) {
		if (debugspawn) {
			System.out.println("DELAYED Transmission to character " + aChar + " on url " + url + " with delay " + i + " and json " + json);
			Thread.dumpStack();
		}
		caller=Thread.currentThread().getStackTrace();
		json = ping;
		this.url = url;
		delay = i;
	}


	@Nullable
	public JSONObject getResponse() { return jsonresponse; }

	// can call .start() to background run this, or .run() to async run inline/inthread
	@Override
	public void run() {
		try { runUnwrapped(); }
		catch (@Nonnull final Exception e) {
			Throwable step=e;
			int sanity=100;
			while (step.getCause()!=null && sanity>=0) {
				step=step.getCause();
				sanity--;
				if (sanity==0) { GPHUD.getLogger("Transmission").log(SEVERE,"Excess exception stepping in Transmission exception reporter"); }
			}
			if (step.getCause()==null) {
				final SystemException se = new SystemImplementationException("Transmission caller stack trace");
				if (caller!=null) { se.setStackTrace(caller); }
				step.initCause(se);
			}
			GPHUD.getLogger("Transmission").log(Level.WARNING,"Transmission threw exception from inner wrapper",e);

		}
	}
	public void runUnwrapped() {
		final boolean debug = false;
		if (delay > 0) {
			try { Thread.sleep(delay * 1000); } catch (@Nonnull final InterruptedException e) {}
		}
		int retries = 5;
		if (character != null) { character.appendConveyance(new net.coagulate.GPHUD.State(character), json); }
		String response = null;
		if (url == null || url.isEmpty()) { return; }
		while (response == null && retries > 0) {
			try {
				response = sendAttempt();
			} catch (@Nonnull final FileNotFoundException e) {
				GPHUD.getLogger().log(FINE, "404 on url, revoked connection while sending " + json);
				GPHUD.purgeURL(url);
				return;
			} catch (@Nonnull final MalformedURLException ex) {
				GPHUD.getLogger().log(WARNING, "MALFORMED URL: " + url + ", revoked connection while sending " + json);
				GPHUD.purgeURL(url);
				return;
			} catch (@Nonnull final IOException e) {
				retries--;
				GPHUD.getLogger().log(INFO, "IOException " + e.getMessage() + " retries=" + retries + " left");
				try { Thread.sleep(5 * 1000); } catch (@Nonnull final InterruptedException ee) {}
			}
		}
		if (response == null) {
			GPHUD.getLogger().log(WARNING, "Failed all retransmission attempts for " + json);
		}
		if (response != null && !response.isEmpty()) {
			try {
				final JSONObject j = new JSONObject(response);
				jsonresponse = j;
				final String incommand = j.optString("incommand", "");
				if ("pong".equals(incommand)) {
					if (j.has("callback")) { Char.refreshURL(j.getString("callback")); }
					if (j.has("callback")) { Region.refreshURL(j.getString("callback")); }
					if (j.has("cookie")) { Cookies.refreshCookie(j.getString("cookie")); }
				}
			} catch (@Nonnull final Exception e) {
				GPHUD.getLogger().log(WARNING, "Exception in response parser",e);
				final StringBuilder body= new StringBuilder(url + "\n<br>\n");
				body.append("Character:").append(character == null ? "null" : character.getNameSafe()).append("\n<br>\n");
				if (caller!=null) {
					for (final StackTraceElement ele : caller) {
						body.append("Caller: ").append(ele.getClassName()).append("/").append(ele.getMethodName()).append(":").append(ele.getLineNumber()).append("\n<br>\n");
					}
				}
				body.append(response);
				try { MailTools.mail("Failed response", body.toString()); } catch (@Nonnull final MessagingException ee){
					GPHUD.getLogger().log(SEVERE,"Mail exception in response parser exception handler",ee);
				}
			}
		}
		succeeded=true;
	}

	@Nonnull
	private String sendAttempt() throws IOException {
		final boolean debug = false;
		if (url==null) { throw new IOException("Target URL is nulL"); }
		final URLConnection transmission = new URL(url).openConnection();
		transmission.setDoOutput(true);
		transmission.setAllowUserInteraction(false);
		transmission.setDoInput(true);
		transmission.setConnectTimeout(5000);
		transmission.setReadTimeout(35000);
		transmission.connect();

		final OutputStreamWriter out = new OutputStreamWriter(transmission.getOutputStream());
		out.write(json + "\n");
		out.flush();
		out.close();
		final BufferedReader rd = new BufferedReader(new InputStreamReader(transmission.getInputStream()));
		String line;
		final StringBuilder response = new StringBuilder();
		while ((line = rd.readLine()) != null) {
			response.append(line).append("\n");
		}
		return response.toString();
	}
}
