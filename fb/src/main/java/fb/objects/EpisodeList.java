package fb.objects;

import java.util.ArrayList;

import fb.db.DBEpisode;
import fb.db.DBRecents;
import fb.db.DBRootEpisodes;

public class EpisodeList {
	public final ArrayList<Episode> episodes;
	
	public EpisodeList(DBRecents recents) {
		this.episodes = new ArrayList<>();
		for (DBEpisode ep : recents.getRecents()) this.episodes.add(new Episode(ep));
	}
	
	public EpisodeList(DBRootEpisodes roots) {
		this.episodes = new ArrayList<>();
		for (DBEpisode ep : roots.getRoots()) this.episodes.add(new Episode(ep));
	}
	
	public EpisodeList(ArrayList<Episode> list) {
		this.episodes = new ArrayList<>(list);
	}
}
