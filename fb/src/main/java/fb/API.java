package fb;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
@Path("")
public class API {
	
	/**
	 * Gets an episode by its id, as a JSON object
	 * 
	 * @param id of episode (1-7-4-...-3)
	 * @return JSon episode, or 404 if episode does not exist
	 */
	@GET
	@Path("getapi/{id}")
	@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
	public Response getapi(@PathParam("id") String id) {
		String r = Story.getAPI(id);
		return ((r==null)?Response.ok("not found").status(404):Response.ok(r)).build();
	}
	
	/**
	 * Adds an episode, as a JSON object, to the story
	 * 
	 * Note that the id and childIDs fields will be ignored, as IDs 
	 * are generated automatically and new episodes have no children.
	 * Those fields still need to be present though...probably...idk
	 * 
	 * @param ep JSon episode object (formatted the same as those returned by getapi/{id})
	 * @return JSon object containing id of new episode
	 */
	@POST
	@Path("addapi")
	@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
	public Response addapi(@FormParam("ep") String ep) {
		return Response.ok(Story.addAPI(ep)).build();
	}
	
	/**
	 * Redirects to the root episode 
	 * @return HTML episode
	 */
	@GET
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response getRoot() {
		try {
			return Response.temporaryRedirect(new URI("/fb/get/1")).build();
		} catch (URISyntaxException e) {
			return get("1");
		}
	}
	
	/**
	 * Redirects to the root episode 
	 * @return HTML episode
	 */
	@GET
	@Path("get")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response getGet() {
		try {
			return Response.temporaryRedirect(new URI("/fb/get/1")).build();
		} catch (URISyntaxException e) {
			return get("1");
		}
	}
	
	/**
	 * Gets an episode by its id 
	 * @param id id of episode (1-7-4-...-3)
	 * @return HTML episode
	 */
	@GET
	@Path("get/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response get(@PathParam("id") String id) {
		return Response.ok(Story.getHTML(id)).build();
	}
	
	/**
	 * Returns the form for adding new episodes
	 * @param id id of parent episode
	 * @return HTML form to add episode
	 */
	@GET
	@Path("add/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response add(@PathParam("id") String id) {
		return Response.ok(Story.addForm(id)).build();
	}
	
	/**
	 * Adds an episode to the story
	 * @param id id of parent episode
	 * @param title title of new episode
	 * @param body body of new episode
	 * @param author author of new episode
	 * @return HTML success page with link to new episode
	 */
	@POST
	@Path("addpost/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response addpost(@PathParam("id") String id, @FormParam("link") String link, @FormParam("title") String title, @FormParam("body") String body, @FormParam("author") String author) {
		return Response.ok(Story.addpost(id, link, title, body, author)).build();
	}
}
