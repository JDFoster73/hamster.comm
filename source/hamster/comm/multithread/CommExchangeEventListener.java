package hamster.comm.multithread;

import java.net.InetSocketAddress;

/**
 * <p>Applications will provide an implementation of this interface to a {@link MultiThreadCommExchanger} instance when creating one.
 * It will be called to handle incoming communication events in an application-specific manner.
 * 
 * @author jdf19
 *
 */
public interface CommExchangeEventListener
{
  public void init(MultiConnectionCommExchanger.Controller controller);
  
  public void handleActiveConnectionSuccess(ConnectionID cid);
  public void handleActiveConnectionFailure(ConnectionID cid, String cause);
  
  public void handlePassiveConnectionSuccess(ConnectionID id, InetSocketAddress remoteAddr);
  public void handlePassiveConnectionFailure(ConnectionID id, String cause);
  
  public void handleIncomingConnectionData(ConnectionID id);
  
  public void handleConnectionClosing(ConnectionID id);
  
  public void handleConnectionClosed(ConnectionID id);
  
  public void handleListenerClosed(ConnectionID id);
}
