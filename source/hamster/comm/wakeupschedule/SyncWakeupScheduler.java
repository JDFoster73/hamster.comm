/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.wakeupschedule;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * The SyncWakeupManager helps schedule events in a single-threaded
 * communication environment. Object instances which use an instance of this
 * class will tell it that they need to be woken up at a certain period. 
 * The nextWakeupMS() method can be used to determine the duration in
 * milliseconds until the next instance needs to be "woken up" so that it can
 * process something.
 * 
 * @author jdf19
 */
public class SyncWakeupScheduler
{
  /**
   * <p>Synchronous wakeup list.  This stores asynchronous wakeup keys. 
   */
  private final List<SyncWakeupKey> callbackMap = new ArrayList<>();
  
  /**
   * Request a wakeup call at a specified time in the future.
   * 
   * The callback will be uniquely identified by the parameter and callerID
   * arguments. If a subsequent call is made with these arguments before the
   * requested callback time, the callback request will be updated.
   * 
   * @param parameter      the user-specified value to return to the requester in
   *                       its WakeupCallback object. Not interpreted in any way
   *                       by the AsyncWakeupScheduler.
   * @param callerID        The caller ID associated with the wakeup call.
   * @param wakeupPeriod    The callback will be called this number of milliseconds apart.
   * @param callbackObject  The object to call back with the given parameter at the requested wakeup time.
   */
  public void requestWakeupCall(int parameter, int callerID, int wakeupPeriod, WakeupCallback callbackObject)
  {
    if (callbackObject == null)
    {
      throw new NullPointerException();
    }

    // Insert.
    restateWakeup(parameter, callerID, wakeupPeriod, callbackObject);
  }

  /**
   * Restate the wakeup call if it has been set for the given parameter and
   * callerID combination. This will reinsert the existing wakeup request at a new
   * time and with the specified callback. If there is no wakeup scheduled with the caller id and parameter then
   * the wakeup will just be scheduled.
   * 
   * @param parameter      the user-specified value to return to the requester in
   *                       its WakeupCallback object. Not interpreted in any way
   *                       by the AsyncWakeupScheduler.
   * @param callerID        The caller ID associated with the wakeup call.
   * @param wakeupPeriod    The callback will be called this number of milliseconds apart.
   * @param callbackObject  The object to call back with the given parameter at the requested wakeup time.
   */
  protected void restateWakeup(int parameter, int callerID, int wakeupPeriod, WakeupCallback callbackObject)
  {
    //Cycle through the entries.  If the parameter and caller id combination is found then replace it.
    for(int i = 0; i < callbackMap.size(); i++)
    {
      SyncWakeupKey swk = callbackMap.get(i);
      if( (swk.callerID == callerID) && (swk.parameter == parameter) )
      {
        //Update the key.
        swk.nextWakeup = System.currentTimeMillis() + wakeupPeriod;
        swk.callback = callbackObject;
        swk.periodMs = wakeupPeriod;
        
        //Return.
        return;
      }
    }
    
    //Create a wakeup key.
    callbackMap.add(new SyncWakeupKey(callbackObject, callerID, parameter, System.currentTimeMillis() + wakeupPeriod, wakeupPeriod));
  }

  /**
   * Cancel the wakeup call if it has been set for the given parameter and
   * callerID combination.
   * 
   * @param parameter      the user-specified value to return to the requester in
   *                       its WakeupCallback object. Not interpreted in any way
   *                       by the AsyncWakeupScheduler.
   * @param callerID        The caller ID associated with the wakeup call.
   */
  public void cancelWakeup(int parameter, int callerID)
  {
    //Cycle through the entries.  If the parameter and caller id combination is found then cancel it.
    for(int i = 0; i < callbackMap.size(); i++)
    {
      SyncWakeupKey awk = callbackMap.get(i);
      if( (awk.callerID == callerID) && (awk.parameter == parameter) )
      {
        //Remove the key.
        callbackMap.remove(i);
        
        //Return.
        return;
      }
    }
  }

  /**
   * Cancel all wakeup calls matching the given callerID.
   * 
   * @param callerID        The caller ID associated with the wakeup call.
   */
  public void cancelWakeup(int callerID)
  {
    //Cycle through the entries.  If the caller id is found then cancel it.
    for(int i = 0; i < callbackMap.size(); i++)
    {
      SyncWakeupKey swk = callbackMap.get(i);
      if( (swk.callerID == callerID) )
      {
        //Remove the key.
        callbackMap.remove(i);        
      }
    }
  }

  /**
   * <p>
   * Service all outstanding objects and return the number of milliseconds until
   * the next object(s) are due for servicing.
   * 
   * @return number of milliseconds until next service.
   */
  public long serviceObjects()
  {
    // Get the next wakeup millisecond delay.
    long nextWakeup = System.currentTimeMillis();

    long lowestNextWakeup = 12000;
    
    for(int i = 0; i < callbackMap.size(); i++)
    {
      SyncWakeupKey swk = callbackMap.get(i);
      long toGo;
      if((toGo = swk.msToGo(nextWakeup)) <= 0)
      {
        //Due - service the wakeup.
        //callbackMap.remove(i);
        swk.callback.wakeup(swk.parameter, swk.nextWakeup);
        
        //Set to next wakeup period.
        swk.nextPeriod();
        
        //Set the lowest next wakeup to 0.  If a wakeup callback has been actioned then
        //there may be resulting comms requests to process.
        lowestNextWakeup = 0;
      }
      else
      {
        //Set the lowest next wakeup.
        lowestNextWakeup = Math.min(lowestNextWakeup, toGo);
      }
    }
    
    // Return lowest next wakeup.
    return Math.max(0, lowestNextWakeup);
  }
  
  /**
   * Delete all scheduled wakeups.
   */
  public void purge()
  {
    callbackMap.clear();
  }

  /**
   * <p>Returns true if there are no pending asynchronous wakeup calls scheduled.
   * 
   * @return true if no scheduled callbacks are pending.
   */
  public boolean isNoCallsPending()
  {
    return callbackMap.isEmpty();
  }
  
  private class SyncWakeupKey
  {
    public WakeupCallback callback;
    
    public final int callerID;
    
    public final int parameter;

    public long nextWakeup;

    private int periodMs;
    
    private SyncWakeupKey(WakeupCallback callback, int callerID, int parameter, long initWakeup, int periodMs)
    {
      this.callback = callback;
      this.callerID = callerID;
      this.parameter = parameter;
      this.nextWakeup = initWakeup;
      this.periodMs = periodMs;
    }

    public long msToGo(long currentTimeMs)
    {
      return nextWakeup - currentTimeMs;
    }

    public SyncWakeupKey nextPeriod()
    {
      nextWakeup += periodMs;
      return this;
    }
  }
}
