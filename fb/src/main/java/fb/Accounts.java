package fb;

import static fb.util.Strings.escape;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.Cookie;

import org.apache.commons.validator.routines.EmailValidator;
import org.mindrot.jbcrypt.BCrypt;

import com.google.gson.Gson;

import fb.db.DB;
import fb.db.DB.DBException;
import fb.objects.Episode;
import fb.objects.FlaggedEpisode;
import fb.objects.User;
import fb.util.Strings;

public class Accounts {
	private static ConcurrentHashMap<String,UserSession> active = new ConcurrentHashMap<>(); //<loginToken>, user>
	private static ConcurrentHashMap<String,PotentialUser> createQueue = new ConcurrentHashMap<>(); //<createToken, user>
	private static ConcurrentHashMap<String,EmailChange> emailChangeQueue = new ConcurrentHashMap<>(); //<changeToken, EmailChange>
	
	private static final String tempPath =System.getProperty("user.home") + "/fbtemp";
	private static final String sessionPath = tempPath + "/sessions/";
	private static final String emailQueuePath = tempPath + "/emailchanges/";
	private static final String potentialUsersPath = tempPath + "/unverifiedusers/";
	
	public static void writeQueuesToFile() {
		Strings.log("Writing queues to file");
		new File(emailQueuePath).mkdirs();
		new File(sessionPath).mkdirs();
		new File(potentialUsersPath).mkdirs();
		for (Entry<String,EmailChange> entry : emailChangeQueue.entrySet()) {
			try (BufferedWriter out = new BufferedWriter(new FileWriter(emailQueuePath + entry.getKey()))) {
				out.write(new Gson().toJson(entry.getValue()));
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
				Strings.log("Error writing email change queue: " + e.getMessage());
			}
		}	
		for (Entry<String,PotentialUser> entry : createQueue.entrySet()) {
			try (BufferedWriter out = new BufferedWriter(new FileWriter(potentialUsersPath + entry.getKey()))) {
				out.write(new Gson().toJson(entry.getValue()));
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
				Strings.log("Error writing potential users queue: " + e.getMessage());
			}
		}
		for (Entry<String,UserSession> entry : active.entrySet()) {
			try (BufferedWriter out = new BufferedWriter(new FileWriter(sessionPath + entry.getKey()))) {
				out.write(new Gson().toJson(entry.getValue()));
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
				Strings.log("Error writing user sessions queue: " + e.getMessage());
			}
		}
		Strings.log("Done writing queues to file");
	}
	
	private static void readQueuesFromFile() {
		Strings.log("Reading queues from file");
		File dir = new File(sessionPath);
		if (dir.exists()) {
			if (dir.isDirectory()) {
				for (File f : dir.listFiles()) {
					UserSession sesh = new Gson().fromJson(Strings.readFile(f), UserSession.class);
					String token = f.getName();
					active.put(token, sesh);
				}
			} else Strings.log("Session directory " + sessionPath + " exists but is a file");
		} else Strings.log("Session directory " + sessionPath + " does not exist");
		
		dir = new File(emailQueuePath);
		if (dir.exists()) {
			if (dir.isDirectory()) {
				for (File f : dir.listFiles()) {
					EmailChange ec = new Gson().fromJson(Strings.readFile(f), EmailChange.class);
					String token = f.getName();
					emailChangeQueue.put(token, ec);
				}
			} else Strings.log("Email queue directory " + emailQueuePath + " exists but is a file");
		} else Strings.log("Email queue directory " + emailQueuePath + " does not exist");
		
		dir = new File(potentialUsersPath);
		if (dir.exists()) {
			if (dir.isDirectory()) {
				for (File f : dir.listFiles()) {
					PotentialUser user = new Gson().fromJson(Strings.readFile(f), PotentialUser.class);
					String token = f.getName();
					createQueue.put(token, user);
				}
			} else Strings.log("Potential users directory " + potentialUsersPath + " exists but is a file");
		} else Strings.log("Potential users directory " + potentialUsersPath + " does not exist");
		Strings.safeDeleteFileDirectory(tempPath);
		Strings.log("Done reading queues from file");
	}
	
