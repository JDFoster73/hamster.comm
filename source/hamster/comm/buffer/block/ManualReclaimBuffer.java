package hamster.comm.buffer.block;

import org.slf4j.Logger;

import hamster.comm.buffer.factory.GrowUpdateExpandableBufferFactory;

/**
 * <p>Don't automatically reclaim buffer space for a variety of reasons.  Using this class instead of {@link BlockBufferBase} <b>puts the responsibility
 * for cleaning up consumed data directly on the instance owner.</b>  Don't forget to manually reclaim consumed data when finished with it.
 * 
 * @author jdf19
 *
 */
public class ManualReclaimBuffer extends BlockBufferBase
{
  /**
   * Create an instance of the pipeline buffer with the given factory and logger instances. 
   * 
   * @param bufferFact the buffer factory to use when creating an instance of the general pipeline buffer.
   * @param bufferId buffer id to use in logging operations.
   * @param logger the logger to use for debugging buffer operations.
   */
  public ManualReclaimBuffer(GrowUpdateExpandableBufferFactory bufferFact, String bufferId, Logger logger)
  {
    super(bufferFact, bufferId, logger);
  }

  /**
   * <p>Override the default reclaim mechanism.  We only reclaim when we reach a certain state, to be determined by the owning implementation.
   */
  @Override
  public void doReclaim(int endReclaimPosition)
  {
  }

  /**
   * <p>Manually reclaim the indicated number of bytes from the front of the buffer.  There must be enough bytes in there to reclaim for this
   * operation to succeed.
   * 
   * @param bytesToReclaim the number of bytes to reclaim from the front of the buffer.
   */
  public void manualReclaim(int bytesToReclaim)
  {
    //Reclaim the buffer space indicated.
    super.doReclaim(bytesToReclaim);
  }

  /**
   * <p>Manually reclaim the indicated number of bytes from the front of the buffer.  There must be enough bytes in there to reclaim for this
   * operation to succeed.
   */
  public void manualReclaimAll()
  {
    //Reclaim all consumed buffer space.
    super.doReclaimAll();
  }

  /**
   * <p>Output the buffer contents.
   * 
   * @return readable string of buffer contents.
   */
  public String outputSendBuffer()
  {
    return super.outputBuffer();
  }
}
