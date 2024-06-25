package test.hamster.util.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class TestLogFormatter extends Formatter
{
  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss.SSS");

  private final String logName;

  public TestLogFormatter(String logName)
  {
    this.logName = logName;
  }

  @Override
  public String format(LogRecord record)
  {
    return Thread.currentThread().getName() + "::" + sdf.format(new Date(record.getMillis())) + " : "
        + record.getLevel().getName() + " : " + logName + " : " + record.getMessage() + "\r\n";
  }

}