package fb.json;

import java.util.Date;

import fb.objects.Episode;
import fb.objects.User;

/**
 * Immutable user object
 */
public class TempUser {
	private String id;
	private String author;
	private String bio;
	private byte level;
	private String theme; // HTML theme name
	private String email;
	private String hashedPassword;
	private TempEpisode[] episodes;
	
	/**
	 * Construct from DBUser database object
	 * @param user
	 */
	public TempUser(User user) {
		this.id = user.id;
		this.author = user.author;
		this.bio = user.bio;
		this.level = user.level;
		this.theme = user.theme;
		this.email = user.email;
		this.hashedPassword = user.hashedPassword;
		episodes = new TempEpisode[user.episodes.length];
		for (int i=0; i<episodes.length; ++i) episodes[i] = new TempEpisode(user.episodes[i]);
	}
	
	public TempUser() {}

	public String getId() {
		return id;
	}

	public String getAuthor() {
		return author;
	}

	public String getBio() {
		return bio;
	}

	public byte getLevel() {
		return level;
	}

	public String getTheme() {
		return theme;
	}
	
	public String getEmail() {
		return email;
	}

	public String getHashedPassword() {
		return hashedPassword;
	}

	public TempEpisode[] getEpisodes() {
		return episodes;
	}
	
	public static class TempEpisode {
		private String id;
		private String title;
		private String link;
		private String authorId;
		private String authorName;
		private boolean isLegacy;
		private String body;
		private Date date;
		private Date editDate;
		private String editorId;
		private String editorName;
		private int count;
		private int depth;
		private String parentId;
		
		/**
		 * Construct a complete Episode from a DBEpisode database object
		 * @param ep
		 */
		public TempEpisode(Episode ep) {
			this.id = ep.id;
			this.title = ep.title;
			this.link = ep.link;
			this.authorId = ep.authorId;
			this.authorName = ep.authorName;
			this.body = ep.body;
			this.date = ep.date;
			this.editDate = ep.editDate;
			this.editorId = ep.editorId;
			this.editorName = ep.editorName;
			this.isLegacy = ep.isLegacy;
			this.depth = ep.depth;
			this.count = ep.count;
			this.parentId = ep.parentId;
		}
		
		public TempEpisode() { }

		public String getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public String getLink() {
			return link;
		}

		public String getAuthorId() {
			return authorId;
		}

		public String getAuthorName() {
			return authorName;
		}

		public boolean isLegacy() {
			return isLegacy;
		}

		public String getBody() {
			return body;
		}

		public Date getDate() {
			return date;
		}

		public Date getEditDate() {
			return editDate;
		}

		public String getEditorId() {
			return editorId;
		}

		public String getEditorName() {
			return editorName;
		}

		public int getCount() {
			return count;
		}

		public int getDepth() {
			return depth;
		}

		public String getParentId() {
			return parentId;
		}

		public void setAuthorId(String userId) {
			this.authorId = userId;
		}
	}
}
