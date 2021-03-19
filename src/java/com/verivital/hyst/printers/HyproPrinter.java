package com.verivital.hyst.printers;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.internalpasses.ConvertToStandardForm;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.Classification;
import com.verivital.hyst.util.FlattenRenameUtils;
import com.verivital.hyst.util.PairStringOptionHandler;
import com.verivital.hyst.util.PreconditionsFlag;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;
import com.verivital.hyst.util.RangeExtractor.UnsupportedConditionException;

/**
 * Printer for Hypro models. Based on Flow* printer.
 * 
 * @author Leander Behr (6-2020)
 *
 */
public class HyproPrinter extends ToolPrinter {
	@Option(name = "-time", usage = "reachability time", metaVar = "VAL")
	String time = "auto";

	@Option(name = "-step", usage = "reachability step", metaVar = "MIN-MAX")
	String step = "auto-auto";

	@Option(name = "-rem", usage = "remainder estimate", metaVar = "VAL")
	String remainder = "1e-4";

	@Option(name = "-precondition", usage = "precondition method", metaVar = "VAL")
	String precondition = "auto";

	@Option(name = "-plot", usage = "output plot line in Hypro file (for example 'gnuplot octagon x,y')", metaVar = "VAL")
	String plot = "auto";

	@Option(name = "-orders", usage = "taylor model orders", metaVar = "MIN-MAX")
	String orders = "3-8";

	@Option(name = "-cutoff", usage = "taylor model cutoff", metaVar = "VAL")
	String cutoff = "1e-15";

	@Option(name = "-ode", usage = "ode integration mode (like 'poly ode 1')", metaVar = "VAL")
	String ode = "auto";

	@Option(name = "-precision", usage = "numerical precision", metaVar = "VAL")
	String precision = "53";

	private int DEFAULT_MAX_JUMPS = 999999999;

	@Option(name = "-jumps", usage = "maximum jumps", metaVar = "VAL")
	String jumps = "" + DEFAULT_MAX_JUMPS;

	@Option(name = "-printoff", usage = "suppress step by step stdout output")
	boolean noPrint = false;

	@Option(name = "-nooutput", usage = "suppress output folder for plotting")
	boolean noOutput = false;

	@Option(name = "-aggregation", usage = "discrete jump successor aggregation method", metaVar = "VAL")
	String aggregation = "parallelotope";

	@Option(name = "-unsafe", usage = "unsafe condition (single mode models only)", metaVar = "COND")
	String unsafe = "auto";

	FlowstarExpressionPrinter flowstarExpressionPrinter;

	@SuppressWarnings("deprecation")
	@Option(name = "-taylor_init", usage = "override the initial states with a taylor model. Expects two arguments: "
			+ "(mode name) (TM expression), where colons in the TM expression are replaced with newlines.", metaVar = "MODE TM", handler = PairStringOptionHandler.class)
	public void setTaylorIinit(String[] params) throws CmdLineException {
		if (params.length != 2)
			throw new CmdLineException("-taylor_init expected exactly two follow-on arguments");

		taylorInit = new ArrayList<String>();
		taylorInit.add(params[0]);
		taylorInit.add(params[1]);
	}

	List<String> taylorInit = null;

	private BaseComponent ha;

	public HyproPrinter() {
		preconditions.skip(PreconditionsFlag.NO_URGENT);
		preconditions.skip(PreconditionsFlag.NO_NONDETERMINISTIC_DYNAMICS);
		preconditions.skip(PreconditionsFlag.CONVERT_NONDETERMINISTIC_RESETS);
		preconditions.skip(PreconditionsFlag.CONVERT_ALL_FLOWS_ASSIGNED);
		preconditions.skip(PreconditionsFlag.CONVERT_TO_FLAT_AUTOMATON);
		preconditions.unskip(PreconditionsFlag.COLLAPSE_IDENTICAL_JUMPS);
	}

	@Override
	protected String getCommentPrefix() {
		return "#";
	}

	/**
	 * Simplify an expression by substituting constants and then doing math
	 * simplification
	 * 
	 * @param e the original expression
	 * @return the modified expression
	 */
	private Expression simplifyExpression(Expression ex) {
		Expression subbed = SubstituteConstantsPass.substituteConstantsIntoExpression(ha.constants, ex);

		return AutomatonUtil.simplifyExpression(subbed);
	}

