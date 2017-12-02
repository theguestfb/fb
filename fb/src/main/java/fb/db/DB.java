package fb.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

//import org.h2.jdbcx.JdbcConnectionPool;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.mindrot.jbcrypt.BCrypt;

import fb.Strings;
import fb.objects.Episode;
import fb.objects.EpisodeList;
import fb.objects.User;

public class DB {
	
	public static final String ROOT_ID = "fbadministrator1";
	
	private static final SessionFactory sessionFactory;
	static final Session session;
	private static final Connection con;
	private static Object dbLock = new Object();
	static {
		synchronized (dbLock) {
			
			Configuration configuration = new Configuration().configure();
			
			configuration.addAnnotatedClass(DBEpisode.class);
			configuration.addAnnotatedClass(DBLegacyId.class);
			configuration.addAnnotatedClass(DBUser.class);
			configuration.addAnnotatedClass(DBEmail.class);
			configuration.addAnnotatedClass(DBRootEpisodes.class);

			StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
			sessionFactory = configuration.buildSessionFactory(builder.build());
			session = sessionFactory.openSession();
			try { // I wish this didn't have to be a separate connection, but idk how to do it differently without using HQL instead of SQL, which I don't care to learn
				//con = cp.getConnection();
				con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/fictionbranches", "fictionbranches", "");
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	static void closeSession() {
		session.close();
		sessionFactory.close();
		try {
			con.close();
			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static class DBException extends Exception {
		/** */
		private static final long serialVersionUID = -1610662405195508706L;

		public DBException(String message) {
			super(message);
		}

		public DBException(Exception e) {
			super(e);
		}
	}
	
	/**
	 * Adds an episode to the story
	 * 
	 * This method checks that the parent episode and author exist, and that the new child id will not be longer than the allowed 4096 characters
	 * 
	 * This method DOES NOT check that link/title/body are within acceptable limits
	 * 
	 * @param id id of parent episode
	 * @param title title of new episode
	 * @param body body of new episode
	 * @param author author of new episode
	 * @return child DBEpisode object
	 * @throws DBException if parent ep or author does not exist, or if new keystring is too long
	 */
	public static Episode addEp(String id, String link, String title, String body, String authorId, Date date) throws DBException {
		synchronized (dbLock) {
			DBEpisode parent = session.get(DBEpisode.class, id);
			DBUser author = session.get(DBUser.class, authorId);

			if (parent == null) throw new DBException("Not found: " + id);
			if (author == null) throw new DBException("Author does not exist");

			DBEpisode child;
			child = new DBEpisode();
			
			String childId = id + "-" + (parent.getChildren().size()+1);
			if (childId.length() > 4096) {
				session.getTransaction().commit();
				throw new DBException("Cannot create new episode, ID string longer than 4096 characters<br/>\n" + childId);
			}
			child.setId(childId);
			child.setDepth(InitDB.keyToArr(childId).length);
			
			child.setTitle(title);
			child.setLink(link);
			child.setBody(body);
			child.setAuthor(author);
			child.setParent(parent);
			child.setDate(date);
			
			author.getEpisodes().add(child);
		
			session.beginTransaction();
			
			parent.getChildren().add(child);
			
			session.save(child);
			session.merge(parent);
			session.merge(author);
			Strings.log(String.format("New: <%s> %s %s", author, title, child.getId()));
			session.getTransaction().commit();
			Episode ret = new Episode(child);
			return ret;
		}
	}
	
	/**
	 * Adds a new root episode to the site
	 * 
	 * This method checks that the author exists, and that the new child id will not be longer than the allowed 4096 characters
	 * 
	 * This method DOES NOT check that link/title/body are within acceptable limits
	 * 
	 * @param title title of new episode
	 * @param body body of new episode
	 * @param author author of new episode
	 * @return child DBEpisode object
	 * @throws DBException if parent ep or author does not exist, or if new keystring is too long
	 */
	public static Episode addRootEp(String link, String title, String body, String authorId, Date date) throws DBException {
		synchronized (dbLock) {
			//DBEpisode parent = session.get(DBEpisode.class, id);
			DBUser author = session.get(DBUser.class, authorId);
			DBRootEpisodes roots = session.get(DBRootEpisodes.class, 1);

			//if (parent == null) throw new DBException("Not found: " + id);
			if (author == null) throw new DBException("Author does not exist");

			DBEpisode child;
			child = new DBEpisode();
			
			String childId = "" + (Integer.parseInt(roots.getRoots().get(roots.getRoots().size()-1).getId()) + 1);
			if (childId.length() > 4096) {
				session.getTransaction().commit();
				throw new DBException("Cannot create new episode, ID string longer than 4096 characters<br/>\n" + childId);
			}
			child.setId(childId);
			child.setDepth(InitDB.keyToArr(childId).length);
			
			child.setTitle(title);
			child.setLink(link);
			child.setBody(body);
			child.setAuthor(author);
			child.setParent(null);
			child.setDate(date);
			
			author.getEpisodes().add(child);
		
			session.beginTransaction();
			
			roots.getRoots().add(child);
			
			//parent.getChildren().add(child);
			
			session.save(child);
			session.merge(roots);
			session.merge(author);
			Strings.log(String.format("New: <%s> %s %s", author, title, child.getId()));
			session.getTransaction().commit();
			Episode ret = new Episode(child);
			return ret;
		}
	}
	
	/**
	 * Modifies an episode of the story
	 * @param id id of episode
	 * @param title new title of new episode
	 * @param body new body of new episode
	 * @param author new author of new episode
	 * @throws DBException if id not found
	 */
	public static void modifyEp(String id, String link, String title, String body) throws DBException {
		synchronized (dbLock) {
			DBEpisode ep = session.get(DBEpisode.class, id);
			if (ep == null) throw new DBException("Not found: " + id);
			ep.setTitle(title);
			ep.setLink(link);
			ep.setBody(body);
			session.beginTransaction();
			session.merge(ep);
			session.getTransaction().commit();
			Strings.log(String.format("Modified: <%s> %s", title, ep.getId()));
		}
	}
	
	
	
	/**
	 * Retrieves an episode from the db by id
	 * @param id
	 * @return 
	 * @throws DBException if episode id does not exist
	 */
	public static Episode getEp(String id) throws DBException {
		synchronized (dbLock) {
			DBEpisode ep = session.get(DBEpisode.class, id);
			if (ep == null) throw new DBException("Not found: " + id);
			Episode ret = new Episode(ep);
			return ret;
		}
	}
	
	/**
	 * Retrieves the newId of an episode by it's oldId
	 * @param id
	 * @return
	 * @throws DBException if id not found
	 */
	public static String getLegacyId(String oldId) throws DBException {
		synchronized(dbLock) {
			DBLegacyId legacy = session.get(DBLegacyId.class, oldId);
			if (legacy == null) throw new DBException("Not found: " + oldId);
			if (legacy.getNewId() == null) throw new DBException("Not found: " + oldId);
			return legacy.getNewId();
		}
	}
	
	/**
	 * Retrieves recent episodes from the db
	 * @return
	 * @throws DBException wtf
	 */
	public static EpisodeList getRecents(int days) throws DBException {
		synchronized(dbLock) {
			ArrayList<Episode> list = new ArrayList<>();
			
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_MONTH, -days);
			Date d = cal.getTime();

			try {

				ResultSet rs = con.prepareStatement(
						"SELECT fbepisodes.id, fbepisodes.link, fbepisodes.date, fbepisodes.depth, fbepisodes.author_id, fbusers.author "
						+ "FROM fbepisodes,fbusers "
						+ "WHERE fbepisodes.author_id = fbusers.id AND fbepisodes.date > '" + Strings.sqlDateFormat(d) + "' "
								+ "ORDER BY fbepisodes.date DESC").executeQuery();
				
				
				
				while(rs.next()) {
					String id = rs.getString("id");
					String link = rs.getString("link");
					String author = rs.getString("author");
					int depth = rs.getInt("depth");
					Date date = new Date(rs.getTimestamp("date").getTime());
					list.add(new Episode(id, link, author, date, depth));
				}
			} catch (SQLException e) {
				e.printStackTrace();
				throw new DBException(e);
			}
			return new EpisodeList(list);
		}
	}
	
	public static EpisodeList getOutline(String rootId, int maxDepth) throws DBException {
		synchronized(dbLock) {
			ArrayList<Episode> list = new ArrayList<>();
			
			int minDepth = DB.getEp(rootId).depth;
			maxDepth += minDepth;

			try {

				ResultSet rs = con.prepareStatement(
						"SELECT id, link, depth "
						+ "FROM fbepisodes "
						+ "WHERE (id LIKE '" + rootId + "-%' OR id = '" + rootId + "' ) AND depth < " + maxDepth).executeQuery();
				
				
				
				while(rs.next()) {
					String id = rs.getString("id");
					String link = rs.getString("link");
					int depth = rs.getInt("depth");
					list.add(new Episode(id, link, "", new Date(), depth));
				}
			} catch (SQLException e) {
				e.printStackTrace();
				throw new DBException(e);
			}
			return new EpisodeList(list);
		}
	}
	
	/**
	 * Retrieves root episodes from the db
	 * @return
	 * @throws DBException wtf
	 */
	public static EpisodeList getRoots() throws DBException {
		synchronized(dbLock) {
			DBRootEpisodes roots = session.get(DBRootEpisodes.class, 1);
			if (roots == null) throw new DBException("Recents not found (tell Phoenix if you see this, it should never happen)");
			return new EpisodeList(roots);
		}
	}
	
	/**
	 * Adds a new user to the database
	 * @param email
	 * @param password HASHED password (NOT PLAINTEXT!)
	 * @param author
	 * @return user ID of new user
	 * @throws DBException 
	 */
	public static String addUser(String email, String password, String author) throws DBException {
		synchronized (dbLock) {
		
			if (session.get(DBEmail.class, email) != null) throw new DBException("Email " + email + " is already in use");
			
			DBUser user = new DBUser();
			DBEmail dbemail = new DBEmail();
			dbemail.setEmail(email);
		
			user.setLevel((byte) 1);
			user.setAuthor(author);
			user.setEmail(dbemail);
			user.setBio("");
			//user.setPassword(BCrypt.hashpw(password,BCrypt.gensalt(10)));
			user.setPassword(password);
			dbemail.setUser(user);
		
			String id = newUserId();
			// Make sure id doesn't already exist
			while (session.get(DBUser.class, id) != null) id = newUserId();
			
			user.setId(id);
			
			session.beginTransaction();
			session.save(user);
			session.save(dbemail);
			session.getTransaction().commit();
			
			return id;
		}
	}
	
	/**
	 * Change a user's author name
	 * @param id id of user
	 * @param newAuthor new author name
	 * @throws DBException if id not found
	 */
	public static void changeAuthorName(String id, String newAuthor) throws DBException {
		synchronized (dbLock) {
			DBUser user = session.get(DBUser.class, id);
			if (user == null) throw new DBException("User id does not exist");
			user.setAuthor(newAuthor);
			session.beginTransaction();
			session.merge(user);
			session.getTransaction().commit();
		}
	}
	
	/**
	 * Change a user's theme
	 * @param id id of user
	 * @param newTheme new theme (HTML name, not file name)
	 * @throws DBException if id not found
	 */
	public static void changeTheme(String id, String newTheme) throws DBException {
		synchronized (dbLock) {
			DBUser user = session.get(DBUser.class, id);
			if (user == null) throw new DBException("User id does not exist");
			user.setTheme(newTheme);
			session.beginTransaction();
			session.merge(user);
			session.getTransaction().commit();
		}
	}
	
	/**
	 * Change a user's bio
	 * @param id id of user
	 * @param newBio new bio 
	 * @throws DBException if id not found
	 */
	public static void changeBio(String id, String newBio) throws DBException {
		synchronized (dbLock) {
			DBUser user = session.get(DBUser.class, id);
			if (user == null) throw new DBException("User id does not exist");
			user.setBio(newBio);
			session.beginTransaction();
			session.merge(user);
			session.getTransaction().commit();
		}
	}
	

	/**
	 * Change a user's author name
	 * @param id id of user
	 * @param newPassword new HASHED password (NOT PLAINTEXT)
	 * @throws DBException if id not found
	 */
	public static void changePassword(String id, String newPassword) throws DBException {
		synchronized (dbLock) {
			DBUser user = session.get(DBUser.class, id);
			if (user == null) throw new DBException("User id does not exist");
			user.setPassword(newPassword);
			session.beginTransaction();
			session.merge(user);
			session.getTransaction().commit();
		}
	}
	
	/**
	 * Change a user's user level (1=user, 10=mod, 100=admin)
	 * @param id user id
	 * @param newLevel new user level
	 * @throws DBException if user id not found
	 */
	public static void changeUserLevel(String id, byte newLevel) throws DBException {
		synchronized (dbLock) {
			DBUser user = session.get(DBUser.class, id);
			if (user == null) throw new DBException("User id does not exist");
			user.setLevel(newLevel);
			session.beginTransaction();
			session.merge(user);
			session.getTransaction().commit();
		}
	}

	/**
	 * Get a DBUser by id
	 * @param id id of user
	 * @throws DBException if id does not exist
	 */
	public static User getUser(String id) throws DBException {
		synchronized (dbLock) {
			DBUser user = session.get(DBUser.class, id);
			if (user == null) throw new DBException("User id does not exist");
			return new User(user);
		}
	}
	
	/**
	 * Get a DBUser by email address
	 * @param email
	 * @return null if email does not exist
	 * @throws DBException 
	 */
	public static User getUserByEmail(String email) throws DBException {
		synchronized(dbLock) {
			DBEmail dbemail = session.get(DBEmail.class, email);
			if (dbemail == null) throw new DBException("Email " + email + " does not exist");
			return new User(dbemail.getUser());
		}
	}
	
	public static boolean emailInUse(String email) {
		synchronized(dbLock) {
			return session.get(DBEmail.class, email) != null;
		}
	}
	
	/**
	 * Changes a user's email address in the db
	 * @param userId
	 * @param email
	 * @throws DBException
	 */
	public static void changeEmail(String userId, String email) throws DBException {
		synchronized (dbLock) {
			DBUser user = session.get(DBUser.class, userId);
			if (user == null) throw new DBException("User id does not exist");
			DBEmail oldEmail = user.getEmail();
			if (oldEmail == null) throw new DBException("Old email does not exist");
			if (session.get(DBEmail.class, email) != null) throw new DBException("New email " + email + " already in use");
			DBEmail newEmail = new DBEmail();
			newEmail.setUser(user);
			newEmail.setEmail(email);
			user.setEmail(newEmail);
			
			session.beginTransaction();
			session.delete(oldEmail);
			session.save(newEmail);
			session.merge(user);
			session.getTransaction().commit();
		}
	}
	
	/**
	 * Checks a plaintext password against the stored hash
	 * @param id id of user
	 * @param password plaintext possible password
	 * @return true if password matches, else false
	 * @throws DBException if id not found
	 */
	public static boolean checkPassword(String id, String password) throws DBException {
		synchronized(dbLock) {
			DBUser user = session.get(DBUser.class, id);
			if (user == null) throw new DBException("User does not exist");
			return BCrypt.checkpw(password, user.getPassword());
		}
	}
	
	private static ArrayList<Character> userIdChars = new ArrayList<>();
	public static Random r = new Random();
	static { // this could be added to the beginning of the static block at the top of this file, but it's fine here since it only affects userIdChars
		for (char c='a'; c<='z'; ++c) userIdChars.add(c);
		for (char c='A'; c<='Z'; ++c) userIdChars.add(c);
		for (char c='0'; c<='9'; ++c) userIdChars.add(c);
	}
	private static String newUserId() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<16; ++i) sb.append(userIdChars.get(r.nextInt(userIdChars.size())));
		return sb.toString();
	}
}