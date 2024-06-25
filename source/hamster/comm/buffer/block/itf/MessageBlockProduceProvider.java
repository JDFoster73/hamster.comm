package hamster.comm.buffer.block.itf;

/**
 * <p>Classes implementing this interface will hold a stream of byte data and will provide discrete messages from these data when the
 * {@link MessageBlockProduceProvider#produceFromHandler(WriteBlockHandler)} method is called.  
 * 
 * 
 * @author jdf19
 *
 */
public interface MessageBlockProduceProvider
{
  /**
   * <p>The implementing instance will provide a mechanism to atomically produce a block of message data.  
   * 
   * @param writeProc callback to produce block message data.
   * @return the number of bytes produced
   */
  public int produceFromHandler(WriteBlockHandler writeProc);
}