	private void printForbidden() {
		if (!unsafe.equals("auto")) {
			BaseComponent base = this.ha;

			if (base.modes.size() != 1)
				throw new AutomatonExportException("Expected single automaton mode with explicit unsafe condition.");

			printLine("");
			printLine("unsafe");
			printLine("{");

			String name = base.modes.values().iterator().next().name;
			printLine(name + " {" + this.unsafe + "}");

			printLine("}");
		} else if (config.forbidden.size() > 0) {
			printLine("");
			printLine("unsafe");
			printLine("{");

			for (Entry<String, Expression> e : config.forbidden.entrySet()) {
				printLine(e.getKey() + " {" + e.getValue() + "}");
			}

			printLine("}");
		}
	}

	private void printSettings() {
		printNewline();
		printLine("setting");
		printLine("{");

		String[] step = getStepParam();

		if (step.length == 1 || step.length == 2 && step[0].equals(step[1]))
			printLine("fixed steps " + step[0]);
		else if (step.length == 2)
			printLine("adaptive steps { min " + step[0] + ", max " + step[1] + " }");
		else
			throw new AutomatonExportException("Param 'step' should have one or two entries: " + step);

		printLine("time " + getTimeParam());

		printLine("remainder estimation " + remainder);

		if (precondition.equals("auto")) {
			// follow recommendation in 1.2 manual
			if (ha.variables.size() > 3)
				printLine("identity precondition");
			else
				printLine("QR precondition");
		} else
			printLine(precondition + " precondition");

		printLine(getPlotParam());

		String[] order = getOrderParam();

		if (order.length == 1 || order.length == 2 && order[0].equals(order[1]))
			printLine("fixed orders " + order[0]);
		else if (order.length == 2)
			printLine("adaptive orders { min " + order[0] + ", max " + order[1] + " } ");
		else
			throw new AutomatonExportException("Param 'orders' should have one or two entries: " + orders);

		printLine("cutoff " + cutoff);
		printLine("precision " + precision);

		if (noOutput)
			printLine("no output");
		else
			printLine("output out");

		int jumps = Integer.parseInt(this.jumps);

		if (jumps == DEFAULT_MAX_JUMPS && config.settings.spaceExConfig.maxIterations > 0)
			jumps = config.settings.spaceExConfig.maxIterations - 1;

		printLine("max jumps " + jumps);

		if (noPrint)
			printLine("print off");
		else
			printLine("print on");
		printLine("}");
	}

	private String[] getOrderParam() {
		return orders.split("-");
	}

	private String getTimeParam() {
		String value = time;

		if (value.equals("auto"))
			value = doubleToString(config.settings.spaceExConfig.timeHorizon);

		return value;
	}

	private String getPlotParam() {
		String auto = "gnuplot octagon";
		String value = plot;

		if (value.equals("auto"))
			value = auto;

		if (value.equals("gnuplot octagon") || value.equals("gnuplot interval") || value.equals("matlab interval")
				|| value.equals("matlab octagon"))
			value = value + " " + ha.variables.get(0) + "," + ha.variables.get(Math.min(1, ha.variables.size() - 1));

		return value;
	}

	private String[] getStepParam() {
		String value = step;

		if (value.contains("auto")) {
			String autoVal = doubleToString(config.settings.spaceExConfig.samplingTime);
			value = value.replace("auto", autoVal);
		}

		return value.split("-");
	}

	/**
	 * Print variable declarations and their initial value assignments plus a list
	 * of all constants
	 */
	private void printVars() {
		printLine("# Vars");

		String varLine = "state var ";

		boolean first = true;

		for (String v : ha.variables) {
			if (first)
				first = false;
			else
				varLine += ", ";

			varLine += v;
		}

		printLine(varLine);
	}

	/**
	 * Print initial states
	 */
	private void printInitialState(String loc, Expression exp) {
		printNewline();
		printLine("init");
		printLine("{");

		printLine(loc);
		printLine("{");

		printFlowRangeConditions(removeConstants(exp, ha.constants.keySet()), true);

		printLine("}"); // end mode

		printLine("}"); // end all initial modes
	}

