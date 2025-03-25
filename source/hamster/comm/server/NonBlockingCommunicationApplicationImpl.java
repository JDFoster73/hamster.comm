package hamster.comm.server;

import hamster.comm.server.listener.CommunicationApplicationController;

/**
 * <p>A non-blocking communication application is used to provide the initial setup of communication connections
 * in a communication application.
 * 
 * @author jdf19
 *
 */
public interface NonBlockingCommunicationApplicationImpl
{
  /**
   * <p>Initially receive the application controller.  This can be used to open channels and have interaction with the containing communication server.
   * <p>This method is guaranteed to be called <b>only once</b>, at the start of the comm loop.  As it is the point at which the communication application
   * has access to the communication services of the {@link NonBlockingCommunicationApplicationServer}, it can be used for initialisation etc.  
   * 
   * @param controller the application controller.
   */
  public void registerAppController(CommunicationApplicationController controller);

  /**
   * An external shutdown required command has been received by the application communication thread server.
   */
  public void externalShutdownCommand();
}
