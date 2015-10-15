package com.verivital.hyst.passes.complex.hybridize;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.HyperRectangle;
import com.verivital.hyst.geometry.HyperRectangleCornerEnumerator;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.complex.hybridize.AffineOptimize.OptimizationParams;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.simulation.RungeKutta;
import com.verivital.hyst.simulation.Simulator;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;

public class HybridizeTimeTriggeredPass extends TransformationPass 
{
	private final static String USAGE = "step=<val>,maxtime=<val>,epsilon=<val>(,picount=#)(,pimaxtime=#)" +
			"(,simtype={CENTER|star|corners|starcorners|rand10|rand#})(,addforbidden={TRUE|false})(,addintermediate={true|FALSE})";
	
	// extracted parameters
	double timeStep;
	double timeMax;
	double epsilon;
	boolean addForbidden = true;
	boolean addIntermediate = false;
	SimulationType simType = SimulationType.CENTER;
	int randCount = -1; // for SimulationType.RAND
	int piCount = 0;
	double piMaxTime = -1;
	
	enum SimulationType
	{
		CENTER, // simulate from the center
		STAR, // simulate from 2*n+1 points (center point as well as limits in each dimension),
		CORNERS, // simulate from n^2+1 points (center point as well as corners),
		STARCORNERS, // both STAR and CORNERS
		RAND, // random points on boundary
	}
	
	final String TT_VARIABLE = "_tt";
	final Expression ttGreaterThanZero = new Operation(Operator.GREATEREQUAL, TT_VARIABLE, 0);
	int ttVarIndex; // the index of timeVarible in ha.variableName
	
	private BaseComponent ha;
    private AutomatonMode originalMode;
    private AutomatonMode errorMode = null;
    
    @Override
    public String getName()
    {
        return "Time-Tiggered Hybridization Pass";
    }

    @Override
    public String getCommandLineFlag()
    {
        return "-hybridizett";
    }

    @Override
    public String getParamHelp()
    {
        return USAGE;
    }
    
    @Override
    protected void checkPreconditons(Configuration c, String name)
	{
		super.checkPreconditons(c, name);
		ha = (BaseComponent)c.root;
		
		// single mode
		if (((BaseComponent)c.root).modes.size() != 1)
			throw new PreconditionsFailedException(name + " requires a single mode.");
	}
    
    private void parseParams(String params)
	{
    	//example params: step=<val>,maxtime=<val>,...

    	// defaults
		timeStep = -1;
		timeMax = -1;
		epsilon = -1;
		addForbidden = true;
		simType = SimulationType.CENTER;
		addIntermediate = false;
		piCount = 0;
		piMaxTime = -1;
		
		String[] parts = params.split(",");
		
		for (String part : parts)
		{
			String[] assignment = part.split("=");
			
			if (assignment.length != 2)
				throw new AutomatonExportException("Pass parameter expected single '=' sign: " + part);
	
			String name = assignment[0].toLowerCase();
			String value = assignment[1].toLowerCase();
			
			try
			{				
				if (name.equals("step"))
					timeStep=Double.parseDouble(value);
				else if (name.equals("maxtime"))
					timeMax=Double.parseDouble(value);
				else if (name.equals("epsilon"))
					epsilon=Double.parseDouble(value);
				else if (name.equals("picount"))
					piCount=Integer.parseInt(value);
				else if (name.equals("pimaxtime"))
					piMaxTime=Double.parseDouble(value);
				else if (name.equals("simtype"))
				{
					if (value.equals("center"))
						simType = SimulationType.CENTER;
					else if (value.equals("star"))
						simType = SimulationType.STAR;
					else if (value.equals("corners"))
						simType = SimulationType.CORNERS;
					else if (value.equals("starcorners"))
						simType = SimulationType.STARCORNERS;
					else if (value.startsWith("rand") && value.length() > 4)
					{
						simType = SimulationType.RAND;
						randCount = Integer.parseInt(value.substring(4));
						
						Hyst.log("Using " + randCount + " random simulations on boundary");
					}
					else
						throw new AutomatonExportException("Unknown simulation type parameter: " + part+ "; usage: " + USAGE);
				}
				else if (name.equals("addforbidden"))
				{
					if (value.equals("true"))
						addForbidden = true;
					else if (value.equals("false"))
						addForbidden = false;
					else
						throw new AutomatonExportException("Unknown addforbidden parameter: " + part + "; usage: " + USAGE);
				}
				else if (name.equals("addintermediate"))
				{
					if (value.equals("true"))
						addIntermediate = true;
					else if (value.equals("false"))
						addIntermediate = false;
					else
						throw new AutomatonExportException("Unknown addintermediate parameter: " + part + "; usage: " + USAGE);
				}
				else
					throw new AutomatonExportException("Unknown parameter for pass: '" + name + "'; usage: " + USAGE);
			}
			catch (NumberFormatException e)
			{
				throw new AutomatonExportException("Error parsing pass parameter '" + part + "': " + USAGE, e);
			}
		}
		
		// errors if not set
		if (timeStep <= 0)
			throw new AutomatonExportException("Positive time must be set as pass parameter: " + USAGE);
		
		if (timeMax <= 0)
			throw new AutomatonExportException("Positive max time must be set as pass parameter: " + USAGE);
		
		if (epsilon < 0)
			throw new AutomatonExportException("Nonnegative epsilon must be set as pass parameter: " + USAGE);
		
		// generated values
		if (piMaxTime < 0)
			piMaxTime = timeStep * 5;
	}
    
