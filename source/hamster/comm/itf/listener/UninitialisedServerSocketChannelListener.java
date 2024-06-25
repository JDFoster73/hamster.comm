package hamster.comm.itf.listener;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import hamster.comm.itf.controller.ServerSocketChannelController;
import hamster.comm.server.NonBlockingCommunicationApplicationServer;

/**
 * <p>When registering a socket channel in a {@link NonBlockingCommunicationApplicationServer non-blocking communication server},
 * the caller of the {@link NonBlockingCommunicationServer#openServerSocket(java.net.InetSocketAddress, hamster.comm.server.itf.listener.ServerChannelListener, hamster.comm.communication.sockopts.SocketChannelOptionSetter...)}
 * method supplies a solid implementation of this interface in the call.  The solid implementation given will be
 * called by the NonBlockingCommunicationServer thread to notify it of events on the channel.
 * 
 * @author jdf19
 *
 */
public interface UninitialisedServerSocketChannelListener extends BaseChannelListener
{
  /**
   * <p>Called by the NonBlockingCommunicationServer instance the channel is registered to
   * when all setup is complete and the channel is ready for communicating.
   * 
   * @param controller the controller to control the channel.
   */
  public void initController(ServerSocketChannelController controller);

  /**
   * <p>Called by the NonBlockingCommunicationServer instance the channel is registered to
   * when a new incoming socket channel connection has been accepted.
   * 
   * @param channel the socket channel to handle the incoming connection for
   * @param socketAddress the address of the incoming connection peer
   */
  public void handleIncomingConnection(SocketChannel channel, SocketAddress socketAddress);
}
