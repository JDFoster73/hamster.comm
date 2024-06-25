package hamster.comm.buffer.block.itf;

import hamster.comm.buffer.block.SequentialMessageBlockReader;

/**
 * <p>Implementations of this interface receive a reader giving access to all data residing
 * in a particular source.
 * 
 * @author jdf19
 *
 */
public interface ReadAllHandler
{
  /**
   * <p>Produce a whole message block of data.
   * 
   * @param reader allows the caller to produce the message block data.
   */
  public void readMessageAll(SequentialMessageBlockReader reader);

}
