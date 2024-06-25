package hamster.comm.buffer.durable;

import hamster.comm.buffer.DrainableChannelBuffer;

/**
 * <p>Sender for durable session data.
 * 
 * @author jdf19
 *
 */
public interface DurableSessionSender
{
  /**
   * <p>Ask the delegate implementation to send communication (i.e. control and data packets of discrete size) data to the
   * remote endpoint.  The implementation is purely responsible for working out how to do this. 
   * 
   * @param writer communication data to be sent to the remote endpoint.
   * @return the number of bytes sent.
   */
  public int sendOutgoingCommunicationData(DrainableChannelBuffer writer);
  
  /**
   * <p>Set the end loop notification.  When ON, this will call the durable session back at the end of every comm loop.
   * 
   * @param notificationOn true if the durable session should receive an end of comm loop notification.
   */
  public void setOutgoingLoopEndNotification(boolean notificationOn);
}
