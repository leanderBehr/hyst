package com.verivital.hyst.generators;

import java.util.ArrayList;
import org.kohsuke.args4j.Option;

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

public class FlashingGenerator extends ModelGenerator {

	@Option(name = "-count", required = true, usage = "number of robots", metaVar = "COUNT")
	private int count = 2;

	@Override
	public String getCommandLineFlag() {
		return "flashing";
	}

	@Override
	public String getName() {
		return "Flashing robots emergence benchmark";
	}

	@Override
	protected Configuration generateModel() {
		NetworkComponent sys = new NetworkComponent();

		ArrayList<String> labels = new ArrayList<String>();
		for (int i = 0; i < count; i += 1) {
			labels.add("flash_" + i);
		}

		labels.add("return");

		sys.labels.addAll(labels);

		BaseComponent c = clock(50);
		ComponentInstance cInst = new ComponentInstance(sys, c);
		c.parent = sys;
		sys.children.put(c.instanceName, cInst);
		
		for (int robotInd = 0; robotInd < count; robotInd += 1) {;
			BaseComponent robot = genRobotInstance(robotInd, count );
			ComponentInstance instance = new ComponentInstance(sys, robot);

			for (String label : labels) {
				instance.labelMapping.add(new ComponentMapping(label, label));
			}

			robot.parent = sys;
			sys.children.put(robot.instanceName, instance);
		}

		Configuration config = new Configuration(sys);
		config.settings.spaceExConfig.timeHorizon = 20;
		config.settings.spaceExConfig.maxIterations = 21;
		config.settings.plotVariableNames[0] = "clock.t";
		config.settings.plotVariableNames[1] = "robot_0.x";
		
		makeInit(config, count);

		return config;
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
	
	private void makeInit(Configuration config, int count) {
		NetworkComponent net = (NetworkComponent)config.root;
		
		String initialNode = net.children.values().stream().reduce("", (prev,b) -> 
		 (prev == "" ? prev : prev + ".") + ((BaseComponent)b.child).initialMode.name, (a,b) -> a+b);
		
		ArrayList<Expression> exprs = new ArrayList<Expression>();
		
		exprs.add(new Operation("clock.t", Operator.EQUAL, 0)); // clock initial value
		for(int index = 0; index < count; index += 1) {
			exprs.add(new Operation(getNameOf(index)+".x", Operator.EQUAL, getInitValOf(index, count)) );
		}
		
		config.init.put(initialNode, makeConjunction(exprs));
	}

	private double getInitValOf(int index, int count) {
		return ((double)index) / ((double)count);
	}
	
	private String getNameOf(int index) {
		return "robot_" + index;
	}
	
	private BaseComponent genRobotInstance(int index, int count) {

		BaseComponent robot = new BaseComponent();

		robot.instanceName = getNameOf(index);
		robot.variables.add("x");

		for (int i = 0; i < count; i += 1) {
			robot.labels.add("flash_" + i);
		}

		robot.labels.add("return");

		AutomatonMode wait = robot.createMode("wait", "x <= 1", "x = 1");
		AutomatonMode adapt = robot.createMode("adapt", "true", "x = 0");

		for (int i = 0; i < count; i += 1) {
			if (i == index)
				continue;

			AutomatonTransition flashOthers = robot.createTransition(wait, adapt);
			flashOthers.guard = Constant.TRUE;
			flashOthers.reset.put("x", new ExpressionInterval("1.1 * x"));

			flashOthers.labels.add("flash_" + i);
		}

		AutomatonTransition backJumpReset = robot.createTransition(adapt, wait);
		backJumpReset.guard = new Operation("x", Operator.GREATEREQUAL, 1);
		backJumpReset.reset.put("x", new ExpressionInterval(0));
		backJumpReset.labels.add("return");
		backJumpReset.urgent = true;

		AutomatonTransition backJumpNoReset = robot.createTransition(adapt, wait);
		backJumpNoReset.guard = new Operation("x", Operator.LESSEQUAL, 1);
		backJumpNoReset.labels.add("return");
		backJumpNoReset.urgent = true;

		AutomatonTransition flashSelf = robot.createTransition(wait, adapt);
		flashSelf.guard = new Operation("x", Operator.GREATEREQUAL, 1);
		flashSelf.reset.put("x", new ExpressionInterval(0));
		flashSelf.labels.add("flash_" + index);
		
		robot.initialMode = wait;
		robot.initialValues.add(new Operation("x", Operator.EQUAL, getInitValOf(index, count) ));

		return robot;
	}

	private BaseComponent clock(double max) {
		BaseComponent component = new BaseComponent();
		component.instanceName = "clock";
		component.variables.add("t");
		
		component.initialMode = component.createMode("l", "t <= " + max, "t = 1");
		
		component.initialValues.add(new Operation("t", Operator.EQUAL, 0));
		 
		return component;
	}
	
}