	private void printFlowRangeConditions(Expression ex, boolean isAssignment) {
		HashMap<String, Interval> ranges = getExpressionVariableRanges(ex);

		for (Entry<String, Interval> e : ranges.entrySet()) {
			String varName = e.getKey();
			Interval inter = e.getValue();

			if (isAssignment)
				printLine(varName + " in [" + doubleToString(inter.min) + ", " + doubleToString(inter.max) + "]");
			else {
				// it's a comparison

				if (inter.min == inter.max)
					printLine(varName + " = " + doubleToString(inter.min));
				else {
					if (inter.min != -Double.MAX_VALUE)
						printLine(varName + " >= " + doubleToString(inter.min));

					if (inter.max != Double.MAX_VALUE)
						printLine(varName + " <= " + doubleToString(inter.max));
				}
			}
		}
	}

	private static Expression removeConstants(Expression e, Collection<String> constants) {
		Operation o = e.asOperation();
		Expression rv = e;

		if (o != null) {
			if (o.op == Operator.AND) {
				Operation rvO = new Operation(Operator.AND);

				for (Expression c : o.children)
					rvO.children.add(removeConstants(c, constants));

				rv = rvO;
			} else if (o.op == Operator.EQUAL) {
				Expression left = o.getLeft();

				if (left instanceof Variable && constants.contains(((Variable) left).name))
					rv = Constant.TRUE;
			}
		}

		return rv;
	}

	/**
	 * Prints the locations with their labels and everything that happens in them
	 * (invariant, flow...)
	 */
	private void printModes() {
		printNewline();

		printLine("modes");
		printLine("{");

		// modename
		boolean first = true;

		for (Entry<String, AutomatonMode> e : ha.modes.entrySet()) {
			AutomatonMode mode = e.getValue();

			// removed this
			if (flowstarExpressionPrinter.inputVariables.size() > 0)
				flowstarExpressionPrinter.extractInputVariableRanges(mode.invariant);

			if (first)
				first = false;
			else
				printNewline();

			String locName = e.getKey();
			printLine(locName);
			printLine("{");

			// From Xin Chen e-mail:
			// lti ode - linear time-invariant, can also have uncertain input
			// note: this used to be called "linear ode" in version 2.0.0 and earlier

			// "poly ode 1" works more efficient than the others on low degree
			// and low dimension (<=3) ODEs.
			// "poly ode 2" works more efficient than the others on low degree
			// and medium dimension (4~6) ODEs.
			// "poly ode 3" works more efficient than the others on medium or
			// high degree and high dimension ODEs.
			// "nonpoly ode" works with nonlinear terms

			// first simplify
			for (Entry<String, ExpressionInterval> entry : mode.flowDynamics.entrySet()) {
				ExpressionInterval ei = entry.getValue();
				ei.setExpression(simplifyExpression(ei.getExpression()));
			}

			// then classify
			if (ode.equals("auto")) {
				if (isNonLinearDynamics(mode.flowDynamics))
					printLine("nonpoly ode");
				else if (Classification.isLinearDynamics(mode.flowDynamics))
					printLine("lti ode");
				else if (ha.variables.size() <= 3)
					printLine("poly ode 1");
				else if (ha.variables.size() <= 6)
					printLine("poly ode 2");
				else
					printLine("poly ode 3");
			} else {
				// force ode line
				printLine(ode);
			}

			// then print
			printLine("{");
			for (Entry<String, ExpressionInterval> entry : mode.flowDynamics.entrySet()) {
				ExpressionInterval ei = entry.getValue();

				// be explicit (even though x' == 0 is implied)
				printLine(entry.getKey() + "' = " + ei);
			}
			printLine("}");

			// invariant
			printLine("inv");
			printLine("{");

			String originalInvariant = mode.invariant.toDefaultString();
			Expression inv = simplifyExpression(mode.invariant);

			if (!inv.equals(Constant.TRUE)) {
				printCommentBlock("Original invariant: " + originalInvariant);

				printLine(inv.toString());
			}

			printLine("}"); // end invariant
			printLine("}"); // end individual mode

		}

		printLine("}"); // end all modes
	}

	private boolean isNonLinearDynamics(LinkedHashMap<String, ExpressionInterval> flowDynamics) {
		boolean rv = false;

		for (ExpressionInterval entry : flowDynamics.values()) {
			Expression e = entry.getExpression();

			byte classification = AutomatonUtil.classifyExpressionOps(e);

			if ((classification & AutomatonUtil.OPS_NONLINEAR) != 0) {
				rv = true;
				break;
			}
		}

		return rv;
	}

	public static HashMap<String, Interval> getExpressionVariableRanges(Expression ex) {
		HashMap<String, Interval> ranges = new HashMap<String, Interval>();

		try {
			RangeExtractor.getVariableRanges(ex, ranges);
		} catch (EmptyRangeException e) {
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		} catch (ConstantMismatchException e) {
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		} catch (UnsupportedConditionException e) {
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		}

		return ranges;
	}

