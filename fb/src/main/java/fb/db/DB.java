package fb.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.mindrot.jbcrypt.BCrypt;

import fb.json.TempUser;
import fb.objects.Episode;
import fb.objects.User;
import fb.util.Comparators;
import fb.util.Strings;

public class DB {
	
	public static boolean READ_ONLY_MODE = false; //set this value to the default (will revert to this value after restarts)
	
	public static final String ROOT_ID = "fbadministrator1";
	
	private static final SessionFactory sessionFactory;
	static final Session session;
	//private static final Connection con;
	private static Object dbLock = new Object();
	static {
		synchronized (dbLock) {
			
			Configuration configuration = new Configuration().configure();
			
			configuration.addAnnotatedClass(DBEpisode.class);
			configuration.addAnnotatedClass(DBUser.class);

			StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
			sessionFactory = configuration.buildSessionFactory(builder.build());
			session = sessionFactory.openSession();
			/*try { // I wish this didn't have to be a separate connection, but idk how to do it differently without using HQL instead of SQL, which I don't care to learn
				con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/fictionbranches", "fictionbranches", "");
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}*/
			Strings.log("Database success");
		}
	}
	
	public static void closeSession() {
		session.close();
		sessionFactory.close();
		/*try {
			con.close();
			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}*/
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
	
	static DBEpisode getEpById(String id) throws DBException {
		synchronized (dbLock) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBEpisode> query = cb.createQuery(DBEpisode.class);
			Root<DBEpisode> root = query.from(DBEpisode.class);
			query.select(root).where(cb.equal(root.get("id"), id));
			List<DBEpisode> result = session.createQuery(query).list();
			if (result.size() == 0) return null;
			else if (result.size() > 1) {
				StringBuilder sb = new StringBuilder();
				for (DBEpisode ep : result) sb.append(ep.getGeneratedId() + " ");
				throw new RuntimeException("Multiple episodes have matching id: " + id + " " + sb);
			} else return result.get(0);
		}
	}
	
