package hamster.comm.itf.listener;

import java.net.SocketAddress;

import hamster.comm.communication.ChannelCreateException;
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
public interface InitialisedServerSocketChannelListener extends BaseChannelListener
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
   * @param socketAddress the address of the peer which is making the incoming connection.
   * @return the user handler that registered the listening socket will supply a socket channel listener, which the NonBlockingCommunicationServer calls with socket channel events.
   * @throws ChannelCreateException if an underlying exception was raised while accepting the incoming connection.
   */
  public SocketChannelListener handleIncomingConnection(SocketAddress socketAddress) throws ChannelCreateException;
}
