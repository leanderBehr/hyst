package com.verivital.hyst.generators;

import java.util.ArrayList;
import java.util.Arrays;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.generators.petrinets.ContinuousPlace;
import com.verivital.hyst.generators.petrinets.ContinuousTransition;
import com.verivital.hyst.generators.petrinets.DiscretePlace;
import com.verivital.hyst.generators.petrinets.DiscreteTransition;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.ComponentMapping;
import com.verivital.hyst.ir.network.NetworkComponent;

public class PetriNetGenerator extends ModelGenerator {

	@Option(name = "-timeHorizon", required = true, usage = "time horizon to set", metaVar = "COUNT")
	private int timeHorizon= 2;
	
	@Override
	public String getCommandLineFlag() {
		return "petri_net";
	}

	@Override
	public String getName() {
		return "Petri Net Generator";
	}

	@Override
	protected Configuration generateModel() {
		ContinuousTransition t1 = new ContinuousTransition(1, 2, null);
		ContinuousTransition t2 = new ContinuousTransition(2, 1, null);

		DiscreteTransition t3 = new DiscreteTransition(3, 1, null);
		DiscreteTransition t4 = new DiscreteTransition(4, 2, null);
		DiscreteTransition t5 = new DiscreteTransition(5, 1, null);
		DiscreteTransition t6 = new DiscreteTransition(6, 1, null);

		ContinuousPlace p1 = new ContinuousPlace(1, t1, t2, 2);

		DiscretePlace p2 = new DiscretePlace(2, t4, t3, 1);
		DiscretePlace p3 = new DiscretePlace(3, t3, t4, 0);
		DiscretePlace p4 = new DiscretePlace(4, t6, t5, 1);
		DiscretePlace p5 = new DiscretePlace(5, t5, t6, 0);

		t1.enabler = p2;
		t2.enabler = p4;

		t3.source = p2;
		t4.source = p3;
		t5.source = p4;
		t6.source = p5;

		ArrayList<DiscretePlace> discretePlaces = new ArrayList<>(Arrays.asList(p2, p3, p4, p5));
		ArrayList<DiscreteTransition> discreteTransitions = new ArrayList<>(Arrays.asList(t3, t4, t5, t6));
		ArrayList<ContinuousPlace> continuousPlaces = new ArrayList<>(Arrays.asList(p1));

		NetworkComponent net = new NetworkComponent();

		for (DiscretePlace discretePlace : discretePlaces) {
			setupChild(net, processDiscretePlace(discretePlace));
		}

		for (DiscreteTransition discreteTransition : discreteTransitions) {
			setupChild(net, processDiscreteTransition(discreteTransition));
		}

		for (ContinuousPlace continuousPlace : continuousPlaces) {
			setupChild(net, processContinuousPlace(continuousPlace));
		}

		setupChild(net, clock(timeHorizon));

		Configuration config = new Configuration(net);
		config.settings.spaceExConfig.timeHorizon = 10000;
		config.settings.spaceExConfig.maxIterations = 10001;
		config.settings.plotVariableNames[0] = "clock.t";
		config.settings.plotVariableNames[1] = "P_1.p";

		//make init
		ArrayList<Expression> exprs = new ArrayList<Expression>();
		exprs.addAll(initExprDiscretePlaces(discretePlaces));
		exprs.addAll(initExprDiscreteTransitions(discreteTransitions));
		exprs.addAll(initExprContinuousPlaces(continuousPlaces));
		exprs.add(new Operation("clock.t", Operator.EQUAL, 0));
		
		
		config.init.put(initalLocation(config), makeConjunction(exprs));
		
		return config;
	}

	private BaseComponent clock(double max) {
		BaseComponent component = new BaseComponent();
		component.instanceName = "clock";
		component.variables.add("t");

		component.initialMode = component.createMode("l", "t <= " + max, "t = 1");

		component.initialValues.add(new Operation("t", Operator.EQUAL, 0));

		return component;
	}

	private void setupChild(NetworkComponent net, BaseComponent base) {

		transformToSingleLabel(base); // for flattening

		base.parent = net;
		ComponentInstance instance = new ComponentInstance(net, base);

		for (String label : base.labels) {
			instance.labelMapping.add(new ComponentMapping(label, label));
			if(net.labels.contains(label)) continue;
			net.labels.add(label);
		}

		net.children.put(base.instanceName, instance);
	}

