package hamster.comm.buffer.block.itf;

import hamster.comm.buffer.block.SequentialMessageBlockReader;

/**
 * <p>Implementations of this interface will calculate the size of a drain block of data within a stream, and return that
 * compound block size to the caller if the block can be read.
 * 
 * @author jdf19
 *
 */
public interface DrainBlockLengthCalculator
{
  /**
   * <p>The data reader will contain all available data in the given buffer.  This method will search known fields
   * to determine the length of a discrete block within that data.  There may well be more than one block's worth 
   * of data or not enough data for a block.
   * <p>This method must return &lt; 1 if a whole block cannot be consumed from the current buffer data.  If a block can be
   * consumed, then this method must return the number of bytes in the complete block.
   * 
   * @param dataReader the reader to use to determine the full drain block length.
   * @return the full drain block length in bytes.
   */
  public int drainBlockByteLength(SequentialMessageBlockReader dataReader);
}
