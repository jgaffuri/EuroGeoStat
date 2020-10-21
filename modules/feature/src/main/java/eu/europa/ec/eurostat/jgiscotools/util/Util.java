
package eu.europa.ec.eurostat.jgiscotools.util;

import java.io.PrintStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author julien Gaffuri
 *
 */
public class Util {

	//round a double
	public static double round(double x, int decimalNB) {
		double pow = Math.pow(10, decimalNB);
		return ( (int)(x * pow + 0.5) ) / pow;
	}

	//print progress in %
	public static void printProgress(int nbDone, int nbTot) {
		int ratio = 100*nbDone/nbTot;
		int ratioP = 100*(nbDone-1)/nbTot;
		if(ratio != ratioP) System.out.println(ratio + "% done");
	}




	//convert scale into ground resolution
	public static double getGroundResolution(int scalek) { return getGroundResolution(scalek, 0.2); }
	public static double getGroundResolution(int scalek, double mapResolutionMM) { return scalek * mapResolutionMM; }

	
	//print stack (for debugging)
	public static void printStackOut(){ printStack(System.out);}
	public static void printStackErr(){ printStack(System.err);}
	private static void printStack(PrintStream ps){
		StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		for(int i=0; i<trace.length; i++){
			if(i==1 || i==2) continue;
			StackTraceElement se = trace[i];
			ps.println((i==0?"":"--- ")+se.toString());
		}
	}

	//clean string
	public static final Pattern DIACRITICS_AND_FRIENDS = Pattern.compile("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+");

	public static String stripDiacritics(String str) {
		str = Normalizer.normalize(str, Normalizer.Form.NFD);
		str = DIACRITICS_AND_FRIENDS.matcher(str).replaceAll("");
		return str;
	}

	public static String stripWeirdCaracters(String str) {
		String string = Normalizer.normalize(str, Normalizer.Form.NFD);
		return string.replaceAll("[^\\p{ASCII}]", "");
	}

	//PARIS into Paris
	public static String capitalizeOnlyFirstLetter(String s){
		String out = s.toLowerCase();
		return out.substring(0,1).toUpperCase() + out.substring(1);
	}



	//index list of hashmaps (only one value)
	public static HashMap<String, String> index(ArrayList<Map<String, String>> data, String indexKey, String valueCol) {
		HashMap<String, String> ind = new HashMap<String, String>();
		for(Map<String, String> elt : data)
			ind.put(elt.get(indexKey), elt.get(valueCol));
		return ind;
	}

	//index list of hashmaps (all values)
	public static HashMap<String,Map<String,String>> index(ArrayList<Map<String, String>> data, String indexKey) {
		HashMap<String,Map<String,String>> ind = new HashMap<>();
		for(Map<String, String> elt : data)
			ind.put(elt.get(indexKey), elt);
		return ind;
	}


	/**
	 * Get few random objects from an input collection
	 * 
	 * @param <T>
	 * @param col
	 * @param nb
	 * @return
	 */
	public static <T> Collection<T> getRandom(Collection<T> col, int nb) {
		ArrayList<T> list = new ArrayList<>();
		list.addAll(col);
		Collections.shuffle(list);
		HashSet<T> set = new HashSet<>();
		int i=0;
		for(T o : list) {
			set.add(o);
			i++;
			if(i==nb) break;
		}
		return set;
	}



	/**
	 * 
	 * value of a function whos value is between 0 and 1 and looks like that:
	 * ___
	 *    \
	 *     \___
	 * 
	 * @param x
	 * @param x1 the first value where the decrease from 1 starts.
	 * @param x2 the second value from which the returned value is 0.
	 * @return
	 */
	public static double getIndicatorValue(double x, double x1, double x2) {
		if(x < x1)
			return 1.0;
		else if (x > x2)
			return 0.0;
		else
			return (x-x2)/(x1-x2);
	}


}