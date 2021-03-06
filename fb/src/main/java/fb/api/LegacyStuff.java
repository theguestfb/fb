package fb.api;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fb.db.DB;
import fb.db.DB.DBException;
import fb.util.Strings;
@Path("fb")
public class LegacyStuff {
	/**
	 * Gets a legacy episode by its legacy id
	 * 
	 * @param id
	 *            id of episode (1-7-4-...-3)
	 * @return redirect to HTML episode
	 */
	@GET
	@Path("legacy/{oldId}")
	@Produces(MediaType.TEXT_HTML)
	public Response legacy(@PathParam("oldId") String oldId, @CookieParam("fbtoken") Cookie fbtoken) {
		String newId;
		System.out.println("0Redirecting to " + GetStuff.createURI("/fb"));
		try {
			newId = DB.getEpByLegacyId(oldId).id;
		} catch (DBException e) {
			return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "Not found: " + oldId)).build();
		}
		return Response.seeOther(GetStuff.createURI("/fb/get/" + newId)).build();
	}
	
	@GET
	@Path("legacy/the-forum/{oldId}")
	@Produces(MediaType.TEXT_HTML)
	public Response legacy1(@PathParam("oldId") String oldId, @CookieParam("fbtoken") Cookie fbtoken) {
		if (oldId.trim().toLowerCase().compareTo("root") == 0) return Response.seeOther(GetStuff.createURI("/fb/get/1")).build();
		return legacy(oldId, fbtoken);
	}
	
	@GET 
	@Path("legacy/you-are-what-you-wish/{oldId}")
	@Produces(MediaType.TEXT_HTML)
	public Response legacy2(@PathParam("oldId") String oldId, @CookieParam("fbtoken") Cookie fbtoken) {
		if (oldId.trim().toLowerCase().compareTo("root") == 0) return Response.seeOther(GetStuff.createURI("/fb/get/2")).build();
		return legacy(oldId, fbtoken);
	}
	
	@GET
	@Path("legacy/altered-fates/{oldId}")
	@Produces(MediaType.TEXT_HTML)
	public Response legacy3(@PathParam("oldId") String oldId, @CookieParam("fbtoken") Cookie fbtoken) {
		if (oldId.trim().toLowerCase().compareTo("root") == 0) return Response.seeOther(GetStuff.createURI("/fb/get/3")).build();
		return legacy(oldId, fbtoken);
	}
	
	@GET
	@Path("legacy/the-future-of-gaming/{oldId}")
	@Produces(MediaType.TEXT_HTML)
	public Response legacy4(@PathParam("oldId") String oldId, @CookieParam("fbtoken") Cookie fbtoken) {
		if (oldId.trim().toLowerCase().compareTo("root") == 0) return Response.seeOther(GetStuff.createURI("/fb/get/4")).build();
		return legacy(oldId, fbtoken);
	}
	
	@GET
	@Path("legacy/cgi-bin/fbstorypage.pl")
	@Produces(MediaType.TEXT_HTML)
	public Response legacy5(@QueryParam("page") String oldId, @CookieParam("fbtoken") Cookie fbtoken) {
		return legacy(oldId, fbtoken);
	}
	
	@GET
	@Path("legacy/cgi-bin/fblatest.pl")
	@Produces(MediaType.TEXT_HTML)
	public Response legacyRecent() {
		return Response.seeOther(GetStuff.createURI("/fb/recent")).build();
	}
	
	@GET
	@Path("legacy/{anything}/{oldId}")
	@Produces(MediaType.TEXT_HTML)
	public Response legacyCatchAll(@PathParam("oldId") String oldId, @PathParam("anything") String anything, @CookieParam("fbtoken") Cookie fbtoken) {
		System.out.println("1Redirecting to " + GetStuff.createURI("/fb"));
		if (oldId.trim().toLowerCase().compareTo("root") == 0) return Response.seeOther(GetStuff.createURI("/fb")).build();
		return legacy(oldId, fbtoken);
	}
	
	@GET
	@Path("legacy/{anything}/{anything2}/{oldId}")
	@Produces(MediaType.TEXT_HTML)
	public Response legacyCatchAll(@PathParam("oldId") String oldId, @CookieParam("fbtoken") Cookie fbtoken, @PathParam("anything") String anything, @PathParam("anything2") String anything2) {
		System.out.println("2Redirecting to " + GetStuff.createURI("/fb"));
		if (oldId.trim().toLowerCase().compareTo("root") == 0) return Response.seeOther(GetStuff.createURI("/fb")).build();
		return legacy(oldId, fbtoken);
	}
	
	@GET
	@Path("legacy/{anything}/{anything2}/{anything3}/{oldId}")
	@Produces(MediaType.TEXT_HTML)
	public Response legacyCatchAll(@PathParam("oldId") String oldId, @CookieParam("fbtoken") Cookie fbtoken, @PathParam("anything") String anything, @PathParam("anything3") String anything3, @PathParam("anything2") String anything2) {
		System.out.println("3Redirecting to " + GetStuff.createURI("/fb"));
		if (oldId.trim().toLowerCase().compareTo("root") == 0) return Response.seeOther(GetStuff.createURI("/fb")).build();
		return legacy(oldId, fbtoken);
	}
	
}
