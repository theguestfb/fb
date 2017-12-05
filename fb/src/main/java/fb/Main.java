package fb;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import fb.api.AccountStuff;
import fb.api.AddStuff;
import fb.api.AdminStuff;
import fb.api.CharsetResponseFilter;
import fb.api.GetStuff;
import fb.api.LegacyStuff;
import fb.api.RssStuff;
import fb.db.DB;
import fb.db.DB.DBException;
import fb.db.InitDB;
import fb.objects.Episode;

public class Main {

	public static void main(String[] args) {
		if (args.length > 1) usage();
		if (args.length == 0) runServer();
		else if (args[0].trim().length() == 0) runServer();
		else switch (args[0].trim().toLowerCase()) {
		case "run":
			runServer();
			break;
		case "init":
			initDB();
			break;
		case "count":
			InitDB.countDB();
			break;
		case "generate":
			try {
				System.out.println("Generating");
				InitDB.generateChildCounts();
			} catch (DBException e) {
				throw new RuntimeException(e);
			}
			break;
		default:
			System.err.println("Unknown argument: " + args[0] + " (" + args[0].length() + ")");
			usage();
		}
	}
	
	private static void usage() {
		System.err.println("USAGE: (run | init | count)");
		System.err.println("If no option is specified, run is default");
		System.exit(1);
	}
	
	private static void initDB() {
		try {
			InitDB.doImport();
		} catch (DBException e) {
			System.err.println("DBException: " + e.getMessage());
			System.exit(2);
		}
	}
	
	private static void runServer() {
		Strings.log("Hi");
		try {
			for (Episode rootEp : DB.getRoots()) {
				Strings.log("Found root episode: " + rootEp.id + " " + rootEp.link);
			}
		} catch (DBException e) {
			System.err.println("No root episodes found");
			throw new RuntimeException(e);
		}
		/*String myid = ("2-216-26-9-1-3-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-"
				+ "1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-2-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-"
				+ "1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-2-1-1-"
				+ "1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-"
				+ "1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-"
				+ "1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-2-1-1-3-2-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-2-"
				+ "2-1-1-1-1-1-2-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-"
				+ "1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-"
				+ "1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-"
				+ "1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-4-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-"
				+ "1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-"
				+ "2-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-2-1-1-1-1-1-1-1-1-2-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-"
				+ "1-2-1-1-2-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-"
				+ "1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-2-1-2-1-2-1-1-1-1-1-1-1-2-1-1-1-1-2-1-1-"
				+ "1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-2-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-2-1-"
				+ "1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-"
				+ "1-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-2-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1-1");
		try {
			System.out.println("Getting episode");
			Episode ep = DB.getEp(myid);
			System.out.println(ep.link + "\n" + ep.title);
			EpisodeList list = DB.getPath(myid);
			System.out.println(list.episodes[0].link);
		} catch (DBException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			System.exit(0);
		}*/
		URI baseUri = UriBuilder.fromUri("http://localhost/fb/").port(8080).build();
		ResourceConfig resourceConfig = new ResourceConfig(AccountStuff.class, AddStuff.class, AdminStuff.class,
				GetStuff.class, LegacyStuff.class, RssStuff.class);
		resourceConfig.register(CharsetResponseFilter.class);
		Strings.log("Starting server");
		try {
			GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig).start();
		} catch (IOException e) {
			Strings.log("Error starting server");
		}
	}
}
