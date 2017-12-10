package fb.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MyEscaper {
	public static final String escapeHtml4(final String input) {
		if (input == null) return "";
		final StringBuilder writer = new StringBuilder(input.length() * 2);
		int pos = 0;
		final int len = input.length();
		while (pos < len) {
			final int consumed = translateB(input, pos, writer);
			if (consumed == 0) {
				final char c1 = input.charAt(pos);
				writer.append(c1);
				pos++;
				if (Character.isHighSurrogate(c1) && pos < len) {
					final char c2 = input.charAt(pos);
					if (Character.isLowSurrogate(c2)) {
						writer.append(c2);
						pos++;
					}
				}
				continue;
			}
			for (int pt = 0; pt < consumed; pt++) {
				pos += Character.charCount(Character.codePointAt(input, pos));
			}
		}
		return writer.toString();
	}
	
	private static final ArrayList<LookupTranslator> translators = new ArrayList<>();
	
	private static int translateB(final CharSequence input, final int index, final StringBuilder out) {
		for (final LookupTranslator translator : translators) {
			final int consumed = translator.translate(input, index, out);
			if (consumed != 0) {
				return consumed;
			}
		}
		return 0;
	}

	private static class LookupTranslator {

		private final HashMap<String, String> lookupMap;
		private final HashSet<Character> prefixSet;
		private final int shortest;
		private final int longest;

		public LookupTranslator(final HashMap<CharSequence, CharSequence> lookupMap) {
			this.lookupMap = new HashMap<>();
			this.prefixSet = new HashSet<>();
			int currentShortest = Integer.MAX_VALUE;
			int currentLongest = 0;

			for (HashMap.Entry<CharSequence, CharSequence> pair : lookupMap.entrySet()) {
				this.lookupMap.put(pair.getKey().toString(), pair.getValue().toString());
				this.prefixSet.add(pair.getKey().charAt(0));
				final int sz = pair.getKey().length();
				if (sz < currentShortest) {
					currentShortest = sz;
				}
				if (sz > currentLongest) {
					currentLongest = sz;
				}
			}
			this.shortest = currentShortest;
			this.longest = currentLongest;
		}

		public int translate(final CharSequence input, final int index, final StringBuilder out) {

			if (prefixSet.contains(input.charAt(index))) {
				int max = longest;
				if (index + longest > input.length()) {
					max = input.length() - index;
				}

				for (int i = max; i >= shortest; i--) {
					final CharSequence subSeq = input.subSequence(index, index + i);
					final String result = lookupMap.get(subSeq.toString());

					if (result != null) {
						out.append(result);
						return i;
					}
				}
			}
			return 0;
		}
	}

	static {

		final HashMap<CharSequence, CharSequence> ISO8859_1_ESCAPE = new HashMap<>();
		final HashMap<CharSequence, CharSequence> HTML40_EXTENDED_ESCAPE = new HashMap<>();
		final HashMap<CharSequence, CharSequence> BASIC_ESCAPE = new HashMap<>();

		ISO8859_1_ESCAPE.put("\u00A0", "&nbsp;");
		ISO8859_1_ESCAPE.put("\u00A1", "&iexcl;");
		ISO8859_1_ESCAPE.put("\u00A2", "&cent;");
		ISO8859_1_ESCAPE.put("\u00A3", "&pound;");
		ISO8859_1_ESCAPE.put("\u00A4", "&curren;");
		ISO8859_1_ESCAPE.put("\u00A5", "&yen;");
		ISO8859_1_ESCAPE.put("\u00A6", "&brvbar;");
		ISO8859_1_ESCAPE.put("\u00A7", "&sect;");
		ISO8859_1_ESCAPE.put("\u00A8", "&uml;");
		ISO8859_1_ESCAPE.put("\u00A9", "&copy;");
		ISO8859_1_ESCAPE.put("\u00AA", "&ordf;");
		ISO8859_1_ESCAPE.put("\u00AB", "&laquo;");
		ISO8859_1_ESCAPE.put("\u00AC", "&not;");
		ISO8859_1_ESCAPE.put("\u00AD", "&shy;");
		ISO8859_1_ESCAPE.put("\u00AE", "&reg;");
		ISO8859_1_ESCAPE.put("\u00AF", "&macr;");
		ISO8859_1_ESCAPE.put("\u00B0", "&deg;");
		ISO8859_1_ESCAPE.put("\u00B1", "&plusmn;");
		ISO8859_1_ESCAPE.put("\u00B2", "&sup2;");
		ISO8859_1_ESCAPE.put("\u00B3", "&sup3;");
		ISO8859_1_ESCAPE.put("\u00B4", "&acute;");
		ISO8859_1_ESCAPE.put("\u00B5", "&micro;");
		ISO8859_1_ESCAPE.put("\u00B6", "&para;");
		ISO8859_1_ESCAPE.put("\u00B7", "&middot;");
		ISO8859_1_ESCAPE.put("\u00B8", "&cedil;");
		ISO8859_1_ESCAPE.put("\u00B9", "&sup1;");
		ISO8859_1_ESCAPE.put("\u00BA", "&ordm;");
		ISO8859_1_ESCAPE.put("\u00BB", "&raquo;");
		ISO8859_1_ESCAPE.put("\u00BC", "&frac14;");
		ISO8859_1_ESCAPE.put("\u00BD", "&frac12;");
		ISO8859_1_ESCAPE.put("\u00BE", "&frac34;");
		ISO8859_1_ESCAPE.put("\u00BF", "&iquest;");
		ISO8859_1_ESCAPE.put("\u00C0", "&Agrave;");
		ISO8859_1_ESCAPE.put("\u00C1", "&Aacute;");
		ISO8859_1_ESCAPE.put("\u00C2", "&Acirc;");
		ISO8859_1_ESCAPE.put("\u00C3", "&Atilde;");
		ISO8859_1_ESCAPE.put("\u00C4", "&Auml;");
		ISO8859_1_ESCAPE.put("\u00C5", "&Aring;");
		ISO8859_1_ESCAPE.put("\u00C6", "&AElig;");
		ISO8859_1_ESCAPE.put("\u00C7", "&Ccedil;");
		ISO8859_1_ESCAPE.put("\u00C8", "&Egrave;");
		ISO8859_1_ESCAPE.put("\u00C9", "&Eacute;");
		ISO8859_1_ESCAPE.put("\u00CA", "&Ecirc;");
		ISO8859_1_ESCAPE.put("\u00CB", "&Euml;");
		ISO8859_1_ESCAPE.put("\u00CC", "&Igrave;");
		ISO8859_1_ESCAPE.put("\u00CD", "&Iacute;");
		ISO8859_1_ESCAPE.put("\u00CE", "&Icirc;");
		ISO8859_1_ESCAPE.put("\u00CF", "&Iuml;");
		ISO8859_1_ESCAPE.put("\u00D0", "&ETH;");
		ISO8859_1_ESCAPE.put("\u00D1", "&Ntilde;");
		ISO8859_1_ESCAPE.put("\u00D2", "&Ograve;");
		ISO8859_1_ESCAPE.put("\u00D3", "&Oacute;");
		ISO8859_1_ESCAPE.put("\u00D4", "&Ocirc;");
		ISO8859_1_ESCAPE.put("\u00D5", "&Otilde;");
		ISO8859_1_ESCAPE.put("\u00D6", "&Ouml;");
		ISO8859_1_ESCAPE.put("\u00D7", "&times;");
		ISO8859_1_ESCAPE.put("\u00D8", "&Oslash;");
		ISO8859_1_ESCAPE.put("\u00D9", "&Ugrave;");
		ISO8859_1_ESCAPE.put("\u00DA", "&Uacute;");
		ISO8859_1_ESCAPE.put("\u00DB", "&Ucirc;");
		ISO8859_1_ESCAPE.put("\u00DC", "&Uuml;");
		ISO8859_1_ESCAPE.put("\u00DD", "&Yacute;");
		ISO8859_1_ESCAPE.put("\u00DE", "&THORN;");
		ISO8859_1_ESCAPE.put("\u00DF", "&szlig;");
		ISO8859_1_ESCAPE.put("\u00E0", "&agrave;");
		ISO8859_1_ESCAPE.put("\u00E1", "&aacute;");
		ISO8859_1_ESCAPE.put("\u00E2", "&acirc;");
		ISO8859_1_ESCAPE.put("\u00E3", "&atilde;");
		ISO8859_1_ESCAPE.put("\u00E4", "&auml;");
		ISO8859_1_ESCAPE.put("\u00E5", "&aring;");
		ISO8859_1_ESCAPE.put("\u00E6", "&aelig;");
		ISO8859_1_ESCAPE.put("\u00E7", "&ccedil;");
		ISO8859_1_ESCAPE.put("\u00E8", "&egrave;");
		ISO8859_1_ESCAPE.put("\u00E9", "&eacute;");
		ISO8859_1_ESCAPE.put("\u00EA", "&ecirc;");
		ISO8859_1_ESCAPE.put("\u00EB", "&euml;");
		ISO8859_1_ESCAPE.put("\u00EC", "&igrave;");
		ISO8859_1_ESCAPE.put("\u00ED", "&iacute;");
		ISO8859_1_ESCAPE.put("\u00EE", "&icirc;");
		ISO8859_1_ESCAPE.put("\u00EF", "&iuml;");
		ISO8859_1_ESCAPE.put("\u00F0", "&eth;");
		ISO8859_1_ESCAPE.put("\u00F1", "&ntilde;");
		ISO8859_1_ESCAPE.put("\u00F2", "&ograve;");
		ISO8859_1_ESCAPE.put("\u00F3", "&oacute;");
		ISO8859_1_ESCAPE.put("\u00F4", "&ocirc;");
		ISO8859_1_ESCAPE.put("\u00F5", "&otilde;");
		ISO8859_1_ESCAPE.put("\u00F6", "&ouml;");
		ISO8859_1_ESCAPE.put("\u00F7", "&divide;");
		ISO8859_1_ESCAPE.put("\u00F8", "&oslash;");
		ISO8859_1_ESCAPE.put("\u00F9", "&ugrave;");
		ISO8859_1_ESCAPE.put("\u00FA", "&uacute;");
		ISO8859_1_ESCAPE.put("\u00FB", "&ucirc;");
		ISO8859_1_ESCAPE.put("\u00FC", "&uuml;");
		ISO8859_1_ESCAPE.put("\u00FD", "&yacute;");
		ISO8859_1_ESCAPE.put("\u00FE", "&thorn;");
		ISO8859_1_ESCAPE.put("\u00FF", "&yuml;");
		HTML40_EXTENDED_ESCAPE.put("\u0192", "&fnof;");
		HTML40_EXTENDED_ESCAPE.put("\u0391", "&Alpha;");
		HTML40_EXTENDED_ESCAPE.put("\u0392", "&Beta;");
		HTML40_EXTENDED_ESCAPE.put("\u0393", "&Gamma;");
		HTML40_EXTENDED_ESCAPE.put("\u0394", "&Delta;");
		HTML40_EXTENDED_ESCAPE.put("\u0395", "&Epsilon;");
		HTML40_EXTENDED_ESCAPE.put("\u0396", "&Zeta;");
		HTML40_EXTENDED_ESCAPE.put("\u0397", "&Eta;");
		HTML40_EXTENDED_ESCAPE.put("\u0398", "&Theta;");
		HTML40_EXTENDED_ESCAPE.put("\u0399", "&Iota;");
		HTML40_EXTENDED_ESCAPE.put("\u039A", "&Kappa;");
		HTML40_EXTENDED_ESCAPE.put("\u039B", "&Lambda;");
		HTML40_EXTENDED_ESCAPE.put("\u039C", "&Mu;");
		HTML40_EXTENDED_ESCAPE.put("\u039D", "&Nu;");
		HTML40_EXTENDED_ESCAPE.put("\u039E", "&Xi;");
		HTML40_EXTENDED_ESCAPE.put("\u039F", "&Omicron;");
		HTML40_EXTENDED_ESCAPE.put("\u03A0", "&Pi;");
		HTML40_EXTENDED_ESCAPE.put("\u03A1", "&Rho;");
		HTML40_EXTENDED_ESCAPE.put("\u03A3", "&Sigma;");
		HTML40_EXTENDED_ESCAPE.put("\u03A4", "&Tau;");
		HTML40_EXTENDED_ESCAPE.put("\u03A5", "&Upsilon;");
		HTML40_EXTENDED_ESCAPE.put("\u03A6", "&Phi;");
		HTML40_EXTENDED_ESCAPE.put("\u03A7", "&Chi;");
		HTML40_EXTENDED_ESCAPE.put("\u03A8", "&Psi;");
		HTML40_EXTENDED_ESCAPE.put("\u03A9", "&Omega;");
		HTML40_EXTENDED_ESCAPE.put("\u03B1", "&alpha;");
		HTML40_EXTENDED_ESCAPE.put("\u03B2", "&beta;");
		HTML40_EXTENDED_ESCAPE.put("\u03B3", "&gamma;");
		HTML40_EXTENDED_ESCAPE.put("\u03B4", "&delta;");
		HTML40_EXTENDED_ESCAPE.put("\u03B5", "&epsilon;");
		HTML40_EXTENDED_ESCAPE.put("\u03B6", "&zeta;");
		HTML40_EXTENDED_ESCAPE.put("\u03B7", "&eta;");
		HTML40_EXTENDED_ESCAPE.put("\u03B8", "&theta;");
		HTML40_EXTENDED_ESCAPE.put("\u03B9", "&iota;");
		HTML40_EXTENDED_ESCAPE.put("\u03BA", "&kappa;");
		HTML40_EXTENDED_ESCAPE.put("\u03BB", "&lambda;");
		HTML40_EXTENDED_ESCAPE.put("\u03BC", "&mu;");
		HTML40_EXTENDED_ESCAPE.put("\u03BD", "&nu;");
		HTML40_EXTENDED_ESCAPE.put("\u03BE", "&xi;");
		HTML40_EXTENDED_ESCAPE.put("\u03BF", "&omicron;");
		HTML40_EXTENDED_ESCAPE.put("\u03C0", "&pi;");
		HTML40_EXTENDED_ESCAPE.put("\u03C1", "&rho;");
		HTML40_EXTENDED_ESCAPE.put("\u03C2", "&sigmaf;");
		HTML40_EXTENDED_ESCAPE.put("\u03C3", "&sigma;");
		HTML40_EXTENDED_ESCAPE.put("\u03C4", "&tau;");
		HTML40_EXTENDED_ESCAPE.put("\u03C5", "&upsilon;");
		HTML40_EXTENDED_ESCAPE.put("\u03C6", "&phi;");
		HTML40_EXTENDED_ESCAPE.put("\u03C7", "&chi;");
		HTML40_EXTENDED_ESCAPE.put("\u03C8", "&psi;");
		HTML40_EXTENDED_ESCAPE.put("\u03C9", "&omega;");
		HTML40_EXTENDED_ESCAPE.put("\u03D1", "&thetasym;");
		HTML40_EXTENDED_ESCAPE.put("\u03D2", "&upsih;");
		HTML40_EXTENDED_ESCAPE.put("\u03D6", "&piv;");
		HTML40_EXTENDED_ESCAPE.put("\u2022", "&bull;");
		HTML40_EXTENDED_ESCAPE.put("\u2026", "&hellip;");
		HTML40_EXTENDED_ESCAPE.put("\u2032", "&prime;");
		HTML40_EXTENDED_ESCAPE.put("\u2033", "&Prime;");
		HTML40_EXTENDED_ESCAPE.put("\u203E", "&oline;");
		HTML40_EXTENDED_ESCAPE.put("\u2044", "&frasl;");
		HTML40_EXTENDED_ESCAPE.put("\u2118", "&weierp;");
		HTML40_EXTENDED_ESCAPE.put("\u2111", "&image;");
		HTML40_EXTENDED_ESCAPE.put("\u211C", "&real;");
		HTML40_EXTENDED_ESCAPE.put("\u2122", "&trade;");
		HTML40_EXTENDED_ESCAPE.put("\u2135", "&alefsym;");
		HTML40_EXTENDED_ESCAPE.put("\u2190", "&larr;");
		HTML40_EXTENDED_ESCAPE.put("\u2191", "&uarr;");
		HTML40_EXTENDED_ESCAPE.put("\u2192", "&rarr;");
		HTML40_EXTENDED_ESCAPE.put("\u2193", "&darr;");
		HTML40_EXTENDED_ESCAPE.put("\u2194", "&harr;");
		HTML40_EXTENDED_ESCAPE.put("\u21B5", "&crarr;");
		HTML40_EXTENDED_ESCAPE.put("\u21D0", "&lArr;");
		HTML40_EXTENDED_ESCAPE.put("\u21D1", "&uArr;");
		HTML40_EXTENDED_ESCAPE.put("\u21D2", "&rArr;");
		HTML40_EXTENDED_ESCAPE.put("\u21D3", "&dArr;");
		HTML40_EXTENDED_ESCAPE.put("\u21D4", "&hArr;");
		HTML40_EXTENDED_ESCAPE.put("\u2200", "&forall;");
		HTML40_EXTENDED_ESCAPE.put("\u2202", "&part;");
		HTML40_EXTENDED_ESCAPE.put("\u2203", "&exist;");
		HTML40_EXTENDED_ESCAPE.put("\u2205", "&empty;");
		HTML40_EXTENDED_ESCAPE.put("\u2207", "&nabla;");
		HTML40_EXTENDED_ESCAPE.put("\u2208", "&isin;");
		HTML40_EXTENDED_ESCAPE.put("\u2209", "&notin;");
		HTML40_EXTENDED_ESCAPE.put("\u220B", "&ni;");
		HTML40_EXTENDED_ESCAPE.put("\u220F", "&prod;");
		HTML40_EXTENDED_ESCAPE.put("\u2211", "&sum;");
		HTML40_EXTENDED_ESCAPE.put("\u2212", "&minus;");
		HTML40_EXTENDED_ESCAPE.put("\u2217", "&lowast;");
		HTML40_EXTENDED_ESCAPE.put("\u221A", "&radic;");
		HTML40_EXTENDED_ESCAPE.put("\u221D", "&prop;");
		HTML40_EXTENDED_ESCAPE.put("\u221E", "&infin;");
		HTML40_EXTENDED_ESCAPE.put("\u2220", "&ang;");
		HTML40_EXTENDED_ESCAPE.put("\u2227", "&and;");
		HTML40_EXTENDED_ESCAPE.put("\u2228", "&or;");
		HTML40_EXTENDED_ESCAPE.put("\u2229", "&cap;");
		HTML40_EXTENDED_ESCAPE.put("\u222A", "&cup;");
		HTML40_EXTENDED_ESCAPE.put("\u222B", "&int;");
		HTML40_EXTENDED_ESCAPE.put("\u2234", "&there4;");
		HTML40_EXTENDED_ESCAPE.put("\u223C", "&sim;");
		HTML40_EXTENDED_ESCAPE.put("\u2245", "&cong;");
		HTML40_EXTENDED_ESCAPE.put("\u2248", "&asymp;");
		HTML40_EXTENDED_ESCAPE.put("\u2260", "&ne;");
		HTML40_EXTENDED_ESCAPE.put("\u2261", "&equiv;");
		HTML40_EXTENDED_ESCAPE.put("\u2264", "&le;");
		HTML40_EXTENDED_ESCAPE.put("\u2265", "&ge;");
		HTML40_EXTENDED_ESCAPE.put("\u2282", "&sub;");
		HTML40_EXTENDED_ESCAPE.put("\u2283", "&sup;");
		HTML40_EXTENDED_ESCAPE.put("\u2284", "&nsub;");
		HTML40_EXTENDED_ESCAPE.put("\u2286", "&sube;");
		HTML40_EXTENDED_ESCAPE.put("\u2287", "&supe;");
		HTML40_EXTENDED_ESCAPE.put("\u2295", "&oplus;");
		HTML40_EXTENDED_ESCAPE.put("\u2297", "&otimes;");
		HTML40_EXTENDED_ESCAPE.put("\u22A5", "&perp;");
		HTML40_EXTENDED_ESCAPE.put("\u22C5", "&sdot;");
		HTML40_EXTENDED_ESCAPE.put("\u2308", "&lceil;");
		HTML40_EXTENDED_ESCAPE.put("\u2309", "&rceil;");
		HTML40_EXTENDED_ESCAPE.put("\u230A", "&lfloor;");
		HTML40_EXTENDED_ESCAPE.put("\u230B", "&rfloor;");
		HTML40_EXTENDED_ESCAPE.put("\u2329", "&lang;");
		HTML40_EXTENDED_ESCAPE.put("\u232A", "&rang;");
		HTML40_EXTENDED_ESCAPE.put("\u25CA", "&loz;");
		HTML40_EXTENDED_ESCAPE.put("\u2660", "&spades;");
		HTML40_EXTENDED_ESCAPE.put("\u2663", "&clubs;");
		HTML40_EXTENDED_ESCAPE.put("\u2665", "&hearts;");
		HTML40_EXTENDED_ESCAPE.put("\u2666", "&diams;");
		HTML40_EXTENDED_ESCAPE.put("\u0152", "&OElig;");
		HTML40_EXTENDED_ESCAPE.put("\u0153", "&oelig;");
		HTML40_EXTENDED_ESCAPE.put("\u0160", "&Scaron;");
		HTML40_EXTENDED_ESCAPE.put("\u0161", "&scaron;");
		HTML40_EXTENDED_ESCAPE.put("\u0178", "&Yuml;");
		HTML40_EXTENDED_ESCAPE.put("\u02C6", "&circ;");
		HTML40_EXTENDED_ESCAPE.put("\u02DC", "&tilde;");
		HTML40_EXTENDED_ESCAPE.put("\u2002", "&ensp;");
		HTML40_EXTENDED_ESCAPE.put("\u2003", "&emsp;");
		HTML40_EXTENDED_ESCAPE.put("\u2009", "&thinsp;");
		HTML40_EXTENDED_ESCAPE.put("\u200C", "&zwnj;");
		HTML40_EXTENDED_ESCAPE.put("\u200D", "&zwj;");
		HTML40_EXTENDED_ESCAPE.put("\u200E", "&lrm;");
		HTML40_EXTENDED_ESCAPE.put("\u200F", "&rlm;");
		HTML40_EXTENDED_ESCAPE.put("\u2013", "&ndash;");
		HTML40_EXTENDED_ESCAPE.put("\u2014", "&mdash;");
		HTML40_EXTENDED_ESCAPE.put("\u2018", "&lsquo;");
		HTML40_EXTENDED_ESCAPE.put("\u2019", "&rsquo;");
		HTML40_EXTENDED_ESCAPE.put("\u201A", "&sbquo;");
		HTML40_EXTENDED_ESCAPE.put("\u201C", "&ldquo;");
		HTML40_EXTENDED_ESCAPE.put("\u201D", "&rdquo;");
		HTML40_EXTENDED_ESCAPE.put("\u201E", "&bdquo;");
		HTML40_EXTENDED_ESCAPE.put("\u2020", "&dagger;");
		HTML40_EXTENDED_ESCAPE.put("\u2021", "&Dagger;");
		HTML40_EXTENDED_ESCAPE.put("\u2030", "&permil;");
		HTML40_EXTENDED_ESCAPE.put("\u2039", "&lsaquo;");
		HTML40_EXTENDED_ESCAPE.put("\u203A", "&rsaquo;");
		HTML40_EXTENDED_ESCAPE.put("\u20AC", "&euro;");
		BASIC_ESCAPE.put("\"", "&quot;");
		BASIC_ESCAPE.put("&", "&amp;");
		BASIC_ESCAPE.put("<", "&lt;");
		BASIC_ESCAPE.put(">", "&gt;");

		translators.add(new LookupTranslator(BASIC_ESCAPE));
		translators.add(new LookupTranslator(ISO8859_1_ESCAPE));
		translators.add(new LookupTranslator(HTML40_EXTENDED_ESCAPE));
	}

}
