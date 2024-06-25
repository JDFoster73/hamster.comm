package hamster.comm.buffer.block.itf;

import hamster.comm.buffer.block.SequentialMessageBlockWriter;

/**
 * <p>Implementations of this interface are called to produce a block of data to the writer
 * passed as a parameter to the {@link #writeMessageBlock(SequentialMessageBlockWriter)} method.
 * <p>This is to provide an abstract mechanism for block data production - the implementation
 * can be anything which produces message block data.
 * 
 * @author jdf19
 *
 */
public interface WriteBlockHandler
{
  /**
   * <p>Produce a whole message block of data.
   * 
   * @param writer allows the caller to produce the message block data.
   */
  public void writeMessageBlock(SequentialMessageBlockWriter writer);

}
