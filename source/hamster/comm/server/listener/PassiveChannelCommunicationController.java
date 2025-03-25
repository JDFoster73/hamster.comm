package hamster.comm.server.listener;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import hamster.comm.communication.sockopts.SocketChannelOptionAccessor;
import hamster.comm.itf.listener.InitialisedServerSocketChannelListener;
import hamster.comm.itf.listener.UninitialisedServerSocketChannelListener;
import hamster.comm.server.NonBlockingCommunicationApplicationServer;
import hamster.comm.server.exception.ChannelRegistrationException;

/**
 * <p>Interface for allowing the opening of a server socket (listening) channel.  The communications base owner will open, set up
 * and manage this channel.  The {@link InitialisedServerSocketChannelListener} reference passed into the communications base owner will 
 * be called when incoming connections are accepted.
 * 
 * @author jdf19
 *
 */
public interface PassiveChannelCommunicationController
{
  /**
   * <p>Open a server socket channel.  When an incoming channel is accepted, it will be automatically registered with the underlying selector mechanism
   * and will be immediately available for sending and receiving data.
   * 
   * @param address the socket address - the ip address of the local adapter to register with and port to register.  If the port is 0 then one will be selected.
   * @param connectionListener the listener for connection events and server channel close events.
   * @param optionSetters server socket channel options.
   * @return the port number, either the one specified or the one chosen.
   * @throws ChannelRegistrationException if there was a problem registering the channel, for example trying to register a listening channel on a port that's already in use.
   */
  public int openServerSocket(InetSocketAddress address, InitialisedServerSocketChannelListener connectionListener, SocketChannelOptionAccessor ... optionSetters) throws ChannelRegistrationException;

  /**
   * <p>Open a server socket channel.  When an incoming channel is accepted, it will be returned directly and not registered with the underlying
   * selector mechanism.  It will be in blocking mode and can be used to send communications.  The main purpose of returning a {@link SocketChannel}
   * instance reference directly is so further setup can be done on it.  In the case of a {@link NonBlockingCommunicationApplicationServer}, after setup the
   * channel can be registered with the {@link EstablishedChannelCommunicationController#registerOpenSocket(SocketChannel, hamster.comm.itf.listener.SocketChannelListener)}
   * method.
   * 
   * @param address the socket address - the ip address of the local adapter to register with and port to register.  If the port is 0 then one will be selected.
   * @param connectionListener the listener for connection events and server channel close events.
   * @param optionSetters server socket channel options.
   * @return the port number, either the one specified or the one chosen.
   * @throws ChannelRegistrationException if there was a problem registering the channel, for example trying to register a listening channel on a port that's already in use.
   */
  public int openServerSocket(InetSocketAddress address, UninitialisedServerSocketChannelListener connectionListener, SocketChannelOptionAccessor ... optionSetters) throws ChannelRegistrationException;
}
