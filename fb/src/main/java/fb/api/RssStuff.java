package fb.api;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
	@Produces("application/rss+xml; charset=UTF-8")
	public Response getFeed() throws IOException, FeedException {
		Writer writer = new StringWriter();
		new SyndFeedOutput().output(generate(), writer);
		return Response.ok(writer.toString()).build();
	}

	private SyndFeed generate() {
		final SyndFeed feed = new SyndFeedImpl();
		feed.setFeedType("rss_2.0");
		feed.setTitle("Fiction Branches");
		feed.setLink("https://" + Strings.DOMAIN);
		feed.setDescription("Fiction Branches is an online software engine which allows the production of multi-plotted stories.");

		final ArrayList<SyndEntry> entries = new ArrayList<>();

		EpisodeList eps;
		try {
			eps = DB.getRecents(25);
		} catch (DBException e) {
			Strings.log("Couldn't get recents for RSS");
			return feed;
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

		return feed;
	}
}
