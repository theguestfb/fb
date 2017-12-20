package fb;

import java.io.IOException;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

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
import fb.util.Strings;

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
		case "export":
			try {
				System.out.println("Exporting");
				InitDB.exportTemp();
				System.out.println("Done exporting");
				DB.closeSession();
				System.out.println("Goodbye");
				System.exit(0);
			} catch (DBException e) {
				throw new RuntimeException(e);
			}
			break;
		case "import":
			try {
				System.out.println("Importing");
				InitDB.importTemp();
				InitDB.generateChildCounts();
				System.out.println("Done importing");
				DB.closeSession();
				System.out.println("Goodbye");
				System.exit(0);
			} catch (DBException e) {
				throw new RuntimeException(e);
			}
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
		Strings.log("Started. Connecting to postgres"); // This line also starts the file watcher threads
		try {
			for (Episode rootEp : DB.getRoots()) {
				Strings.log("Found root episode: " + rootEp.id + " " + rootEp.link);
			}
		} catch (DBException e) {
			System.err.println("No root episodes found");
			throw new RuntimeException(e);
		}
		Strings.log("Postgres connected successfully");
		
		Accounts.bump(); // Force temp accounts to be loaded and account cleaner thread to start
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				Accounts.writeQueuesToFile();
			}
		});
		
		ResourceConfig resourceConfig = new ResourceConfig(AccountStuff.class, AddStuff.class, AdminStuff.class,
				GetStuff.class, LegacyStuff.class, RssStuff.class);
		resourceConfig.register(CharsetResponseFilter.class);
		Strings.log("Starting server");
		try {
			GrizzlyHttpServerFactory.createHttpServer(UriBuilder.fromUri("http://0.0.0.0/").port(Strings.READ_ONLY_MODE?8081:8080).build(), resourceConfig).start();
			Strings.log("Server started");
		} catch (IllegalArgumentException | UriBuilderException | IOException  e) {
			Strings.log("Could not start server: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
