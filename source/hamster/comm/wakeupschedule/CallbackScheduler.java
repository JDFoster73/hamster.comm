/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.wakeupschedule;

/**
 * <p><code>CallbackScheduler</code> provides facilities to:</p>
 * <ul>
 * <li>Request a callback for a given number of milliseconds in the future.  The callbackObject will be called with the callbackParameter at the requested time.</li>
 * <li>Cancel a previously requested wakeup, identified by the callback parameter.</li>
 * </ul>
 * <p>The callback mechanism works inside a single-threaded environment.  The thread that is used to call the <code>requestWakeupCall</code>
 * method will be used to call the <code>callbackObject</code>.  There is no need to provide any synchronisation: this mechanism is 
 * supposed to be used exclusively in a single-threaded environment.</p>
 * 
 * @author Jim Foster &lt;jdfoster73@gmail.com&gt;
 */
public interface CallbackScheduler
{  
  /**
   * The interface owner requests that the callback object supplied will be called after the specified number of milliseconds.
   * The callback parameter will be the integer supplied.  If the callback parameter corresponds to an existing
   * requested wakeup call then it will resinsert the request at the new time.
   * 
   * @param callbackParameter the parameter to call the callback object with at the requested time in the future
   * @param milliseconds the number of milliseconds in the future to call the callbackObject back
   * @param callbackObject object to call at the requested time in the future
   */
  public void requestWakeupCall(int callbackParameter, int milliseconds, WakeupCallback callbackObject);

  /**
   * Cancel a previously-requested wakeup call.  Useful if a timeout monitor has been set (for example) and the monitored
   * condition occurred before the timeout elapsed.  The monitor can then be cancelled using this method, referencing
   * the callback parameter given in the request.
   * 
   * @param callbackParameter the callback parameter that references the requestWakeupCall(...) method call.
   */
  public void cancelWakeupCall(int callbackParameter);
  
  /**
   * <p>Register for a synchronous wakeup.  This will call the given callback at the requested time period until unregistered.
   * @param callbackParameter the callback parameter that references the requestWakeupCall(...) method call.
   * @param periodMS the millisecond period that the callback object will be called.
   * @param callbackObject object to call at the requested time in the future
   */
  public void registerSyncWakeup(int callbackParameter, int periodMS, WakeupCallback callbackObject);
  
  /**
   * <p>Unregister the synchronous wakeup with the parameter and period combination.
   * 
   * @param callbackParameter the callback parameter that references the requestWakeupCall(...) method call.
   */
  public void unregisterSyncWakeup(int callbackParameter);

  /**
   * <p>Unregister all sync wakeups for this scheduler instance.
   */
  public void unregisterAllSyncWakeups();
}
