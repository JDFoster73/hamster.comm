package hamster.comm.ipc.blob;

import hamster.comm.buffer.FillableChannelBuffer;
import hamster.comm.buffer.block.itf.MessageBlockProduceProvider;
import hamster.comm.buffer.block.itf.WriteBlockHandler;

/**
 * <p>The {@link BLOBProducer} interface allows BLOB data to be produced into a BLOB instance.
 * 
 * @author jdf19
 *
 */
public interface BLOBProducer extends FillableChannelBuffer, MessageBlockProduceProvider, WriteBlockHandler
{
  /**
   * <P>Return the number of bytes that have been loaded into the BLOB.</P>
   * 
   * @return the number of bytes present in the BLOB.
   */
  public int blobDataPresent();
  
  /**
   * <p>Get a consumer for the BLOB data.
   * 
   * @return a consumer interface for accessing BLOB data for consumption.
   */
  public BLOBConsumer getConsumer();
}