    /**
     * Get the initial set of states as a HyperRectangle.
     * @return the initial set of states
     * @throws AutomatonExportException if the initial set of states is not a box
     */
    public HyperRectangle getInitialBox()
    {
    	int numDims = ha.variables.size();
    	HyperRectangle rv = new HyperRectangle(numDims);
    	
    	// start in the middle of the initial state set
		TreeMap <String, Interval> ranges = new TreeMap <String, Interval>();
		
		try
		{
			RangeExtractor.getVariableRanges(config.init.values().iterator().next(), ranges);
		} 
		catch (EmptyRangeException e)
		{
			throw new AutomatonExportException("Could not determine ranges for inital values (not rectangluar initial states).", e);
		}
		catch (ConstantMismatchException e)
		{
			throw new AutomatonExportException("Constant mismatch in initial values.", e);
		}
		
		int numVars = ha.variables.size();
		
		for (int i = 0; i < numVars; ++i)
		{
			if (i == ttVarIndex)
				rv.dims[i] = new Interval(0);
			else
			{
				String var = ha.variables.get(i);
				
				Interval dimRange = ranges.get(var);
				
				if (dimRange == null)
					throw new AutomatonExportException("Range for '" + var + "' was not set (not rectangluar initial states).");
				else
					rv.dims[i] = dimRange;
			}
		}
		
		return rv;
    }
    
    /**
     * Gets the start of the simulation, depending on the simType parameter
     * @param initBox the initial box of states (from getInitialBox())
     * @return a set of points
     */
    private ArrayList <HyperPoint> getSimulationStart()
    {
    	HyperRectangle initBox = getInitialBox();
    	HyperPoint center = initBox.center();
		
		final ArrayList <HyperPoint> rv = new ArrayList <HyperPoint>();
		rv.add(center); // all sim types include center
		
		if (simType == SimulationType.STAR || simType == SimulationType.STARCORNERS)
		{
			rv.addAll(initBox.getStarPoints());
		}
		
		if (simType == SimulationType.CORNERS  || simType == SimulationType.STARCORNERS)
		{
			initBox.enumerateCornersUnique(new HyperRectangleCornerEnumerator()
			{
				@Override
				public void enumerate(HyperPoint p)
				{
					rv.add(p);
				}
			});
		}
		
		if (simType == SimulationType.RAND)
		{
			Random rand = new Random(0); // use constant seed for reproducibility
			int numDims = ha.variables.size();
			
			for (int i = 0; i < randCount; ++i)
			{
				HyperPoint hp = new HyperPoint(numDims);
				
				// on the boundary means that one of the variables is forced to be at the minimum or maximum
				int boundaryVar = rand.nextInt(numDims - 1); // subtract one for the time triggered variable
				
				if (boundaryVar >= ha.variables.indexOf(ttVarIndex))
					++boundaryVar;
				
				for (int d = 0; d < numDims; ++d)
				{
					Interval dimInterval = initBox.dims[d];
					
					if (d == boundaryVar)
					{
						if (rand.nextBoolean())
							hp.dims[d] = dimInterval.max;
						else
							hp.dims[d] = dimInterval.min;
					}
					else
						hp.dims[d] = dimInterval.min + rand.nextDouble() * dimInterval.width();
				}
				
				rv.add(hp);
			}
		}
		
		return rv;
    }

