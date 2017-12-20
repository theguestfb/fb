package fb.objects;

import java.util.Date;

import fb.db.DBFlaggedEpisode;

public class FlaggedEpisode {
	public final long id;
	public final String text;
	public final Date date;
	public final User user;
	public final Episode episode;
	
	public FlaggedEpisode(DBFlaggedEpisode ep) {
		this.id = ep.getId();
		this.text = ep.getText();
		this.date = ep.getDate();
		this.user = new User(ep.getUser());
		this.episode = new Episode(ep.getEpisode());
	}
}
