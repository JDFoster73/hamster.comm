package hamster.comm.server.listener;

/**
 * <p>Callback interface so that communication applications can supply an instance which will be called
 * when there is a channel readable event.
 * 
 * @author jdf19
 *
 */
public interface ReadTargetListener
{
  /**
   * <p>The implementation is called to handle a channel readable event.
   */
  void handleChannelReadableEvent();
  
  /**
   * <p>Handle the comm loop end.
   */
  void handleCommLoopEnd();
  
}
