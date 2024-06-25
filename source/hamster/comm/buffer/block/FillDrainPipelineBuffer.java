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
import hamster.comm.logging.DummyLogger;

/**
 * <p>Pipeline buffer implementation which allows <i>filling</i> and <i>draining</i>.  Data are sourced by filling the buffer from a channel
 * and are drained to a target channel.  Buffer space is reclaimed manually as the user decides when data are no longer required.
 * 
 * @author jdf19
 *
 */
public class FillDrainPipelineBuffer implements FillableChannelBuffer, DrainableChannelBuffer, MessageBlockCalculateProvider
{
  /**
   * <p>Buffer object that is wrapped by this utility class.
   */
  private final BlockBufferBase delegateBuffer;

  
  /**
   * Create an instance of the pipeline buffer with the given factory and logger instances.  A buffer
   * id string is also specified which will be appended to the logs.
   * 
   * @param bufferFact the buffer factory to use when creating an instance of the general pipeline buffer.
   * @param bufferId buffer id to use in logging operations.
   * @param logger the logger to use for debugging buffer operations.
   */
  public FillDrainPipelineBuffer(GrowUpdateExpandableBufferFactory bufferFact, String bufferId, Logger logger)
  {
    delegateBuffer = new ManualReclaimBuffer(bufferFact, bufferId, logger);
  }

  /**
   * Create an instance of the pipeline buffer with the given factory instance.  No logger will be used for debugging buffer operations.
   * 
   * @param bufferFact the buffer factory to use when creating an instance of the general pipeline buffer.
   */
  public FillDrainPipelineBuffer(GrowUpdateExpandableBufferFactory bufferFact)
  {
    delegateBuffer = new ManualReclaimBuffer(bufferFact, "", new DummyLogger());
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
  public int drainBufferToChannel(WritableByteChannel channel, int maxBytesToSend) throws IOException
  {
    return delegateBuffer.drainData(channel, maxBytesToSend);
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
  public void doBlockCalculation(ConsumableCalculationBlockHandler callback)
  {
    //Run the block calculator.
    delegateBuffer.runReadableBlockCalculation(callback);
  }
    
  /**
   * <p>Reclaim retained data from the front of the buffer when it is no longer needed.
   * 
   * @param bytesToReclaim the number of bytes to reclaim from the front of the buffer.
   */
  public void reclaimConsumedDataSpace(int bytesToReclaim)
  {
    //Reclaim the buffer space indicated.
    delegateBuffer.doReclaim(bytesToReclaim);
  }
  
  class ManualReclaimBuffer extends BlockBufferBase
  {
    public ManualReclaimBuffer(GrowUpdateExpandableBufferFactory bufferFact, String bufferId, Logger logger)
    {
      super(bufferFact, bufferId, logger);
      // TODO Auto-generated constructor stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doReclaim(int endReclaimPosition)
    {
      //Override the default reclaim mechanism.  We only reclaim when we get positive acknowledgement from the connected peer.
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canAcceptTransfer()
  {
    // TODO Auto-generated method stub
    return delegateBuffer.canAcceptTransfer();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasSpaceFor(int requiredBufferLen)
  {
    // TODO Auto-generated method stub
    return delegateBuffer.hasSpaceFor(requiredBufferLen);
  }
  
  /**
   * <p>Provide a string representation of the buffer.  Take care in using this if the buffer
   * is large.
   * 
   * @return string representation of the buffer contents.
   */
  @Override
  public String toString()
  {
    return delegateBuffer.toString();
  }

}
