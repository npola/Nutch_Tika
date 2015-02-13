import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    static Pattern mimePat,urlPat,unFetchPat;
    static HashMap<String,Integer> mimeCount;
    static List<String> unfetchedURLs;

    public static void main(String[] args) {
        final File meta = new File(args[0]);
        List<String> fileName = new ArrayList<String>();
        List<String> mimeTypes = new ArrayList<String>();

        unfetchedURLs = new ArrayList<String>();
        mimeCount = new HashMap<String, Integer>();

        String filePattern = "^(part-[0-9]*)$";
        String mimePattern = "^(Content-Type=)(.*)";
        String urlPattern = "(^http[^ ]*)";
        String unFetPattern = "Status: 1 \\(db_unfetched\\)";

        Pattern filePat = Pattern.compile(filePattern);
        urlPat = Pattern.compile(urlPattern);
        mimePat = Pattern.compile(mimePattern);
        unFetchPat = Pattern.compile(unFetPattern);

        for(final File f:meta.listFiles())
        {
            Matcher m = filePat.matcher(f.getName());
            if(m.find())
            {
                //System.out.println(m.group(0));
                fileName.add(m.group(0));
            }

        }

        try {
            printMimeTypesForFile(args[0] + fileName.get(0));
        }
        catch (IOException e)
        {
            System.err.println(e.getMessage());
        }

       // printUnfetchedURLs();
        printMimeData();
    }

    public static void printUnfetchedURLs()
    {

        for(int i=0;i<unfetchedURLs.size();i++)
            System.out.println(unfetchedURLs.get(i));

    }

    public static void printMimeData()
    {
        System.out.println("Total of "+mimeCount.size()+" Mime Types found. They are listed below.");
        Set<String> keys = mimeCount.keySet();
        for(String s:keys)
            System.out.println("Type: "+s+"   Files: "+mimeCount.get(s));
    }

    public static void printMimeTypesForFile(String fname) throws IOException
    {
        File file = new File(fname);
        String line,nline;
        BufferedReader br = new BufferedReader(new FileReader(file));
        while((line = br.readLine()) != null) {
            Matcher m = urlPat.matcher(line.trim());
            if (m.find()) {
                nline = br.readLine();
                Matcher m1 = unFetchPat.matcher(nline);
                if (m1.find()) {
                    unfetchedURLs.add(line);
                }
                //System.out.println(nline);
                //System.out.println(m.group(0));
            } else {
                m = mimePat.matcher(line.trim());
                if (m.find()) {
                    if (mimeCount.containsKey(m.group(2)))
                        mimeCount.put(m.group(2), mimeCount.get(m.group(2)) + 1);
                    else
                        mimeCount.put(m.group(2), 1);
                    //System.out.println(m.group(2));
                }

            }
        }

    }
}
