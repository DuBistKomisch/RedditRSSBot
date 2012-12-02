import java.io.*;
import java.net.*;
import java.util.*;

// manages the reddit_session cookie
class RedditCookieHandler extends CookieHandler
{
  private String reddit_session = null;

  // retrieves cookies for a request
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

  // saves cookies from a response
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
