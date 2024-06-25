package hamster.comm.multithread;

import hamster.comm.buffer.block.itf.ReadBlockHandler;

/**
 * <p>The comm exchanger is entirely protocol-agnostic, so the owner needs to supply it with a resource
 * to be able to determine how long in bytes a particular incoming message is.
 * 
 * @author jdf19
 *
 */
public interface CommExchangeProtocolDescriptor extends ReadBlockHandler
{
  /**
   * <p>Use the {@link ReadBlockHandler#readMessageBlock(hamster.comm.buffer.block.SequentialMessageBlockReader)} method
   * to load in the protocol header and examine it for the whole message block length.  Then, this method can be called
   * to determine the length of the last message queried.
   * 
   * @return the block length as described above.
   */
  public int queryMessageLen();
  
  /**
   * <p>Returns in bytes the length of the protocol header.
   * 
   * @return the number of bytes in the protocol header.
   */
  public int queryProtocolHeaderSize();
  
}
