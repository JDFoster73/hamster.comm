package hamster.comm.server.listener;

import hamster.comm.server.CommLoopCloseProvider;
import hamster.comm.server.CommLoopInteractor;
import hamster.comm.server.CommLoopNudgeRequester;
import hamster.comm.wakeupschedule.ProcessCallbackScheduler;

/**
 * <p>Controller that a communication application can use to interact with the communication server, and create communication channels
 * within the communication server.
 * 
 * @author jdf19
 *
 */
public interface CommunicationApplicationController extends MainCommunicationController, CommLoopCloseProvider, CommLoopNudgeRequester //ResendCommunicationController TODO REMOVE
{
  /**
   * <p>Set the comm loop interactor.  This allows the application to determine if the comm loop should wait for the next network event or process immediately.  It is 
   * also called at the start of every new comm loop iteration for information only.
   * @param interactor the provided callback implementation for interacting with the communication server loop.
   */
  public void setLoopInteractor(CommLoopInteractor interactor);
  
  /**
   * <p>Get access to the process scheduler.  This allows the application to schedule callbacks up to 65s in the future for tasks such as scheduled messages and checking for
   * timely responses.
   * 
   * @return a process scheduler object.
   */
  public ProcessCallbackScheduler getProcessScheduler();

  /**
   * Don't process any more incoming messages on this loop iteration.
   */
  public void skipRemainingIncomingThisLoop();
}
