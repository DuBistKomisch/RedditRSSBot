import com.sun.syndication.feed.synd.*;

class TitleFormat
{
  public static String format(String style, SyndEntry entry)
  {
    switch (style)
    {
      case "homestuck":
        return "[UPDATE " + entry.getLink().substring(entry.getLink().indexOf("p=") + 4) + "] " + entry.getTitle().substring(entry.getTitle().indexOf(":") + 2);
      default:
        return entry.getTitle();
    }
  }
}
