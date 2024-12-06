package hamster.comm.buffer.block;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.slf4j.Logger;

import hamster.comm.buffer.DrainableChannelBuffer;
import hamster.comm.buffer.FillableChannelBuffer;
import hamster.comm.buffer.block.itf.ConsumableCalculationBlockHandler;
import hamster.comm.buffer.block.itf.MessageBlockCalculateProvider;
import hamster.comm.buffer.block.itf.MessageBlockConsumeProvider;
import hamster.comm.buffer.block.itf.MessageBlockProduceProvider;
import hamster.comm.buffer.block.itf.MessageBlockTransferProvider;
import hamster.comm.buffer.block.itf.ReadAllHandler;
import hamster.comm.buffer.block.itf.ReadableCompletionBlockHandler;
import hamster.comm.buffer.block.itf.WholeBufferConsumeProvider;
import hamster.comm.buffer.block.itf.WriteBlockHandler;
import hamster.comm.buffer.factory.GrowUpdateExpandableBufferFactory;

/**
 * <p>The general pipeline buffer simply buffers data bytes.  Data can be consumed in defined block using methods from the
 * {@link MessageBlockConsumeProvider}, {@link WholeBufferConsumeProvider} and {@link MessageBlockCalculateProvider} interfaces.
 * 
 * @author jdf19
 *
 */
public class GeneralPipelineBuffer implements MessageBlockTransferProvider, MessageBlockProduceProvider, MessageBlockConsumeProvider, WholeBufferConsumeProvider, DrainableChannelBuffer, FillableChannelBuffer, MessageBlockCalculateProvider, WriteBlockHandler
{
  /**
   * <p>Buffer object that is wrapped by this utility class.
   */
  private final BlockBufferBase delegateBuffer;

  /**
   * Create an instance of the general pipeline buffer with the given factory and logger instances.
   *
   * @param bufferFact the buffer factory to use when creating an instance of the general pipeline buffer.
   * @param logger the logger to use for debugging buffer operations.
   */
  public GeneralPipelineBuffer(GrowUpdateExpandableBufferFactory bufferFact, Logger logger)
  {
    delegateBuffer = new BlockBufferBase(bufferFact, "", logger);
  }

  /**
   * Create an instance of the general pipeline buffer with the given factory instance.  No logger will be used for debugging buffer operations.
   *
   * @param bufferFact the buffer factory to use when creating an instance of the general pipeline buffer.
   */
  public GeneralPipelineBuffer(GrowUpdateExpandableBufferFactory bufferFact)
  {
    delegateBuffer = new BlockBufferBase(bufferFact, "");
  }

  /**
   * Create an instance of the general pipeline buffer with the given factory and logger instances.  A buffer
   * id string is also specified which will be appended to the logs.
   *
   * @param bufferFact the buffer factory to use when creating an instance of the general pipeline buffer.
   * @param bufferId buffer id to use in logging operations.
   * @param logger the logger to use for debugging buffer operations.
   */
  public GeneralPipelineBuffer(GrowUpdateExpandableBufferFactory bufferFact, String bufferId, Logger logger)
  {
    delegateBuffer = new BlockBufferBase(bufferFact, bufferId, logger);
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
  public boolean consumeToHandler(ReadableCompletionBlockHandler callback)
  {
    return delegateBuffer.consumeData(callback);
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
  public boolean hasDataToConsume()
  {
    return delegateBuffer.hasDataToConsume();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void transferFromReader(SequentialMessageBlockReader blockReader)
  {
    delegateBuffer.transferFrom(blockReader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void consumeStartAllBufferData(ReadAllHandler callback)
  {
    delegateBuffer.consumeAllData(callback);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void consumeNextBufferData(ReadAllHandler callback)
  {
    delegateBuffer.consumeNextData(callback);
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
   *
   * @return this instance reference.
   */
  public GeneralPipelineBuffer clearAll()
  {
    delegateBuffer.clearAll();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int readableBytesInBuffer()
  {
    return delegateBuffer.consumableBytesInBuffer();
  }

  /**
   * {@inheritDoc}
   */
  public int getCapacity()
  {
    return delegateBuffer.getCapacity();
  }

  @Override
  public String toString()
  {
    return delegateBuffer.toString();
  }
//
//  @Override
//  public int write(ByteBuffer src)
//  {
//    // TODO Auto-generated method stub
//    return delegateBuffer.transferFromSourceBuffer(src);
//  }

  @Override
  public void writeMessageBlock(SequentialMessageBlockWriter writer)
  {
    // TODO Auto-generated method stub
    delegateBuffer.transferTo(writer);
  }
}