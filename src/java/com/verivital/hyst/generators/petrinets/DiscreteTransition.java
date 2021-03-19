package com.verivital.hyst.generators.petrinets;

public class DiscreteTransition {
	public DiscreteTransition(int id, double delay, DiscretePlace source) {
		super();
		this.id = id;
		this.delay = delay;
		this.source = source;
	}
	public boolean initiallyEnabled() {
		return source.initialTokens > 0;
	}
	public int id;
	public double delay;
	public DiscretePlace source;
}
