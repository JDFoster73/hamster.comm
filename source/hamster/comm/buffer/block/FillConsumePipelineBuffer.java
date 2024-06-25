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
 * is not possible with this implementation.
 * 
 * @author jdf19
 *
 */
public class FillConsumePipelineBuffer implements MessageBlockConsumeProvider, FillableChannelBuffer
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
  public FillConsumePipelineBuffer(GrowUpdateExpandableBufferFactory bufferFact, String bufferId, Logger logger)
  {
    //super(bufferFact, bufferId, logger);
    delegateBuffer = new BlockBufferBase(bufferFact, bufferId, logger);
  }

  /**
   * Create an instance of the pipeline buffer with the given factory but no logger required
   * 
   * @param bufferFact the buffer factory to use when creating an instance of the general pipeline buffer.
   */
  public FillConsumePipelineBuffer(GrowUpdateExpandableBufferFactory bufferFact)
  {
    //super(bufferFact, bufferId, logger);
    delegateBuffer = new BlockBufferBase(bufferFact, "", null);
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
   * <p>
   * Clear ALL data in buffer and completely re-initialise.
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
   * <p>Provide a string representation of the buffer.  Take care in using this if the buffer
   * is large.
   * 
   * @return string representation of the buffer contents.
   */
  public String outputReceiveBuffer()
  {
    return delegateBuffer.outputBuffer();
  }
  
}
