package hamster.comm.multithread;

public class ConnectionID
{
  public ConnectionID()
  {
    super();
  }
  
  public String getIdentifier()
  {
    return String.format("%1$08x", hashCode());
  }
}
