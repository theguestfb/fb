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
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import fb.Accounts;
import fb.Accounts.FBLoginException;
import fb.objects.User;
import fb.util.Strings;

@Path("fb")
public class AccountStuff {
	
	@GET
	@Path("becomeadmin")
	@Produces(MediaType.TEXT_HTML)
	public Response becomeAdmin(@CookieParam("fbtoken") Cookie fbtoken) {		
		return Response.ok(Accounts.becomeAdmin(fbtoken)).build();
	}
	
	@GET
	@Path("becomemod")
	@Produces(MediaType.TEXT_HTML)
	public Response becomeMod(@CookieParam("fbtoken") Cookie fbtoken) {		
		return Response.ok(Accounts.becomeMod(fbtoken)).build();
	}
	
	@GET
	@Path("becomenormal")
	@Produces(MediaType.TEXT_HTML)
	public Response becomeNormal(@CookieParam("fbtoken") Cookie fbtoken) {		
		return Response.ok(Accounts.becomeNormal(fbtoken)).build();
	}
	
	@GET
	@Path("user/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response user(@PathParam("id") String id, @CookieParam("fbtoken") Cookie fbtoken) {		
		return Response.ok(Accounts.getUserPage(id,fbtoken)).build();
	}
	
	/**
	 * Returns the form for logging in
	 * 
	 * @return HTML form to log in
	 */
	@GET
	@Path("login")
	@Produces(MediaType.TEXT_HTML)
	public Response login(@CookieParam("fbtoken") Cookie fbtoken) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		Strings.log("Someone's on the login page");
		if (Accounts.isLoggedIn(fbtoken)) return Response.ok("Already logged in").build();
		return Response.ok(Strings.getFile("loginform.html", fbtoken).replace("$EXTRA", "")).build();
	}
	
	@GET
	@Path("logout")
	@Produces(MediaType.TEXT_HTML)
	public Response logout(@CookieParam("fbtoken") Cookie fbtoken) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		Accounts.logout(fbtoken);
		return Response.seeOther(GetStuff.createURI("/fb")).build();
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
	@Produces(MediaType.TEXT_HTML)
	public Response loginpost(@FormParam("email") String email, @FormParam("password") String password,
			@FormParam("g-recaptcha-response") String google) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", null).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		Strings.log("Login attempt: " + email);
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
		System.out.println("Login success: " + email + " " + token);
		//return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "Successfully logged in")).build();
		return Response.seeOther(GetStuff.createURI("/fb")).cookie(fbtoken).build();
	}
	
	/**
	 * Confirms that email address exists and is accessible by user
	 * @param token
	 * @return
	 */
	@GET
	@Path("confirmaccount/{token}")
	@Produces(MediaType.TEXT_HTML)
	public Response confirmaccount(@PathParam("token") String token) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", null).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
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
	@Produces(MediaType.TEXT_HTML)
	public Response createaccount(@CookieParam("fbtoken") Cookie fbtoken) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
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
	@Produces(MediaType.TEXT_HTML)
	public Response createaccountpost(@FormParam("email") String email, @FormParam("username") String username, @FormParam("password") String password, @FormParam("password2") String password2, 
			@FormParam("author") String author, @FormParam("g-recaptcha-response") String google) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", null).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (Strings.RECAPTCHA) {
			String response = Strings.checkGoogle(google);
			switch(response) {
			case "true": break;
			case "false": return Response.ok("reCAPTCHA failed").build();
			default: return Response.ok(response).build();
			}
		}
		return Response.ok(Accounts.create(email, password, password2, author, username)).build();
	}
	
	@GET
	@Path("useraccount")
	@Produces(MediaType.TEXT_HTML)
	public Response useraccount(@CookieParam("fbtoken") Cookie fbtoken) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (!Accounts.isLoggedIn(fbtoken)) return Response.ok("You must be logged in to do that").build();
		User user;
		try {
			user = Accounts.getUser(fbtoken);
		} catch (FBLoginException e) {
			return Response.ok("You must be logged in to do that").build();
		}
		
		return Response.ok(Strings.getFile("useraccount.html", fbtoken).replace("$ID", user.id)).build();
	}
	
	@GET
	@Path("changeauthor")
	@Produces(MediaType.TEXT_HTML)
	public Response changeauthor(@CookieParam("fbtoken") Cookie fbtoken) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (!Accounts.isLoggedIn(fbtoken)) return Response.ok("You must be logged in to do that").build();
		return Response.ok(Strings.getFile("changeauthorform.html", fbtoken).replace("$EXTRA", "")).build();
	}
	
	@POST
	@Path("changeauthorpost")
	@Produces(MediaType.TEXT_HTML)
	public Response changeauthorpost(@FormParam("author") String author, @CookieParam("fbtoken") Cookie fbtoken, @FormParam("g-recaptcha-response") String google) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (Strings.RECAPTCHA) {
			String response = Strings.checkGoogle(google);
			switch(response) {
			case "true": break;
			case "false": return Response.ok("reCAPTCHA failed").build();
			default: return Response.ok(response).build();
			}
		}
		try {
			Accounts.changeAuthor(fbtoken, author);
		} catch (FBLoginException e) {
			return Response.ok(e.getMessage()).build();  //failed, try again
		}
		return Response.seeOther(GetStuff.createURI("/fb/useraccount")).build(); //redirect on success
	}
	
	@GET
	@Path("changetheme")
	@Produces(MediaType.TEXT_HTML)
	public Response changetheme(@CookieParam("fbtoken") Cookie fbtoken) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (!Accounts.isLoggedIn(fbtoken)) return Response.ok("You must be logged in to do that").build();
		return Response.ok(Strings.getFile("changethemeform.html", fbtoken).replace("$EXTRA", "").replace("$THEMES", Strings.getSelectThemes())).build();
	}
	
	@POST
	@Path("changethemepost")
	@Produces(MediaType.TEXT_HTML)
	public Response changethemepost(@FormParam("theme") String theme, @CookieParam("fbtoken") Cookie fbtoken, @FormParam("g-recaptcha-response") String google) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (Strings.RECAPTCHA) {
			String response = Strings.checkGoogle(google);
			switch(response) {
			case "true": break;
			case "false": return Response.ok("reCAPTCHA failed").build();
			default: return Response.ok(response).build();
			}
		}
		try {
			Accounts.changeTheme(fbtoken, theme);
		} catch (FBLoginException e) {
			return Response.ok(e.getMessage()).build();  //failed, try again
		}
		return Response.seeOther(GetStuff.createURI("/fb/useraccount")).build(); //redirect on success
	}
	
	@GET
	@Path("changebio")
	@Produces(MediaType.TEXT_HTML)
	public Response changebio(@CookieParam("fbtoken") Cookie fbtoken) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (!Accounts.isLoggedIn(fbtoken)) return Response.ok("You must be logged in to do that").build();
		String bio;
		try {
			bio = Accounts.getUser(fbtoken).bio;
		} catch (FBLoginException e) {
			return Response.ok("You must be logged in to do that").build();
		}
		return Response.ok(Strings.getFile("changebioform.html", fbtoken).replace("$EXTRA", "").replace("$BODY", bio)).build();
	}
	
	@POST
	@Path("changebiopost")
	@Produces(MediaType.TEXT_HTML)
	public Response changebiopost(@FormParam("bio") String bio, @CookieParam("fbtoken") Cookie fbtoken, @FormParam("g-recaptcha-response") String google) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (Strings.RECAPTCHA) {
			String response = Strings.checkGoogle(google);
			switch(response) {
			case "true": break;
			case "false": return Response.ok("reCAPTCHA failed").build();
			default: return Response.ok(response).build();
			}
		}
		try {
			Accounts.changeBio(fbtoken, bio);
		} catch (FBLoginException e) {
			return Response.ok(e.getMessage()).build();  //failed, try again
		}
		return Response.seeOther(GetStuff.createURI("/fb/useraccount")).build(); //redirect on success
	}
	
	@GET
	@Path("changepassword")
	@Produces(MediaType.TEXT_HTML)
	public Response changepassword(@CookieParam("fbtoken") Cookie fbtoken) {	
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (!Accounts.isLoggedIn(fbtoken)) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA","You must be logged in to do that")).build();
		return Response.ok(Strings.getFile("changepasswordform.html", fbtoken).replace("$EXTRA", "")).build();
	}
	
	@POST
	@Path("changepasswordpost")
	@Produces(MediaType.TEXT_HTML)
	public Response changepasswordpost(@FormParam("newpass") String newpass, @FormParam("newpass2") String newpass2 ,@FormParam("password") String password, @CookieParam("fbtoken") Cookie fbtoken, @FormParam("g-recaptcha-response") String google) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (Strings.RECAPTCHA) {
			String response = Strings.checkGoogle(google);
			switch(response) {
			case "true": break;
			case "false": return Response.ok("reCAPTCHA failed").build();
			default: return Response.ok(response).build();
			}
		}
		try {
			Accounts.changePassword(fbtoken, newpass, newpass2, password);
		} catch (FBLoginException e) {
			return Response.ok(e.getMessage()).build(); //failed, try again
		}
		return Response.seeOther(GetStuff.createURI("/fb/useraccount")).build(); //redirect on success
	}
	
	@GET
	@Path("changeemail")
	@Produces(MediaType.TEXT_HTML)
	public Response changeemail(@CookieParam("fbtoken") Cookie fbtoken) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (!Accounts.isLoggedIn(fbtoken)) return Response.ok("You must be logged in to do that").build();
		return Response.ok(Strings.getFile("changeemailform.html", fbtoken).replace("$EXTRA", "")).build();
	}
	
	@POST
	@Path("changeemailpost")
	@Produces(MediaType.TEXT_HTML)
	public Response changeemailpost(@FormParam("email") String email, @FormParam("password") String password, @CookieParam("fbtoken") Cookie fbtoken, @FormParam("g-recaptcha-response") String google) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		if (Strings.RECAPTCHA) {
			String response = Strings.checkGoogle(google);
			switch(response) {
			case "true": break;
			case "false": return Response.ok("reCAPTCHA failed").build();
			default: return Response.ok(response).build();
			}
		}
		return Response.ok(Accounts.changeEmail(fbtoken, email, password)).build();
	}
	
	/**
	 * Confirms that email address exists and is accessible by user
	 * @param token
	 * @return
	 */
	@GET
	@Path("confirmemailchange/{token}")
	@Produces(MediaType.TEXT_HTML)
	public Response confirmemailchange(@PathParam("token") String token, @CookieParam("fbtoken") Cookie fbtoken) {
		if (Strings.READ_ONLY_MODE) return Response.ok(Strings.getFile("generic.html", fbtoken).replace("$EXTRA", "This site is currently in read-only mode.")).build();
		
		Strings.log("Verifying: " + token);
		return Response.ok(Accounts.verifyNewEmail(token, fbtoken)).build();
	}
}