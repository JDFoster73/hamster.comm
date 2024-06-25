package hamster.comm.buffer.block;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import org.slf4j.Logger;

import hamster.comm.buffer.DrainableChannelBuffer;
import hamster.comm.buffer.block.itf.ConsumableCalculationBlockHandler;
import hamster.comm.buffer.block.itf.MessageBlockProduceProvider;
import hamster.comm.buffer.block.itf.WriteBlockHandler;
import hamster.comm.buffer.factory.GrowUpdateExpandableBufferFactory;

/**
 * <p>This class is an extension to the {@link ProduceDrainPipelineBuffer} which allows the owner to manually reclaim data and set the consume
 * position to any point behind the produce position.  This can be used to retain buffer data, for example, until there is an acknowledgement that the
 * data have been received.  If data have not been received then the {@link #locateReadPosition(int)} method can be used to resend unreceived data.
 * 
 * @author jdf19
 *
 */
public class RetainingProduceDrainPipelineBuffer implements MessageBlockProduceProvider, DrainableChannelBuffer//extends BlockBufferBase // extends ProduceDrainPipelineBuffer
{
  /**
   * <p>Buffer object that is wrapped by this utility class.
   */
  private final ManualReclaimBuffer delegateBuffer;

  /**
   * <p>Construct the pipeline buffer.
   * 
   * @param bufferFact the buffer factory to use when creating an instance of the pipeline buffer.
   * @param bufferId buffer id to use in logging operations.
   * @param logger the logger to use for debugging buffer operations.
   */
  public RetainingProduceDrainPipelineBuffer(GrowUpdateExpandableBufferFactory bufferFact, String bufferId, Logger logger)
  {
    delegateBuffer = new ManualReclaimBuffer(bufferFact, bufferId, logger);
  }

  /**
   * <p>Locate the <i>read position</i>, which is the buffer position that calls to this buffer instance
   * will consume or drain data from.  This is useful if retained data need to be re-sent for any reason.
   * 
   * @param readPosition the read position to locate to.
   */
  public void locateReadPosition(int readPosition)
  {
    delegateBuffer.locateReadPos(readPosition);
  }
  
  /**
   * <p>Reclaim retained data from the front of the buffer when it is no longer needed.
   * 
   * @param bytesToReclaim the number of bytes to reclaim from the front of the buffer.
   */
  public void reclaimConsumedDataSpace(int bytesToReclaim)
  {
    //Reclaim the buffer space indicated.
    delegateBuffer.manualReclaim(bytesToReclaim);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public int produceFromHandler(WriteBlockHandler writeProc)
  {
    return delegateBuffer.produceData(writeProc);    
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int drainBufferToChannel(WritableByteChannel channel) throws IOException
  {
    return delegateBuffer.drainData(channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int drainBufferToChannel(WritableByteChannel channel, int maxToDrain) throws IOException
  {
    return delegateBuffer.drainData(channel, maxToDrain);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasDataToConsume()
  {
    return delegateBuffer.hasDataToConsume();
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return delegateBuffer.toString();
  }

  /**
   * <p>Output a readable string of the buffer contents.
   * 
   * @return readable string of the buffer contents.
   */
  public String outputSendBuffer()
  {
    return delegateBuffer.outputSendBuffer();
  }

  /**
   * <p>Consume a block of data to the given handler if it calculates that there are enough
   * consumable data available to do so.
   * 
   * @param handler handler to consume block data to.
   */
  public void scanConsumable(ConsumableCalculationBlockHandler handler)
  {
    delegateBuffer.scanConsumable(handler);
  }
  
  /**
   * <p>Clear all data.
   */
  public void clearBuffer()
  {
    delegateBuffer.clearAll();
  }
}