	private void printJumps() {
		printNewline();
		printLine("jumps");
		printLine("{");

		boolean first = true;

		for (AutomatonTransition t : ha.transitions) {
			Expression guard = simplifyExpression(t.guard);

			if (guard == Constant.FALSE)
				continue;

			if (first)
				first = false;
			else
				printNewline();

			String fromName = t.from.name;
			String toName = t.to.name;

			printLine(fromName + " -> " + toName);

			if (t.urgent)
				printLine("urgent");

			printLine("guard");
			printLine("{");

			if (!guard.equals(Constant.TRUE)) {
				printCommentBlock("Original guard: " + t.guard.toDefaultString());
				printLine(guard.toString());
			}

			printLine("}");

			printLine("reset");
			printLine("{");

			for (Entry<String, ExpressionInterval> e : t.reset.entrySet()) {
				ExpressionInterval ei = e.getValue();
				ei.setExpression(simplifyExpression(ei.getExpression()));
				printLine(e.getKey() + "' := " + ei);
			}

			printLine("}");

			if (!t.labels.isEmpty()) {
				printLine("label { " + t.labels.stream().reduce((a, b) -> a + " " + b).get() + " }");
			}
			if (aggregation.equals("parallelotope"))
				printLine("parallelotope aggregation {}");
			else if (aggregation.equals("interval"))
				printLine("interval aggregation");
			else
				throw new AutomatonExportException("Unknown aggregation method: " + aggregation);
		}

		printLine("}");

	}

	public static class FlowstarExpressionPrinter extends DefaultExpressionPrinter {
		public ArrayList<String> inputVariables = new ArrayList<String>(); // populated externally

		TreeMap<String, Interval> inputVariableRanges;

		public FlowstarExpressionPrinter() {
			super();

			opNames.put(Operator.AND, " ");
		}

		/**
		 * Extract the input variable ranges from a mode's invariant. Afterwards,
		 * instead of printing the variable you will print the interval range.
		 * 
		 * @param invariant the current mode's invariant expression
		 */
		public void extractInputVariableRanges(Expression invariant) {
			inputVariableRanges = new TreeMap<String, Interval>();

			try {
				RangeExtractor.getVariableRanges(invariant, inputVariableRanges);
			} catch (EmptyRangeException e) {
				throw new AutomatonExportException(e.getLocalizedMessage(), e);
			} catch (ConstantMismatchException e) {
				throw new AutomatonExportException(e.getLocalizedMessage(), e);
			} catch (UnsupportedConditionException e) {
				throw new AutomatonExportException(e.getLocalizedMessage(), e);
			}
		}

		@Override
		public String printOperator(Operator op) {
			if (op.equals(Operator.GREATER) || op.equals(Operator.LESS) || op.equals(Operator.NOTEQUAL)
					|| op == Operator.OR)
				throw new AutomatonExportException("Hypro doesn't support operator '" + op.toDefaultString() + "'");

			return super.printOperator(op);
		}

		@Override
		protected String printTrue() {
			return " ";
		}

		@Override
		protected String printFalse() {
			return "1 <= 0"; // not really sure if this will work
		}

		@Override
		protected String printVariable(Variable v) {
			String rv = null;

			if (inputVariables.contains(v.name)) {
				Interval range = inputVariableRanges.get(v.name);

				if (range.isConstant())
					rv = this.printConstantValue(range.min);
				else
					rv = "[" + this.printConstantValue(range.min) + ", " + this.printConstantValue(range.max) + "]";
			} else
				rv = super.printVariable(v);

			return rv;
		}

