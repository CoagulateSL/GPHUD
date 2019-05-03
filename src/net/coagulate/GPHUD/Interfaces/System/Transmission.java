package net.coagulate.GPHUD.Interfaces.System;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Cookies;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.GPHUD;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static java.util.logging.Level.*;

/**
 * Implements a callback transmission.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Transmission extends Thread {
	public static boolean debugspawn = false;
	String url;
	JSONObject json = null;
	JSONObject jsonresponse = null;
	int delay = 0;
	Char character = null;
	Region region = null;

	public Transmission(Char character, JSONObject json, String oldurl) {
		if (debugspawn) {
			System.out.println("Transmission to character " + character + " on url " + oldurl + " with json " + json.toString());
			Thread.dumpStack();
		}
		this.url = oldurl;
		this.json = json;
	}

	public Transmission(Char character, JSONObject json) {
		if (debugspawn) {
			System.out.println("Transmission to character " + character + " with json " + json.toString());
			Thread.dumpStack();
		}
		this.character = character;
		this.url = character.getURL();
		this.json = json;
	}

	public Transmission(Char character, JSONObject json, int i) {
		if (debugspawn) {
			System.out.println("Transmission to character " + character + " with json " + json.toString());
			Thread.dumpStack();
		}
		this.character = character;
		this.url = character.getURL();
		this.delay = i;
		this.json = json;
	}

	public Transmission(Region region, JSONObject message) {
		if (debugspawn) {
			System.out.println("Transmission to region " + region + " with json " + json.toString());
			Thread.dumpStack();
		}
		this.region = region;
		this.url = region.getURL();
		json = message;
	}

	public Transmission(Region region, JSONObject message, int i) {
		if (debugspawn) {
			System.out.println("Transmission to region " + region + " with json " + json.toString());
			Thread.dumpStack();
		}
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
		this.url = oldurl;
		json = message;
	}

	public Transmission(Char aChar, JSONObject ping, String url, int i) {
		if (debugspawn) {
			System.out.println("DELAYED Transmission to character " + aChar + " on url " + url + " with delay " + i + " and json " + json.toString());
			Thread.dumpStack();
		}
		this.json = ping;
		this.url = url;
		this.delay = i;
	}


	public JSONObject getResponse() { return jsonresponse; }

	// can call .start() to background run this, or .run() to async run inline/inthread
	@Override
	public void run() {
		boolean debug = false;
		if (delay > 0) {
			if (debug) { System.out.println("Delay " + delay); }
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
				if (incommand.equals("pong")) {
					if (j.has("callback")) { Char.refreshURL(j.getString("callback")); }
					if (j.has("callback")) { Region.refreshURL(j.getString("callback")); }
					if (j.has("cookie")) { Cookies.refreshCookie(j.getString("cookie")); }
				}
			} catch (Exception e) {
				GPHUD.getLogger().log(WARNING, "Exception in response parser", e);
			}
		}
	}

	private String sendAttempt() throws MalformedURLException, IOException {
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
		String response = "";
		while ((line = rd.readLine()) != null) {
			response += line + "\n";
		}
		if (debug) { GPHUD.getLogger().log(FINER, "Push: " + json.toString() + "\ngives " + response); }
		return response;
	}
}
