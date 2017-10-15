package fb;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Scanner;

import fb.db.DBEpisode;

public class Strings {
	public static String RECAPTCHA_SECRET = readFile("/opt/fb/recaptcha_secret.txt");
	
	public static final String welcomeDefault = readFile("/opt/fb/static_snippets/welcome.html");
	public static final String storyDefault = readFile("/opt/fb/static_snippets/story.html");
	public static final String formDefault = readFile("/opt/fb/static_snippets/addform.html");
	public static final String successDefault = readFile("/opt/fb/static_snippets/success.html");
	public static final String failureDefault = readFile("/opt/fb/static_snippets/failure.html");
	public static final String recentsDefault = readFile("/opt/fb/static_snippets/recents.html");
	
	public static String readFile(String path) {
		try {
			StringBuilder sb = new StringBuilder();
			Scanner f = new Scanner(new File(path));
			while (f.hasNextLine()) sb.append(f.nextLine() + "\n");
			f.close();
			return sb.toString();
		} catch (IOException e) {
			return "Not found";
		}
	}
	
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
			return keyStringComparator.compare(B.getId(), A.getId());
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
	
	/**
	 * Correctly sorts key strings. 1-10 will come after 1-9 instead of after 1-1
	 */
	public static Comparator<String> keyStringComparator = new Comparator<String>() {
		@Override
		public int compare(String A, String B) {
			//System.out.printf("Comparing \"%s\" with \"%s\"%n", A, B);
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
}