    /**
     * Construct the time-triggered modes and transitions
     */
	private void simulateAndConstruct()
	{
		// simulate from initial state
		ArrayList <HyperPoint> simPoints = getSimulationStart();
				
		Hyst.log("Initial simulation points (" + simPoints.size() + "): " + simPoints);

		MySimulator sim = new MySimulator(simPoints);
		
		long simStartMs = System.currentTimeMillis();
		sim.run(timeMax);
		long simEndMs = System.currentTimeMillis();
		
		final PythonBridge pb = new PythonBridge();
		pb.open();

		long optStartMs = System.currentTimeMillis();
		hybridizeFlows(sim.modes, sim.rects, pb);
		long optTime = System.currentTimeMillis() - optStartMs;
		long simTime = simEndMs - simStartMs;
		
		pb.close();
		
		int numOpt = sim.modes.size() * (sim.modes.get(0).flowDynamics.size() - 1);
		
		Hyst.log("Simulation time to construct " + sim.modes.size() + " modes: " + simTime + " milliseconds.");
		Hyst.log("Completed " + numOpt + " optimizations in " + optTime + " milliseconds. " +
				(1000.0 * numOpt / optTime) + " per second.");
	}

	private class MySimulator
	{
		// variables used during construction
    	private ArrayList<HyperPoint> simPoints = null; // set of points being simulated which guide the construction
    	private Expression nextTransitionGuard = null; // the guard to use for the transition to the next mode
    	
    	// settings
    	final private double simTimeMicroStep;
    	final private Map <String, Expression> flowDynamics; 
    	final private List <String> varNames;
    	
    	// constants
    	final String MODE_PREFIX = "_m_";
    	final int SIMULATION_MICROSTEPS_PER_TT = 20;
    	
    	// results
    	public ArrayList <AutomatonMode> modes = new ArrayList <AutomatonMode>(); // constructed modes
    	public ArrayList <HyperRectangle> rects = new ArrayList <HyperRectangle>(); // constructed invariant rectangles
    	
    	public MySimulator(ArrayList<HyperPoint> simPoints)
    	{
    		this.simTimeMicroStep = timeStep / SIMULATION_MICROSTEPS_PER_TT;
    		this.simPoints = simPoints;
    		
    		if (isNondeterministicDynamics(originalMode.flowDynamics))
    			throw new AutomatonExportException("Nondeterministic Dynamics are not implemented in simulation.");
    		
    		this.flowDynamics = Simulator.centerDynamics(originalMode.flowDynamics);
    		this.varNames = originalMode.automaton.variables;
    		
    		HyperRectangle.setDimensionNames(this.varNames);
    	}

    	/**
    	 * Actually run the simulation
    	 * @param timeMax the amount of time to run the simulation for
    	 */
    	public void run(double timeMax)
		{
    		double TOL = 1e-12;
    		double elapsed = 0;
    		
    		// pseudo-invariants
    		double piStepTime = -1;
    		double piNextTime = -1; 
    		
    		if (piCount > 0)
    		{
    			piStepTime = timeMax / piCount;
    			piNextTime = 0;
    		}
    		
    		while (elapsed + TOL < timeMax)
    		{
    			if (piNextTime >= 0 && elapsed + TOL > piNextTime)
    			{
    				// try a pseudo invariant step (no time elapse)
    				piNextTime += piStepTime;
    				
    				HyperPoint piPoint = getPseudoInvariantPoint();
    				
    				if (piPoint != null)
    				{
    					Hyst.log("Doing pseudo-invariant step at sim-time: " + elapsed);
    					stepPseudoInvariant(piPoint);
    					continue;
    				}
    				else
    					Hyst.log("Skipping pseudo-invariant step at sim-time: " + elapsed);
    			}
    			else
    			{
	    			// do a time-triggered step
	    			stepTimeTrigger();
	    			elapsed += timeStep;
    			}
    		}
		}

