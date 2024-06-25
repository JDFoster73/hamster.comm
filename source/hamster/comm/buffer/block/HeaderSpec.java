package hamster.comm.buffer.block;

public class HeaderSpec
{
  /**
   * The byte width of the header field.
   */
  public final HEADER_WIDTH width;
  
  /**
   * True if the length header should include itself, false if the length header field should only
   * include the element length; 
   */
  public final boolean includeHeaderInLength;
  
  
  public HeaderSpec(HEADER_WIDTH width, boolean includeHeaderInLength)
  {
    this.width = width;
    this.includeHeaderInLength = includeHeaderInLength;
  }

  /**
   * The {@link HEADER_WIDTH} enumeration specifies the header field width in a character or byte string.  It is used
   * in calls to consume and produce byte or char strings and refers to the number of bytes contained in the length header field.
   * Each char or byte string is prefixed with a header field specifying the number of elements which follow.  In a wide string
   * (16-bit size elements) then it will be the number of <b>elements</b>, not the number of <b>bytes</b>. 
   */
  public enum HEADER_WIDTH
  {
    U1(1), //1-byte unsigned length header field.
    U2(2), //2-byte unsigned length header field.
    U4(4); //4-byte unsigned length header field.
    
    private final int widthBytes;

    private HEADER_WIDTH(int bytesForWidth)
    {
      this.widthBytes = bytesForWidth;
    }
    
    public int getWidthBytes()
    {
      return widthBytes;
    }
  }
}
