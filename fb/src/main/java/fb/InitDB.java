package fb;

import org.hibernate.Session;

public class InitDB {

	/*public static void main(String[] args) {
		
		System.out.println("Making parent episode");
		
		FBEpisode parent = new FBEpisode();
		parent.setTitle("This is the root episode");
		parent.setBody("This is the body of the root episode");
		parent.setAuthor("Phoenix");
		
		Session session = DB.getSession();
		session.beginTransaction();
		Long parentID = (Long) session.save(parent);
		session.getTransaction().commit();
		session.close();
		
		System.out.println("Root ID: " + parentID);
		
		DB.getSessionFactory().close();
		System.out.println("Fin");
	}*/
}
