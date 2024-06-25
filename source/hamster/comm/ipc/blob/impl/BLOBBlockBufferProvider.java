package hamster.comm.ipc.blob.impl;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.slf4j.Logger;

import hamster.comm.buffer.DrainableChannelBuffer;
import hamster.comm.buffer.FillableChannelBuffer;
import hamster.comm.buffer.block.BlockBufferBase;
import hamster.comm.buffer.block.itf.MessageBlockProduceProvider;
import hamster.comm.buffer.block.itf.ReadAllHandler;
import hamster.comm.buffer.block.itf.WholeBufferConsumeProvider;
import hamster.comm.buffer.block.itf.WriteBlockHandler;
import hamster.comm.buffer.factory.GrowUpdateExpandableBufferFactory;

/**
 * <p>Using the {@link BlockBufferBase} as the underlying mechanism for storing and accessing BLOB data, instances of this class perform all of
 * the necessary storage and retrieval operations.
 * 
 * @author jdf19
 *
 */
public class BLOBBlockBufferProvider implements DrainableChannelBuffer, FillableChannelBuffer, MessageBlockProduceProvider, WholeBufferConsumeProvider
{
  /**
   * <p>Buffer object that is wrapped by this utility class.
   */
  private final BlockBufferBase delegateBuffer;
  
  /**
   * <p>Create a provider for BLOB data.
   * 
   * @param bufferFact the buffer factory to use to construct this buffer provider.
   * @param logger the logger to use for debugging.
   */
  public BLOBBlockBufferProvider(GrowUpdateExpandableBufferFactory bufferFact, Logger logger)
  {
    delegateBuffer = new BlockBufferBase(bufferFact, "", logger);
  }

  /**
   * <p>Create a provider for BLOB data with the given buffer ID.
   * 
   * @param bufferFact the buffer factory to use to construct this buffer provider.
   * @param bufferId a string identifier to use in logging.
   * @param logger the logger to use for debugging.
   */
  public BLOBBlockBufferProvider(GrowUpdateExpandableBufferFactory bufferFact, String bufferId, Logger logger)
  {
    delegateBuffer = new BlockBufferBase(bufferFact, bufferId, logger);
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
    return delegateBuffer.consumableBytesInBuffer() > 0;
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
  public int produceFromHandler(WriteBlockHandler writeProc)
  {
    return delegateBuffer.produceData(writeProc);    
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
   * {@inheritDoc}
   */
  public void clearAll()
  {
    delegateBuffer.clearAll();
  }

  /**
   * {@inheritDoc}
   */
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
  
  /**
   * <p>Set the whole BLOB as consumable, i.e. set the consume position to 0.
   */
  public void setAllConsumable()
  {
    delegateBuffer.locateReadPos(0);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return delegateBuffer.toString();
  }
  
}
