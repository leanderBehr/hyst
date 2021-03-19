package com.verivital.hyst.generators.petrinets;

public class DiscretePlace {
	public DiscretePlace(int id, DiscreteTransition in, DiscreteTransition out, int initialTokens) {
		super();
		this.id = id;
		this.in = in;
		this.out = out;
		this.initialTokens = initialTokens;
	}
	
	public boolean initiallyEnabled() {
		return initialTokens > 0;
	}
	
	public int id;
	public DiscreteTransition in;
	public DiscreteTransition out;
	public int initialTokens;
}
