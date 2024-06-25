package hamster.comm.multithread;

import java.net.InetSocketAddress;

/**
 * <p>Base comm exchange event listener.  This implements all of the {@link CommExchangeEventListener} interface methods with no-op stub methods.
 * Subclasses can override only the methods that are relevant to the application.  For example, if the application that uses a {@link MultiConnectionCommExchanger}
 * instance only opens active connections then there will never be any events for passive connections, so there is no need to provide callbacks for them.
 * <p>The base class still requires, however, that the connection management methods are all implemented for every application.
 * 
 * @author jdf19
 *
 */
public abstract class BaseCommExchangeEventListener implements CommExchangeEventListener
{

  @Override
  public void handleActiveConnectionSuccess(ConnectionID cid)
  {
  }

  @Override
  public void handleActiveConnectionFailure(ConnectionID cid, String cause)
  {
  }

  @Override
  public void handlePassiveConnectionSuccess(ConnectionID id, InetSocketAddress remoteAddr)
  {
  }

  @Override
  public void handlePassiveConnectionFailure(ConnectionID id, String cause)
  {
  }
}
