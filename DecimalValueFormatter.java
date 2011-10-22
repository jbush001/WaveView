public class DecimalValueFormatter implements ValueFormatter
{
	public String format(BitVector bits)
	{
		return bits.toString(10);
	}
}
