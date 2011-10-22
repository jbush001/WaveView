public class ASCIIValueFormatter implements ValueFormatter
{
	public String format(BitVector bits)
	{
		return "" + (char) bits.intValue();
	}
}
