package fb;

import java.util.Comparator;

import fb.db.DBEpisode;

public class Comparators {

	/**
	 * Episode with most children comes first
	 */
	public static Comparator<DBEpisode> childrenMostLeastComparator = new Comparator<DBEpisode>() {
		@Override
		public int compare(DBEpisode A, DBEpisode B) {
			return Integer.compare(B.getChildren().size(), A.getChildren().size());
		}
	};
	/**
	 * Episode with least children comes first
	 */
	public static Comparator<DBEpisode> childrenLeastMostComparator = new Comparator<DBEpisode>() {
		@Override
		public int compare(DBEpisode A, DBEpisode B) {
			return Integer.compare(A.getChildren().size(), B.getChildren().size());
		}
	};
	/**
	 * Episode with later key comes first
	 */
	public static Comparator<DBEpisode> reverseKeyComparator = new Comparator<DBEpisode>() {
		@Override
		public int compare(DBEpisode A, DBEpisode B) {
			return Comparators.keyStringComparator.compare(B.getId(), A.getId());
		}
	};
	/**
	 * Correctly sorts key strings. 1-10 will come after 1-9 instead of after 1-1
	 */
	public static Comparator<String> keyStringComparator = new Comparator<String>() {
		@Override
		public int compare(String A, String B) {
			String[] a = A.split("-");
			String[] b = B.split("-");
			for (int i=0; i<a.length && i<b.length; ++i) {
				Integer x, y;
				try {
					x = Integer.parseInt(a[i]);
				} catch (NumberFormatException e) {
					throw new RuntimeException("Illegal keystring: " + A);
				}
				try {
					y = Integer.parseInt(b[i]);
				} catch (NumberFormatException e) {
					throw new RuntimeException("Illegal keystring: " + B);
				}
				int comp = x.compareTo(y);
				if (comp != 0) return comp;
			}
			return Integer.compare(a.length, b.length);
		}
		
	};
	
	/**
	 * Episode with earlier key comes first
	 */
	public static Comparator<DBEpisode> keyComparator = new Comparator<DBEpisode>() {
		@Override
		public int compare(DBEpisode A, DBEpisode B) {
			return keyStringComparator.compare(A.getId(), B.getId());
		}
	};

}