	public static void logActiveSessions() {
		Strings.log("Active login sessions:");
		for (UserSession sesh : active.values()) {
			User user;
			try {
				user = DB.getUser(sesh.userID);
			} catch (DBException e) {
				Strings.log("Couldn't find user with id " + sesh.userID);
				continue;
			}
			Strings.log(user.id + " " + user.author + " " + user.email + " " + sesh.lastActive);
		}
		Strings.log("That's all folks");
	}
	
	public static void bump() {
		// Intentionally empty, used to force initAccounts to start
	}
	
	/*
	 * Scan the active sessions and createQueue maps for expired
	 */
	static {
		initAccounts();
	}
	
	private static void initAccounts() {
		readQueuesFromFile();
		Thread t = new Thread() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(1000 * 60 * 60);// run every hour
					} catch (InterruptedException e) {
						Strings.log("session prunning thread interrupted " + e.getMessage());
					}
					ArrayList<String> deleteTheseTokens = new ArrayList<>();
					for (String createToken : createQueue.keySet()) {
						Date now = new Date();
						Date then = createQueue.get(createToken).date;
						if (then == null) deleteTheseTokens.add(createToken);
						else {
							double hours = ((double) (now.getTime() - then.getTime())) / (1000.0 * 60.0 * 60.0);
							if (hours > 24) deleteTheseTokens.add(createToken); // expires after 24 hours
						}
					}
					for (String createToken : deleteTheseTokens) createQueue.remove(createToken);

					deleteTheseTokens = new ArrayList<>();
					for (String loginToken : active.keySet()) {
						Date now = new Date();
						Date then = active.get(loginToken).lastActive();
						double hours = ((double) (now.getTime() - then.getTime())) / (1000.0 * 60.0 * 60.0);
						if (hours > 24*7) deleteTheseTokens.add(loginToken); // expires after 7 days
					}
					for (String loginToken : deleteTheseTokens) active.remove(loginToken);
					
