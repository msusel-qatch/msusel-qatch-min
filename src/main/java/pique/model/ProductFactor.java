package pique.model;

import pique.evaluation.DefaultFactorEvaluator;
import pique.evaluation.IEvaluator;
import pique.evaluation.INormalizer;
import pique.evaluation.IUtilityFunction;

import java.util.HashMap;
import java.util.Map;

public class ProductFactor extends ModelNode {

	// Constructors

	public ProductFactor(String name, String description) {
		super(name, description, new DefaultFactorEvaluator(), null);
	}

	public ProductFactor(String name, String description, IEvaluator evaluator, INormalizer normalizer,
						 IUtilityFunction utilityFunction, Map<String, Double> weights, Double[] thresholds) {
		super(name, description, evaluator, normalizer, utilityFunction, weights, thresholds);
	}

	public ProductFactor(String name, String description, IEvaluator evaluator) {
		super(name, description, new DefaultFactorEvaluator(), null);
		this.evaluator = evaluator;
	}
	
	public ProductFactor(String name, String description, ModelNode measure){
		super(name, description, new DefaultFactorEvaluator(), null);
		this.children.put(measure.getName(), measure);
	}

	// Used for cloning
	public ProductFactor(double value, String name, String description, IEvaluator evaluator, INormalizer normalizer,
						 IUtilityFunction utilityFunction, Map<String, Double> weights, Double[] thresholds, Map<String,
			ModelNode> children) {
		super(value, name, description, evaluator, normalizer, utilityFunction, weights, thresholds, children);
	}



	//region Methods

	@Override
	public ModelNode clone() {

		Map<String, ModelNode> clonedChildren = new HashMap<>();
		getChildren().forEach((k, v) -> clonedChildren.put(k, v.clone()));

		return new ProductFactor(getValue(), getName(), getDescription(), getEvaluator(), getNormalizer(),
				getUtilityFunction(), getWeights(), getThresholds(), clonedChildren);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ProductFactor)) { return false; }
		ProductFactor otherProductFactor = (ProductFactor) other;

		return getName().equals(otherProductFactor.getName())
				&& getAnyChild().equals(otherProductFactor.getAnyChild());
	}

	//endregion

}
