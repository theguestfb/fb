package fb.api;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.google.common.html.HtmlEscapers;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

import fb.Story;
import fb.Strings;
import fb.db.DB;
import fb.db.DB.DBException;
import fb.objects.Episode;
import fb.objects.EpisodeList;

@Path("")
public class RssStuff {
	
	@GET
	@Path("feed")
	@Produces("application/rss+xml")
	public Response getFeed() {
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
		try {
			return Response.ok(feeds.get(story)).build();
		} catch (IndexOutOfBoundsException e) {
			return getFeed();
		}
	}

	private static TreeMap<Integer,String> feeds;
	
	static {
		updateFeeds();
		Thread t = new Thread() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(1000*60*60);
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
		TreeMap<Integer,String> list = new TreeMap<>();
		list.put(0, generate(0));
		try {
			for (Episode root : DB.getRoots().episodes) {
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

		EpisodeList eps;
		try {
			eps = DB.getRecents(story, 25);
		} catch (DBException e) {
			Strings.log("Couldn't get recents for RSS");
			return feedToString(feed);
		}
		for (Episode ep : eps.episodes) {
			SyndEntry entry = new SyndEntryImpl();
			entry.setTitle(HtmlEscapers.htmlEscaper().escape(ep.link));
			entry.setLink("https://" + Strings.DOMAIN + "/fb/get/" + ep.id);
			entry.setPublishedDate(ep.date);
			entry.setAuthor(HtmlEscapers.htmlEscaper().escape(ep.authorName));
			
			SyndContent desc = new SyndContentImpl();
			desc.setType("text/html");
			StringBuilder body = new StringBuilder();
			body.append("<h1>" + HtmlEscapers.htmlEscaper().escape(ep.title) + "</h1>\n");
			body.append(Story.formatBody(ep.body, 0));
			desc.setValue(body.toString());
			entry.setDescription(desc);
			entries.add(entry);
		}

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
