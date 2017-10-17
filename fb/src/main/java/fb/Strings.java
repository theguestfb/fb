package fb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import com.google.gson.Gson;

import fb.json.JsonCaptchaResponse;

public class Strings {
	
	public static String RECAPTCHA_SECRET = readFile("/opt/fb/recaptcha_secret.txt");
	public static String modifyPasswordhash = readFile("/opt/fb/modify_pass.txt");
	
	public static String welcomeDefault = readFile("/opt/fb/static_snippets/welcome.html");
	public static String storyDefault = readFile("/opt/fb/static_snippets/story.html");
	public static String addFormDefault = readFile("/opt/fb/static_snippets/addform.html");
	public static String successDefault = readFile("/opt/fb/static_snippets/success.html");
	public static String failureDefault = readFile("/opt/fb/static_snippets/failure.html");
	public static String recentsDefault = readFile("/opt/fb/static_snippets/recents.html");
	public static String modifyFormDefault = readFile("/opt/fb/static_snippets/modifyform.html");
		
	public static String readFile2(String path) {
		try {
			StringBuilder sb = new StringBuilder();
			Scanner f = new Scanner(new File(path));
			while (f.hasNextLine()) sb.append(f.nextLine() + "\n");
			f.close();
			return sb.toString();
		} catch (IOException e) {
			return "Not found";
		}
	}
	
	public static String readFile(String path) {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path)))) {
			StringBuilder sb = new StringBuilder();
			int x;
			while ((x = in.read()) != -1) sb.append((char)x);
			return sb.toString();
		} catch (IOException e) {
			return "Not found";
		}
	}
	
	/**
	 * Prepends message with the current date, and writes it to stdout
	 * @param message
	 */
	public static void log(String message) {
		int y = Calendar.getInstance().get(Calendar.YEAR);
		int mo = Calendar.getInstance().get(Calendar.MONTH);
		int d = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		int mi = Calendar.getInstance().get(Calendar.MINUTE);
		int s = Calendar.getInstance().get(Calendar.SECOND);
		try (BufferedWriter out = new BufferedWriter(new FileWriter("/opt/fb/log.txt", true))) {
			out.write(String.format("%04d-%02d-%02d %02d:%02d:%02d %s", y, mo, d, h, mi, s, message));
			out.newLine();
			out.flush();
		} catch (IOException e) {
			System.err.printf("%04d-%02d-%02d %02d:%02d:%02d %s%n", y, mo, d, h, mi, s, "Could not open log file");
		} finally {
			System.out.printf("%04d-%02d-%02d %02d:%02d:%02d %s%n", y, mo, d, h, mi, s, message);
		}
	}
	
	private static void updateFile(File f) {
		switch (f.getName()) {
		case "welcome.html" :
			welcomeDefault = readFile("/opt/fb/static_snippets/welcome.html");
			break;
		case "story.html":
			storyDefault = readFile("/opt/fb/static_snippets/story.html");
			break;
		case "addform.html":
			addFormDefault = readFile("/opt/fb/static_snippets/addform.html");
			break;
		case "success.html":
			successDefault = readFile("/opt/fb/static_snippets/success.html");
			break;
		case "failure.html":
			failureDefault = readFile("/opt/fb/static_snippets/failure.html");
			break;
		case "recents.html":
			recentsDefault = readFile("/opt/fb/static_snippets/recents.html");
			break;
		case "modifyform.html":
			modifyFormDefault = readFile("/opt/fb/static_snippets/modifyform.html");
			break;
		}
	}
	
	static {
		new Thread() {
			public void run() {
				init();
			}
		}.start();
	}
	private static void init()  {
		WatchService watcher = null;
		try {
			watcher = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			log("Error creating watch service");
			return;
		}
		File dirFile = new File("/opt/fb/static_snippets");
		if (dirFile.exists()) {
			if (!dirFile.isDirectory()) {
				log(dirFile.getAbsolutePath() + " exists and is not a directory");
				return;
			}
		} else log("Directory " + dirFile.getAbsolutePath() + " does not exist");
		Path dir = dirFile.toPath();
		try {
			dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e1) {
			log("IOException registering dir with watcher");
			return;
		}
		WatchKey key = null;
		while (true) {
			try {
				key = watcher.take();
			} catch (InterruptedException e) {
				log(e.getMessage());
				return;
			}
			for (WatchEvent<?> event : key.pollEvents()) {
				Kind<?> kind = event.kind();
				if (kind == StandardWatchEventKinds.ENTRY_MODIFY || kind == StandardWatchEventKinds.ENTRY_CREATE) {
					File f = dir.resolve((Path) (event.context())).toFile();
					if (!f.getName().startsWith(".")) {
						log("MODIFY: " + f.getAbsolutePath());
						updateFile(f);
					}
				}
			}
			if (!key.reset()) break;
		}
	}

	/**
	 * Check a recaptcha response token
	 * @return "true" if user passed, "false" if user failed, anything else indicated an error
	 */
	public static String checkGoogle(String google) {
		URL url;
		try {
			url = new URL("https://www.google.com/recaptcha/api/siteverify");
		} catch (MalformedURLException e1) {
			Strings.log("MalformedURLException? Really? wtf " + e1.getMessage());
			return "Tell Phoenix you got recaptcha MalformedURLException";
		}
		Map<String, String> params = new LinkedHashMap<>();
		params.put("secret", Strings.RECAPTCHA_SECRET);
		params.put("response", google);
		StringBuilder postData = new StringBuilder();
		byte[] postDataBytes;
		try {
			for (Map.Entry<String, String> param : params.entrySet()) {
				if (postData.length() != 0)
					postData.append('&');
				postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				postData.append('=');
				postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
			postDataBytes = postData.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			Strings.log("UnsupportedEncodingException? Really? wtf " + e.getMessage());
			return "Tell Phoenix you got recaptcha UnsupportedEncodingException";
		}
		HttpURLConnection conn;
		try {
			conn = (HttpURLConnection) url.openConnection();
		} catch (IOException e2) {
			Strings.log("IOException1? Really? wtf " + e2.getMessage());
			return "Tell Phoenix you got recaptcha IOException1";
		}
		try {
			conn.setRequestMethod("POST");
		} catch (ProtocolException e) {
			Strings.log("ProtocolException? Really? wtf " + e.getMessage());
			return "Tell Phoenix you got recaptcha ProtocolException";
		}
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
		conn.setDoOutput(true);
		try {
			conn.getOutputStream().write(postDataBytes);
		} catch (IOException e1) {
			Strings.log("IOException2? Really? wtf " + e1.getMessage());
			return "Tell Phoenix you got recaptcha IOException2";
		}
		Reader in;
		try {
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			Strings.log("UnsupportedEncodingException2? Really? wtf " + e.getMessage());
			return "Tell Phoenix you got recaptcha UnsupportedEncodingException2";
		} catch (IOException e) {
			Strings.log("IOException3? Really? wtf " + e.getMessage());
			return "Tell Phoenix you got recaptcha IOException3";
		}

		StringBuilder json = new StringBuilder();
		try {
			for (int c; (c = in.read()) >= 0;)
				json.append((char) c);
		} catch (IOException e) {
			Strings.log("IOException3? Really? wtf " + e.getMessage());
			return "Tell Phoenix you got a IOException4 when adding an episode";
		}
		JsonCaptchaResponse response = new Gson().fromJson(json.toString(), JsonCaptchaResponse.class);
		return response.getSuccess()?"true":"false";
	}
}
