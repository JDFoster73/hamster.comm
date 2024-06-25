/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.wakeupschedule;

/**
 * <p>Hamster handlers provide solid implementations of this <code>WakeupCallback</code> interface when registering
 * for a callback.  When the callback period has elapsed, the <code>wakeup</code> method will be called.</p>
 * 
 */
public interface WakeupCallback
{
  /**
   * <p>Perform callback-specific actions in response to a registered callback.</p>
   * 
   * @param parameter the given parameter that was registered with the callback instance.
   * @param wakeupTime the actual system time that the wakeup event was scheduled for.
   */
  public void wakeup(int parameter, long wakeupTime);
}
