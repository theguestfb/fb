package fb.db;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

public class DB {
	private static SessionFactory sessionFactory;
	private static Session session;
	private static Object dbLock = new Object();
	static {
		Configuration configuration = new Configuration().configure();
		configuration.addAnnotatedClass(DBEpisode.class);	
		configuration.addAnnotatedClass(DBRecents.class);	
		configuration.addAnnotatedClass(DBLegacyId.class);	
		
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
				.applySettings(configuration.getProperties());
		sessionFactory = configuration.buildSessionFactory(builder.build());
		session = sessionFactory.openSession();
	}

	static SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	static Session getSession() {
		return session;
	}
	
	/**
	 * Adds an episode to the story
	 * @param id id of parent episode
	 * @param title title of new episode
	 * @param body body of new episode
	 * @param author author of new episode
	 * @return HTML success page
	 */
	public static DBEpisode addEp(String id, String link, String title, String body, String author, Date date) {
		Session session = DB.getSession();
		synchronized (dbLock) {
			session.beginTransaction();
			DBEpisode parent = session.get(DBEpisode.class, id);
			DBRecents recents = session.get(DBRecents.class, 1);
			/*if (recents == null) {
				recents = new DBRecents();
				recents.setId(1);
				session.save(recents);
				//session.getTransaction().commit();
				//session.beginTransaction();
			}*/
			DBEpisode child;
			if (parent == null)
				child = null;
			else {
				child = new DBEpisode();
				child.setTitle(title);
				child.setLink(link);
				child.setBody(body);
				child.setAuthor(author);
				child.setParent(parent);
				child.setDate(date);
				child.setId(id + "-" + (1 + parent.getChildren().size()));
				parent.getChildren().add(child);

				// DBRecentId
				//if (recents.getRecents() == null) recents.setRecents(new ArrayList<DBEpisode>());
				while (recents.getRecents().size() >= 25)
					recents.getRecents().remove(recents.getRecents().size() - 1);
				recents.getRecents().add(0, child);

				session.save(child);
				session.merge(parent);
				session.merge(recents);
				log(String.format("New: <%s> %s %s", author, title, child.getId()));
			}
			session.getTransaction().commit();
			return child;
		}
	}
	
	
	
	/**
	 * Retrieves an episode from the db by id
	 * @param id
	 * @return
	 */
	public static DBEpisode getEp(String id) {
		synchronized (dbLock) {
			Session session = DB.getSession();
			session.beginTransaction();
			DBEpisode ep = session.get(DBEpisode.class, id);
			session.getTransaction().commit();
			return ep;
		}
	}
	
	/**
	 * Retrieves the newId of an episode by it's oldId
	 * @param id
	 * @return
	 */
	public static String getLegacyId(String oldId) {
		synchronized (dbLock) {
			Session session = DB.getSession();
			session.beginTransaction();
			DBLegacyId legacy = session.get(DBLegacyId.class, oldId);
			session.getTransaction().commit();
			if (legacy == null) return null;
			if (legacy.getNewId() == null) return null;
			return legacy.getNewId();
		}
	}
	
	/**
	 * Retrieves recent episodes from the db
	 * @return
	 */
	public static DBRecents getRecents() {
		synchronized (dbLock) {
			Session session = DB.getSession();
			session.beginTransaction();
			DBRecents recents = session.get(DBRecents.class, 1);
			session.getTransaction().commit();
			return recents;
		}
	}
	
	/**
	 * Prepends message with the current date, and writes it to stdout
	 * @param message
	 */
	private static void log(String message) {
		int y = Calendar.getInstance().get(Calendar.YEAR);
		int mo = Calendar.getInstance().get(Calendar.MONTH);
		int d = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		int mi = Calendar.getInstance().get(Calendar.MINUTE);
		int s = Calendar.getInstance().get(Calendar.SECOND);
		System.out.printf("%04d-%02d-%02d %02d:%02d:%02d %s%n", y, mo, d, h, mi, s, message);

	}
}