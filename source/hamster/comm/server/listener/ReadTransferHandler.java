package hamster.comm.server.listener;

import hamster.comm.buffer.FillableChannelBuffer;

/**
 * <p>The implementation of this interface will take the fillable channel buffer given and fill it with either as many bytes as can
 * be transferred or upto the capacity of the buffer object supplied.</p>
 * 
 * @author jdf19
 *
 */
public interface ReadTransferHandler
{
  /**
   * <p>Transfer as many bytes as possible into the given buffer argument, possibly filling it.</p>
   * 
   * @param reader the buffer to transfer bytes into.
   * @return the number of bytes transferred into the buffer.
   */
  public int transferChannelReadData(FillableChannelBuffer reader);

  /**
   * <p>Transfer UPTO maxBytes bytes into the given buffer argument, possibly filling it.</p>
   * 
   * @param reader the buffer to transfer bytes into.
   * @param maxBytes the maximum number of bytes to transfer - may be less but will not be more.
   * @return the number of bytes transferred into the buffer.
   */
  public int transferChannelReadData(FillableChannelBuffer reader, int maxBytes);
}
