import java.io.InputStream;

//
// Classes override this interface to create an object that can load traces
// from a file.
//
interface TraceLoader
{
	/// @todo Some kind of file detection APIs (register by extension, sniff, etc)
	
	public boolean load(InputStream is, TraceBuilder builder);
}

