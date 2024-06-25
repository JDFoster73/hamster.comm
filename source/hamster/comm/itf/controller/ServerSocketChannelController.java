package hamster.comm.itf.controller;

import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;

import hamster.comm.server.NonBlockingCommunicationApplicationServer;

/**
 * <p>The server socket channel controller provides all methods for controlling a ServerSocketChannel
 * instance registered to a {@link NonBlockingCommunicationApplicationServer non-blocking communication server} instance.
 *  
 * @author jdf19
 *
 */
public interface ServerSocketChannelController extends BaseChannelController
{
  /**
   * <p>The owner of this controller instance can call the liberate method to
   * remove the socket channel from the NonBlockingCommunicationServer it is
   * registered to.  The channel will be completely deregistered and liberated.
   * <p>Any calls to this controller will throw runtime exceptions after than point.
   * 
   * @return the liberated socket channel.
   */
  public ServerSocketChannel liberate();
  
  /**
   * Pause listening for incoming connections.  Calling more than once has no effect.
   */
  public void pauseIncoming();
  
  /**
   * Resume listening for incoming connections.  Calling more than once has no effect.
   */
  public void resumeIncoming();

  /**
   * Get the internet address of the socket controller.
   * 
   * @return local listening socket address.
   */
  SocketAddress getLocalAddress();
}