	private BaseComponent processDiscreteTransition(DiscreteTransition transition) {
		BaseComponent component = new BaseComponent();
		component.instanceName = "T_" + transition.id;
		component.variables.add("t");

		AutomatonMode on = component.createMode("on", "t <= " + transition.delay, "t = 1");
		AutomatonMode off = component.createMode("off", "true", "t = 0");

		AutomatonTransition toOn = component.createTransition(off, on);
		toOn.guard = Constant.TRUE;
		// no reset
		toOn.labels.add("t_" + transition.source.in.id + "_stay_on");
		toOn.labels.add("t_" + transition.source.in.id + "_off_on");

		AutomatonTransition fire = component.createTransition(on, on);
		fire.guard = new Operation("t", Operator.EQUAL, transition.delay);
		fire.reset.put("t", new ExpressionInterval(0));
		fire.labels.add("t_" + transition.id + "_stay_stay");
		fire.labels.add("t_" + transition.id + "_stay_on");

		AutomatonTransition fireToOff = component.createTransition(on, off);
		fireToOff.guard = new Operation("t", Operator.EQUAL, transition.delay);
		fireToOff.reset.put("t", new ExpressionInterval(0));
		fireToOff.labels.add("t_" + transition.id + "_off_stay");
		fireToOff.labels.add("t_" + transition.id + "_off_on");

		component.collectLabels();

		component.initialMode = transition.initiallyEnabled() ? on : off;
		component.initialValues.add(new Operation("t", Operator.EQUAL, 0)); // start timer at delay or zero?

		return component;
	}

	private BaseComponent processContinuousPlace(ContinuousPlace place) {
		BaseComponent tank = new BaseComponent();
		tank.instanceName = "P_" + place.id;
		tank.variables.add("p");
		tank.variables.add("flowRate");

		AutomatonMode loc = tank.createMode("l", "p <= 10 & p >= 0", "p = flowRate");

		AutomatonTransition onIn = tank.createTransition(loc, loc);
		onIn.guard = Constant.TRUE;
		onIn.reset.put("flowRate", new ExpressionInterval("flowRate + " + place.input.flow));
		onIn.labels.add("t_" + place.input.enabler.in.id + "_stay_on");
		onIn.labels.add("t_" + place.input.enabler.in.id + "_off_on");

		AutomatonTransition offIn = tank.createTransition(loc, loc);
		offIn.guard = Constant.TRUE;
		offIn.reset.put("flowRate", new ExpressionInterval("flowRate - " + place.input.flow));
		offIn.labels.add("t_" + place.input.enabler.out.id + "_off_stay");
		offIn.labels.add("t_" + place.input.enabler.out.id + "_off_on");

		AutomatonTransition onOut = tank.createTransition(loc, loc);
		onOut.guard = Constant.TRUE;
		onOut.reset.put("flowRate", new ExpressionInterval("flowRate - " + place.output.flow));
		onOut.labels.add("t_" + place.output.enabler.in.id + "_stay_on");
		onOut.labels.add("t_" + place.output.enabler.in.id + "_off_on");

		AutomatonTransition offOut = tank.createTransition(loc, loc);
		offOut.guard = Constant.TRUE;
		offOut.reset.put("flowRate", new ExpressionInterval("flowRate + " + place.output.flow));
		offOut.labels.add("t_" + place.output.enabler.out.id + "_off_stay");
		offOut.labels.add("t_" + place.output.enabler.out.id + "_off_on");

		int initialFlowRate = place.input.initiallyEnabled() ? place.input.flow : 0;
		initialFlowRate -= place.output.initiallyEnabled() ? place.output.flow : 0;

		tank.initialMode = loc;
		tank.initialValues.add(new Operation("p", Operator.EQUAL, place.initialTokens));
		tank.initialValues.add(new Operation("flowRate", Operator.EQUAL, initialFlowRate));

		tank.collectLabels();

		return tank;
	}

