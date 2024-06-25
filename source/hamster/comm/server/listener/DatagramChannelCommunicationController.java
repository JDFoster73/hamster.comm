package hamster.comm.server.listener;

import java.net.InetSocketAddress;

import hamster.comm.communication.sockopts.SocketChannelOptionAccessor;
import hamster.comm.itf.listener.ClientSocketChannelListener;
import hamster.comm.itf.listener.DatagramChannelListener;

/**
 * <p>Active channel communication controller.  This specifies the single operation {@link #openClientSocket(InetSocketAddress, ClientSocketChannelListener, SocketChannelOptionAccessor...)}
 * which directs the solid implementation to open a client socket (active) connection to the given destination address.
 * @author jdf19
 *
 */
public interface DatagramChannelCommunicationController 
{
  /**
   * <p>Open an active connection to a listening socket at the given address.  The connection listener will be called with either the connection
   * success or connection failure event.  If successful, the owner will be given a write controller for writing data and closing the channel.
   * It will provide a connection listener implementation to listen for incoming data and channel closure events.
   * 
   * @param address the address to open the active connection to.
   * @param connectionListener the connection listener - will be called with either success or failure of the socket channel open operation.
   * @param optionSetters option setters for a created channel.
   * 
   */
//   * @throws ChannelRegistrationException if the channel couldn't be registered to the underlying communication server mechanism.

  public void openDatagramSocket(InetSocketAddress localAddress, InetSocketAddress remoteSAddress, DatagramChannelListener connectionListener, SocketChannelOptionAccessor... optionSetters);
}
