package com.verivital.hyst.generators.petrinets;


public class ContinuousPlace {
	public ContinuousPlace(int id, ContinuousTransition input, ContinuousTransition output, double initialTokens) {
		super();
		this.id = id;
		this.input = input;
		this.output = output;
		this.initialTokens = initialTokens;
	}
	
	public int initialFlowRate() {
		return (input.initiallyEnabled() ? input.flow : 0) - (output.initiallyEnabled() ? output.flow : 0);
	}
	
	public int id;
	public ContinuousTransition input;
	public ContinuousTransition output;
	public double initialTokens;
}
