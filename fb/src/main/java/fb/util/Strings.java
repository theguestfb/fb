package fb.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.Cookie;

import com.google.gson.Gson;

import fb.Accounts;
import fb.Accounts.FBLoginException;
import fb.json.JsonCaptchaResponse;

public class Strings {
	
	public static boolean READ_ONLY_MODE = false; //set this value to the default (will revert to this value after restarts)

	public static final String BASE_DIR = "/opt/fb" + (Strings.READ_ONLY_MODE?"ro":""); // no trailing /
	
	public static final boolean RECAPTCHA = false;
	
	private static ConcurrentHashMap<String,String> files = new ConcurrentHashMap<>();
	
	private static ConcurrentHashMap<String,String> styles = new ConcurrentHashMap<>(); // <HTML name, css file name (without .css)>
	private static Object styleLock = new Object();
	
	public static String DOMAIN = readFile(BASE_DIR + "/domain.txt").trim();
	public static String SMTP_PASSWORD = readFile(BASE_DIR + "/smtp_password.txt");
	public static String RECAPTCHA_SECRET = readFile(BASE_DIR + "/recaptcha_secret.txt");
	
	public static String escape(String string) {
		return MyEscaper.escapeHtml4(string);
	}
	
	public static String getFile(String name, Cookie fbtoken) {
		String theme;
		try {
			theme = styles.get(Accounts.getUser(fbtoken).theme);
		} catch (FBLoginException e) {
			theme = null;
		}
		if (theme == null) theme = "default";
		return files.get(name).replace("$ACCOUNT", Accounts.getAccount(fbtoken)).replace("$STYLE", theme);
	}
	
	public static String readFile(String path) {
		return readFile(new File(path));
	}
	
	public static String readFile(File file) {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
			StringBuilder sb = new StringBuilder();
			int x;
			while ((x = in.read()) != -1) sb.append((char)x);
			
			if (file.getName().toLowerCase().contains("styles.txt")) try (Scanner scan = new Scanner(sb.toString())) { synchronized (styleLock){
				styles.clear();
				Strings.log("Updating styles");
				while (scan.hasNext()) {
					String key = scan.nextLine();
					String val = scan.nextLine();
					styles.put(key, val);
				}
			}}
			
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
		synchronized (logLock) {
			Calendar c = Calendar.getInstance();
			int y = c.get(Calendar.YEAR);
			int mo = c.get(Calendar.MONTH);
			int d = c.get(Calendar.DAY_OF_MONTH);
			int h = c.get(Calendar.HOUR_OF_DAY);
			int mi = c.get(Calendar.MINUTE);
			int s = c.get(Calendar.SECOND);
			try (BufferedWriter out = new BufferedWriter(new FileWriter(BASE_DIR + "/log.txt", true))) {
				out.write(String.format("%04d-%02d-%02d %02d:%02d:%02d %s", y, mo, d, h, mi, s, message));
				out.newLine();
				out.flush();
			} catch (IOException e) {
				System.err.printf("%04d-%02d-%02d %02d:%02d:%02d %s%n", y, mo, d, h, mi, s, "Could not open log file");
			} finally {
				System.out.printf("%04d-%02d-%02d %02d:%02d:%02d %s%n", y, mo, d, h, mi, s, message);
			}
		}
	}
	
	private static Object logLock = new Object();
	
