package hamster.comm.server;

/**
 * <p>Nudge (wake up) a communication server.  Can be called by a different thread to the thread
 * running the selector.
 * 
 * @author jdf19
 *
 */
public interface CommLoopNudgeRequester
{
  /**
   * <p>Wake up the associated communication server so that it produces its communication loop.
   */
  public void nudge();
}