		/**
    	 * Check if a pseudo-invariant step can be performed. This is the case if the center point's simulation can go to a point
    	 * where the constructed auxiliary hyperplane would be entirely on one side of the startBox. If so, return the final
    	 * simulated center point. If not (if the timeout value of piMaxTime is reached first), return null
    	 * @return the point used to construct the auxiliary hyperplane, if a PI is possible, else null
    	 */
		private HyperPoint getPseudoInvariantPoint()
		{
			// the first point of simPoints is the center point we should simulate
			HyperPoint p = simPoints.get(0).copy();
			HyperRectangle startbox = null;// TODO working here
			
			return null;
		}
		
		/**
		 * Add a space-triggered mode using an auxiliary hyperplane (pseudo-invariant). There is no guarantee of time elapsing in
		 * the constructed mode.
		 * 
		 * @param piPoint the point where the construct the pseudo-invariant
		 */
		private void stepPseudoInvariant(HyperPoint piPoint)
		{
			// TODO Auto-generated method stub
			
		}

		private boolean isNondeterministicDynamics(	LinkedHashMap<String, ExpressionInterval> dy)
		{
			boolean rv = false;
			
			for (ExpressionInterval ei : dy.values())
			{
				if (ei.getInterval() != null)
				{
					rv = true;
					break;
				}
			}
			
			return rv;
		}

		/**
    	 * Advance a simulation point by the simTimeMicroStep
    	 * @param p [inout] the point to advance (in place)
    	 */
    	private void microStep(HyperPoint p)
    	{
    		RungeKutta.singleStepRk(flowDynamics, varNames, p, simTimeMicroStep);
    	}
    	
		/**
		 * create one time-triggered mode
		 */
		public void stepTimeTrigger()
		{
			HyperRectangle simBox = boundingBox(simPoints);
			HyperRectangle startBox = HyperRectangle.bloatAdditive(simBox, epsilon);
			String modeName = MODE_PREFIX + modes.size();
			
			Hyst.logDebug("simulation bounding box upon entering mode " + modeName + " was " + simBox + "; points were: " + simPoints);
			
			// simulate for a number of microsteps until the time trigger is reached
			for (int i = 0; i < SIMULATION_MICROSTEPS_PER_TT; ++i)
			{
				for (HyperPoint sp : simPoints)
					microStep(sp);
			}
			
			// a time-triggered transition should occur here
			HyperRectangle endBox = HyperRectangle.bloatAdditive(boundingBox(simPoints), epsilon);
			HyperRectangle invariantBox = HyperRectangle.union(startBox, endBox);
			
			AutomatonMode am = ha.createMode(modeName);
			
			// store mode and rectangle for optimization
			modes.add(am);
			rects.add(invariantBox);
			
			// set dynamics
			am.flowDynamics = originalMode.flowDynamics;
			am.flowDynamics.put(TT_VARIABLE, new ExpressionInterval(new Constant(-1)));
			
			// set invariant
			am.invariant = Expression.and(originalMode.invariant.copy(), ttGreaterThanZero);
			addRectangleInvariant(am, invariantBox);
			
	        // add transitions to error mode during the computation in this box
			addModeErrorTransitions(am, invariantBox);
			
			// add transition from the previous state
			if (nextTransitionGuard != null)
			{
				// there was a previous state, create the transition
				AutomatonMode prevMode = modes.get(modes.size() - 2);
				AutomatonTransition at = ha.createTransition(prevMode, am);
				
				at.guard = nextTransitionGuard.copy();
				at.reset.put(TT_VARIABLE, new ExpressionInterval(timeStep));
				
				// add error transitions at guard
				addErrorTransitionsAtGuard(prevMode, nextTransitionGuard, startBox);
				
				// possibly add a second transition to an intermediate pre-mode with zero dynamics (for plotting)
				if (addIntermediate)
				{
					AutomatonMode preMode = ha.createMode("_pre" + modeName, new ExpressionInterval("0"));
					
					preMode.invariant = Constant.TRUE;
					preMode.flowDynamics.put(TT_VARIABLE, new ExpressionInterval(new Constant(0)));
					ha.createTransition(prevMode, preMode).guard = nextTransitionGuard.copy();
				}
			}
			else
			{
				// there was no previous state, update the initial set of states to reflect the time trigger
				Expression init = config.init.values().iterator().next();
				config.init.put(originalMode.name, Expression.and(init, new Operation(Operator.EQUAL, TT_VARIABLE, timeStep)));
			}
			
			// add transition condition for next state
			nextTransitionGuard = new Operation(Operator.EQUAL, TT_VARIABLE, 0);
		}
	}
	
