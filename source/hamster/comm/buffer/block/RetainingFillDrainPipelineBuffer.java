package hamster.comm.buffer.block;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.slf4j.Logger;

import hamster.comm.buffer.DrainableChannelBuffer;
import hamster.comm.buffer.FillableChannelBuffer;
import hamster.comm.buffer.block.itf.ConsumableCalculationBlockHandler;
import hamster.comm.buffer.block.itf.MessageBlockCalculateProvider;
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
public class RetainingFillDrainPipelineBuffer implements DrainableChannelBuffer, FillableChannelBuffer, MessageBlockCalculateProvider
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
  public RetainingFillDrainPipelineBuffer(GrowUpdateExpandableBufferFactory bufferFact, String bufferId, Logger logger)
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
   * <p>Clear all data.
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
   * <p>Output a readable string of the buffer contents.
   * 
   * @return readable string of the buffer contents.
   */
  public String outputReceiveBuffer()
  {
    return delegateBuffer.outputBuffer();
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void doBlockCalculation(ConsumableCalculationBlockHandler callback)
  {
    //Run the block calculator.
    delegateBuffer.runReadableBlockCalculation(callback);
  }
    
}
