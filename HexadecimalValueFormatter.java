public class HexadecimalValueFormatter implements ValueFormatter
{
	public String format(BitVector bits)
	{
		return bits.toString(16);
	}
}
