package fb.db;

import org.hibernate.Session;

import fb.Story;

/**
 * Run this class's main() (as a regular Java Application, not on tomcat) to
 * initialize the database
 */
public class InitDB {

	public static void main(String[] args) throws Exception {

		System.out.println("Making parent episode");

		// DB.addEp() cannot be used since it requires
		// a parent ID and the root has no parent. Instead,
		// it must be created and added manually

		// Create the episode
		DBEpisode parent = new DBEpisode();
		parent.setTitle("And So It Begins...");
		parent.setBody(Story.readFile("ep1.txt"));
		parent.setLink("");
		parent.setAuthor("Zach");
		parent.setId("1");

		// Commit the episode to the DB
		Session session = DB.getSession();
		session.beginTransaction();
		String parentID = (String) session.save(parent);
		session.getTransaction().commit();

		// Now we can use DB.addEp() for the first child
		DBEpisode child = DB.addEp("1", "episode two", "Enter the Bitch", Story.readFile("ep2.txt"),
				"Jezzi_Belle_Stewart");

		System.out.println("Root ID: " + parentID);
		System.out.println("Chld ID: " + child.getId());

		session.close();
		DB.getSessionFactory().close();
		System.out.println("Fin");
	}
}
