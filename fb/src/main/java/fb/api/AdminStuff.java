package fb.api;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fb.Accounts;
import fb.Accounts.FBLoginException;
import fb.Strings;
import fb.objects.User;

@Path("")
public class AdminStuff {
	
	@GET
	@Path("admin")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response admin(@CookieParam("fbtoken") Cookie fbtoken) {
		User user;
		try {
			user = Accounts.getUser(fbtoken);
		} catch (FBLoginException e) {
			return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA","You must be logged in to do that")).build();
		}
		if (user.level<100) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA","You must be an admin to do that")).build();
		return Response.ok(Strings.getFile("adminform.html", fbtoken).replace("$EXTRA", "")).build();
	}
	
	@POST
	@Path("admin/makemod")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response makemod(@FormParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		return Response.ok(Accounts.changeLevel(id, (byte)10, fbtoken)).build(); //failed, try again
	}
	
	@POST
	@Path("admin/makeadmin")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response makeadmin(@FormParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		return Response.ok(Accounts.changeLevel(id, (byte)100, fbtoken)).build(); //failed, try again
	}
	
	@POST
	@Path("admin/makenormal")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response makenormal(@FormParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {
		return Response.ok(Accounts.changeLevel(id, (byte)1, fbtoken)).build(); //failed, try again
	}
}
