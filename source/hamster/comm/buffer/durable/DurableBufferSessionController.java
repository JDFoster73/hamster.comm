package hamster.comm.buffer.durable;

import hamster.comm.server.listener.ReadTransferHandler;

/**
 * <p>Durable session controller provides a simple interface to the communication system to manage
 * sending and receiving data.
 * 
 * @author jdf19
 *
 */
public interface DurableBufferSessionController 
{
  /**
   * <p>Notify the durable buffer session to send session data, usually at the end of the NBCS processing loop.
   */
  public void continueSending();
  
  /**
   * <p>Replace the session data sender instance currently held.  The current instance is invalid and will throw an
   * exception 
   * 
   * @param sessionListener the session listener to use henceforth.
   */
  public void reinitialiseCommunicationState(DurableSessionSender sessionListener);
  
  /**
   * <p>Call the durable session with a {@link ReadTransferHandler} instance.  The durable session instance can use this to
   * retrieve incoming data from the underlying network channel.
   * 
   * @param transferHandler reference to a {@link ReadTransferHandler}, enabling network channel data to be retrieved.
   */
  public void handleCommunicationDataRead(ReadTransferHandler transferHandler);

  /**
   * <p>Notify the durable session that the comm loop has ended in case it wants to make a single write call to the
   * connected channel.
   */
  public void handleLoopEnd();

  
}
