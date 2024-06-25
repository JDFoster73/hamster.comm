package hamster.comm.buffer.block.itf;

/**
 * <P>Implementors of this interface allow consumption of all available data.  This is useful when the data in a buffer are homogeneous and
 * not separate messages in blocks.
 * 
 * @author jdf19
 *
 */
public interface WholeBufferConsumeProvider
{
  /**
   * <p>Consume all available buffer data.  Start the consume position from the very start of the buffer.
   * This method can be called over and over again; the same buffer data will be re-consumed.
   * 
   * @param callback the receiver of all available buffer data from the buffer start.
   */
  public void consumeStartAllBufferData(ReadAllHandler callback);
  
  /**
   * <p>Consume buffer data from the buffer.  This will continue consuming buffer data if any are available.
   *  
   * @param callback the receiver of the next block of buffer data from the previous consumed position.
   */
  public void consumeNextBufferData(ReadAllHandler callback);
  
  /**
   * <p>Return the number of consumable bytes.
   * 
   * @return the number of consumable bytes in the buffer.
   */
  public int readableBytesInBuffer();

}
