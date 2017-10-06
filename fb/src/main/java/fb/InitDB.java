package fb;

import org.hibernate.Session;

public class InitDB {

	/*public static void main(String[] args) throws Exception {
		
		System.out.println("Making parent episode");
		
		FBEpisode parent = new FBEpisode();
		parent.setTitle("And So It Begins...");
		parent.setBody(Stuff.readFile("ep1.txt"));
		parent.setLink("");
		parent.setAuthor("Zach");
		parent.setId("1");
		
		Session session = DB.getSession();
		session.beginTransaction();
		String parentID = (String) session.save(parent);
		session.getTransaction().commit();
		
		FBEpisode child = new FBEpisode();
		child.setTitle("Enter the Bitch");
		child.setLink("episode two");
		child.setBody(Stuff.readFile("ep2.txt"));
		child.setAuthor("Jezzi_Belle_Stewart");
		
		session = DB.getSession();
		session.beginTransaction();
		
		parent = session.get(FBEpisode.class, parentID);
		child.setId(parent.getId()+"-"+(1+parent.getChildren().size()));
		child.setParent(parent);
		parent.getChildren().add(child);
		
		String childID = (String) session.save(child);
		session.merge(parent);
		session.getTransaction().commit();
		session.close();
		
		System.out.println("Root ID: " + parentID);
		System.out.println("Chld ID: " + childID);
		
		session.close();
		DB.getSessionFactory().close();
		System.out.println("Fin");
	}*/
}
