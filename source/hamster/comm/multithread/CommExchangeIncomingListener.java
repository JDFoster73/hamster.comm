package hamster.comm.multithread;

import hamster.comm.ipc.blob.BLOBConsumer;

/**
 * <p>Applications will provide an implementation of this interface to a {@link MultiThreadCommExchanger} instance when creating one.
 * It will be called to handle incoming communication events in an application-specific manner.
 * 
 * @author jdf19
 *
 */
public interface CommExchangeIncomingListener
{
  /**
   * <p>A block of message data has been received from the server and this callback method will be called
   * to handle it.
   * 
   * @param dataBlock the block of message data received from the server that can be handled.
   */
  public void handleIncomingData(BLOBConsumer dataBlock);
  
  /**
   * <p>Handle a server channel closing event.
   */
  public void handleChannelClosing();
  
  /**
   * <p>Handle a server channel shut event.
   */
  public void handleChannelShut();
}
