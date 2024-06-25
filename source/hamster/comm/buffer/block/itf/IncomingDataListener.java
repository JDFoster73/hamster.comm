package hamster.comm.buffer.block.itf;

import hamster.comm.buffer.block.SequentialMessageBlockReader;

/**
 * <p>Abstract incoming data receiver interface, implemented by classes which receive incoming message data.</p>
 * 
 * @author jdf19
 *
 */
public interface IncomingDataListener
{
  /**
   * <p>Handle a block of message data.  The data are packaged into a block so the target does not have to
   * worry about message framing etc.  The limits are set and the user can only navigate within the bounds
   * of the message block.
   * 
   * @param reader the message consumption provider, allowing the callee to access the defined block of data.
   */
  public void handleIncomingData(SequentialMessageBlockReader reader);
}
