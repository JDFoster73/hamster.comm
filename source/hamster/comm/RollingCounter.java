package hamster.comm;

/**
 * <p>Simple implementation of a rolling counter, which goes from the start value to the end value and then back to the start again.
 * 
 * @author jdf19
 *
 */
public class RollingCounter
{
  /**
   * <p>The current counter value.
   */
  private int countVal;
  
  /**
   * <p>The start value for the counter.
   */
  private final int startValue;
  
  /**
   * <p>The end value for the counter.
   */
  private final int endValue;

  /**
   * <p>Create the rolling counter and set the count value to the start value.
   * 
   * @param startValue the count start value.
   * @param endValue the count end value.
   */
  public RollingCounter(int startValue, int endValue)
  {
    this.countVal = startValue;
    this.startValue = startValue;
    this.endValue = endValue;
  }
  
  /**
   * <p>Create the rolling counter and set the count value to the initial value.
   * 
   * @param startValue the count start value.
   * @param endValue the count end value.
   * @param initValue the initial count value.
   */
  public RollingCounter(int startValue, int endValue, int initValue)
  {
    this.countVal = initValue;
    this.startValue = startValue;
    this.endValue = endValue;
  }

  /**
   * <p>Advance the counter and return the advanced value.
   * 
   * @return advanced counter value.
   */
  public int preincrementCount()
  {
    this.countVal++;
    if(this.countVal > endValue) this.countVal = startValue;
    
    //Return the counter value.
    return countVal;
  }

  /**
   * <p>Advance the counter and return the pre-advanced value.
   * 
   * @return advanced counter value.
   */
  public int postincrementCount()
  {
    int retval = this.countVal++;
    if(this.countVal > endValue) this.countVal = startValue;
    
    //Return the counter value.
    return retval;
  }


  /**
   * <p>Return the ID value which will be the current value after the given MID in the parameter.
   * 
   * @param mID the MID to specify in the operation. 
   * 
   * @return the next MID after the one specified.
   */
  public int peekNextCount(int mID)
  {
    int testVal = mID + 1;
    if(testVal > endValue) testVal = startValue;
    
    //Return the counter test value.
    return testVal;
  }

  /**
   * <p>Return the current counter value.
   * 
   * @return current counter value.
   */
  public int getCurrentCount()
  {
    //Return the counter value.
    return countVal;
  }

  @Override
  public String toString()
  {
    return "Count:" + countVal;
  }
}
