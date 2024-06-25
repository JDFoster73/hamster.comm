package hamster.comm.server.listener;

/**
 * <p>TODO
 * 
 * @author jdf19
 *
 */
public interface ResendCommunicationController
{  
  /**
   * Sweep all connections for incoming data in the receive buffer.  If any incoming data
   * are found then the handler for the connection will be called to handle it.  This
   * is useful for if handlers don't automatically process all available incoming
   * data.
   */
  public void reprocessReceivedData();
}
