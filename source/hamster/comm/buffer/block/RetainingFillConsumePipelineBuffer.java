package hamster.comm.buffer.block;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

import org.slf4j.Logger;

import hamster.comm.buffer.FillableChannelBuffer;
import hamster.comm.buffer.block.itf.MessageBlockConsumeProvider;
import hamster.comm.buffer.block.itf.ReadableCompletionBlockHandler;
import hamster.comm.buffer.factory.GrowUpdateExpandableBufferFactory;

/**
 * <p>Pipeline buffer implementation which allows <i>filling</i> and <i>consuming</i>.  Data are sourced by filling the buffer from a channel
 * and are consumed by a user handler.  Buffer space is reclaimed in the default way - when {@link #fromChannel(ReadableByteChannel)} is called.
 * <p>This is a one-way buffer in that buffer space that has been consumed will then become available for reclaim.  Setting the consume position
 * is not possible with this implementation.  Consumed buffer space MUST be reclaimed manually in this implementation.
 * 
 * @author jdf19
 *
 */
public class RetainingFillConsumePipelineBuffer implements MessageBlockConsumeProvider, FillableChannelBuffer
{
  /**
   * <p>Buffer object that is wrapped by this utility class.
   */
  private final ManualReclaimBuffer delegateBuffer;
  
  /**
   * <p>Construct the buffer.
   * 
   * @param bufferFact the factory to use to construct the buffer.
   * @param bufferId string ID for logging.
   * @param logger logger to log buffer operations for debugging.
   */
  public RetainingFillConsumePipelineBuffer(GrowUpdateExpandableBufferFactory bufferFact, String bufferId, Logger logger)
  {
    //super(bufferFact, bufferId, logger);
    delegateBuffer = new ManualReclaimBuffer(bufferFact, bufferId, logger);
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
   * <p>Reclaim all consumed data from the front of the buffer when it is no longer needed.
   */
  public void reclaimAllConsumedDataSpace()
  {
    //Reclaim the buffer space indicated.
    delegateBuffer.manualReclaimAll();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean consumeToHandler(ReadableCompletionBlockHandler callback)
  {
    return delegateBuffer.consumeData(callback);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int fromChannel(ReadableByteChannel channel) throws IOException
  {
    return delegateBuffer.fillData(channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int fromChannel(ReadableByteChannel channel, int maxBytes) throws IOException
  {
    return delegateBuffer.fillData(channel, maxBytes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canAcceptTransfer()
  {
    return delegateBuffer.canAcceptTransfer();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasSpaceFor(int requiredBufferLen)
  {
    return delegateBuffer.hasSpaceFor(requiredBufferLen);
  }

  /**
   * <p>Clear all buffer data.
   */
  public void clearAll()
  {
    delegateBuffer.clearAll();
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
   * Output a readable string of the buffer contents.
   * 
   * @return readable string of the buffer contents.
   */
  public String outputReceiveBuffer()
  {
    return delegateBuffer.outputBuffer();
  }
  
}
