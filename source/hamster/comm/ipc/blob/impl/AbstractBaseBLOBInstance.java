package hamster.comm.ipc.blob.impl;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

import hamster.comm.buffer.block.itf.WriteBlockHandler;
import hamster.comm.ipc.blob.BLOBProducer;

abstract class AbstractBaseBLOBInstance implements BLOBProducer
{
  /**
   * <p>
   * The internal read/write buffer that holds the BLOB data.
   */
  protected BLOBBlockBufferProvider BLOBData;

  /**
   * <p>
   * Construct the BLOB with a (possibly cached) byte buffer.
   * 
   * @param BLOBID
   * @param BLOBData
   */
  public AbstractBaseBLOBInstance(BLOBBlockBufferProvider BLOBData)
  {
    this.BLOBData = BLOBData;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int fromChannel(ReadableByteChannel channel) throws IOException
  {
    return BLOBData.fromChannel(channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int fromChannel(ReadableByteChannel channel, int maxBytes) throws IOException
  {
    return BLOBData.fromChannel(channel, maxBytes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canAcceptTransfer()
  {
    return BLOBData.canAcceptTransfer();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int produceFromHandler(WriteBlockHandler writeProc)
  {
    // Produce BLOB data from handler. Append if called multiple times.
    return BLOBData.produceFromHandler(writeProc);
  }
  
  /**
   * <p>
   * Check the BLOB is not finished. Throw runtime exception if so.
   * </p>
   */
  protected abstract void check() throws IllegalStateException;

  /**
   * {@inheritDoc}
   */
  @Override
  public int blobDataPresent()
  {
    return BLOBData.readableBytesInBuffer();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasSpaceFor(int requiredBufferLen)
  {
    return BLOBData.hasSpaceFor(requiredBufferLen);
  }
  
  /**
   * <p>Finish the given target.  If all targets have finished consuming the BLOB then the BLOB will be returned to the cache.
   * 
   * @param targetHALO
   */
  protected abstract void finish();    
}
