package signanalysischecker;

import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

import edu.cmu.cs.crystal.IAnalysisReporter.SEVERITY;
import edu.cmu.cs.crystal.analysis.constant.ConstantTransferFunction;
import edu.cmu.cs.crystal.annotations.AnnotationDatabase;
import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.flow.BooleanLabel;
import edu.cmu.cs.crystal.flow.ILabel;
import edu.cmu.cs.crystal.flow.ILatticeOperations;
import edu.cmu.cs.crystal.flow.IResult;
import edu.cmu.cs.crystal.flow.LabeledResult;
import edu.cmu.cs.crystal.flow.LabeledSingleResult;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.simple.TupleLatticeOperations;
import edu.cmu.cs.crystal.tac.AbstractTACBranchSensitiveTransferFunction;
import edu.cmu.cs.crystal.tac.model.ArrayInitInstruction;
import edu.cmu.cs.crystal.tac.model.AssignmentInstruction;
import edu.cmu.cs.crystal.tac.model.BinaryOperation;
import edu.cmu.cs.crystal.tac.model.BinaryOperator;
import edu.cmu.cs.crystal.tac.model.CopyInstruction;
import edu.cmu.cs.crystal.tac.model.LoadArrayInstruction;
import edu.cmu.cs.crystal.tac.model.LoadLiteralInstruction;
import edu.cmu.cs.crystal.tac.model.MethodCallInstruction;
import edu.cmu.cs.crystal.tac.model.NewArrayInstruction;
import edu.cmu.cs.crystal.tac.model.NewObjectInstruction;
import edu.cmu.cs.crystal.tac.model.OneOperandInstruction;
import edu.cmu.cs.crystal.tac.model.StoreArrayInstruction;
import edu.cmu.cs.crystal.tac.model.StoreFieldInstruction;
import edu.cmu.cs.crystal.tac.model.UnaryOperation;
import edu.cmu.cs.crystal.tac.model.Variable;

/**
 * These transfer functions are identical to the ones for the annotated NPE analysis except
 * 1) Its now branching, so the transfer function signatures are slightly different.
 * 2) we transfer on BinaryOperation and put different values down the two branches.
 * 
 * @author Satya
 *
 */
public class SATransferFunction extends AbstractTACBranchSensitiveTransferFunction<TupleLatticeElement<Variable, SignLatticeElement>> {
	/**
	 * The operations for this lattice. We want to have a tuple lattice from variables to null lattice elements, so we
	 * give it an instance of NullLatticeOperations. We also want the default value to be maybe null.
	 */
	TupleLatticeOperations<Variable, SignLatticeElement> ops =
		new TupleLatticeOperations<Variable, SignLatticeElement>(new SignLatticeOperations(), SignLatticeElement.UNKNOWN);
	
private AnnotationDatabase annoDB;
	
	public SATransferFunction(AnnotationDatabase annoDB) {
		this.annoDB = annoDB;
	}
	/**
	 * The operations will create a default lattice which will map all variables to UNKNOWN
	 */
	public TupleLatticeElement<Variable, SignLatticeElement> createEntryValue(
			MethodDeclaration method) {
		TupleLatticeElement<Variable, SignLatticeElement> def = ops.getDefault();
		//def.put(getAnalysisContext().getThisVariable(), SignLatticeElement.UNKNOWN);
		
		for (int ndx = 0; ndx < method.parameters().size(); ndx++) {
			SingleVariableDeclaration decl = (SingleVariableDeclaration) method.parameters().get(ndx);
			
			Variable paramVar = getAnalysisContext().getSourceVariable(decl.resolveBinding());
						
				def.put(paramVar, SignLatticeElement.UNKNOWN);
				//System.out.println("Method Declaration:"+def);
		}
	
		return def;
	}

	/**
	 * Just return our lattice ops.
	 */
	public ILatticeOperations<TupleLatticeElement<Variable, SignLatticeElement>> getLatticeOperations() {
		return ops;
	}

