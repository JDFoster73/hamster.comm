package hamster.comm.server;

/**
 * <p>Instances of this object can close a given communications loop and either return control
 * of the thread to the caller of its {@link Thread#run()} method or terminate a thread that is
 * running it.
 * 
 * @author jdf19
 *
 */
public interface CommLoopCloser
{
  /**
   * <p>Close the communications loop and exit the run method.
   */
  public void closeCommLoop();
}
