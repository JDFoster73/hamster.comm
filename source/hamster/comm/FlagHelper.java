package hamster.comm;

/**
 * <p>Utility helper for flag bits - individual bits in an integer.  This class allows the setting/
 * clearing of individual bits in an integer, and the detection of bit state within an integer.
 *  
 * @author jdf19
 *
 */
public class FlagHelper
{
  /**
   * <p>Set a bit on or off depending on the state value.
   * 
   * @param bitField the integer to set an individual bit state of.
   * @param bit the bit number to set 0 &gt;= bit &lt;= 31.
   * @param state the on or off state to set the bit to.
   * 
   * @return the bitField value which has been altered as a result of setting an individual bit state.  The result of the bit operation.
   */
  public static int setFlag(int bitField, int bit, boolean state)
  {
    return (state) ? bitField | (1 << bit) : bitField & (~(1 << bit));
  }
  
  /**
   * <p>Detect a bit state on or off in an integer.
   * 
   * @param bitField the integer to test for an individual bit on or off.
   * @param bit the bit number to test 0 &gt;= bit &lt;= 31.
   * @return true if the bit state is 1, false if it is 0.
   */
  public static boolean isFlag(int bitField, int bit)
  {
    return (bitField & (1 << bit)) != 0;
  }
}
