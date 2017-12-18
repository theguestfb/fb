package fb.api;

import java.net.URI;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import fb.Story;
import fb.db.DB;
import fb.db.DB.DBException;
import fb.objects.Episode;
import fb.util.Strings;

@Path("fb")
public class GetStuff {
	
	public static URI createURI(String url) {
		URI uri = UriBuilder.fromPath(url).scheme("https").build();
		System.out.println("Redirecting to " + uri);
		return uri;
	}
	
	public static URI createURI(String url, HashMap<String,String> params) {
		UriBuilder builder = UriBuilder.fromPath(url).scheme("https");
		
		for (Entry<String,String> e : params.entrySet()) builder.queryParam(e.getKey(), e.getValue());
		
		URI uri = builder.build();
		System.out.println("Redirecting to " + uri);
		return uri;
	}
	
	/**
	 * Displays welcome page (not intro page, which is statically served by nginx)
	 * 
	 * @return HTML welcome page
	 */
	@GET
	@Produces(MediaType.TEXT_HTML)
	public Response getRoot(@CookieParam("fbtoken") Cookie fbtoken) {
		return Response.ok(Story.getWelcome(fbtoken)).build();
	}

	/**
	 * Redirects to welcome page
	 * @return HTTP 302 redirect
	 */
	@GET
	@Path("get")
	@Produces(MediaType.TEXT_HTML)
	public Response getGet() {
		return Response.seeOther(createURI("/fb")).build();
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
	@Produces(MediaType.TEXT_HTML)
	public Response get(@PathParam("id") String id, @QueryParam("sort") String sortString, @CookieParam("fbtoken") Cookie fbtoken) {
		int sort = 0;
		if (sortString != null) switch (sortString.toLowerCase()) {
		case "newest":
			sort = 1;
			break;
		case "mostfirst":
			sort = 2;
			break;
		case "leastfirst":
			sort = 3;
			break;
		case "random":
			sort = 4;
			break;
		}
		return Response.ok(Story.getHTML(id, sort, fbtoken)).build();
	}

	/**
	 * Gets an episode by its id, newest first sort
	 * 
	 * DEPRECATED! Will be removed in a future update
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return HTML episode
	 */
	@GET
	@Path("getnewest/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getnewest(@PathParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		//return Response.ok(Story.getHTML(id, 1, fbtoken)).build();
		HashMap<String,String> params = new HashMap<>();
		params.put("sort","newest");
		return Response.seeOther(createURI("/fb/get/" + id, params)).build();
	}

	/**
	 * Gets an episode by its id, most children first sort
	 * 
	 * DEPRECATED! Will be removed in a future update
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return HTML episode
	 */
	@GET
	@Path("getmostfirst/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getmostfirst(@PathParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		//return Response.ok(Story.getHTML(id, 2, fbtoken)).build();
		HashMap<String,String> params = new HashMap<>();
		params.put("sort","mostfirst");
		return Response.seeOther(createURI("/fb/get/" + id, params)).build();	}

	/**
	 * Gets an episode by its id, least children first sort
	 * 
	 * DEPRECATED! Will be removed in a future update
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return HTML episode
	 */
	@GET
	@Path("getleastfirst/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getleastfirst(@PathParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		//return Response.ok(Story.getHTML(id, 3, fbtoken)).build();
		HashMap<String,String> params = new HashMap<>();
		params.put("sort","leastfirst");
		return Response.seeOther(createURI("/fb/get/" + id, params)).build();	}

	/**
	 * Gets an episode by its id, least children first sort
	 * 
	 * DEPRECATED! Will be removed in a future update
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return HTML episode
	 */
	@GET
	@Path("getrandom/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getrandom(@PathParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		//return Response.ok(Story.getHTML(id, 4, fbtoken)).build();
		HashMap<String,String> params = new HashMap<>();
		params.put("sort","random");
		return Response.seeOther(createURI("/fb/get/" + id, params)).build();	}
	
	/**
	 * Gets an episode by its id, least children first sort
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return HTML episode
	 */
	@GET
	@Path("getraw/{id}")
	@Produces(MediaType.TEXT_PLAIN)
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

	@GET
	@Path("recent/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response recentstory(@CookieParam("fbtoken") Cookie fbtoken, @QueryParam("num") String num, @PathParam("id") String id) {
		return Response.ok(Story.getRecents(fbtoken, num, id)).build();
	}
	
	@GET
	@Path("recent")
	@Produces(MediaType.TEXT_HTML)
	public Response recent(@CookieParam("fbtoken") Cookie fbtoken, @QueryParam("num") String num) {
		return Response.ok(Story.getRecents(fbtoken, num, "0")).build();
	}
	
	@GET
	@Path("outline/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response outline(@CookieParam("fbtoken") Cookie fbtoken, @PathParam("id") String id, @QueryParam("depth") String depth) {
		return Response.ok(Story.getOutline(fbtoken, id, depth)).build();
	}
	
	@GET
	@Path("path/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response path(@CookieParam("fbtoken") Cookie fbtoken, @PathParam("id") String id) {
		return Response.ok(Story.getPath(fbtoken, id)).build();
	}
	
	@GET
	@Path("complete/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getcomplete(@CookieParam("fbtoken") Cookie fbtoken, @PathParam("id") String id) {
		System.out.println("Complete request : " + id);
		String ret = Story.getCompleteHTML(fbtoken, id);
		System.out.println("Complete return  : " + id);
		Response response = Response.ok(ret).build();
		System.out.println("Complete response: " + id);
		return response;
	}
	
	@GET
	@Path("formatting")
	@Produces(MediaType.TEXT_HTML)
	public Response formatting(@CookieParam("fbtoken") Cookie fbtoken) {
		return Response.ok(Strings.getFile("formatting.html", fbtoken)).build();
	}
}
