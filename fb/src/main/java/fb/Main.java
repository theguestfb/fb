package fb;

import java.io.IOException;
import java.net.URI;
import java.util.Scanner;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import fb.api.AccountStuff;
import fb.api.AddStuff;
import fb.api.AdminStuff;
import fb.api.GetStuff;
import fb.api.LegacyStuff;
import fb.api.RssStuff;
import fb.db.DB;
import fb.db.DB.DBException;
import fb.db.InitDB;

public class Main {

	public static void main(String[] args) throws IOException {
		
		if (args.length > 1) usage();
		if (args.length == 0) runServer();
		else switch (args[0].trim().toLowerCase()) {
		case "run":
			runServer();
			break;
		case "init":
			try {
				InitDB.doImport();
			} catch (DBException e) {
				System.err.println("DBException: " + e.getMessage());
				System.exit(2);
			}
			break;
		case "count":
			InitDB.countDB();
			break;
		default:
			usage();
		}
	}
	
	private static void usage() {
		System.err.println("USAGE: (run | init | count)");
		System.err.println("If no option is specified, run is default");
		System.exit(1);
	}
	
	private static void runServer() {
		Strings.log("Hi");
		URI baseUri = UriBuilder.fromUri("http://localhost/fb/").port(8080).build();
		ResourceConfig resourceConfig = new ResourceConfig(AccountStuff.class, AddStuff.class, AdminStuff.class,
				GetStuff.class, LegacyStuff.class, RssStuff.class);
		HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);
		System.out.println("Type exit to quit.");
		try (Scanner in = new Scanner(System.in)) {
			while (true) if (in.nextLine().trim().toLowerCase().equals("exit")) break;
		}

		server.shutdown();
		DB.closeSession();

		Strings.log("Bye");
		System.exit(0);
	}

}
