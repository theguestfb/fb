package fb.objects;

import java.util.ArrayList;

import fb.db.DBEpisode;
import fb.db.DBUser;

public class User {
	public final String id;
	public final String author;
	public final byte level;
	public final ArrayList<Episode> episodes;
	public User(DBUser user) {
		this.id = user.getId();
		this.author = user.getAuthor();
		this.level = user.getLevel();
		this.episodes = new ArrayList<>();
		for (DBEpisode ep : user.getEpisodes()) this.episodes.add(new Episode(ep));
	}
}
