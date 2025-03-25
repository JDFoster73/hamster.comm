package hamster.comm.itf.controller;

import hamster.comm.buffer.FillableChannelBuffer;
import hamster.comm.itf.listener.TransferrableDataListener;
import hamster.comm.server.NonBlockingCommunicationApplicationServer;

/**
 * <p>The readable channel controller is the basis of all readable channel controllers which are provided by the
 *  {@link NonBlockingCommunicationApplicationServer non-blocking communication server} when a channel is registered
 *  to it.
 *  <p>All channels which are readable can be paused and resumed.
 *  
 * @author jdf19
 *
 */
public interface ReadableChannelController
{
  /**
   * <p>This is called in response to the channel being put in the READ_STOP condition, caused by the channel listener
   * not consuming readable data when instructed.  
   * <p>If the communication server has readable data but can't put any into the buffer because the buffer is full then
   * it will go into READ_STOP mode to avoid going into a spinlock (where the the selection operation will always return
   * immediately because the OP_READ interest op is set and the channel has readable data available).  In order to leave
   * READ_STOP and go back into full operation, some space must be made in the receive buffer by consuming it.  Afterwards,
   * this method can be called to reintroduce the OP_READ interest to the channel's selector thereby waking up when there
   * are read data to service.
   */
  //public void readRestart();
  
  /**
   * <p>Call this method to transfer any incoming channel data which may be available to read.  This method may be called at
   * any time but when called in response to a {@link TransferrableDataListener#handleDataRead()} event there will be 
   * data available to read.
   *    
   * @param targetBuffer the fillable buffer to transfer the incoming channel data into. 
   * @return the number of bytes read, if any.
   */
  public int fillBufferFromChannel(FillableChannelBuffer targetBuffer);

  
  /**
   * <p>Call this method to transfer any incoming channel data which may be available to read.  This method may be called at
   * any time but when called in response to a {@link TransferrableDataListener#handleDataRead()} event there will be 
   * data available to read.  This specifies the maximum number of bytes to fill.
   *    
   * @param targetBuffer the fillable buffer to transfer the incoming channel data into.
   * @param maxBytesToFill fill available bytes from the channel up to this number of bytes. 
   * @return the number of bytes read, if any.
   */
  public int fillBufferFromChannel(FillableChannelBuffer targetBuffer, int maxBytesToFill);
}
