package hamster.comm.internalchannel;

import java.nio.channels.SocketChannel;

/**
 * <p>Internal channel connection.  This contains two socket channels that are connected to each other on the
 * loopback interface.  They can be used to send and receive data in an internal IPC-type arrangement.
 * <p>Using socket channels for internal IPC means that the same selectors can be used for internal and external
 * messages in a fully bidirectional way.
 *  
 * @author jdf19
 *
 */
public class InternalChannelConnection
{
  /**
   * 'A' end of the connected internal channel.
   */
  private final SocketChannel aEnds;
  
  /**
   * 'B' end of the connected internal channel.
   */
  private final SocketChannel bEnds;

  /**
   * <p>Create the internal channel using two connected socket channels.
   * 
   * @param aEnds 'A' ends
   * @param bEnds 'B' ends
   */
  public InternalChannelConnection(SocketChannel aEnds, SocketChannel bEnds)
  {
    super();
    this.aEnds = aEnds;
    this.bEnds = bEnds;
  }

  /**
   * <p>Get the 'A' ends (arbitary description) of this internal channel connection.
   * 
   * @return SocketChannel connected to the 'B' channel.
   */
  public SocketChannel getAEnds()
  {
    return aEnds;
  }

  /**
   * <p>Get the 'B' ends (arbitary description) of this internal channel connection.
   * 
   * @return SocketChannel connected to the 'A' channel.
   */
  public SocketChannel getBEnds()
  {
    return bEnds;
  }
}
