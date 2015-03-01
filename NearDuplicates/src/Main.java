import java.io.*;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;


public class Main {

    //URLMetric class is used to store important information about URL
    public static class URLMetric{
        public BigInteger MinHash;                  // Contains the fingerprint of the parsed text
        public String URL;                          // Fetched URL
        public HashMap<String,Integer> weights;     // Map to store weights of tokens in parsed text.
        public List<String> nearDuplicates;         // List of all the url's that are near duplicates of the URL
    }


    static Pattern recPat,mimePat,urlPat,parsePat,conPat;   // Regular Expression pattern object declarations;
    static HashMap<String,URLMetric> hashMap,dupHash;       // HashMap to store URLMetric information of original urls and their duplicates.
    static File dump = null;


    //This function is used to print the urls with their fingerprints and also list all its near-duplicates.
    public static void printMap(HashMap<String,URLMetric> mp) {
        Iterator it = mp.entrySet().iterator();                     //Iterator to iterate over map objects;
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String url = (String) pair.getKey();
            URLMetric urlMetric = (URLMetric) pair.getValue();

            if(urlMetric.nearDuplicates.size() != 0)                    //If the url has duplicates list the duplicate urls and their hamming distance.
            {
                System.out.println(url + " = " + urlMetric.MinHash);
                System.out.println("--Duplicates--");
                for(int i=0; i<urlMetric.nearDuplicates.size(); i++) {
                    BigInteger xor = dupHash.get(urlMetric.nearDuplicates.get(i)).MinHash.xor(urlMetric.MinHash); //Computes the hamming distance between the original url and its duplicate.
                    System.out.println("Dup: " + urlMetric.nearDuplicates.get(i)+" = "+ xor.toString(2));
                }
                System.out.println("");
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    //This function is used to compute the fingerprint of list of words that have significant weights.
    public static BigInteger computeFigerprint(List<String> words) throws NoSuchAlgorithmException
    {
        /**
         *      Algorithm for Calculating Fingerprint
         *
         *      1. Find 128-bit MD5 has on each word (w) in the list of words.
         *      2. Foreach bit i in 128 bit hash of w
         *          a. If i = 0, then add 1 to v[i]
         *          b. If i = 1, then sub 1 from v[i]
         *      3. Calculate 128-bit final fingerprint as
         *          a. If v[i] > 0, then v[i] = 1
         *          b. If v[i] < 0, then v[i] = 0
         */

        BigInteger temp;
        int v[] = new int[128];                                     //Array to store intermediate bits of 128-bit fingerprint.
        MessageDigest MD5Gen = MessageDigest.getInstance("MD5");    //Message Digest object to obtain MD5 of each word.
        StringBuilder buff = new StringBuilder();


        for(String word:words)
        {
            MD5Gen.reset();
            MD5Gen.update(word.getBytes());                         //Update digest with the bytes of the word.
            temp = new BigInteger(1,MD5Gen.digest());               //Obtain the 128-bit hash in decimal.
            String str = temp.toString(2);                          //Convert hash value from decimal to binary string.
            while(str.length() < 128)                               //Prepend with 0's if the binary hash is less than 128-bits
                str = 0+str;

            for(int j=0;j<128;j++)                                   //Calculate the intermediate fingerprint.
                if(str.charAt(j) == '0')
                    v[j] += 1;
                else
                    v[j] -= 1;
        }

        for(int i=0;i<128;i++)                                      //Generate the fingerprint.
            if(v[i] > 0)
                buff.append('1');
            else
                buff.append('0');

        return (new BigInteger(buff.toString(),2));                //Return the fingerprint.
    }

    // This function is used to remove stopwords from the parsed text, obtain weights for token words and compute fingerprint.
    public static URLMetric removeStopWords(String data) throws NoSuchAlgorithmException
    {
        data = data.trim();                                     //Ignore leading and tailing blank-spaces and new-line characters.

        if(data.equals("") || data.length() < 5)                //Ignore the parse text if its lesser than length 5, because it does not provide
            return null;                                        //appropriate information.

        URLMetric metric = new URLMetric();                     //Instantiating URLMetric class object.
        metric.weights = new HashMap<String,Integer>();
        List<String> wordList = new ArrayList<String>();
        String words[] = data.split(" ");                       //Split the string on space to obtain array of words.

        for(String word:words)
            if(!Stopwords.isStopword(word)) {                   //for each word check if its a stopword or not.
                wordList.add(word.toLowerCase());               //Add the lowercase of the token to a list of tokens
                if(metric.weights.containsKey(word)) {
                    int val = metric.weights.get(word) + 1;     //Compute the weight of the token.
                    metric.weights.put(word,val);
                }
                else
                    metric.weights.put(word,1);
            }

        //Collections.sort(wordList);                             //Sort the list of tokens in alphabetical order if required.
        metric.MinHash = computeFigerprint(wordList);           //Compute the fingerprint for the list of words.

        return metric;
    }

    public static void main(String[] args) {
        String line = "";
        hashMap = new HashMap<String, URLMetric>();
        dupHash = new HashMap<String, URLMetric>();

        try {

            String recPattern = "Recno::";                                      //Regex pattern to obtain Records Numbers.
            String mimePattern = "(Content-Type=)([^ \\s \\n ;]*)";             //Regex pattern to obtain Mime Types.
            String urlPattern = "(URL:: )([^ \\n]*)";                           //Regex pattern to obtain URL
            String parsePattern = "(ParseText::[\n])(.*)";                      //Regex pattern to obtain parsed text.

            recPat = Pattern.compile(recPattern);
            mimePat = Pattern.compile(mimePattern);
            urlPat = Pattern.compile(urlPattern);
            parsePat = Pattern.compile(parsePattern);

            try {
                dump = new File(args[0]);                                       //Input the path for dump file of Segments of your crawl.
                BufferedReader br = new BufferedReader(new FileReader(dump));
                StringBuffer parseData = new StringBuffer();
                while ((line = br.readLine()) != null) {
                    Matcher recMat = recPat.matcher(line.trim());                   //Apply Record Regex.
                    parseData.append("\n").append(line);
                    if (recMat.find()) {                                            //Check if the line is Starting of a new record.

                            String parsedD = parseData.toString();                  //Obtain the entire text of the record.
                            Matcher parseMat = parsePat.matcher(parsedD);           //Apply Regex for Parsed Data, URL and Mime Type.
                            Matcher urlMat = urlPat.matcher(parsedD);
                            Matcher mimeMat = mimePat.matcher(parsedD);

                            if(mimeMat.find() && mimeMat.group(2).equals("text/html")) {                //Retrieve only HTML files
                                if (parseMat.find() && urlMat.find()) {                                 //Obtain parsed text of the HTML content. This parsed text is generated from he HTML parser included in the Tika Parser
                                    URLMetric metric = removeStopWords(parseMat.group(2));               //obtain URL information.
                                    if(metric == null) {                                                // If it does not contain parsed text or the text it not sufficient to generate fingerprint obtain next record.
                                        parseData = new StringBuffer();
                                        continue;
                                    }

                                    BigInteger temp;
                                    int k = -1;                                                         //Initialize temporary hamming distance to -1.
                                    String url = "";

                                    Iterator it = hashMap.entrySet().iterator();                        //Iterator to iterate over map objects and compare with other url fingerprints.

                                    while (it.hasNext() && (k > 0 || k == -1)) {
                                        Map.Entry map = (Map.Entry) it.next();
                                        URLMetric value = (URLMetric) map.getValue();
                                        temp = metric.MinHash.xor(value.MinHash);
                                        String t = temp.toString(2);
                                        int l = 0;

                                        for (int j = 0; j < t.length(); j++)                            //Compute hamming distance of the current url with already seen url.
                                            if (t.charAt(j) == '1')
                                                l++;

                                        if (k == -1 || l < k) {
                                            k = l;
                                            url = value.URL;
                                        }

                                    }

                                    if (k == -1 || k > 5) {                                             //If the hamming distance is greater than 5, it is considered as not a near duplicate.
                                                                                                        //this value of hamming distance was chosen based on the fetched urls data-set after several trials of different values.
                                        metric.URL = urlMat.group(2);
                                        metric.nearDuplicates = new ArrayList<String>();
                                        hashMap.put(metric.URL, metric);
                                    } else if ( k < 5) {                                                //If the hamming distance is less than 5, it is considered as a near duplicate.

                                        URLMetric uMet = hashMap.get(url);                               //All the information about duplicate urls is stored in an separate HashMap object(dupHash).
                                        uMet.nearDuplicates.add(urlMat.group(2));
                                        metric.URL = urlMat.group(2);
                                        metric.nearDuplicates = null;
                                        dupHash.put(metric.URL,metric);
                                    }
                                }
                            }

                        parseData = new StringBuffer();                                                   //Refresh string buffer to erase the existing record information.
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
        catch (NoSuchAlgorithmException exp)
        {
            System.err.println(exp.getMessage());
        }
        finally {
            printMap(hashMap);                                                                           //Print the urls and duplicates to console.
        }
    }

}