	/**
	 * Change the (nonlinear) flow in each mode to a hybridized one with affine dynamics
	 * @param am the mode to change 
	 * @param hr the constraint set, in the order of ha.variablenames
	 */
	private void hybridizeFlows(ArrayList<AutomatonMode> modes,
			ArrayList<HyperRectangle> rects, PythonBridge pb)
	{
		if (modes.size() == 0)
			throw new AutomatonExportException("hybridizeFlows was called 0 modes");
		
		List<OptimizationParams> params = new ArrayList<OptimizationParams>();
		
		for (int i = 0; i < modes.size(); ++i)
		{
			AutomatonMode am = modes.get(i);
			HyperRectangle hr = rects.get(i);
			
			OptimizationParams op = new OptimizationParams();
			op.original = am.flowDynamics;
			
			for (int dim = 0; dim < ha.variables.size(); ++dim)
			{
				String name = ha.variables.get(dim);
				op.bounds.put(name, hr.dims[dim]);
			}
			
			params.add(op);
		}
		
		 AffineOptimize.createAffineDynamics(pb, params);
		
		 for (int i = 0; i < modes.size(); ++i)
		 {
			AutomatonMode am = modes.get(i);
			OptimizationParams op = params.get(i);
			
			am.flowDynamics = op.result;
		 }
	}
	
	/**
	 * Add transitions to the error mode on each side of the box for a given mode
	 * @param am the mode
	 * @param hr the invariant box of the mode
	 */ 
	private void addModeErrorTransitions(AutomatonMode am, HyperRectangle hr)
	{
		for (int d = 0; d < hr.dims.length; ++d)
		{
			if (d == ttVarIndex)
				continue;
			
			Interval i = hr.dims[d];
			
			Variable v = new Variable(ha.variables.get(d));
			Expression le = new Operation(Operator.LESSEQUAL, v, new Constant(i.min));
			Expression ge = new Operation(Operator.GREATEREQUAL, v, new Constant(i.max));
			
			ha.createTransition(am, errorMode).guard = le;
			ha.createTransition(am, errorMode).guard = ge;
		}
	}
	
	/**
	 * Add transitions at the guard (for example time trigger) to the error mode (negation of invariant)
	 * @param am the mode to add to
	 * @param tt the time-trigger time
	 * @param hr the rectangle bounds
	 */
	private void addErrorTransitionsAtGuard(AutomatonMode am, Expression guard, HyperRectangle nextModeInvariant)
	{
		for (int d = 0; d < nextModeInvariant.dims.length; ++d)
		{
			if (d == ttVarIndex)
				continue;
			
			Interval i = nextModeInvariant.dims[d];
			
			Variable v = new Variable(ha.variables.get(d));
			Expression le = new Operation(Operator.LESSEQUAL, v, new Constant(i.min));
			Expression ge = new Operation(Operator.GREATEREQUAL, v, new Constant(i.max));
			
			ha.createTransition(am, errorMode).guard = Expression.and(guard.copy(), le);
			ha.createTransition(am, errorMode).guard = Expression.and(guard.copy(), ge);
		}
	}

