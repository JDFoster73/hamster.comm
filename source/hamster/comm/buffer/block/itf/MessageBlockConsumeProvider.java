package hamster.comm.buffer.block.itf;

/**
 * <p>Classes implementing this interface will hold a stream of byte data and will provide discrete messages from these data when the
 * {@link MessageBlockConsumeProvider#consumeToHandler(ReadableCompletionBlockHandler)} method is called.  
 * If the block length calculator indicates that there are enough data in the stream to read a message (by checking internal fields in
 * the message indicating the length of the overall message block) then the block callback is called which will allow the callee to
 * consume message data from that defined message block.
 * 
 * @author jdf19
 *
 */
public interface MessageBlockConsumeProvider
{
  /**
   * <p>The implementing instance will provide a mechanism to atomically consume a block of message data.  The block complete
   * calculator is used by the implementation to determine if there are enough data to read, and if so, how much data should be
   * provided in the block.
   * 
   * @param callback handler for block message data.
   * @return if a data block was consumed.
   */
  public boolean consumeToHandler(ReadableCompletionBlockHandler callback);
}
