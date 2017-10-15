package fb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

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

import fb.db.DB;
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

	/**
	 * Returns the form for adding new episodes
	 * 
	 * @param id
	 *            id of parent episode
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
	@Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
	public Response addpost(@PathParam("id") String id, @FormParam("link") String link,
			@FormParam("title") String title, @FormParam("body") String body, @FormParam("author") String author,
			@FormParam("g-recaptcha-response") String google) {
		URL url;
		try {
			url = new URL("https://www.google.com/recaptcha/api/siteverify");
		} catch (MalformedURLException e1) {
			System.out.println("MalformedURLException? Really? wtf");
			return Response.ok("Tell Phoenix you got a MalformedURLException when adding an episode").build();
		}
		Map<String, String> params = new LinkedHashMap<>();
		params.put("secret", Strings.RECAPTCHA_SECRET);
		params.put("response", google);
		StringBuilder postData = new StringBuilder();
		byte[] postDataBytes;
		try {
			for (Map.Entry<String, String> param : params.entrySet()) {
				if (postData.length() != 0)
					postData.append('&');
				postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				postData.append('=');
				postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
			postDataBytes = postData.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.out.println("UnsupportedEncodingException? Really? wtf");
			return Response.ok("Tell Phoenix you got a UnsupportedEncodingException when adding an episode").build();
		}
		HttpURLConnection conn;
		try {
			conn = (HttpURLConnection) url.openConnection();
		} catch (IOException e2) {
			System.out.println("IOException1? Really? wtf");
			return Response.ok("Tell Phoenix you got a IOException1 when adding an episode").build();
		}
		try {
			conn.setRequestMethod("POST");
		} catch (ProtocolException e) {
			System.out.println("ProtocolException? Really? wtf");
			return Response.ok("Tell Phoenix you got a ProtocolException when adding an episode").build();
		}
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
		conn.setDoOutput(true);
		try {
			conn.getOutputStream().write(postDataBytes);
		} catch (IOException e1) {
			System.out.println("IOException2? Really? wtf");
			return Response.ok("Tell Phoenix you got a IOException2 when adding an episode").build();
		}
		Reader in;
		try {
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			System.out.println("UnsupportedEncodingException2? Really? wtf");
			return Response.ok("Tell Phoenix you got a UnsupportedEncodingException2 when adding an episode").build();
		} catch (IOException e) {
			System.out.println("IOException3? Really? wtf");
			return Response.ok("Tell Phoenix you got a IOException3 when adding an episode").build();
		}

		StringBuilder json = new StringBuilder();
		try {
			for (int c; (c = in.read()) >= 0;)
				json.append((char) c);
		} catch (IOException e) {
			System.out.println("IOException3? Really? wtf");
			return Response.ok("Tell Phoenix you got a IOException4 when adding an episode").build();
		}
		JsonCaptchaResponse response = new Gson().fromJson(json.toString(), JsonCaptchaResponse.class);
		if (!response.getSuccess())
			return Response.ok("reCAPTCHA failed").build();
		return Response.ok(Story.addpost(id, link, title, body, author)).build();
	}
}
