package hamster.comm.buffer.block.itf;

import hamster.comm.buffer.block.SequentialMessageBlockReader;

/**
 * <p>Allows the transfer of data to a reader.
 * 
 * @author jdf19
 *
 */
public interface MessageBlockTransferProvider
{
  /**
   * Transfer all available block data from the given reader to the implementing instance.
   *  
   * @param blockReader the reader to transfer the data from.
   */
  public void transferFromReader(SequentialMessageBlockReader blockReader);
}
