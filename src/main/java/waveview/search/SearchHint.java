package waveview.search;

//
// This object is filled in during node evaluation to help optimize searching.
// The members represent the nearest timestamp (smallest absolute value of delta)
// to the evaluated timestamp where the return value of eval
// could change.
// Note that this doesn't guarantee the value will change (that's why it's only
// a hint), only that the node won't change at a closer timestamp.
//
class SearchHint {
    // Closest transition that has a larger timestamp (in the future)
    // This is set to Long.MAX_VALUE to indicate there are no more transitions
    // in this direction.
    long forward;

    // Closest transition that has a smaller timestamp (in the past)
    // This is set to Long.MIN_VALUE to indicate there are no more transitions
    // in this direction.
    long backward;
}
