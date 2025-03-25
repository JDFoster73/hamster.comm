package hamster.comm.buffer;

/**
 * <p>Implemented by an <i>accepting</i> channel buffer.  The methods in this interface determine whether or not a buffer can accept data in its current state.
 * 
 * @author jdf19
 *
 */
public interface AcceptingChannelBuffer
{
  /**
   * <p>Return <code>true</code> if the underlying buffer can accept a transfer of data.  
   * 
   * <p>If there are bytes in the OS channel buffer that are ready to be consumed, but the target buffer has no more space, 
   * then a non-blocking environment could have a problem in that 0 bytes were transferred from a channel because it has readable 
   * data and the target buffer can not accept the transfer.  OP_READ would still be ON so we could go into a thread spin until data 
   * have been consumed from the target buffer and the accepting transfer can be satisfied.
   * 
   * @return true if the buffer can accept a transfer of at least one byte.
   */  
  public boolean canAcceptTransfer();

  /**
   * <p>Returns true if the fillable buffer implementation has the capacity for the indicated number of bytes.</p>
   * 
   * @param requiredBufferLen the length of the required block of data which should be fully accepted by the buffer.
   * @return true if the implementing buffer can accept the indicated whole block of data.
   */
  public boolean hasSpaceFor(int requiredBufferLen);
}
