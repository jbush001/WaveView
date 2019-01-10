package waveview.search;

//
// This object is filled in during node evaluation to help optimize searching.
// The members represent the nearest timestamp where the return value of eval
// could change (using the same unit for timestamps as TransitionVector).
// Note that this doesn't guarantee the value will change (that's why it's only
// a hint), only that the node won't change at a closer timestamp.
//
class SearchHint {
    long forward;
    long backward;
}