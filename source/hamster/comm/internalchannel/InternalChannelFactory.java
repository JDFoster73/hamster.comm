package hamster.comm.internalchannel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;

/**
 * Synchronous factory for creating internal channel connection instances in the same thread as the caller.
 */
public class InternalChannelFactory
{
  /**
   * <p>Authentication buffer.  Will be used to send and receive authentication data between the newly-created channels.
   */
  private static final ByteBuffer authenticationBuffer = ByteBuffer.allocate(Long.BYTES);

  /**
   * <p>Secure random authentication data generator.
   */
  private static final SecureRandom authRandom = new SecureRandom();

  public synchronized static InternalChannelConnection createConnection() throws IOException
  {
    //Open a ServerSocketChannel to create the internal connections.
    ServerSocketChannel ssc = ServerSocketChannel.open();

    //Create address to bind server socket to - loopback adapter address and ephemeral port.
    InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);

    //Bind to the server address.
    ssc.bind(inetSocketAddress);

    //Get the loopback address plus port number that the server channel has picked up.
    InetSocketAddress localServerAddress = (InetSocketAddress) ssc.getLocalAddress();

    //Create socket end 1.
    SocketChannel aEnds = SocketChannel.open(localServerAddress);

    //Get the authenticator.
    long auth = authRandom.nextLong();

    //Write authentication data to aEnds.
    authenticationBuffer.clear();
    authenticationBuffer.putLong(auth);
    authenticationBuffer.flip();

    while(authenticationBuffer.hasRemaining())
    {
      aEnds.write(authenticationBuffer);
    }

    //Accept the other end.
    SocketChannel bEnds = ssc.accept();
    //Check authentication.
    authenticationBuffer.clear();
    while(authenticationBuffer.hasRemaining())
    {
      bEnds.read(authenticationBuffer);
    }

    //Check auth.
    authenticationBuffer.flip();
    if(auth == authenticationBuffer.getLong())
    {
      //Channels connected and authenticated.  Add to the map.
      InternalChannelConnection retConnection = new InternalChannelConnection(aEnds, bEnds);
      return retConnection;
    }
    else
    {
      //PROBLEM!!  Authentiation failed!
      throw new IOException("Pipe authentication failure.");
    }
  }
}
