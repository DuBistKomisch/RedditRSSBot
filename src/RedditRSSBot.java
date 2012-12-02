import java.io.*;
import java.net.*;
import java.util.*;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.*;
import org.json.*;

public class RedditRSSBot
{
  // config
  static final String agent = "RedditRSSBot";
  static String user, passwd, feed, sr, logFile;
  static long threshold, interval;

  // state
  static String reddit_session = null, modhash = null;
  static CookieHandler rch = new RedditCookieHandler();
  static long lastUpdate = (new Date()).getTime();
  static PrintWriter log;
  
  public static void main(String args[])
    throws Exception
  {
    // get login details
    try
    {
      Properties prop = new Properties();
      prop.load(new FileReader(args[0]));

      user = prop.getProperty("user");
      passwd = prop.getProperty("passwd");
      feed = prop.getProperty("feed");
      sr = prop.getProperty("sr");
      logFile = prop.getProperty("logfile");
      threshold = 1000 * Long.parseLong(prop.getProperty("threshold"));
      interval = 1000 * Long.parseLong(prop.getProperty("interval"));
    }
    catch (Exception e)
    {
      System.out.println("invalid or missing configuration file");
      System.out.println("Usage: java RedditRSSBot <config>");
      return;
    }

    // start logging
    log = new PrintWriter(new FileWriter(logFile, true), true);

    // sign in for cookie and modhash
    if (!login(user, passwd))
      return;

    // continually poll
    while (true)
    {
      try
      {
        poll(feed);
      }
      catch (Exception e)
      {
        e.printStackTrace(log);
        System.err.println("error: " + e.getMessage());
      }
      Thread.sleep(interval);
    }
  }

  // checks for new updates
  public static void poll(String url)
    throws Exception
  {
    log.printf("\n[%s] >> poll\n", new Date());

    // fetch
    SyndFeed feed = (new SyndFeedInput()).build(new XmlReader(new URL(url)));

    // iterate
    List entries = feed.getEntries();
    for (int i = entries.size() - 1; i >= 0; i--)
    {
      SyndEntry entry = (SyndEntry)entries.get(i);
      if (entry.getPublishedDate().getTime() - lastUpdate > threshold)
      {
        // found
        lastUpdate = entry.getPublishedDate().getTime();

        // submit
        submit("[UPDATE " + entry.getLink().substring(entry.getLink().indexOf("p=") + 4) + "] " + entry.getTitle().substring(entry.getTitle().indexOf(":") + 2), entry.getLink(), sr);
        return;
      }
    }
  }

  // generic api call which returns JSON
  public static JSONObject api(String call, String data)
    throws Exception
  {
    // set up connection
    CookieHandler.setDefault(rch);
    URL address = new URL("http://www.reddit.com/api/" + call);
    HttpURLConnection connection = (HttpURLConnection)address.openConnection();
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);
    connection.setReadTimeout(10000);
    connection.setRequestProperty("User-Agent", agent);
    connection.connect();
    
    // send data
    OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
    wr.write(data);
    wr.flush();
    
    // get response
    BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = rd.readLine()) != null)
      sb.append(line);

    // return response body string
    CookieHandler.setDefault(null);
    return new JSONObject(sb.toString());
  }

  // retrieves the cookie and modhash so that we can submit
  public static boolean login(String user, String passwd)
    throws Exception
  {
    // input
    log.printf("\n[%s] >> login\n", new Date());
    log.printf("user: %s\n", user);

    // call api
    JSONObject json = api("login", "user=" + URLEncoder.encode(user, "UTF-8") + "&passwd=" + URLEncoder.encode(passwd, "UTF-8") + "&api_type=json");

    // check for errors
    JSONArray errors = json.getJSONObject("json").getJSONArray("errors");
    if (errors.length() > 0)
    {
      log.printf("error: %s (%s)\n", errors.getJSONArray(0).getString(1), errors.getJSONArray(0).getString(0));
      return false;
    }

    // results
    modhash = json.getJSONObject("json").getJSONObject("data").getString("modhash");
    log.printf("reddit_session: %s\n", reddit_session);
    log.printf("modhash: %s\n", modhash);
    return true;
  }

  // submit a link, detects errors and redirects
  public static boolean submit(String title, String url, String sr)
    throws Exception
  {
    // input
    System.out.printf("submitting... ");
    log.printf("\n[%s] >> submit\n", new Date());
    log.printf("title: %s\n", title);
    log.printf("url: %s\n", url);
    log.printf("sr: %s\n", sr);

    // call api
    JSONObject json = api("submit", "uh=" + modhash + "&title=" + URLEncoder.encode(title, "UTF-8") + "&kind=link&url=" + URLEncoder.encode(url, "UTF-8") + "&sr=" + URLEncoder.encode(sr, "UTF-8"));

    // check what happened
    JSONArray jquery = json.getJSONArray("jquery");
    String redirect = null, text = null;
    for (int i = 0; i < jquery.length(); i++)
    {
      JSONArray line = jquery.getJSONArray(i);
      if (!line.getString(2).equals("attr"))
        continue;
      if (line.getString(3).equals("redirect"))
      {
        redirect = jquery.getJSONArray(i+1).getJSONArray(3).getString(0);
      }
      else if ( line.getString(3).equals("text"))
      {
        text = jquery.getJSONArray(i+1).getJSONArray(3).getString(0);
      }
    }

    // results
    if (text != null)
    {
      System.out.println("error");
      log.printf("error: %s\n", text);
      if (redirect != null)
        log.printf("page: %s\n", redirect);
      return false;
    }
    else
    {
      System.out.println("done");
      log.printf("page: %s\n", redirect);
      return true;
    }
  }
  
  // manages the reddit_session cookie
  static class RedditCookieHandler extends CookieHandler {
    public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders)
        throws IOException
    {
      Map<String, List<String>> map = new HashMap<String, List<String>>();
      if (reddit_session != null)
      {
        List<String> l = new ArrayList<String>();
        l.add(reddit_session);
        map.put("Cookie", l);
      }
      return Collections.unmodifiableMap(map);
    }

    public void put(URI uri, Map<String,List<String>> responseHeaders)
        throws IOException
    {
      List<String> l = (List<String>)responseHeaders.get("Set-Cookie");
      for (String s : l)
        if (s.substring(0, s.indexOf("=")).equals("reddit_session"))
          //reddit_session = s.substring(s.indexOf("=") + 1, s.indexOf(";"));
          reddit_session = s;
    }
  }
}
