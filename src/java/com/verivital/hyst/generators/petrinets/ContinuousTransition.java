package com.verivital.hyst.generators.petrinets;

public class ContinuousTransition {

	public ContinuousTransition(int id, int flow, DiscretePlace enabler) {
		super();
		this.id = id;
		this.flow = flow;
		this.enabler = enabler;
	}
	
	public boolean initiallyEnabled() {
		return enabler.initialTokens > 0;
	}
	
	public int id;
	public int flow;
	public DiscretePlace enabler;
}
