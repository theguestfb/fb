package fb;

import java.util.ArrayList;
import java.util.Date;
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

import com.google.common.html.HtmlEscapers;

import fb.db.DB;
import fb.db.DB.DBException;
import fb.objects.Episode;
import fb.objects.User;

public class Accounts {
	private static ConcurrentHashMap<String,UserSession> active = new ConcurrentHashMap<>(); //<loginToken>, user>
	private static ConcurrentHashMap<String,PotentialUser> createQueue = new ConcurrentHashMap<>(); //<createToken, user>
	private static ConcurrentHashMap<String,EmailChange> emailChangeQueue = new ConcurrentHashMap<>(); //<changeToken, EmailChange>
	
	/*
	 * Scan the active sessions and createQueue maps for expired
	 */
	static {
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
						double hours = ((double) (now.getTime() - then.getTime())) / (1000.0 * 60.0 * 60.0);
						if (hours > 24) deleteTheseTokens.add(createToken); // expires after 24 hours
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
		public final String email;
		public final String passwordHash;
		public final String author;
		public final Date date;
		public PotentialUser(String email, String passwordHash, String author) {
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
		String token;
		if (fbtoken==null) token = "asdf";
		else token = fbtoken.getValue();
		String notLoggedIn = "<a href=/fb/createaccount>Create account</a><br/><a href=/fb/login>Log in</a>";
		if (token == null) return notLoggedIn;
		if (token.length() == 0) return notLoggedIn;
		UserSession sesh = active.get(token);
		if (sesh == null) return notLoggedIn;
		
		User user;
		try {
			user = DB.getUser(sesh.userID);
		} catch (DBException e) {
			return notLoggedIn;
		}
		
		String response = "Logged in as <a href=/fb/useraccount>" + HtmlEscapers.htmlEscaper().escape(user.author) + "</a><br/><a href=/fb/logout>Log out</a>";
		if (user.level>=(byte)100) response +="<br/><a href=/fb/admin>Admin stuff</a>";
		
		return response;
	}
	/**
	 * 
	 * @param id user id
	 * @param fbtoken
	 * @return HTML user page for id
	 */
	public static String getUserPage(String id, Cookie fbtoken) {
		User user;
		try {
			user = DB.getUser(id);
		} catch (DBException e) {
			return Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "User UD " + id + " does not exist");
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
			sb.append("<a href=/fb/get/" + ep.id + ">" + HtmlEscapers.htmlEscaper().escape(ep.title) + "</a> " + Strings.outputDateFormat(ep.date) + " " + story + "<br/>");
		}
		String bio = Story.formatBody(user.bio, 0);
		return Strings.getFile("profilepage.html", fbtoken).replace("$AUTHOR", user.author).replace("$BODY", bio).replace("$EPISODES", sb.toString());
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
			user = DB.getUserByEmail(email);
			if (!DB.checkPassword(user.id, password)) throw new FBLoginException(Strings.getFile("loginform.html", null).replace("$EXTRA", "Incorrect email or password, or email does not exist"));
		} catch (DBException e) {
			throw new FBLoginException(Strings.getFile("loginform.html", null).replace("$EXTRA", "Incorrect email or password, or email does not exist"));
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
				DB.addUser(createUser.email, createUser.passwordHash, createUser.author);
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
	public static String create(String email, String password, String password2, String author) {
		{
			if (email == null || email.length() == 0) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Email address is required");
			if (DB.emailInUse(email)) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Email address " + email + " is already in use");
			if (!EmailValidator.getInstance().isValid(email)) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Invalid email address " + email);
			if (!password.equals(password2)) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Passwords do not match");
			if (password.length() < 8) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Password must be at least 8 characters long");
			if (author == null || author.trim().length() == 0) return Strings.getFile("createaccountform.html", null).replace("$EXTRA", "Author name is required"); 
		}
		String createToken = newToken(createQueue);
		PotentialUser createUser = new PotentialUser(email, BCrypt.hashpw(password, BCrypt.gensalt(10)), author);
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
}
