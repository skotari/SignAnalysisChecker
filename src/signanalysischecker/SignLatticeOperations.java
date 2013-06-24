package signanalysischecker;

import edu.cmu.cs.crystal.simple.SimpleLatticeOperations;

public class SignLatticeOperations extends SimpleLatticeOperations<SignLatticeElement> {
	
	@Override
	public boolean atLeastAsPrecise(SignLatticeElement left,
			SignLatticeElement right) {
		if (left == right)
			return true;
		else if (left == SignLatticeElement.BOTTOM)
			return true;
		else if (right == SignLatticeElement.UNKNOWN)
			return true;
		else
			return false;
	}

	@Override
	public SignLatticeElement bottom() {
		return SignLatticeElement.BOTTOM;
	}

	@Override
	public SignLatticeElement copy(SignLatticeElement original) {
		return original;
	}

	@Override
	public SignLatticeElement join(SignLatticeElement left,
			SignLatticeElement right) {
		if (left == right)
			return left;
		else if (left == SignLatticeElement.BOTTOM)
			return right;
		else if (right == SignLatticeElement.BOTTOM)
			return left;
		else 
			return SignLatticeElement.UNKNOWN;
	}


}