		@Override
		protected String printOperation(Operation o) {
			String rv = null;

			if (Operator.isComparison(o.op)) {
				rv = "";
				Operator op = o.op;

				// print nothing if the expression contains an input variable
				// this will omit it in the invariant expressions
				boolean hasInputVariable = false;
				Collection<String> allVars = AutomatonUtil.getVariablesInExpression(o);

				for (String v : allVars) {
					if (inputVariables.contains(v)) {
						hasInputVariable = true;
						break;
					}
				}

				if (!hasInputVariable) {
					// make sure it's of the form p ~ c
					if (o.children.size() == 2 && o.getRight() instanceof Constant)
						rv = super.printOperation(o);
					else {
						// change 'p1 ~ p2' to 'p1 - (p2) ~ 0'
						rv += o.getLeft() + " - (" + o.getRight() + ") " + printOperator(op) + " 0";
					}
				}
			} else if (o.op == Operator.MULTIPLY && o.children.size() == 2) {
				// special handling for the case of constant * input interval
				Expression num = o.children.get(0);
				Expression inputVar = o.children.get(1);

				// swap order if necessary
				if (inputVar instanceof Constant && num instanceof Variable) {
					Expression temp = inputVar;
					inputVar = num;
					num = temp;
				}

				// check if it's the special case
				if (inputVar instanceof Variable && num instanceof Constant) {
					Variable v = (Variable) inputVar;
					double d = ((Constant) num).getVal();

					if (this.inputVariables.contains(v.name)) {
						Interval range = inputVariableRanges.get(v.name);

						if (range.isConstant())
							rv = this.printConstantValue(range.min * d);
						else {
							// negative constant flips interval range

							if (d >= 0)
								rv = "[" + this.printConstantValue(range.min * d) + ", "
										+ this.printConstantValue(range.max * d) + "]";
							else
								rv = "[" + this.printConstantValue(range.max * d) + ", "
										+ this.printConstantValue(range.min * d) + "]";
						}
					}
				}
			}

			if (rv == null)
				rv = super.printOperation(o);

			return rv;
		}
	}

	@Override
	public void setOutputFile(String filename) {
		outputType = OutputType.FILE;
		outputFilename = filename;
	}

	private void setOrChangeFile(String filename) {
		if (outputType != OutputType.FILE)
			throw new AutomatonExportException("Can not change file when output type is not file.");
		if (outputStream != null)
			changeOutputFile(filename);
		super.setOutputFile(filename);
	}

	@Override
	protected void printAutomaton() {
		//FlattenRenameUtils.convertToFullyQualifiedParams(config.root);

		int counter = 0;
		String outputFilenameOrig = new String(outputFilename);
		for (ComponentInstance entry : ((NetworkComponent) config.root).children.values()) {
			this.ha = (BaseComponent) entry.child;
			setOrChangeFile(filenameForComponent(outputFilenameOrig, ha));
		
			printAutomatonImpl(ha.initialMode.name, makeConjunction(ha.initialValues));	
			counter += 1;
		}

		printForbidden();
	}

	String filenameForComponent(String baseFileName, Component c) {

		String woExt = baseFileName.substring(0, baseFileName.lastIndexOf("."));

		String newName = woExt + "_" + c.instanceName + getExtension();

		return newName;
	}

	Entry<String, Expression> extractLocalInitialstate(Entry<String, Expression> state, Component c, int pos) {
		String name = c.instanceName;

		String transformedStr = state.getKey().split("\\.")[pos];
		Operation transformedExpr = new Operation(Operator.AND);

		Deque<Expression> stack = new ArrayDeque<Expression>();

		stack.push(state.getValue());

		while (!stack.isEmpty()) {
			Expression current = stack.getFirst();
			stack.pop();

			if (current.asOperation() == null)
				continue;
			if (current.asOperation().op == Operator.EQUAL) {

				Variable var = (Variable) current.asOperation().children.get(0);
				if (var.name.contains(name) || c.variables.contains(var.name)) {
					var.name = var.name.replaceFirst("\\.", "_");

					transformedExpr.children.add(current.copy());
				}
			} else {
				stack.addAll(current.asOperation().children);
			}
		}

		Operation folded = new Operation(Operator.AND);
		if (transformedExpr.children.isEmpty()) {
			throw new AutomatonExportException("No initial states for " + name + ".");

		} else if (transformedExpr.children.size() == 1) {
			folded = (Operation) transformedExpr.children.iterator().next();
		} else {
			Operation iter = folded;
			iter.children.add(transformedExpr.children.get(0));

			for (int i = 1; i < transformedExpr.children.size() - 1; i += 1) {
				Operation newAnd = new Operation(Operator.AND);
				newAnd.children.add(transformedExpr.children.get(i));
				iter.children.add(newAnd);
				iter = newAnd;
			}
			iter.children.add(transformedExpr.children.get(transformedExpr.children.size() - 1));
		}

		return new AbstractMap.SimpleEntry<String, Expression>(transformedStr, folded);
	}
	
