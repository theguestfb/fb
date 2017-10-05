package fb;

import java.util.Calendar;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.hibernate.Session;

@Path("asdf")
public class FBAPI {
	
	private static final String storyDefault = "<html><head><title>Fiction Branches</title></head><body><h1>$TITLE</h1><h3>Author: $AUTHOR</h3><p><a href=$PARENTID>Go back</a></p><hr/><p>$BODY</p><hr/><p>$CHILDREN</p><hr/><p><a href=../add/$ID>Add a new episode</a></p></body></html>";
	private static final String formDefault = "<html><head><title>Fiction Branches</title></head><body><h1>Add to '$TITLE'</h1><form action=\"../addapi/$ID\" method=\"post\"><p>Title: <input type=\"text\" name=\"title\" /></p><p>Body: <br/><textarea name=\"body\" rows=\"40\" cols=\"1000\"></textarea></p><p>Your name:<input type=\"text\" name=\"author\"/></p><input type=\"submit\" value=\"Submit\"/></form></body></html>";
	private static final String successDefault = "<html><head><title>Fiction Branches</title></head><body><h1>Success!</h1><p><a href=../get/$ID>View your new episode</a></p></body></html>";

	
	@GET
	@Path("get/{id}")
	public Response get(@PathParam("id") Long id) {
		Session session = HibernateUtil.getSession();
		session.beginTransaction();
		FBEpisode ep = session.get(FBEpisode.class, id);
		Response response;
		if (ep == null) response = notFound(id);
		else {
			String ret = storyDefault;
			ret = ret.replace("$TITLE", ep.getTitle());
			ret = ret.replace("$BODY", formatBody(ep.getBody()));
			ret = ret.replace("$AUTHOR", ep.getAuthor());
			ret = ret.replace("$PARENTID", (ep.getParent() == null)?(""):(ep.getParent().getId()+""));
			ret = ret.replace("$ID", id+"");
			StringBuilder sb = new StringBuilder();
			List<FBEpisode> children = ep.getChildren();
			if (children != null) for (FBEpisode child : children) if (child != null){
				sb.append("<p><a href=" + child.getId() + ">" + child.getTitle() + "</a></p>");
			}
			response = Response.ok(ret.replace("$CHILDREN", sb.toString())).build();
		}
		session.getTransaction().commit();
		return response;
	}
	
	private String formatBody(String body) {
		StringBuilder sb = new StringBuilder();
		
		int count = -1;
		for (char c : body.toCharArray()) {
			++count;
			if (count > 100 || c=='\n') {
				count = 0;
				sb.append("<br/>");
			}
			sb.append(c);
		}
		
		return sb.toString();
	}
	
	@GET
	@Path("add/{id}")
	public Response add(@PathParam("id") Long id) {
		Session session = HibernateUtil.getSession();
		session.beginTransaction();
		FBEpisode ep = session.get(FBEpisode.class, id);
		Response response;
		if (ep == null) response = notFound(id);
		else {
			String ret = formDefault;
			ret = ret.replace("$TITLE", ep.getTitle());
			ret = ret.replace("$BODY", ep.getBody());
			ret = ret.replace("$ID", ""+id);
			response = Response.ok(ret).build();
		}
		session.getTransaction().commit();
		return response;
	}

	@POST
	@Path("addapi/{id}")
	public Response add(@PathParam("id") Long id, @FormParam("title") String title, @FormParam("body") String body, @FormParam("author") String author) {
		Session session = HibernateUtil.getSession();
		session.beginTransaction();
		FBEpisode parent = session.get(FBEpisode.class, id);
		Response response;
		
		if (parent == null) response = notFound(id);
		else {
			FBEpisode child = new FBEpisode();
			child.setTitle(title);
			child.setBody(body);
			child.setAuthor(author);
			child.setParent(parent);
			parent.getChildren().add(child);
			Long childID = (Long) session.save(child);
			session.merge(parent);
			//Long childID = FBEpisode.newEpisode(session, parent, title, body, author);
			response = Response.ok(successDefault.replace("$ID", childID + "")).build();
		}
		session.getTransaction().commit();
		
		log(String.format("New: <%s> %s", author, title));
		
		return response;
	}
	
	private Response notFound(Long id) {
		return Response.ok(storyDefault.replace("$TITLE", "Not found").replace("$BODY", id+"").replace("$CHILDREN", "")).build();
	}
	
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
