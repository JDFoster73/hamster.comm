package hamster.comm.buffer.block.itf;

import hamster.comm.buffer.block.SequentialMessageBlockReader;

/**
 * <p>Buffers can use instances of this callback to provide block calculation services to a caller.
 * The callback instance is called with the MessageBlockReader interface.  The implemented callback method
 * allows for general calculation based on the available readable message data present in the buffer.
 * 
 * @author jdf19
 *
 */
public interface ConsumableCalculationBlockHandler
{
  /**
   * <p>Implementations will be called to delimit a block of data in the given data reader.
   * 
   * @param dataReader consumable data to find a discrete block within.
   */
  public void calculateBlock(SequentialMessageBlockReader dataReader);  
}
