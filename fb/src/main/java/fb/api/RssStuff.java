package fb.api;
import static fb.util.Strings.escape;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

import fb.Story;
import fb.db.DB;
import fb.db.DB.DBException;
import fb.objects.Episode;
import fb.util.Strings;

@Path("fb")
public class RssStuff {
	
	@GET
	@Path("feed")
	@Produces("application/rss+xml")
	public Response getFeed() {
		String ret = feeds.get(0);
		if (ret == null || ret.length() == 0) return Response.ok(generateEmpty()).build();
		return Response.ok(feeds.get(0)).build();
	}
	
	@GET
	@Path("feed/{id}")
	@Produces("application/rss+xml")
	public Response getFeedStory(@PathParam("id") String id) {
		int story;
		try {
			story = Integer.parseInt(id);
		} catch (NumberFormatException e) {
			return getFeed();
		}
		String ret = feeds.get(story);
		if (ret == null || ret.length() == 0) return Response.ok(generateEmpty()).build();
		return Response.ok(feeds.get(story)).build();
	}

	private static HashMap<Integer,String> feeds;
	static {
		updateFeeds();
		Thread t = new Thread() {
			public void run() {
				final long sleepTime = 1000*60*60;
				while (true) {
					try {
						Thread.sleep(sleepTime);
						updateFeeds();
					} catch (InterruptedException e) {
						Strings.log("Feed updater thread interrupted");
					}
				}
			}
		};
		t.setName("RSSFeedUpdater");
		t.start();
	}
	
	private static void updateFeeds() {
		HashMap<Integer,String> list = new HashMap<>();
		list.put(0, generate(0));
		try {
			for (Episode root : DB.getRoots()) {
				int id = Integer.parseInt(root.id);
				list.put(id, generate(id));
			}
		} catch (DBException e) {
			Strings.log("Couldn't get roots for RSS");
		} finally {
			feeds = list;
			StringBuilder sb = new StringBuilder("Updated RSS feeds: ");
			for (int id : list.keySet()) sb.append(id + " ");
			Strings.log(sb.toString());
		}
	}
	
	private static String generate(int story) {
		final SyndFeed feed = new SyndFeedImpl();
		feed.setFeedType("rss_2.0");
		feed.setTitle("Fiction Branches");
		feed.setLink("https://" + Strings.DOMAIN);
		feed.setDescription("Fiction Branches is an online software engine which allows the production of multi-plotted stories.");
		final ArrayList<SyndEntry> entries = new ArrayList<>();
		Episode[] eps;
		try {
			eps = DB.getRecents(story, 25);
		} catch (DBException e) {
			Strings.log("Couldn't get recents for RSS");
			return feedToString(feed);
		}
		for (Episode ep : eps) {
			SyndEntry entry = new SyndEntryImpl();
			entry.setTitle(escape(ep.link));
			entry.setLink("https://" + Strings.DOMAIN + "/fb/get/" + ep.id);
			entry.setPublishedDate(ep.date);
			entry.setAuthor(escape(ep.authorName));
			
			SyndContent desc = new SyndContentImpl();
			desc.setType("text/html");
			StringBuilder body = new StringBuilder();
			body.append("<h1>" + escape(ep.title) + "</h1>\n");
			body.append(Story.formatBody(ep.body));
			desc.setValue(body.toString());
			entry.setDescription(desc);
			entries.add(entry);
		}

		feed.setEntries(entries);

		return feedToString(feed);
		
	}
	
	private static String generateEmpty() {
		final SyndFeed feed = new SyndFeedImpl();
		feed.setFeedType("rss_2.0");
		feed.setTitle("Fiction Branches");
		feed.setLink("https://" + Strings.DOMAIN);
		feed.setDescription("Fiction Branches is an online software engine which allows the production of multi-plotted stories.");
		final ArrayList<SyndEntry> entries = new ArrayList<>();
		feed.setEntries(entries);
		return feedToString(feed);
		
	}
	
	private static String feedToString(SyndFeed feed) {
		Writer writer = new StringWriter();
		try {
			new SyndFeedOutput().output(feed, writer);
		} catch (IOException e) {
			Strings.log("RSS: there was some problem writing to the Writer");
		} catch (FeedException e) {
			Strings.log("RSS: the XML representation for the feed could not be created");
		}
		return writer.toString();
	}
}
