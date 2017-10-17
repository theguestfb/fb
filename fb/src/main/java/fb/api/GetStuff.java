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
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import fb.Story;
import fb.Strings;
import fb.db.DB;

@Path("")
public class GetStuff {

	@GET
	@Path("test1")
	public Response createCookies() {
		NewCookie cookie1 = new NewCookie("cookieTest", "it worked!");
		Response.ResponseBuilder rb = Response.ok("myStrCookie, myDateCookie and myIntCookie sent to the browser");
		Response response = rb.cookie(cookie1).build();
		return response;
	}

	@GET
	@Path("test2")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public String readAllCookies(@CookieParam("cookieTest") Cookie cookieTest) {
		if (cookieTest == null)
			return "fail";
		else
			return cookieTest.getValue();
	}

	/**
	 * Redirects to the root episode
	 * 
	 * @return HTML episode
	 */
	@GET
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response getRoot() {
		return Response.ok(Strings.welcomeDefault).build();
	}

	/**
	 * Redirects to the root page
	 * 
	 * @return HTML episode
	 */
	@GET
	@Path("get")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response getGet() {
		return Response.ok(Strings.welcomeDefault).build();
	}

	/**
	 * Gets a legacy episode by its legacy id
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return redirect to HTML episode
	 */
	@GET
	@Path("legacy/{oldId}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response legacy(@PathParam("oldId") String oldId) {
		String newId = DB.getLegacyId(oldId);
		if (newId == null)
			return Response.ok("Not found: " + oldId).build();
		try {
			return Response.temporaryRedirect(new URI("/fb/get/" + newId)).build();
		} catch (URISyntaxException e) {
			return getRoot();
		}
	}

	/**
	 * Gets an episode by its id, default sort
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return HTML episode
	 */
	@GET
	@Path("get/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response get(@PathParam("id") String id) {
		return Response.ok(Story.getHTML(id, 0)).build();
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
	public Response getnewest(@PathParam("id") String id) {
		return Response.ok(Story.getHTML(id, 1)).build();
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
	public Response getmostfirst(@PathParam("id") String id) {
		return Response.ok(Story.getHTML(id, 2)).build();
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
	public Response getleastfirst(@PathParam("id") String id) {
		return Response.ok(Story.getHTML(id, 3)).build();
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
	public Response getrandom(@PathParam("id") String id) {
		return Response.ok(Story.getHTML(id, 4)).build();
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
	public Response recent() {
		return Response.ok(Story.getRecents()).build();
	}
}