	public static void log(Exception e) {
		ArrayList<String> lines = new ArrayList<>();
		try (StringWriter sw = new StringWriter()) {
			try (PrintWriter writer = new PrintWriter(sw)) {
				e.printStackTrace(writer);
			}
			try (Scanner s = new Scanner(sw.getBuffer().toString())) {
				while (s.hasNext()) lines.add(s.nextLine());
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			log(e.getMessage());
			log("Trouble logging previous exception's stack trace: " + ioe.getMessage());
		}
		synchronized (logLock) {
			for (String line : lines) log(line);
		}
	}
	
	private static void updateFile(File f) {
		log("Updating modified file " + f.getName());
		files.put(f.getName(), readFile(f));
	}
	static {
		initFiles();
	}
	private static void initFiles()  {
		File dirFile = firstRead(); // ALWAYS CALL THIS!
		startFileWatcher(dirFile); // only call this if you want file changes to be watched for and loaded (you probably do)
		startCommandWatcher(new File(BASE_DIR + "/commands"));
	}
	private static File firstRead() {
		File dirFile = new File(BASE_DIR + "/static_snippets");
		if (dirFile.exists()) {
			if (!dirFile.isDirectory()) {
				log(dirFile.getAbsolutePath() + " exists and is not a directory");
				System.exit(1);
				return null;
			}
		} else log("Directory " + dirFile.getAbsolutePath() + " does not exist");
		
		for (File file : dirFile.listFiles()) if (!file.getName().startsWith(".") ) {
			files.put(file.getName(), readFile(file));
			Strings.log("Loading file " + file.getName());
		}
		return dirFile;
	}
	
	private static void startFileWatcher(File dirFile) {
		WatchService watcher = null;
		try {
			watcher = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			log("Error creating watch service");
			return;
		}
		Path dir = dirFile.toPath();
		try {
			dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e1) {
			log("IOException registering dir with watcher");
			return;
		}
		final WatchService finalWatcher = watcher;
		Thread t = new Thread() {
			public void run() {
				WatchKey key = null;
				while (true) {
					try {
						key = finalWatcher.take();
					} catch (InterruptedException e) {
						log(e.getMessage());
						return;
					}
					for (WatchEvent<?> event : key.pollEvents()) {
						Kind<?> kind = event.kind();
						if (kind == StandardWatchEventKinds.ENTRY_MODIFY
								|| kind == StandardWatchEventKinds.ENTRY_CREATE) {
							File f = dir.resolve((Path) (event.context())).toFile();
							if (!f.getName().startsWith(".")) {
								log("MODIFY: " + f.getAbsolutePath());
								updateFile(f);
							}
						}
					}
					if (!key.reset())
						break;
				}
			}
		};
		t.setName("FileTrackerThread");
		t.start();
	}
	
	private static void startCommandWatcher(File dirFile) {
		if (!dirFile.exists()) dirFile.mkdirs();
		else if (!dirFile.isDirectory()) {
			Strings.log("Could not start command watcher, command dir is file " + dirFile);
			return;
		}
		WatchService watcher = null;
		try {
			watcher = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			log("Error creating watch service");
			return;
		}
		Path dir = dirFile.toPath();
		try {
			dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
		} catch (IOException e1) {
			log("IOException registering dir with watcher");
			return;
		}
		final WatchService finalWatcher = watcher;
		Thread t = new Thread() {
			public void run() {
				WatchKey key = null;
				while (true) {
					try {
						key = finalWatcher.take();
					} catch (InterruptedException e) {
						log(e.getMessage());
						return;
					}
					for (WatchEvent<?> event : key.pollEvents()) {
						File f = dir.resolve((Path) (event.context())).toFile();
						f.delete();
						if (!f.getName().startsWith(".")) {
							String command = f.getName().toLowerCase();
							switch(f.getName().toLowerCase()) {
							case "sessions":
								Accounts.logActiveSessions();
								break; 
							case "read_only_true":
								if (Strings.READ_ONLY_MODE == false) {
									Strings.log("Enabling read-only");
									Strings.READ_ONLY_MODE = true;
								} else Strings.log(command + ": read-only was already enabled");
								break;
							case "read_only_false":
								if (Strings.READ_ONLY_MODE == true) {
									Strings.log("Disabling read-only");
									Strings.READ_ONLY_MODE = false;
								} else Strings.log(command + ": read-only was already disabled");
								break;
							default:
								Strings.log("Unrecognized command: " + command);
								break;
							}
						}
					}
					if (!key.reset())
						break;
				}
			}
		};
		t.setName("CommandWatcherThread");
		t.start();
	}
	
	private static final DateFormat outputDate = new SimpleDateFormat("EEE, MMM d yyyy HH:mm:ss");
	public static String outputDateFormat(Date date) {
		String ret;
		synchronized(outputDate) {
			ret = outputDate.format(date);
		}
		return ret;
	}
	
	private static final ThreadLocal<DateFormat> sqlDate = new ThreadLocal<DateFormat>() {
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
	};	
			
	public static String sqlDateFormat(Date date) {
		return sqlDate.get().format(date);
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

	public static String getSelectThemes() {
		ArrayList<String> list = new ArrayList<>();
		synchronized (styleLock) {
			for (String theme : styles.keySet()) list.add(theme);
		}
		Collections.sort(list);
		StringBuilder sb = new StringBuilder();
		for (String theme : list) sb.append(String.format("<option value=\"%s\">%s</option>%n", theme, theme));
		return sb.toString();
	}
	
	public static void safeDeleteFileDirectory(String dirPath) {
		File f = new File(dirPath);
		if (f.exists()) {
			if (f.isDirectory()) {
				Path directory = Paths.get(dirPath);

				try {
					Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							Files.delete(file);
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							Files.delete(dir);
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					Strings.log("Error deleting directory " + dirPath);
					e.printStackTrace();
				}
			} else f.delete();
		}
	}

}
