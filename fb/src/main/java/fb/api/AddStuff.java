package fb.api;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fb.Story;
import fb.Strings;

@Path("")
public class AddStuff {
	/**
	 * Returns the form for adding new episodes
	 * 
	 * @param id
	 *            id of parent episode
	 * @return HTML form to add episode
	 */
	@GET
	@Path("add/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response add(@PathParam("id") String id) {
		return Response.ok(Story.addForm(id)).build();
	}
	
	/**
	 * Returns the form for adding new episodes
	 * 
	 * @param id
	 *            id of parent episode
	 * @return HTML form to add episode
	 */
	@GET
	@Path("modify/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response modify(@PathParam("id") String id) {
		return Response.ok(Story.modifyForm(id)).build();
	}

	/**
	 * Adds an episode to the story
	 * 
	 * @param id
	 *            id of parent episode
	 * @param title
	 *            title of new episode
	 * @param body
	 *            body of new episode
	 * @param author
	 *            author of new episode
	 * @return HTML success page with link to new episode
	 */
	@POST
	@Path("addpost/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response addpost(@PathParam("id") String id, @FormParam("link") String link,
			@FormParam("title") String title, @FormParam("body") String body, @FormParam("author") String author,
			@FormParam("g-recaptcha-response") String google) {
		
		String response = Strings.checkGoogle(google);
		switch(response) {
		case "true": break;
		case "false": return Response.ok("reCAPTCHA failed").build();
		default: return Response.ok(response).build();
		}
		return Response.ok(Story.addPost(id, link, title, body, author)).build();
	}
	
	/**
	 * Adds an episode to the story
	 * 
	 * @param id
	 *            id of parent episode
	 * @param title
	 *            title of new episode
	 * @param body
	 *            body of new episode
	 * @param author
	 *            author of new episode
	 * @return HTML success page with link to new episode
	 */
	@POST
	@Path("modifypost/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response modifypost(@PathParam("id") String id, @FormParam("link") String link,
			@FormParam("title") String title, @FormParam("body") String body, @FormParam("author") String author,
			@FormParam("password") String password, @FormParam("g-recaptcha-response") String google) {
		
		String response = Strings.checkGoogle(google);
		switch(response) {
		case "true": break;
		case "false": return Response.ok("reCAPTCHA failed").build();
		default: return Response.ok(response).build();
		}
		return Response.ok(Story.modifyPost(id, link, title, body, author, password)).build();
	}
}