	static DBUser getUserById(String id) throws DBException {
		synchronized (dbLock) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBUser> query = cb.createQuery(DBUser.class);
			Root<DBUser> root = query.from(DBUser.class);
			query.select(root).where(cb.equal(root.get("id"), id));
			List<DBUser> result = session.createQuery(query).list();
			if (result.size() == 0) return null;
			else if (result.size() > 1) {
				StringBuilder sb = new StringBuilder();
				for (DBUser ep : result) sb.append(ep.getId() + " ");
				throw new RuntimeException("Multiple users have matching id: " + id + " " + sb);
			} else return result.get(0);
		}
	}
	
	public static User getUserByEmail(String email) throws DBException {
		synchronized (dbLock) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBUser> query = cb.createQuery(DBUser.class);
			Root<DBUser> root = query.from(DBUser.class);
			query.select(root).where(cb.equal(root.get("email"), email));
			List<DBUser> result = session.createQuery(query).list();
			if (result.size() == 0)
				throw new DBException("Email not found: " + email);
			else if (result.size() > 1) {
				StringBuilder sb = new StringBuilder();
				for (DBUser ep : result) sb.append(ep.getId() + " ");
				throw new RuntimeException("Multiple users have matching email: " + email + " " + sb);
			} else return new User(result.get(0));
		}
	}
	
	static TempUser[] getTempUsers() {
		synchronized (dbLock) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBUser> query = cb.createQuery(DBUser.class);
			Root<DBUser> root = query.from(DBUser.class);
			query.select(root).where(cb.isNotNull(root.get("email")));
			List<DBUser> result = session.createQuery(query).list();

			TempUser[] list = new TempUser[result.size()];
			for (int i = 0; i < result.size(); ++i) list[i] = new TempUser(new User(result.get(i)));

			return list;
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
			DBEpisode parent = getEpById(id);
			DBUser author = getUserById( authorId);

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
			child.setChildCount(1);
			child.setEditDate(date);
			child.setEditor(author);
			author.getEditors().add(child);
			
			author.getEpisodes().add(child);
		
			session.beginTransaction();
			
			parent.getChildren().add(child);
			
			ArrayList<DBEpisode> parents = new ArrayList<>(child.getDepth());
			parent.setChildCount(parent.getChildCount()+1);
			parents.add(parent);
			while (parents.get(parents.size()-1).getParent() != null && !parents.get(parents.size()-1).getParent().equals(parents.get(parents.size()-1))) {
				parent = parent.getParent();
				parent.setChildCount(parent.getChildCount()+1);
				parents.add(parent);
			}
			
			session.save(child);
			for (DBEpisode parEp : parents) session.merge(parEp);
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
			DBUser author = getUserById( authorId);
			
			Episode[] roots = getRoots();

			if (author == null) throw new DBException("Author does not exist");

			DBEpisode child;
			child = new DBEpisode();
			
			String childId = "" + (Integer.parseInt(roots[roots.length-1].id) + 1);
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
			child.setChildCount(1);
			child.setEditDate(date);
			child.setEditor(author);
			author.getEditors().add(child);
			
			author.getEpisodes().add(child);
		
			session.beginTransaction();
			
			session.save(child);
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
	public static void modifyEp(String id, String link, String title, String body, String editorId) throws DBException {
		synchronized (dbLock) {
			DBEpisode ep = getEpById(id);
			if (ep == null) throw new DBException("Not found: " + id);
			DBUser editor = getUserById( editorId);
			if (editor == null) throw new DBException("Editor not found: " + editorId);
			DBUser oldEditor = ep.getEditor();
			ep.setTitle(title);
			ep.setLink(link);
			ep.setBody(body);
			ep.setEditDate(new Date());
			ep.setEditor(editor);
			editor.getEditors().add(ep);
			oldEditor.getEditors().remove(ep);
			session.beginTransaction();
			session.merge(ep);
			session.merge(oldEditor);
			session.merge(editor);
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
			DBEpisode ep = getEpById(id);
			if (ep == null) throw new DBException("Not found: " + id);
			Episode ret = new Episode(ep);
			return ret;
		}
	}
	
	/**
	 * Retrieves an episode by its oldId
	 * @param id
	 * @return
	 * @throws DBException if id not found
	 */
	public static Episode getEpByLegacyId(String oldId) throws DBException {	
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<DBEpisode> query = cb.createQuery(DBEpisode.class);
		Root<DBEpisode> root = query.from(DBEpisode.class);
		
		query.select(root).where(cb.equal(root.get("legacyId"), oldId));
		
		List<DBEpisode> result = session.createQuery(query).list();
		
		if (result.size() == 0) return null;
		else if (result.size() > 1) {
			StringBuilder sb = new StringBuilder();
			for (DBEpisode ep : result) sb.append(ep.getGeneratedId() + " ");
			throw new RuntimeException("Multiple episodes have matching id: " + oldId + " " + sb);
		} else
			return new Episode(result.get(0));
	}
	
	/**
	 * Get num most recent episodes of a particular story, or of all stories
	 * @param rootId root id for story, or 0 to get all stories
	 * @param num number of episodes to get
	 * @return
	 * @throws DBException
	 */
	public static Episode[] getRecents(int rootId, int num) throws DBException {
		synchronized(dbLock) {

			if (rootId != 0) DB.getEp(""+rootId);
			
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBEpisode> query = cb.createQuery(DBEpisode.class);
			Root<DBEpisode> root = query.from(DBEpisode.class);

			query.select(root).orderBy(cb.desc(root.get("date")));
			
			if (rootId != 0) {
				Predicate idPredicate = cb.or(
						cb.like(root.get("id"), rootId + "-%"), 
						cb.equal(root.get("id"), rootId+""));
				query = query.where(idPredicate);
			}
			
			List<DBEpisode> result = session.createQuery(query).setMaxResults(num).list();
			Episode[] list = new Episode[result.size()];
			for (int i=0; i<result.size(); ++i) list[i] = new Episode(result.get(i));
			
			return list;
		}
	}
	
	public static Episode[] getOutline(String rootId, int maxDepth) throws DBException {
		synchronized(dbLock) {
			
			int minDepth = DB.getEp(rootId).depth;
			maxDepth += minDepth;
			
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBEpisode> query = cb.createQuery(DBEpisode.class);
			Root<DBEpisode> root = query.from(DBEpisode.class);
			
			Predicate idPredicate = cb.or(
					cb.like(root.get("id"), rootId + "-%"), 
					cb.equal(root.get("id"), rootId+""));
			
			Predicate depthPredicate = cb.lt(root.get("depth"), maxDepth);
			
			query.select(root).where(cb.and(idPredicate,depthPredicate));
			
			List<DBEpisode> result = session.createQuery(query).list();
			Episode[] list = new Episode[result.size()];
			for (int i=0; i<result.size(); ++i) list[i] = new Episode(result.get(i));
			
			return list;
		}
	}
	
	public static Episode[] getPath(String id) throws DBException {
		synchronized (dbLock) {
			DBEpisode ep = DB.getEpById(id);
			if (ep == null) throw new DBException("Not found: " + id);
			ArrayList<Episode> episodeList = new ArrayList<>(ep.getDepth());
			while (ep != null && ep.getParent() != null && !ep.getParent().getId().equals(ep.getId())) {
				episodeList.add(new Episode(ep));
				ep = DB.getEpById(ep.getParent().getId());
			}
			if (ep != null) episodeList.add(new Episode(ep));
			Collections.reverse(episodeList);
			
			Episode[] ret = new Episode[episodeList.size()];
			ret = episodeList.toArray(ret);
			return ret;
			
		}
	}
	
	public static Episode[] getRoots() throws DBException {
		synchronized(dbLock) {
			//ArrayList<Episode> list = new ArrayList<>();
			
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<DBEpisode> query = cb.createQuery(DBEpisode.class);
			Root<DBEpisode> root = query.from(DBEpisode.class);			
						
			query.select(root).where(cb.isNull(root.get("parent")));
			
			List<DBEpisode> result = session.createQuery(query).list();
			Episode[] list = new Episode[result.size()];
			for (int i=0; i<result.size(); ++i) list[i] = new Episode(result.get(i));
			
			Arrays.sort(list, new Comparator<Episode>() {
				public int compare(Episode o1, Episode o2) {
					return Comparators.keyStringComparator.compare(o1.id, o2.id);
				}
			});
			
			return list;
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
		
			if (emailInUse(email)) throw new DBException("Email " + email + " is already in use");
			
			DBUser user = new DBUser();
		
			user.setLevel((byte) 1);
			user.setAuthor(author);
			user.setEmail(email);
			user.setBio("");
			user.setPassword(password);
		
			String id = newUserId();
			// Make sure id doesn't already exist
			while (getUserById( id) != null) id = newUserId();
			
			user.setId(id);
			
			session.beginTransaction();
			session.save(user);
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
			DBUser user = getUserById( id);
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
			DBUser user = getUserById( id);
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
			DBUser user = getUserById( id);
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
			DBUser user = getUserById( id);
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
			DBUser user = getUserById( id);
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
			DBUser user = getUserById( id);
			if (user == null) throw new DBException("User id does not exist");
			return new User(user);
		}
	}
	
	public static boolean emailInUse(String email) {
		synchronized(dbLock) {
			try {
				getUserByEmail(email);
			} catch (DBException e) {
				return false;
			}
			return true;
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
			DBUser user = getUserById( userId);
			if (user == null) throw new DBException("User id does not exist");
			if (emailInUse(email)) throw new DBException("New email " + email + " already in use");

			user.setEmail(email);
			
			session.beginTransaction();
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
			DBUser user = getUserById( id);
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