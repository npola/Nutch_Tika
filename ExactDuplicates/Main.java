import java.io.*;
import java.text.*;
import java.math.BigInteger;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.MessageDigest;

public class Main {

	public static class URLMetric {
		public BigInteger MinHash;
		public String URL;
		public List<String> nearDuplicates;
		public HashMap<String, URLMetric> hashmap;
	}

	static Pattern recPat, mimePat, urlPat, parsePat, conPat, datePat;
	static HashMap<String, URLMetric> hashMap;
	static File dump = null;
	static MessageDigest MD5Gen = null;

	public static void printMap(HashMap<String, URLMetric> mp) {
		Iterator it = mp.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			String url = (String) pair.getKey();
			URLMetric urlMetric = (URLMetric) pair.getValue();
			System.out.println(url + " = " + urlMetric.MinHash);
			it.remove(); // avoids a ConcurrentModificationException
		}
	}

	public static boolean ModifiedComparison(Date datetime,
		HashMap<String, Date> hm, String url) {                   // function created in case modified time needs to be compared
		Date matchdt = datetime;
		int j = 0;
		if (hm.containsKey(url)) {
			j = hm.get(url).compareTo(matchdt);
		}

		return (j == 0);
	}
	public static void main(String[] args) {
		String line = "";
		hashMap = new HashMap<String, URLMetric>();
		HashMap<String, String> hmap = new HashMap<String, String>();
		HashMap<String, Date> hm = new HashMap<String, Date>();
		List<String> exactdup = new ArrayList<String>();
		String recnumber = "", datevalue = "";
		try {
			MD5Gen = MessageDigest.getInstance("MD5");                 // MD5 hash java library declaration

			String recPattern = "Recno::";                             //Regular expression to parse the data
			String mimePattern = "(Content-Type=)([^ \\s \\n ;]*)";
			String urlPattern = "(URL:: )([^\\n]*)";
			String parsePattern = "(ParseText::[\n])(.*)";
			String datePattern = "(Modified time: )(.*)";
			// Wed Dec 31 16:00:00 PST 1969
			String contentPattern = "(Content:\n)([\\s\\S]*)(CrawlDatum::)";

			recPat = Pattern.compile(recPattern);
			mimePat = Pattern.compile(mimePattern);
			urlPat = Pattern.compile(urlPattern);
			parsePat = Pattern.compile(parsePattern);
			conPat = Pattern.compile(contentPattern);
			datePat = Pattern.compile(datePattern);

			int len = 3;

			try {
				dump = new File("testing.txt");
				BufferedReader br = new BufferedReader(new FileReader(dump));
				StringBuffer parseData = new StringBuffer();
				while ((line = br.readLine()) != null && len > 0) {               //Iterating through each line in the dump file
					Matcher recMat = recPat.matcher(line.trim());
					parseData.append("\n" + line.trim());

					if (recMat.find()) {
						recnumber = line;
						String parsedD = parseData.toString();
						Matcher parseMat = parsePat.matcher(parsedD);
						Matcher urlMat = urlPat.matcher(parsedD);
						Matcher mimeMat = mimePat.matcher(parsedD);
						Matcher dateMat = datePat.matcher(parsedD);

						if (mimeMat.find()
								&& mimeMat.group(2).equals("text/html")) {
							System.out.println("fhriueg " + line);
							if (parseMat.find() && urlMat.find()) {
								System.out.println(recnumber);
								if (dateMat.find()) {
									datevalue = dateMat.group(0).toString();
								}
								System.out.println("here is the url: "
										+ urlMat.group(2));
								System.out
										.println(parseMat.group(2).toString());
								MD5Gen.update(parseMat.group(2).getBytes());
								BigInteger bi = new BigInteger(1,
										MD5Gen.digest());
								String dt[] = datevalue.split(" ");
								String finaldate = dt[7] + "-" + dt[3] + "-"
										+ dt[4] + " " + dt[5];
								SimpleDateFormat sdf = new SimpleDateFormat(
										"yyyy-MMM-dd' 'HH:mm:ss");
								Date matchdt = null;
								try {
									matchdt = sdf.parse(finaldate);         //An approach was to compare "Modified time:" as well,discarded

								} catch (ParseException e) {
									e.printStackTrace();
								}
								if (hm.containsKey(urlMat.group(2))) {      //If url  is already present in the database, declare duplicate
									exactdup.add(urlMat.group(2));
								} else {
									hm.put(urlMat.group(2), matchdt);       //Update the databse if url not already in it
								}
								if (!hmap.containsKey(urlMat.group(2))) {     //If exact url not present check for content hash
									if (!hmap.containsValue(bi.toString())) {   //If content hash is not same, update database with url
										hmap.put(urlMat.group(2), bi.toString());
									} else {
										exactdup.add(urlMat.group(2));           //If content has same, declare as duplicate url
									}
								} else if (hmap.containsKey(urlMat.group(2))) {

									//if (ModifiedComparison(matchdt, hm,urlMat.group(2))) {
										exactdup.add(urlMat.group(2));
									//}
								}
							}
						}
						parseData = new StringBuffer();
					}
				}
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		} catch (NoSuchAlgorithmException exp) {
			System.err.println(exp.getMessage());
		} finally {
			// do nothing
		}

		System.out.println("These are the exact duplicate urls:");                   //List of all duplicate urls
		for (int i = 0; i < exactdup.size(); ++i) {
			System.out.print(exactdup.get(i));

		}
	}

}