					deleteTheseTokens = new ArrayList<>();
					for (String changeToken : emailChangeQueue.keySet()) {
						Date now = new Date();
						Date then = emailChangeQueue.get(changeToken).date;
						double hours = ((double) (now.getTime() - then.getTime())) / (1000.0 * 60.0 * 60.0);
						if (hours > 24) deleteTheseTokens.add(changeToken); // expires after 24 hours
					}
					for (String changeToken : deleteTheseTokens) emailChangeQueue.remove(changeToken);
				}
			}
		};
		t.setName("AccountTrackerThread");
		t.start();
	}
	
	public static class UserSession { 
		public final String userID;
		private Date lastActive;
		public UserSession(String userID) {
			this.userID = userID;
			this.lastActive = new Date();
		}
		public Date lastActive() {
			return lastActive;
		}
		public void ping() {
			lastActive = new Date();
		}
	}
	
	private static class EmailChange {
		public final String userID;
		public final String newEmail;
		public final Date date;
		public EmailChange(String userID, String newEmail) {
			this.userID = userID;
			this.newEmail = newEmail;
			date = new Date();
		}
	}
	
	public static class PotentialUser {
		public final String username;
		public final String email;
		public final String passwordHash;
		public final String author;
		public final Date date;
		public PotentialUser(String username, String email, String passwordHash, String author) {
			this.username = username;
			this.email = email;
			this.passwordHash = passwordHash;
			this.author = author;
			this.date = new Date();
		}
	}
	
	/**
	 * Get HTML account stuff (click here to log in, or go to your user page)
	 * @param token
	 * @return HTML
	 */
	public static String getAccount(Cookie fbtoken) {
		if (Strings.READ_ONLY_MODE) return "";
		String notLoggedIn = "<div class=\"loginstuff\"><a href=/fb/createaccount>Create account</a><br/><a href=/fb/login>Log in</a></div>";
		if (fbtoken == null) return notLoggedIn;
		String token = fbtoken.getValue();
		
		if (token == null) return notLoggedIn;
		
		if (token.length() == 0) return notLoggedIn;
		
		UserSession sesh = active.get(token);
		if (sesh == null) return notLoggedIn;
		
		
		User user;
		try {
			user = DB.getUser(sesh.userID);
			System.out.println(token + ": " + user.author);
		} catch (DBException e) {
			System.out.println(token + " not logged in");
			return notLoggedIn;
		}
		
		String response = "Logged in as <a href=/fb/useraccount>" + escape(user.author) + "</a><br/><a href=/fb/logout>Log out</a>";
		if (user.level>=(byte)100) response +="<br/><a href=/fb/admin>Admin stuff</a>";
		if (user.level>=(byte)10) response +="<br/><a href=/fb/flagqueue>Flagged episodes</a>";
		response+="<br/>";
		if (user.level != ((byte)1)) response+="<br/><a href=/fb/becomenormal>Become a normal user</a>";
		if (user.level != ((byte)10)) response+="<br/><a href=/fb/becomemod>Become a moderator</a>";
		if (user.level != ((byte)100)) response+="<br/><a href=/fb/becomeadmin>Become an admin</a>";
		
		return "<div class=\"loginstuff\">" + response + "</div>";
	}
	/**
	 * 
	 * @param id user id
	 * @param fbtoken
	 * @return HTML user page for id
	 */
	public static String getUserPage(String id, Cookie fbtoken) {
		User user;
		if (id == null || id.length() == 0) return Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "User ID " + id + " does not exist");
		id = id.toLowerCase();
		try {
			user = DB.getUser(id);
		} catch (DBException e) {
			return Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "User ID " + id + " does not exist");
		}
		StringBuilder sb = new StringBuilder();
		for (Episode ep : user.episodes) {
			String story = "";
			switch (ep.id.split("-")[0]) {
			case "1":
				story = "(Forum)";
				break;
			case "2":
				story = "(You Are What You Wish)";
				break;
			case "3":
				story = "(Altered Fates)";
				break;
			case "4":
				story = "(The Future of Gaming)";
				break;
			}
			sb.append("<a href=/fb/get/" + ep.id + ">" + escape(ep.title) + "</a> " + Strings.outputDateFormat(ep.date) + " " + story + "<br/>");
		}
		String bio = Story.formatBody(user.bio);
		return Strings.getFile("profilepage.html", fbtoken).replace("$AUTHOR", user.author).replace("$BODY", bio).replace("$EPISODES", sb.toString());
	}
	
	public static String becomeNormal(Cookie token) {
		try {
			User user = getUser(token);
			DB.changeUserLevel(user.id, (byte)1);
			return Strings.getFile("generic.html", token).replace("$EXTRA", "You are now just a regular user.");
		} catch (DBException | FBLoginException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", "You must be logged in to do that");
		}
	}
	
	public static String becomeMod(Cookie token) {
		try {
			User user = getUser(token);
			DB.changeUserLevel(user.id, (byte)10);
			return Strings.getFile("generic.html", token).replace("$EXTRA", "You are now a moderator.<br/>Please only use your power for testing, not abuse.");
		} catch (DBException | FBLoginException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", "You must be logged in to do that");
		}
	}
	
	public static String becomeAdmin(Cookie token) {
		try {
			User user = getUser(token);
			DB.changeUserLevel(user.id, (byte)100);
			return Strings.getFile("generic.html", token).replace("$EXTRA", "You are now an admin.<br/>Please only use your power for testing, not abuse.");
		} catch (DBException | FBLoginException e) {
			return Strings.getFile("generic.html", token).replace("$EXTRA", "You must be logged in to do that");
		}
	}
	
	/**
	 * Get DBUser object from session token, or null if sesh does not exist
	 * @param token
	 * @return
	 * @throws DBException if id does not exist
	 * @throws FBLoginException if not logged in
	 */
	public static User getUser(Cookie token) throws FBLoginException {
		if (token == null) throw new FBLoginException(""); // return null;
		UserSession sesh = active.get(token.getValue());
		if (sesh == null) throw new FBLoginException(""); // return null;
		try {
			return DB.getUser(sesh.userID);
		} catch (DBException e) {
			throw new FBLoginException("");
		}
	}
	
	/**
	 * Return whether fbtoken is an active login session
	 * 
	 * Also updates active session with current time
	 * 
	 * @param fbtoken token from cookie
	 * @return true if active user session, else false
	 */
	public static boolean isLoggedIn(Cookie fbtoken) {
		if (fbtoken == null) return false;
		UserSession sesh = active.get(fbtoken.getValue());
		if (sesh == null) return false;
		try {
			DB.getUser(sesh.userID);
		} catch (DBException e) {
			return false;
		}
		sesh.ping();
		return true;
	}
	
	/**
	 * Log in using email and password
	 * @param email email address
	 * @param password plaintext password
	 * @return login token to be added as cookie
	 * @throws FBLoginException if email or password is wrong (e.getMessage() contains HTML)
	 */
	public static String login(String email, String password) throws FBLoginException {
		User user;
		try {
			email = email.toLowerCase();
			user = email.contains("@") ? DB.getUserByEmail(email) : DB.getUser(email);
			if (!DB.checkPassword(user.id, password)) throw new FBLoginException(Strings.getFile("loginform.html", null).replace("$EXTRA", "Incorrect username/email or password, or username/email does not exist"));
		} catch (DBException e) {
			throw new FBLoginException(Strings.getFile("loginform.html", null).replace("$EXTRA", "Incorrect username/email or password, or username/email does not exist"));
		}
		
		String newToken = newToken(active);
		active.put(newToken, new UserSession(user.id));
		
		return newToken;
	}
	
	public static void logout(Cookie fbtoken) {
		if (fbtoken == null) return;
		active.remove(fbtoken.getValue());
	}
	
	public static class FBLoginException extends Exception {
		/** */
		private static final long serialVersionUID = -3144814684721361990L;
		public FBLoginException(String message) {
			super(message);
		}
	}
	
	/**
	 * Verify a new account
	 * @param createToken 
	 * @return HTML account confirmed, or plaintext error
	 */
	public static String verify(String createToken) {
		StringBuilder sb = new StringBuilder("Verifying token " + createToken + " against ");
		for (String key : createQueue.keySet()) sb.append(key + " ");
		Strings.log(sb.toString());
		if (createQueue.containsKey(createToken)) {
			PotentialUser createUser = createQueue.remove(createToken);
			try {
				DB.addUser(createUser.username, createUser.email, createUser.passwordHash, createUser.author);
			} catch (DBException e) { 
				return Strings.getFile("generic.html",null).replace("$EXTRA", "Email address associated with that confirmation token is already verified");
			}
			return Strings.getFile("accountconfirmed.html", null);
		} else return Strings.getFile("generic.html",null).replace("$EXTRA", "Confirmation link is expired, invalid, or has already been used");
	}
	
	/**
	 * Create a new account
	 * @param email email address
	 * @param password password
	 * @param password2 confirm password
	 * @param author author name
	 * @return Success as plaintext or form with error 
	 */
	public static String create(String email, String password, String password2, String author, String username) {
		{
			if (email == null || email.length() == 0) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Email address is required");
			
			email = email.toLowerCase();
			
			if (DB.emailInUse(email)) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Email address " + email + " is already in use");
			if (!EmailValidator.getInstance().isValid(email)) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Invalid email address " + email);
			if (!password.equals(password2)) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Passwords do not match");
			if (password.length() < 8) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Password must be at least 8 characters long");
			if (author == null || author.trim().length() == 0) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Author name is required");
			if (username == null || username.length() == 0) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Username is required");
			username = username.toLowerCase();
			if (DB.usernameInUse(username)) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Username " + username + " is already in use");
			for (char c : username.toCharArray()) if (!allowedUsernameChars.contains(c)) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Username may not contain " + c);
			try {
			for (PotentialUser pu : createQueue.values()) {
				if (pu.username.equals(username)) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Username " + username + " is already in use");
			}
			} catch (Exception e) { e.printStackTrace(); }
		}
		String createToken = newToken(createQueue);
		PotentialUser createUser = new PotentialUser(username, email, BCrypt.hashpw(password, BCrypt.gensalt(10)), author);
		createQueue.put(createToken, createUser);
		if (!sendEmail(email, "Confirm your Fiction Branches account", 
				"<html><body>Please click the following link (or copy/paste it into your browser) to verify your account: <a href=https://" + Strings.DOMAIN + "/fb/confirmaccount/" + createToken + ">https://" + Strings.DOMAIN + "/fb/confirmaccount/" + createToken + "</a> (This link is only good for 24 hours.)</body></html>")) {
			return Strings.getFile("generic.html",null).replace("$EXTRA", "Unable to send verification email, talk to Phoenix about it");
		}
		return Strings.getFile("generic.html",null).replace("$EXTRA", "Check your email (and your spam folder) for a confirmation email from noreply@fictionbranches.net");
	}
	
	/**
	 * Changes the author name of the currently logged in user
	 * @param fbtoken
	 * @param author new author name
	 * @throws FBLoginException if user is not logged in, or is root
	 */
	public static void changeAuthor(Cookie fbtoken, String author) throws FBLoginException {
		if (fbtoken == null) throw new FBLoginException(Strings.getFile("changeauthorform.html", fbtoken).replace("$EXTRA", "You must be logged in to do that"));
		UserSession sesh = active.get(fbtoken.getValue());
		if (sesh == null) throw new FBLoginException(Strings.getFile("changeauthorform.html", fbtoken).replace("$EXTRA", "You must be logged in to do that"));
		User user;
		try {
			user = DB.getUser(sesh.userID);
		} catch (DBException e) {
			throw new FBLoginException(Strings.getFile("changeauthorform.html", fbtoken).replace("$EXTRA", "Invalid user"));
		}
		if (user.id.equals(DB.ROOT_ID)) throw new FBLoginException(Strings.getFile("changeauthorform.html", fbtoken).replace("$EXTRA", "This user account may not be modified"));
		if (author.length() == 0) throw new FBLoginException(Strings.getFile("changeauthorform.html", fbtoken).replace("$EXTRA", "Author cannot be empty"));
		if (author.length() > 200) throw new FBLoginException(Strings.getFile("changeauthorform.html", fbtoken).replace("$EXTRA", "Author cannot be longer than 200 character"));
		try {
			DB.changeAuthorName(user.id, author);
		} catch (DBException e) {
			throw new FBLoginException(Strings.getFile("changeauthorform.html", fbtoken).replace("$EXTRA", "Invalid user"));
		}
	}
	
	/**
	 * Changes the author name of the currently logged in user
	 * @param fbtoken
	 * @param theme new theme name
	 * @throws FBLoginException if user is not logged in, or is root
	 */
	public static void changeTheme(Cookie fbtoken, String theme) throws FBLoginException {
		if (fbtoken == null) throw new FBLoginException(Strings.getFile("changethemeform.html", fbtoken).replace("$EXTRA", "You must be logged in to do that"));
		UserSession sesh = active.get(fbtoken.getValue());
		if (sesh == null) throw new FBLoginException(Strings.getFile("changethemeform.html", fbtoken).replace("$EXTRA", "You must be logged in to do that"));
		User user;
		try {
			user = DB.getUser(sesh.userID);
		} catch (DBException e) {
			throw new FBLoginException(Strings.getFile("changethemeform.html", fbtoken).replace("$EXTRA", "Invalid user"));
		}
		//if (user.id.equals(DB.ROOT_ID)) throw new FBLoginException(Strings.getFile("changethemeform.html", fbtoken).replace("$EXTRA", "This user account may not be modified"));
		try {
			DB.changeTheme(user.id, theme);
		} catch (DBException e) {
			throw new FBLoginException(Strings.getFile("changethemeform.html", fbtoken).replace("$EXTRA", "Invalid user"));
		}
	}
	
	/**
	 * Changes the author bio of the currently logged in user
	 * @param fbtoken
	 * @param theme new theme name
	 * @throws FBLoginException if user is not logged in, or is root, or bio is incorrect
	 */
	public static void changeBio(Cookie fbtoken, String bio) throws FBLoginException {
		if (fbtoken == null) throw new FBLoginException(Strings.getFile("changebioform.html", fbtoken).replace("$EXTRA", "You must be logged in to do that"));
		UserSession sesh = active.get(fbtoken.getValue());
		if (sesh == null) throw new FBLoginException(Strings.getFile("changebioform.html", fbtoken).replace("$EXTRA", "You must be logged in to do that"));
		User user;
		try {
			user = DB.getUser(sesh.userID);
		} catch (DBException e) {
			throw new FBLoginException(Strings.getFile("changebioform.html", fbtoken).replace("$EXTRA", "Invalid user"));
		}
		
		// Check new bio for correctness
		StringBuilder errors = new StringBuilder();
		if (bio.length() == 0) errors.append("Body cannot be empty<br/>\n");
		if (bio.length() > 10000) errors.append("Body cannot be longer than 10000 (" + bio.length() + ")<br/>\n");
		TreeSet<String> list = new TreeSet<>();
		for (String s : Story.replacers) if (bio.contains(s)) list.add(s);
		if (list.size() > 0) {
			errors.append("Bio may not contain any of the following strings: ");
			for (String s : list) errors.append("\"" + s + "\"");
			errors.append("<br/>\n");
		}
		if (errors.length() > 0) throw new FBLoginException(Strings.getFile("changebioform.html", fbtoken).replace("$EXTRA", errors.toString()));
		
		try {
			DB.changeBio(user.id, bio);
		} catch (DBException e) {
			throw new FBLoginException(Strings.getFile("changebioform.html", fbtoken).replace("$EXTRA", "Invalid user"));
		}
	}
		
	/**
	 * 
	 * @param fbtoken
	 * @param newpass
	 * @param newpass2
	 * @param password
	 * @throws FBLoginException if password does not meet requirements, passwords do not match, or user does not exist
	 */
	public static void changePassword(Cookie fbtoken, String newpass, String newpass2, String password) throws FBLoginException {
		if (fbtoken == null) throw new FBLoginException(Strings.getFile("changepasswordform.html", fbtoken).replace("$EXTRA", "You must be logged in to do that"));
		UserSession sesh = active.get(fbtoken.getValue());
		if (sesh == null) throw new FBLoginException(Strings.getFile("changepasswordform.html", fbtoken).replace("$EXTRA", "You must be logged in to do that"));
		User user;
		try {
			user = DB.getUser(sesh.userID);
		} catch (DBException e) {
			throw new FBLoginException(Strings.getFile("changepasswordform.html", fbtoken).replace("$EXTRA", "Invalid user"));
		}
		if (user.id.equals(DB.ROOT_ID)) throw new FBLoginException("This user account may not be modified");
		if (newpass.length() < 8) throw new FBLoginException(Strings.getFile("changepasswordform.html", fbtoken).replace("$EXTRA", "Password cannot be shorter than 8 characters"));
		if (!newpass.equals(newpass2)) throw new FBLoginException(Strings.getFile("changepasswordform.html", fbtoken).replace("$EXTRA", "Passwords do not match"));
		try {
			if (!DB.checkPassword(user.id, password)) throw new FBLoginException(Strings.getFile("changepasswordform.html", fbtoken).replace("$EXTRA", "Incorrect current password"));
		} catch (DBException e1) {
			throw new FBLoginException(Strings.getFile("changepasswordform.html", fbtoken).replace("$EXTRA", "Invalid user"));
		}
		try {
			DB.changePassword(user.id, BCrypt.hashpw(newpass, BCrypt.gensalt(10)));
		} catch (DBException e) {
			throw new FBLoginException(Strings.getFile("changepasswordform.html", fbtoken).replace("$EXTRA", "Invalid user"));
		}
	}
	
	/**
	 * Begin process of changing current user's email address
	 * @param fbtoken
	 * @param email
	 * @param password
	 * @return HTML page with instructions on how to fix input or continue process
	 */
	public static String changeEmail(Cookie fbtoken, String email, String password) {
		if (fbtoken == null) return Strings.getFile("changeemailform.html", fbtoken).replace("$EXTRA", "You must be logged in to do that");
		UserSession sesh = active.get(fbtoken.getValue());
		if (sesh == null) return Strings.getFile("changeemailform.html", fbtoken).replace("$EXTRA", "You must be logged in to do that");
		

		if (sesh.userID.equals(DB.ROOT_ID)) return "This user account may not be modified";
		try {
			if (!DB.checkPassword(sesh.userID, password)) return Strings.getFile("changeemailform.html", fbtoken).replace("$EXTRA", "Incorrect current password");
		} catch (DBException e) {
			return "Invalid user";
		}
		
		if (!EmailValidator.getInstance().isValid(email)) return Strings.getFile("changeemailform.html", fbtoken).replace("$EXTRA", "Invalid email address " + email);
		
		if (DB.emailInUse(email)) return Strings.getFile("changeemailform.html", fbtoken).replace("$EXTRA", email + " is already in use by another account");
		
		String changeToken = newToken(emailChangeQueue);
		EmailChange emailChange = new EmailChange(sesh.userID, email);
		emailChangeQueue.put(changeToken, emailChange);
		if (!sendEmail(email, "<html><body>Confirm your new Fiction Branches account email", 
				"Please click the following link (or copy/paste it into your browser) to verify your new email address: <a href=https://" + Strings.DOMAIN + "/fb/confirmemailchange/" + changeToken + ">https://" + Strings.DOMAIN + "/fb/confirmemailchange/" + changeToken + "</a> (This link is only good for 24 hours.)\nAfter taking this action, you will have to use your new email address to log in.</body></html>")) {
			return "Unable to send verification email, talk to Phoenix about it";
		}
		return Strings.getFile("generic.html",null).replace("$EXTRA", "Check your email (and your spam folder) for a confirmation email from noreply@fictionbranches.net");
	}
	
	/**
	 * Verify a new account
	 * @param createToken 
	 * @return HTML email change confirmed, or error page
	 */
	public static String verifyNewEmail(String changeToken, Cookie fbtoken) {
		if (emailChangeQueue.containsKey(changeToken)) {
			EmailChange changeEmail = emailChangeQueue.remove(changeToken);
			try {
				DB.changeEmail(changeEmail.userID, changeEmail.newEmail);
			} catch (DBException e) {
				return "Unable to change email address";
			}
			return Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "Email address successfully changed");
		} else return Strings.getFile("generic.html",fbtoken).replace("$EXTRA", "Confirmation link is expired, invalid, or has already been used");
	}
	
	/**
	 * @param userID
	 * @param level
	 * @param fbtoken
	 * @return HTML page containing result
	 */
	public static String changeLevel(String userID, byte level, Cookie fbtoken) {
		User admin;
		try {
			admin = Accounts.getUser(fbtoken);
		} catch (FBLoginException e ) {
			return Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "You must be logged in to do that");
		}
		if (userID.equals(DB.ROOT_ID)) return Strings.getFile("adminform.html", fbtoken).replace("$EXTRA", "This user account may not be modified");
		if (admin.level<100) return Strings.getFile("generic.html", fbtoken).replace("$EXTRA","You must be an admin to do that");
		try {
			DB.changeUserLevel(userID, level);
		} catch (DBException e) {
			return Strings.getFile("adminform.html", fbtoken).replace("$EXTRA", "User id does not exist");
		}
		return Strings.getFile("adminform.html", fbtoken).replace("$EXTRA", "User " + userID + " level successfully changed");
	}
	
	public static String getFlagQueue(Cookie fbtoken) {
		User user;
		try {
			user = Accounts.getUser(fbtoken);
		} catch (FBLoginException e ) {
			return Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "You must be logged in to do that");
		}
		if (user.level<10) return Strings.getFile("generic.html", fbtoken).replace("$EXTRA","You must be a mod to do that");
		FlaggedEpisode[] arr = DB.getFlags();
		StringBuilder sb = new StringBuilder();
		sb.append("<h1> Flagged episodes</h1>\n");
		for (FlaggedEpisode flag : arr) {
			sb.append("<a href=/fb/getflag/" + flag.id + ">" + Strings.escape(flag.episode.link) + "</a> flagged by <a href=/fb/user/" + flag.user.id + ">" + Strings.escape(flag.user.author) + "</a> on " + Strings.outputDateFormat(flag.date) + "<br/>\n");
		}
		return Strings.getFile("generic.html", fbtoken).replace("$EXTRA", sb.toString());
	}
	
	public static String getFlag(long id, Cookie fbtoken) {
		User user;
		try {
			user = Accounts.getUser(fbtoken);
		} catch (FBLoginException e ) {
			return Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "You must be logged in to do that");
		}
		if (user.level<10) return Strings.getFile("generic.html", fbtoken).replace("$EXTRA","You must be a mod to do that");
		FlaggedEpisode flag;
		try {
			flag = DB.getFlag(id);
		} catch (DBException e) {
			return Strings.getFile("generic.html", fbtoken).replace("$EXTRA",e.getMessage());
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<h1> Flagged episode</h1>\n");
		sb.append("<a href=/fb/get/" + flag.episode.id + ">" + Strings.escape(flag.episode.link) + "</a> flagged by <a href=/fb/user/" + flag.user.id + ">" + Strings.escape(flag.user.author) + "</a> on " + Strings.outputDateFormat(flag.date) + "<br/>\n");
		sb.append("<a href=/fb/clearflag/" + flag.id + ">Delete this flag</a><br/>\n");
		sb.append("<p>" + Strings.escape(flag.text) + "</p>");
		return Strings.getFile("generic.html", fbtoken).replace("$EXTRA",sb.toString());
	}
	
	public static void clearFlag(long id, Cookie fbtoken) throws FBLoginException {
		User user;
		try {
			user = Accounts.getUser(fbtoken);
		} catch (FBLoginException e ) {
			throw new FBLoginException(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "You must be logged in to do that"));
		}
		if (user.level<10) throw new FBLoginException(Strings.getFile("generic.html", fbtoken).replace("$EXTRA","You must be a mod to do that"));
		try {
			DB.clearFlag(id);
		} catch (DBException e) {
			throw new FBLoginException(Strings.getFile("generic.html", fbtoken).replace("$EXTRA",e.getMessage()));
		}
	}
	
	/**
	 * Generate a new 32-char token, ensuring that the map does not contain it as a key
	 * @param map map to check for collision
	 * @return token
	 */
 	private static String newToken(ConcurrentHashMap<String,?> map) {
		StringBuilder token = new StringBuilder();
		for (int i=0; i<32; ++i) token.append((char)('a'+DB.r.nextInt(26)));
		while (map.containsKey(token.toString())) {
			token = new StringBuilder();
			for (int i=0; i<32; ++i) token.append((char)('a'+DB.r.nextInt(26)));
		}
		return token.toString();
	}
	
	/**
	 * Send an email
	 * @param toAddress
	 * @param subject
	 * @param body
	 * @return whether it sent successfully or not
	 */
	private static boolean sendEmail(String toAddress, String subject, String body) {
		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.zoho.com");
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		Authenticator auth = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("noreply@fictionbranches.net", Strings.SMTP_PASSWORD);
			}
		};
		Session session = Session.getInstance(props, auth);
		try {
			MimeMessage msg = new MimeMessage(session);
			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.addHeader("format", "flowed");
			msg.addHeader("Content-Transfer-Encoding", "8bit");
			msg.setFrom(new InternetAddress("noreply@fictionbranches.net", "Fiction Branches"));
			msg.setReplyTo(InternetAddress.parse("noreply@fictionbranches.net", false));
			msg.setSubject(subject, "UTF-8");
			msg.setText(body, "UTF-8", "html");
			msg.setSentDate(new Date());
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress, false));
			Transport.send(msg);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	private static HashSet<Character> allowedUsernameChars;
	static {
		allowedUsernameChars = new HashSet<>();
		for (char c = '0'; c<='9'; ++c) allowedUsernameChars.add(c);
		for (char c = 'a'; c<='z'; ++c) allowedUsernameChars.add(c);
		for (char c = 'A'; c<='Z'; ++c) allowedUsernameChars.add(c);
		allowedUsernameChars.add('-');
		allowedUsernameChars.add('_');
		allowedUsernameChars.add('.');
	}
}
