package fb.api;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fb.Story;
import fb.Strings;
import fb.db.DB;
import fb.db.DB.DBException;
import fb.objects.Episode;

@Path("")
public class GetStuff {
	
	public static URI createURI(String url) {
		try {
			return new URI("https", Strings.DOMAIN, url, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Displays welcome page (not intro page, which is statically served by nginx)
	 * 
	 * @return HTML welcome page
	 */
	@GET
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response getRoot(@CookieParam("fbtoken") Cookie fbtoken) {
		return Response.ok(Story.getWelcome(fbtoken)).build();
	}

	/**
	 * Redirects to welcome page
	 * @return HTTP 302 redirect
	 */
	@GET
	@Path("get")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response getGet() {
		return Response.temporaryRedirect(createURI("/fb")).build();
	}

	/**
	 * Gets an episode by its id, default sort
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return HTML episode with id
	 */
	@GET
	@Path("get/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response get(@PathParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		return Response.ok(Story.getHTML(id, 0, "0", fbtoken)).build();
	}
	
	/**
	 * Gets an episode by its id, default sort
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return HTML episode with id
	 */
	@GET
	@Path("get/{id}/{settings}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response get(@PathParam("id") String id, @PathParam("settings") String settings, @CookieParam("fbtoken") Cookie fbtoken) {
		return Response.ok(Story.getHTML(id, 0, settings, fbtoken)).build();
	}

	/**
	 * Gets an episode by its id, newest first sort
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return HTML episode
	 */
	@GET
	@Path("getnewest/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response getnewest(@PathParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		return Response.ok(Story.getHTML(id, 1, "0", fbtoken)).build();
	}

	/**
	 * Gets an episode by its id, most children first sort
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return HTML episode
	 */
	@GET
	@Path("getmostfirst/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response getmostfirst(@PathParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		return Response.ok(Story.getHTML(id, 2, "0", fbtoken)).build();
	}

	/**
	 * Gets an episode by its id, least children first sort
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return HTML episode
	 */
	@GET
	@Path("getleastfirst/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response getleastfirst(@PathParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		return Response.ok(Story.getHTML(id, 3, "0", fbtoken)).build();
	}

	/**
	 * Gets an episode by its id, least children first sort
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return HTML episode
	 */
	@GET
	@Path("getrandom/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response getrandom(@PathParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		return Response.ok(Story.getHTML(id, 4, "0", fbtoken)).build();
	}
	
	/**
	 * Gets an episode by its id, least children first sort
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return HTML episode
	 */
	@GET
	@Path("getraw/{id}")
	@Produces(MediaType.TEXT_PLAIN + "; charset=UTF-8")
	public String getraw(@PathParam("id") String id) {
		Episode ep;
		try {
			ep = DB.getEp(id);
		} catch (DBException e) {
			return "Not found: " + id;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(ep.id + "\n");
		sb.append(ep.link + "\n");
		sb.append(ep.title + "\n");
		sb.append(ep.authorName + "\n");
		sb.append(Strings.outputDateFormat(ep.date) + "\n");
		sb.append(ep.body + "\n");
		return sb.toString();
	}

	/**
	 * Gets a list of recent episodes
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return HTML episode
	 */
	@GET
	@Path("recent")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response recent(@CookieParam("fbtoken") Cookie fbtoken) {
		return Response.ok(Story.getRecents(fbtoken)).build();
	}
	
	@GET
	@Path("formatting")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response formatting(@CookieParam("fbtoken") Cookie fbtoken) {
		return Response.ok(Strings.getFile("formatting.html", fbtoken)).build();
	}
}
