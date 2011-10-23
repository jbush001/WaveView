import java.util.Iterator;

interface AbstractTransitionIterator extends Iterator<Transition>
{
    Transition current();
    public long getNextTimestamp();
    public long getPrevTimestamp();
}
