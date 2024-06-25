package hamster.comm.buffer.block;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import org.slf4j.Logger;

import hamster.comm.buffer.DrainableChannelBuffer;
import hamster.comm.buffer.block.itf.MessageBlockProduceProvider;
import hamster.comm.buffer.block.itf.WriteBlockHandler;
import hamster.comm.buffer.factory.GrowUpdateExpandableBufferFactory;

/**
 * <p>Pipeline buffer implementation which allows <i>producing</i> and <i>draining</i>.  Data are sourced by producing to the buffer from a user handler
 * and are drained to a channel.  Buffer space is reclaimed in the default way - when {@link #produceFromHandler(WriteBlockHandler)} is called.
 * <p>This is a one-way buffer in that buffer space that has been drained will then become available for reclaim.  Setting the drain position
 * is not possible with this implementation.
 * 
 * @author jdf19
 *
 */
public class ProduceDrainPipelineBuffer implements MessageBlockProduceProvider, DrainableChannelBuffer//extends BlockBufferBase 
{
  /**
   * <p>Buffer object that is wrapped by this utility class.
   */
  private final BlockBufferBase delegateBuffer;

  /**
   * <p>Construct the buffer.
   * 
   * @param bufferFact the factory to use to construct the buffer.
   * @param bufferId string ID for logging.
   * @param logger logger to log buffer operations for debugging.
   */
  public ProduceDrainPipelineBuffer(GrowUpdateExpandableBufferFactory bufferFact, String bufferId, Logger logger)
  {
    delegateBuffer = new BlockBufferBase(bufferFact, bufferId, logger);
  }

  /**
   * <p>Construct the buffer.
   * 
   * @param bufferFact the factory to use to construct the buffer.
   */
  public ProduceDrainPipelineBuffer(GrowUpdateExpandableBufferFactory bufferFact)
  {
    delegateBuffer = new BlockBufferBase(bufferFact, "", null);
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
   * <p>Clear all buffer data.
   */
  public void clearAll()
  {
    delegateBuffer.clearAll();
  }
}
