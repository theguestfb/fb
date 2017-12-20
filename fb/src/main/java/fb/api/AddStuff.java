package fb.api;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fb.Story;
import fb.Story.EpisodeException;
import fb.db.DB;
import fb.util.Strings;

@Path("fb")
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
	@Produces(MediaType.TEXT_HTML)
	public Response add(@PathParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		if (DB.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		return Response.ok(Story.addForm(id, fbtoken)).build();
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
	@Produces(MediaType.TEXT_HTML)
	public Response modify(@PathParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		if (DB.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		return Response.ok(Story.modifyForm(id, fbtoken)).build();
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
	@Produces(MediaType.TEXT_HTML)
	public Response addpost(@PathParam("id") String id, @FormParam("link") String link,
			@FormParam("title") String title, @FormParam("body") String body, 
			@CookieParam("fbtoken") Cookie fbtoken, @FormParam("g-recaptcha-response") String google) {
		if (DB.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (Strings.RECAPTCHA) {
			String response = Strings.checkGoogle(google);
			switch(response) {
			case "true": break;
			case "false": return Response.ok("reCAPTCHA failed").build();
			default: return Response.ok(response).build();
			}
		}
		try {
			String childID = Story.addPost(id, link, title, body, fbtoken);
			return Response.seeOther(GetStuff.createURI("/fb/get/" + childID)).build();
		} catch (EpisodeException e) {
			return Response.ok(e.getMessage()).build();
		}
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
	@Produces(MediaType.TEXT_HTML)
	public Response modifypost(@PathParam("id") String id, @FormParam("link") String link,
			@FormParam("title") String title, @FormParam("body") String body, 
			@CookieParam("fbtoken") Cookie fbtoken, @FormParam("g-recaptcha-response") String google) {
		if (DB.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (Strings.RECAPTCHA) {
			String response = Strings.checkGoogle(google);
			switch(response) {
			case "true": break;
			case "false": return Response.ok("reCAPTCHA failed").build();
			default: return Response.ok(response).build();
			}
		}
		try {
			String modifiedID = Story.modifyPost(id, link, title, body, fbtoken);
			return Response.seeOther(GetStuff.createURI("/fb/get/" + modifiedID)).build();
		} catch (EpisodeException e) {
			return Response.ok(e.getMessage()).build();
		}
		
	}
	
	@GET
	@Path("flag/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response flag(@PathParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		if (DB.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		return Response.ok(Story.flagForm(id, fbtoken)).build();
	}
	
	@POST
	@Path("flagpost/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response flagpost(@PathParam("id") String id, @FormParam("body") String body,
			@CookieParam("fbtoken") Cookie fbtoken, @FormParam("g-recaptcha-response") String google) {
		if (DB.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (Strings.RECAPTCHA) {
			String response = Strings.checkGoogle(google);
			switch(response) {
			case "true": break;
			case "false": return Response.ok("reCAPTCHA failed").build();
			default: return Response.ok(response).build();
			}
		}
		try {
			Story.flagPost(id, body, fbtoken);
			return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "Episode successfully flagged")).build();
		} catch (EpisodeException e) {
			return Response.ok(e.getMessage()).build();
		}
		
	}
}
