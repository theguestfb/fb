package fb.api;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import fb.Accounts;
import fb.Accounts.FBLoginException;
import fb.Strings;

@Path("")
public class AccountStuff {
	/**
	 * Returns the form for logging in
	 * 
	 * @return HTML form to log in
	 */
	@GET
	@Path("login")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response login(@CookieParam("fbtoken") Cookie fbtoken) {
		if (Accounts.isLoggedIn(fbtoken)) return Response.ok("Already looged in").build();
		return Response.ok(Strings.getFile("loginform.html", fbtoken).replace("$EXTRA", "")).build();
	}

	/**
	 * Logs in to the site
	 * @param email email address
	 * @param password plaintext password
	 * @param google recaptcha 
	 * @return
	 */
	@POST
	@Path("loginpost")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response loginpost(@FormParam("email") String email, @FormParam("password") String password,
			@FormParam("g-recaptcha-response") String google) {
		
		if (Strings.RECAPTCHA) {
			String response = Strings.checkGoogle(google);
			switch(response) {
			case "true": break;
			case "false": return Response.ok("reCAPTCHA failed").build();
			default: return Response.ok(response).build();
			}
		}
		String token;
		try {
			token = Accounts.login(email, password);
		} catch (FBLoginException e){
			return Response.ok(e.getMessage()).build();
		}
		NewCookie fbtoken = new NewCookie("fbtoken", token);
		try {
			URI redirect = new URI("/fb");
			return Response.temporaryRedirect(redirect).cookie(fbtoken).build();
		} catch (URISyntaxException e) {
			return Response.ok("You've successfully logged in, but let Phoenix know that you didn't get redirected properly").cookie(fbtoken).build();
		}
	}
	
	/**
	 * Confirms that email address exists and is accessible by user
	 * @param token
	 * @return
	 */
	@GET
	@Path("confirmaccount/{token}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response confirmaccount(@PathParam("token") String token) {
		Strings.log("Verifying: " + token);
		return Response.ok(Accounts.verify(token)).build();
	}
	
	/**
	 * Returns form to create a new account
	 * @param fbtoken
	 * @return
	 */
	@GET
	@Path("createaccount")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response createaccount(@CookieParam("fbtoken") Cookie fbtoken) {
		if (Accounts.isLoggedIn(fbtoken)) return Response.ok("Already looged in").build();
		return Response.ok(Strings.getFile("createaccountform.html", fbtoken).replace("$EXTRA", "")).build();
	}
	
	/**
	 * Stores account details in memory and email user with verification link
	 * @param email
	 * @param password
	 * @param password2
	 * @param author
	 * @param google
	 * @return
	 */
	@POST
	@Path("createaccountpost")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response createaccountpost(@FormParam("email") String email, @FormParam("password") String password, @FormParam("password2") String password2, 
			@FormParam("author") String author, @FormParam("g-recaptcha-response") String google) {
		
		if (Strings.RECAPTCHA) {
			String response = Strings.checkGoogle(google);
			switch(response) {
			case "true": break;
			case "false": return Response.ok("reCAPTCHA failed").build();
			default: return Response.ok(response).build();
			}
		}
		return Response.ok(Accounts.create(email, password, password2, author)).build();
	}
}