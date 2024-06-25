package test.hamster.util.log;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLogUtil
{ 
  public static Logger retrieveLogger(String loggerName, Formatter logFormatter)
  {
    // LogManager logManager = LogManager.getLogManager();
    java.util.logging.Logger logger2 = java.util.logging.Logger.getLogger(loggerName);
    logger2.setLevel(Level.ALL);
    logger2.setUseParentHandlers(false);

    ConsoleHandler ch = new ConsoleHandler();
    ch.setLevel(Level.ALL);
    ch.setFormatter(logFormatter);
    logger2.addHandler(ch);

    // Create logger.
    Logger logger = LoggerFactory.getLogger(loggerName);
    
    //Return the newly-created logger.
    return logger;
  }

  public static Logger retrieveLogger(String loggerName)
  {
    // LogManager logManager = LogManager.getLogManager();
    java.util.logging.Logger logger2 = java.util.logging.Logger.getLogger(loggerName);
    logger2.setLevel(Level.ALL);
    logger2.setUseParentHandlers(false);

    ConsoleHandler ch = new ConsoleHandler();
    ch.setLevel(Level.ALL);
    logger2.addHandler(ch);

    // Create logger.
    Logger logger = LoggerFactory.getLogger(loggerName);
    
    //Return the newly-created logger.
    return logger;
  }
}
