package com.verivital.hyst.passes.basic;

import java.util.ArrayList;
import java.util.Optional;

import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.util.Preconditions;

public class CollapseIdenticalJumpsPass extends TransformationPass {

	public CollapseIdenticalJumpsPass() {
		// skip all preconditions
		preconditions = new Preconditions(true);
	}

	@Override
	public String getCommandLineFlag() {
		return "collapse_identical_jumps";
	}

	@Override
	public String getName() {
		return "Collapse identical jumps into one with potentially multiple labels";
	}

	@Override
	protected void runPass() {
		if(config.root instanceof BaseComponent) {
			processBaseComponent((BaseComponent)config.root);
		} else {
			processNetworkComponent((NetworkComponent)config.root);
		}
	}

	private void processNetworkComponent(NetworkComponent component) {
		for (ComponentInstance child : component.children.values()) {
			if (child.child instanceof BaseComponent) {
				processBaseComponent((BaseComponent)child.child);
			} else {
				processNetworkComponent((NetworkComponent)child.child);
			}
		}
	}

	private void processBaseComponent(BaseComponent component) {
		ArrayList<AutomatonTransition> newTransitions = new ArrayList<AutomatonTransition>();

		for (AutomatonTransition transition : component.transitions) {
			Optional<AutomatonTransition> existing = newTransitions.stream()
					.filter(elem -> jumpsEquiv(elem, transition)).findFirst();
			if (existing.isPresent()) {
				existing.get().labels.addAll(transition.labels);
			} else {
				newTransitions.add(transition);
			}
		}

		component.transitions = newTransitions;
	}

	private boolean jumpsEquiv(AutomatonTransition left, AutomatonTransition right) {
		return left.parent == right.parent && left.from == right.from && left.to == right.to
				&& left.urgent == right.urgent && left.guard.equals(right.guard) && left.reset.equals(right.reset);
	}

}
