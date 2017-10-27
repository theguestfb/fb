package fb.objects;

import java.util.ArrayList;

import fb.db.DBEpisode;
import fb.db.DBRecents;

public class Recents {
	public final ArrayList<Episode> recents;
	
	public Recents(DBRecents recents) {
		this.recents = new ArrayList<>();
		for (DBEpisode ep : recents.getRecents()) this.recents.add(new Episode(ep));
	}
}
