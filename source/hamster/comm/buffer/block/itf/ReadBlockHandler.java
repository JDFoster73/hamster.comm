package hamster.comm.buffer.block.itf;

import hamster.comm.buffer.block.SequentialMessageBlockReader;

/**
 * <p>Implementations of this interface will receive a defined block of data
 * and process it.
 * 
 * @author jdf19
 *
 */
public interface ReadBlockHandler
{
  /**
   * <p>Produce a whole message block of data.
   * 
   * @param reader allows the caller to produce the message block data.
   * @return true if a message block was consumed.
   */
  public boolean readMessageBlock(SequentialMessageBlockReader reader);

}
