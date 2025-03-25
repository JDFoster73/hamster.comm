package hamster.comm.wakeupschedule;

/**
 * <p>Implementation of a callback scheduler with a unique caller id.  These instances can be created and 
 * given to different callers so that a caller does not need to know its ID when calling through to the
 * underlying schedulers.
 * 
 * @author jdf19
 *
 */
public class ProcessCallbackScheduler implements CallbackScheduler
{
  /**
   * <p>Incrementing count of caller IDs.  The number space means that caller IDs will not be repeated in practice.
   */
  private static int callerIDs = 0;
  
  /**
   * The async (one-off) future callback scheduler.
   */
  private final AsyncWakeupScheduler asyncScheduler;
  
  /**
   * The sync (periodically repeating) future callback scheduler.
   */
  private final SyncWakeupScheduler syncScheduler;
  
  /**
   * <p>The caller id for this instance.
   */
  private final int callerID;

  /**
   * <p>Create the scheduler.
   * 
   * @param asyncScheduler the async (one-off) future callback scheduler.
   * @param syncScheduler the sync (periodically repeating) future callback scheduler.
   */
  public ProcessCallbackScheduler(AsyncWakeupScheduler asyncScheduler, SyncWakeupScheduler syncScheduler)
  {
    this.asyncScheduler = asyncScheduler;
    this.syncScheduler = syncScheduler;
    this.callerID = callerIDs++;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void requestWakeupCall(int callbackParameter, int milliseconds, WakeupCallback callbackObject)
  {
    asyncScheduler.requestWakeupCall(callbackParameter, callerID, milliseconds, callbackObject);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cancelWakeupCall(int callbackParameter)
  {
    asyncScheduler.cancelWakeup(callbackParameter, callerID);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerSyncWakeup(int callbackParameter, int periodMS, WakeupCallback callbackObject)
  {
    syncScheduler.requestWakeupCall(callbackParameter, callerID, periodMS, callbackObject);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterSyncWakeup(int callbackParameter)
  {
    syncScheduler.cancelWakeup(callbackParameter, callerID);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterAllSyncWakeups()
  {
    syncScheduler.cancelWakeup(callerID);
  }
  
}
