package fb.objects;

import java.util.ArrayList;

import fb.db.DBEpisode;
import fb.db.DBRootEpisodes;

public class EpisodeList {
	public final ArrayList<Episode> episodes;
	
	public EpisodeList(DBRootEpisodes roots) {
		this.episodes = new ArrayList<>();
		for (DBEpisode ep : roots.getRoots()) this.episodes.add(new Episode(ep));
	}
	
	public EpisodeList(ArrayList<Episode> list) {
		this.episodes = new ArrayList<>(list);
	}
}
