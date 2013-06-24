package signanalysischecker;


/**
 * There are only 4 possibilities for nullness:
 * ZERO: if the value is definitely zero.
 * POSITIVE: if the value is definitely positive.
 * NEGATIVE: if the value is definitely negative.
 * UNKNOWN: if the value is unknown.
 * Bottom: if the concept of sign does not apply.
 * @author Satya
 *
 */
public enum SignLatticeElement {
	ZERO,POSITIVE,NEGATIVE,UNKNOWN,BOTTOM;
}