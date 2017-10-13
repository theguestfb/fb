package fb;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
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

import com.google.gson.Gson;

import fb.json.JsonCaptchaResponse;
@Path("")
public class API {
	
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
		if (cookieTest == null) return "fail";
		else return cookieTest.getValue();
	}
	
	/**
	 * Redirects to the root episode 
	 * @return HTML episode
	 */
	@GET
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response getRoot() {
		try {
			return Response.temporaryRedirect(new URI("/")).build();
		} catch (URISyntaxException e) {
			return get("1");
		}
	}
	
	/**
	 * Redirects to the root page 
	 * @return HTML episode
	 */
	@GET
	@Path("get")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response getGet() {
		try {
			return Response.temporaryRedirect(new URI("/")).build();
		} catch (URISyntaxException e) {
			return get("1");
		}
	}
	
	/**
	 * Gets an episode by its id, default sort
	 * @param id id of episode (1-7-4-...-3)
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
	 * @param id id of episode (1-7-4-...-3)
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
	 * @param id id of episode (1-7-4-...-3)
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
	 * @param id id of episode (1-7-4-...-3)
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
	 * @param id id of episode (1-7-4-...-3)
	 * @return HTML episode
	 */
	@GET
	@Path("getrandom/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response getrandom(@PathParam("id") String id) {
		return Response.ok(Story.getHTML(id, 4)).build();
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
	public Response addpost(@PathParam("id") String id, @FormParam("link") String link,
			@FormParam("title") String title, @FormParam("body") String body, @FormParam("author") String author, @FormParam("g-recaptcha-response") String google) {
		try {
			String params = "secret=" + Strings.RECAPTCHA_SECRET + "&response=" + google;
			
			URL url = new URL("https://www.google.com/recaptcha/api/siteverify");

			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			int plength = params.length();
			conn.setRequestProperty("Content-Length", String.valueOf(plength));
			conn.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			wr.writeBytes(params);
			wr.flush();
			wr.close();

			JsonCaptchaResponse jsonReturn = new Gson().fromJson(readStream(conn.getInputStream()), JsonCaptchaResponse.class);
			if (!jsonReturn.getSuccess()) Response.ok("reCAPTCHA failed").build();
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.ok(Story.addpost(id, link, title, body, author)).build();
	}
	
	private static String readStream(InputStream is) {
	    try {
	      ByteArrayOutputStream bo = new ByteArrayOutputStream();
	      int i = is.read();
	      while(i != -1) {
	        bo.write(i);
	        i = is.read();
	      }
	      return bo.toString();
	    } catch (IOException e) {
	      return "";
	    }
	}
}