	private BaseComponent processDiscretePlace(DiscretePlace place) {
		BaseComponent component = new BaseComponent();
		component.instanceName = "P_" + place.id;
		component.variables.add("m");

		AutomatonMode on = component.createMode("on", "true", "m = 0");
		AutomatonMode off = component.createMode("off", "true", "m = 0");

		AutomatonTransition onLoopInc = component.createTransition(on, on);
		onLoopInc.guard = Constant.TRUE;
		onLoopInc.reset.put("m", new ExpressionInterval("m + 1"));
		onLoopInc.labels.add("t_" + place.in.id + "_stay_stay");
		onLoopInc.labels.add("t_" + place.in.id + "_off_stay");

		AutomatonTransition onLoopDec = component.createTransition(on, on);
		onLoopDec.guard = new Operation("m", Operator.GREATEREQUAL, 2);
		onLoopDec.reset.put("m", new ExpressionInterval("m - 1"));
		onLoopDec.labels.add("t_" + place.out.id + "_stay_stay");
		onLoopDec.labels.add("t_" + place.out.id + "_stay_on");

		AutomatonTransition toOff = component.createTransition(on, off);
		toOff.guard = new Operation("m", Operator.EQUAL, 1);
		toOff.reset.put("m", new ExpressionInterval(0));
		toOff.labels.add("t_" + place.out.id + "_off_stay");
		toOff.labels.add("t_" + place.out.id + "_off_on");

		AutomatonTransition toOn = component.createTransition(off, on);
		toOn.guard = Constant.TRUE;
		toOn.reset.put("m", new ExpressionInterval(1));
		toOn.labels.add("t_" + place.in.id + "_stay_on");
		toOn.labels.add("t_" + place.in.id + "_off_on");

		component.initialMode = place.initiallyEnabled() ? on : off;
		component.initialValues.add(new Operation("m", Operator.EQUAL, place.initialTokens));

		component.collectLabels();

		return component;
	}

	private void transformToSingleLabel(BaseComponent component) {
		ArrayList<AutomatonTransition> transitions = component.transitions;
		component.transitions = new ArrayList<AutomatonTransition>();

		for (AutomatonTransition t : transitions) {
			for (String l : t.labels) {
				AutomatonTransition copied = t.copy(component);
				copied.labels.clear();
				copied.labels.add(l);
			}
		}

	}

	ArrayList<Expression> initExprDiscreteTransitions(ArrayList<DiscreteTransition> ts) {
		ArrayList<Expression> exprs = new ArrayList<Expression>();

		for (DiscreteTransition t : ts) {
			exprs.add(new Operation("T_" + t.id + ".t", Operator.EQUAL, 0));
		}

		return exprs;
	}

	ArrayList<Expression> initExprContinuousPlaces(ArrayList<ContinuousPlace> ps) {
		ArrayList<Expression> exprs = new ArrayList<Expression>();

		for (ContinuousPlace p : ps) {
			exprs.add(new Operation("P_" + p.id + ".flowRate", Operator.EQUAL, p.initialFlowRate()));
			exprs.add(new Operation("P_" + p.id + ".p", Operator.EQUAL, p.initialTokens));
		}

		return exprs;
	}

	ArrayList<Expression> initExprDiscretePlaces(ArrayList<DiscretePlace> ps) {
		ArrayList<Expression> exprs = new ArrayList<Expression>();

		for (DiscretePlace p : ps) {
			exprs.add(new Operation("P_" + p.id + ".m", Operator.EQUAL, p.initialTokens));
		}

		return exprs;
	}

	private String initalLocation(Configuration config) {
		NetworkComponent net = (NetworkComponent) config.root;
		return net.children.values().stream().reduce("",
				(prev, b) -> (prev == "" ? prev : prev + ".") + ((BaseComponent) b.child).initialMode.name,
				(a, b) -> a + b);
	}

	Expression makeConjunction(ArrayList<Expression> exps) {
		if (exps.isEmpty())
			return null;
		if (exps.size() == 1)
			return exps.get(0);

		Expression curr = exps.get(0);

		for (int i = 0; i < exps.size(); i += 1) {
			curr = new Operation(curr, Operator.AND, exps.get(i));
		}

		return curr;
	}
}
