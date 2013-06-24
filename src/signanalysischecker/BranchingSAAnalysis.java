package signanalysischecker;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import edu.cmu.cs.crystal.AbstractCrystalMethodAnalysis;
import edu.cmu.cs.crystal.IAnalysisReporter.SEVERITY;
import edu.cmu.cs.crystal.annotations.AnnotationSummary;
import edu.cmu.cs.crystal.simple.TupleLatticeElement;
import edu.cmu.cs.crystal.tac.TACFlowAnalysis;
import edu.cmu.cs.crystal.tac.model.Variable;
import edu.cmu.cs.crystal.util.Utilities;

/**
 *  Sign Analysis Implementation
 * 
 * @author Satya
 */
public class BranchingSAAnalysis extends AbstractCrystalMethodAnalysis {
	
	TACFlowAnalysis<TupleLatticeElement<Variable, SignLatticeElement>> flowAnalysis;

	@Override
	public String getName() {
		return "Sign Analysis";
	}

	@Override
	public void analyzeMethod(MethodDeclaration d) {
		SATransferFunction tf = new SATransferFunction(getInput().getAnnoDB());
		flowAnalysis = new TACFlowAnalysis<TupleLatticeElement<Variable,SignLatticeElement>>(tf, getInput().getComUnitTACs().unwrap());
		
		d.accept(new NPEFlowVisitor());
	}

	/**
	 * The visitor for the analysis.
	 * @author Satya
	 */
	public class NPEFlowVisitor extends ASTVisitor {

		private void checkVariable(TupleLatticeElement<Variable, SignLatticeElement> tuple, Expression nodeToCheck) {
			
//			Variable varToCheck = flowAnalysis.getVariable(nodeToCheck);
//			SignLatticeElement element = tuple.get(varToCheck);
//			System.out.println(varToCheck+": "+element);
			/*
			if (element == NullLatticeElement.MAYBE_NULL)
				getReporter().reportUserProblem("The expression " + nodeToCheck + " may be null.", nodeToCheck, getName(), SEVERITY.WARNING);		
			else if (element == NullLatticeElement.NULL)
				getReporter().reportUserProblem("The expression " + nodeToCheck + " is null.", nodeToCheck, getName(), SEVERITY.ERROR);		
		*/}

		@Override
		public void endVisit(ArrayAccess node) {
		//	System.out.println("ArrayAccess:"+node);
			TupleLatticeElement<Variable, SignLatticeElement> beforeTuple = flowAnalysis.getResultsBeforeAST(node);
			checkVariable(beforeTuple, node.getIndex());
			Variable varToCheck = flowAnalysis.getVariable(node.getIndex());
			SignLatticeElement element = beforeTuple.get(varToCheck);
			if(element==SignLatticeElement.NEGATIVE)
			{
				System.out.println("The index of " + node + " is negative.");
				getReporter().reportUserProblem("The index of " + node + " is negative.", node, getName(), SEVERITY.ERROR);	
			}
			else if(element==SignLatticeElement.UNKNOWN)
			{
				System.out.println("The index of " + node + " may be negative.");
				getReporter().reportUserProblem("The index of" + node + " may be negative.", node, getName(), SEVERITY.WARNING);	
			}

		//	System.out.println(varToCheck+": "+element);
			
		}

		@Override
		public void endVisit(FieldAccess node) {
			TupleLatticeElement<Variable, SignLatticeElement> beforeTuple = flowAnalysis.getResultsBeforeAST(node);
			
			if (node.getExpression() != null)
				checkVariable(beforeTuple, node.getExpression());
		}
		
		@Override
		public void endVisit(MethodInvocation node) {
			TupleLatticeElement<Variable, SignLatticeElement> beforeTuple = flowAnalysis.getResultsBeforeAST(node);
			
			//if (node.getExpression() != null)
			//	checkVariable(beforeTuple, node.getExpression());
			
		//	AnnotationSummary summary = getInput().getAnnoDB().getSummaryForMethod(node.resolveMethodBinding());
			
			for (int ndx = 0; ndx < node.arguments().size(); ndx++) {	
				///if (summary.getParameter(ndx, NON_NULL_ANNO) != null) //is this parameter annotated with @Nonnull?
					checkVariable(beforeTuple, (Expression) node.arguments().get(ndx));
			}
		}

		@Override
		public void endVisit(QualifiedName node) {
			//Due to an ambiguity within the parser, a qualified name may actually be a FieldAccess.
			//To check for this, see what the binding is.
			if (node.resolveBinding() instanceof IVariableBinding) {
				//now we know it's field access.
				TupleLatticeElement<Variable, SignLatticeElement> beforeTuple = flowAnalysis.getResultsBeforeAST(node);
				
				checkVariable(beforeTuple, node.getQualifier());
			}
		}
		
		@Override
		public void endVisit(Assignment node) {
		//	System.out.println("Assignment:"+node);
			Expression left = node.getLeftHandSide();
			Expression right = node.getRightHandSide();
			checkVariable(flowAnalysis.getResultsBeforeAST(left), right);
		
		}
		
		
	}
}
