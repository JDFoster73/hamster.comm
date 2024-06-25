package hamster.comm.turbologging;

import hamster.comm.buffer.block.itf.WriteBlockHandler;

/**
 * <p>Turbo logging instance.  Implementations can be provided to clients for them to be able to raise TurboLogging notifications.
 * 
 * @author jdf19
 *
 */
public interface TurboLogInstance
{
  /**
   * <p>Optional should log status for the given level.  If the underlying TL implementation will not log messages at a given level
   * then this method can be used to check if that is the case and save processing time on the call.
   * 
   * @param level the logging level to check.
   * @return true if the given level is greater than the configured level of the logger implementation.
   */
  public boolean shouldLog(int level);
  
  /**
   * <p>Log the event.
   * 
   * @param eventID event ID
   * @param level the event log level
   * @param parameters data to log.
   */
  public void logEvent(int eventID, int level, WriteBlockHandler parameters);

  /**
   * <p>Log the event.
   * 
   * @param eventID event ID
   * @param level the event log level
   * @param parameters1 data to log.
   * @param parameters2 data to log.
   */
  public void logEvent(int eventID, int level, WriteBlockHandler parameters1, WriteBlockHandler parameters2);

  /**
   * <p>Log the event.
   * 
   * @param eventID event ID
   * @param level the event log level
   * @param parameters1 data to log.
   * @param parameters2 data to log.
   * @param parameters3 data to log.
   */
  public void logEvent(int eventID, int level, WriteBlockHandler parameters1, WriteBlockHandler parameters2, WriteBlockHandler parameters3);

  /**
   * <p>Log the event.
   * 
   * @param eventID event ID
   * @param level the event log level
   * @param parameters1 data to log.
   * @param parameters2 data to log.
   * @param parameters3 data to log.
   * @param parameters4 data to log.
   */
  public void logEvent(int eventID, int level, WriteBlockHandler parameters1, WriteBlockHandler parameters2, WriteBlockHandler parameters3, WriteBlockHandler parameters4);
}