	@Override
	public IResult<TupleLatticeElement<Variable, SignLatticeElement>> transfer(
			BinaryOperation binop, List<ILabel> labels,
			TupleLatticeElement<Variable, SignLatticeElement> value) {
		
		//System.out.println(binop.getOperand1());
		//System.out.println(binop.getOperand2());
		SignLatticeElement leftValue = value.get(binop.getOperand1());
		SignLatticeElement rightValue = value.get(binop.getOperand2());
	
		Variable opToChange= binop.getTarget();
					switch(leftValue)
				{
				case POSITIVE:
					switch(rightValue)
					{
					case POSITIVE:
						switch (binop.getOperator()) {
						case ARIT_SUBTRACT:
							value.put(opToChange, SignLatticeElement.UNKNOWN);
						/*	System.out.println(" 1"+binop.getOperand1()+" "+leftValue+" "+
							binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
						*/
							break;
						case ARIT_ADD:
						case ARIT_MULTIPLY:
							value.put(opToChange, leftValue);
//							System.out.println(" 2"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
//							
							break;
						default:
							value.put(opToChange, SignLatticeElement.UNKNOWN);
//							System.out.println(" 3"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;
						}
						break;
					case NEGATIVE:
						switch (binop.getOperator()) {
						case ARIT_ADD:
						case ARIT_MULTIPLY:
							value.put(opToChange, SignLatticeElement.NEGATIVE);
//							System.out.println(" 4"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;
						case ARIT_SUBTRACT:
						
							value.put(opToChange, leftValue);
//							System.out.println(" 5"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;
						default:
							value.put(opToChange, SignLatticeElement.UNKNOWN);
//							System.out.println(" 6"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;
						}
						break;
					case ZERO:
						switch (binop.getOperator()) {
						case ARIT_SUBTRACT:
						case ARIT_ADD:
							value.put(opToChange, leftValue);
//							System.out.println(" 7"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;
						case ARIT_MULTIPLY:
							value.put(opToChange, rightValue);
//							System.out.println(" 8"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;
						default:
							value.put(opToChange, SignLatticeElement.UNKNOWN);
//							System.out.println(" 9"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;
						}
						break;
								
					}
					break;
				case NEGATIVE:
					switch(rightValue)
					{
					case POSITIVE:
						switch (binop.getOperator()) {
						case ARIT_ADD:
							value.put(opToChange, SignLatticeElement.UNKNOWN);
//							System.out.println(" 10"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;

						case ARIT_SUBTRACT:
						case ARIT_MULTIPLY:
							value.put(opToChange, SignLatticeElement.NEGATIVE);
//							System.out.println(" 11"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;
						default:
							value.put(opToChange, SignLatticeElement.UNKNOWN);
//							System.out.println(" 12"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;
						}
													
						break;
					case NEGATIVE:
						switch (binop.getOperator()) {
						case ARIT_ADD:
							value.put(opToChange, SignLatticeElement.NEGATIVE);
//							System.out.println(" 13"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;
						case ARIT_MULTIPLY:
						case ARIT_DIVIDE:
							value.put(opToChange, SignLatticeElement.POSITIVE);
//							System.out.println(" 14"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;
						default:
							value.put(opToChange, SignLatticeElement.UNKNOWN);
//							System.out.println(" 15"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;
						}
												
						break;
					case ZERO:
						switch (binop.getOperator()) {
						case ARIT_SUBTRACT:
						case ARIT_ADD:
							value.put(opToChange, leftValue);
//							System.out.println(" 16"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;
						case ARIT_MULTIPLY:
							value.put(opToChange, rightValue);
//							System.out.println(" 17"+binop.getOperand1()+" "+leftValue+" "+
//									binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
							break;
						default:
							break;
						}
								
					}
					break;
					
				case ZERO:
					switch (binop.getOperator()) {
					case ARIT_SUBTRACT:
					case ARIT_ADD:
						value.put(opToChange, rightValue);
//						System.out.println(" 18"+binop.getOperand1()+" "+leftValue+" "+
//								binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
						break;
					case ARIT_MULTIPLY:
						value.put(opToChange, leftValue);
//						System.out.println(" 19"+binop.getOperand1()+" "+leftValue+" "+
//								binop.getOperator()+" "+binop.getOperand2()+" "+rightValue+" "+value.get(opToChange));
						break;
					default:
						break;
					}
				}
				
				LabeledResult<TupleLatticeElement<Variable, SignLatticeElement>> result =
						LabeledResult.createResult(value);
		//		System.out.println("Binary:"+value);

				return result;
		
	}

	
	@Override
	public IResult<TupleLatticeElement<Variable, SignLatticeElement>> transfer(
			CopyInstruction instr, List<ILabel> labels,
			TupleLatticeElement<Variable, SignLatticeElement> value) {
		value.put(instr.getTarget(), value.get(instr.getOperand()));
	//	System.out.println("Copy:"+value);

		return LabeledSingleResult.createResult(value, labels);
	}
	
