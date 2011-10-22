//
// This is the base class for classes that convert a binary representation of 4 valued logic into human
// readable strings.
//

public interface ValueFormatter
{
	String format(BitVector values);
}
