package hamster.comm.server;

/**
 * <p>Allows interaction with the non-blocking communication server thread loop.
 * 
 * @author jdf19
 *
 */
public interface CommLoopInteractor
{
  /**
   * <p>If the implementation returns <code>true</code>, the communication server will do a select operation
   * which waits for a communication event before proceeding.
   * 
   * @return true if the implementation wishes the communication server to wait for communication events on a selector.
   */
  public boolean shouldWaitForNetworkEvent();
  
  /**
   * <p>Called every start of loop.  Implementation can do anything with this information.
   */
  public void handleLoopStart();
}