	public IResult<TupleLatticeElement<Variable, SignLatticeElement>> transfer(
			StoreArrayInstruction instr, List<ILabel> labels,
			TupleLatticeElement<Variable, SignLatticeElement> value) {
	//	System.out.println("Store Array"+instr.getDestinationArray());
	//	System.out.println(instr.getSourceOperand()+" "+instr.getAccessedArrayOperand()+"  "+instr.getArrayIndex());
		value.put(instr.getDestinationArray(), value.get(instr.getSourceOperand()));
	//	System.out.println(value);
		return LabeledSingleResult.createResult(value, labels);
		
	}

	public IResult<TupleLatticeElement<Variable, SignLatticeElement>> transfer(
			LoadArrayInstruction instr, List<ILabel> labels,
			TupleLatticeElement<Variable, SignLatticeElement> value) {
		
	//	System.out.println("Load array");
		
	//	System.out.println(instr.getSourceArray());
	//	System.out.println(instr.getTarget());
		value.put(instr.getTarget(), value.get(instr.getSourceArray()));
	//	System.out.println(value);
		return LabeledSingleResult.createResult(value, labels);
	
	}
	@Override
	public IResult<TupleLatticeElement<Variable, SignLatticeElement>> transfer(
			LoadLiteralInstruction instr, List<ILabel> labels,
			TupleLatticeElement<Variable, SignLatticeElement> value) {
	//	System.out.println("load started");
//	System.out.println(instr.getLiteral().getClass());
		if(instr.getLiteral() instanceof String)
		{
			
		//	System.out.println(instr.getLiteral());
			String con=instr.getLiteral().toString();
			//int constant=(Integer)(instr.getLiteral());
			int constant=(Integer.parseInt(con));
			if (constant>0)
				value.put(instr.getTarget(), SignLatticeElement.POSITIVE);
			else if(constant==0)
				value.put(instr.getTarget(), SignLatticeElement.ZERO);
			else if(constant<0)
				value.put(instr.getTarget(), SignLatticeElement.NEGATIVE);
		//	System.out.println("Load:"+value);
		}

		
		return LabeledSingleResult.createResult(value, labels);
	}
	
	public IResult<TupleLatticeElement<Variable, SignLatticeElement>> transfer(
			UnaryOperation instr, List<ILabel> labels,
			TupleLatticeElement<Variable, SignLatticeElement> value) {
		//System.out.println("unary started");
		switch(instr.getOperator())
		{
		case ARIT_PLUS:
			value.put(instr.getTarget(), value.get(instr.getOperand()));
			break;
		case ARIT_MINUS:
			switch(value.get(instr.getOperand()))
			{
				case POSITIVE:
					value.put(instr.getTarget(), SignLatticeElement.NEGATIVE);
					break;
				case NEGATIVE:
					value.put(instr.getTarget(), SignLatticeElement.POSITIVE);
					break;
				default:
					value.put(instr.getTarget(), value.get(instr.getOperand()));
					break;				
			}
			break;
		}
	//	System.out.println("Unary:"+value);
		return LabeledSingleResult.createResult(value, labels);
	}

	public IResult<TupleLatticeElement<Variable, SignLatticeElement>> transfer(
			StoreFieldInstruction instr, List<ILabel> labels,
			TupleLatticeElement<Variable, SignLatticeElement> value) {
		
		return LabeledSingleResult.createResult(value, labels);
	}
	}
