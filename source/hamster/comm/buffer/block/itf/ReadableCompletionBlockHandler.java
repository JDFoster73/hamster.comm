package hamster.comm.buffer.block.itf;

import hamster.comm.buffer.block.SequentialMessageBlockReader;

/**
 * <p>Readable block callback.  Objects which wish to receive message block data will implement this interface, so the
 * buffer implementation will be able to call them so they can consume block message data.
 * <p>Implementations of this interface will also calculate the size of a block of data within a stream, and return that
 * block size to the caller if the block can be read.
 * @author jdf19
 *
 */
public interface ReadableCompletionBlockHandler extends ReadBlockHandler
{
  /**
   * <p>The data reader will contain all available data in the given buffer.  This method will search known fields
   * to determine the length of a discrete block within that data.  There may well be more than one block's worth 
   * of data or not enough data for a block.
   * <p>This method must return &lt; 1 if a whole block cannot be consumed from the current buffer data.  If a block can be
   * consumed, then this method must return the number of bytes in the complete block.
   * 
   * @param dataReader the reader to detect a block of data from.
   * @return the number of bytes in the complete block, or 1 if no complete block is present in the given reader instance.
   */
  public int messageBlockCompleteLength(SequentialMessageBlockReader dataReader);
  
  /**
   * <p>Prior to calling the {@link #messageBlockCompleteLength(SequentialMessageBlockReader)} method, this method
   * should be called to determine if there are even enough bytes to process to determine if the block is complete.
   * <p>If implemented, it can be used to return the number of bytes in a header, for example, so that when
   * {@link #messageBlockCompleteLength(SequentialMessageBlockReader)} is called, there will definitely be
   * enough bytes to process and determine if the rest of the message is complete.
   * 
   * @return the minimum number of bytes to read.
   */
  public default int minimumBytes()
  {
    return 1;
  }
}
