package fb.objects;

import java.util.ArrayList;

import fb.db.DBEpisode;
import fb.db.DBUser;

/**
 * Immutable user object
 */
public class User {
	public final String id;
	public final String author;
	public final String bio;
	public final byte level;
	public final String theme; // HTML theme name
	public final String email;
	public final String hashedPassword;
	public final Episode[] episodes;
	
	/**
	 * Construct from DBUser database object
	 * @param user
	 */
	public User(DBUser user) {
		this.id = user.getId();
		this.author = user.getAuthor();
		this.bio = user.getBio();
		this.level = user.getLevel();
		this.theme = user.getTheme();
		this.email = user.getEmail();
		this.hashedPassword = user.getPassword();
		ArrayList<Episode> episodeList = new ArrayList<>();
		for (DBEpisode ep : user.getEpisodes()) episodeList.add(new Episode(ep));
		Episode[] arr = new Episode[episodeList.size()];
		this.episodes = episodeList.toArray(arr);
	}
}
