package hamster.comm.server;

/**
 * <p>Provides a comm loop closer.  This is an object which can be used to end a communications loop and return or terminate its thread.
 * 
 * @author jdf19
 *
 */
public interface CommLoopCloseProvider
{
  /**
   * <p>Get a comm loop closer.
   * 
   * @return object that can be used to close the relevant communication loop.
   */
  public CommLoopCloser getCommLoopCloser();
}