	Expression makeConjunction(ArrayList<Expression> exps) {
		if(exps.isEmpty()) return null;
		if(exps.size() == 1) return exps.get(0);
		
		Expression curr = exps.get(0);
		
		for(int i = 0; i < exps.size(); i+=1) {
			curr = new Operation(curr, Operator.AND, exps.get(i));
		}
		
		return curr;
	}

	void printAutomatonImpl(String initLoc, Expression initExp) {
		flowstarExpressionPrinter = new FlowstarExpressionPrinter();
		Expression.expressionPrinter = flowstarExpressionPrinter;

		if (ha.modes.containsKey("init"))
			throw new AutomatonExportException("mode named 'init' is not allowed in Hypro printer");

		if (ha.modes.containsKey("start"))
			throw new AutomatonExportException("mode named 'start' is not allowed in Hypro printer");

		this.printCommentHeader();

		// begin printing the actual program
		printNewline();

		printLine("hybrid reachability");
		printLine("{");

		// printLine("name " + ha.instanceName);

		printVars();

		printSettings();

		printModes();

		printJumps();

		printInitialState(initLoc, initExp);

		printLine("}");
	}

	public static void convertInitialStatesToUrgent(Configuration config) {
		LinkedHashMap<String, Expression> initCopy = new LinkedHashMap<String, Expression>();
		initCopy.putAll(config.init);

		ConvertToStandardForm.convertInit(config);

		updateInitCondition(config, initCopy);
	}

	/**
	 * Update the initial condition to be the weak union of all the other modes
	 * 
	 * @param config the config object
	 * @param init2  the list of all the initial expressions in all the modes
	 */
	private static void updateInitCondition(Configuration config, LinkedHashMap<String, Expression> initMap) {
		Map<String, Interval> weakVarBounds = new HashMap<String, Interval>();

		// get weak bounds for each variable over all the initial states
		for (Entry<String, Expression> entry : initMap.entrySet()) {
			Expression e = entry.getValue();
			Map<String, Interval> bounds = getExpressionWeakVariableRanges(e);

			// add variables that weren't found
			for (String var : config.root.variables) {
				if (!bounds.containsKey(var))
					throw new AutomatonExportException("Could not determine constant upper/lower bounds on variable '"
							+ var + "' in mode '" + entry.getKey() + "'");
			}

			for (Entry<String, Interval> boundsEntry : bounds.entrySet()) {
				String var = boundsEntry.getKey();
				Interval i = boundsEntry.getValue();

				if (i.min == -Double.MAX_VALUE)
					throw new AutomatonExportException("Could not determine constant lower bound on variable '" + var
							+ "' in mode '" + entry.getKey() + "'");
				else if (i.max == Double.MAX_VALUE)
					throw new AutomatonExportException("Could not determine constant upper bound on variable '" + var
							+ "' in mode '" + entry.getKey() + "'");

				// merge i into the existing interval bounds
				Interval cur = weakVarBounds.get(var);

				if (cur == null)
					weakVarBounds.put(var, i);
				else
					weakVarBounds.put(var, Interval.union(cur, i));
			}
		}

		// apply weak bounds for each variable to initial state
		Expression init = config.init.values().iterator().next();

		for (Entry<String, Interval> e : weakVarBounds.entrySet()) {
			String v = e.getKey();
			Interval i = e.getValue();

			if (i.min != -Double.MAX_VALUE) {
				Operation cond = new Operation(i.min, Operator.LESSEQUAL, v);
				init = Expression.and(init, cond);
			}

			if (i.max != Double.MAX_VALUE) {
				Operation cond = new Operation(v, Operator.LESSEQUAL, i.max);
				init = Expression.and(init, cond);
			}
		}

		AutomatonMode initMode = ConvertToStandardForm.getInitMode((BaseComponent) config.root);
		config.init.put(initMode.name, init);
	}

	/**
	 * Gets the weak ranges for the given expression. Only interval ranges are
	 * extracted... other ranges are ignored.
	 * 
	 * @param ex the input expression
	 * @return
	 */
	private static Map<String, Interval> getExpressionWeakVariableRanges(Expression ex) {
		HashMap<String, Interval> ranges = new HashMap<String, Interval>();

		RangeExtractor.getWeakVariableRanges(ex, ranges);

		return ranges;
	}

	@Override
	public String getToolName() {
		return "Hypro";
	}

	@Override
	public String getCommandLineFlag() {
		return "hypro";
	}

	@Override
	public boolean isInRelease() {
		return false;
	}

	@Override
	public String getExtension() {
		return ".model";
	}
}