	/**
	 * Set the invariant for a newly-constructed mode
	 * @param am the mode
	 * @param hr the rectangle invariant
	 */
	private void addRectangleInvariant(AutomatonMode am, HyperRectangle hr)
	{
		for (int d = 0; d < hr.dims.length; ++d)
		{
			if (d == ttVarIndex)
				continue;
			
			Interval i = hr.dims[d];
			Variable v = new Variable(ha.variables.get(d));
			Expression ge = new Operation(Operator.GREATEREQUAL, v, new Constant(i.min));
			Expression le = new Operation(Operator.LESSEQUAL, v, new Constant(i.max));
			Expression constraint = Expression.and(ge, le);
			
			am.invariant = Expression.and(am.invariant, constraint);
		}
	}

	/**
	 * Get the bounding box of two sets of points
	 * @param setA one of the sets
	 * @param setB the other set
	 * @return a hyperrectngle which tightly includes all the points
	 */
	@SafeVarargs
	private static HyperRectangle boundingBox(ArrayList<HyperPoint> ... sets)
	{
		HyperPoint firstPoint = sets[0].get(0);
		int numDims = firstPoint.dims.length;
		HyperRectangle rv = firstPoint.toHyperRectangle();
		
		for (int d = 0; d < numDims; ++d)
		{
			for (int list = 0; list < sets.length; ++list)
			{
				for (HyperPoint hp : sets[list])
					rv.dims[d].expand(hp.dims[d]);
			}
		}
		
		return rv;
	}
	
	/**
	 * makes a time-triggered variable
	 */
	private void makeTimeTriggerVariable()
	{
		// this assumes a single-mode automaton with a single initial state expression
		ha.variables.add(TT_VARIABLE);
		ttVarIndex = ha.variables.size() - 1;
		
		originalMode.flowDynamics.put(TT_VARIABLE, new ExpressionInterval("0"));
	}

	private void makeErrorMode()
	{
		String name = originalMode.name + "_error"; 
		errorMode = ha.createMode(name);
		errorMode.invariant = Constant.TRUE;
		
		for (String v : ha.variables)
			errorMode.flowDynamics.put(v, new ExpressionInterval("0"));
	}
	
	private void assignInitialStates()
	{
		Expression init = config.init.values().iterator().next();
		TreeMap<String, Interval> initRanges = RangeExtractor.getVariableRanges(init, "initial states");
		config.init.clear();
		
		final String FIRST_MODE_NAME = "_m_0";
		AutomatonMode am = ha.modes.get(FIRST_MODE_NAME);
		TreeMap<String, Interval> modeRanges = RangeExtractor.getVariableRanges(am.invariant, "mode _m_0 invariant");
		
		for (int i = 0; i < ha.variables.size(); ++i)
		{
			String var = ha.variables.get(i);
			
			if (i == ttVarIndex)
				continue;
			
			// make sure mode range is contained entirely in initRanges
			Interval modeInterval = modeRanges.get(var);
			Interval initInterval = initRanges.get(var);
			
			if (initInterval.min < modeInterval.min || initInterval.max > modeInterval.max)
			{
				throw new AutomatonExportException("Hybridized first mode's invariant does not entirely contain the " +
						"initial set of states. " + var + " is " + initInterval + " in initial states, and " + modeInterval 
						+ " in the first mode. Consider increasing the bloating term (epsilon).");
			}
		}
		
		config.init.put(FIRST_MODE_NAME, init);
	}
	
	private void assignForbiddenStates()
	{
		if (config.forbidden.size() > 0)
		{
			Expression e = config.forbidden.values().iterator().next();
			config.forbidden = HybridizeGridPass.expressionInvariantsIntersection(ha, e, "forbidden states");
		}
		
		if (addForbidden)
		{
			// add error mode to the forbidden states
			String name = originalMode.name + "_error";
			config.forbidden.put(name, Constant.TRUE);
		}
	}

	@Override
    protected void runPass(String params)
    {
        Expression.expressionPrinter = DefaultExpressionPrinter.instance;

        this.ha = (BaseComponent)config.root;
        this.originalMode = ha.modes.values().iterator().next();
        parseParams(params);
        makeTimeTriggerVariable(); // sets timeVariable
        makeErrorMode(); // sets errorMode
        ha.validate();

        ha.modes.remove(originalMode.name);
        simulateAndConstruct();
        assignInitialStates();
        assignForbiddenStates();

        config.settings.spaceExConfig.timeTriggered = true;
    }
}