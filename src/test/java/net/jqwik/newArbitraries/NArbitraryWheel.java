package net.jqwik.newArbitraries;

import java.util.*;
import java.util.concurrent.atomic.*;

public class NArbitraryWheel<T> implements NArbitrary<T> {
	private final T[] values;

	public NArbitraryWheel(T... values) {
		this.values = values;
	}

	@Override
	public NShrinkableGenerator<T> generator(int tries) {
		AtomicInteger index = new AtomicInteger(0);
		return new NShrinkableGenerator<T>() {
			@Override
			public NShrinkable<T> next(Random random) {
				if (index.get() < values.length) {
					T value = values[index.getAndIncrement()];
					NShrinker<T> shrinker = new WheelShrinker(value);
					NShrinkableValue<T> shrinkable = new NShrinkableValue<>(value, Integer.MAX_VALUE, shrinker);
					return shrinkable;
				} else {
					index.set(0);
					return next(random);
				}
			}
		};
	}

	private class WheelShrinker implements NShrinker<T> {
		private final T valueToShrink;

		private WheelShrinker(T valueToShrink) {
			this.valueToShrink = valueToShrink;
		}

		@Override
		public Set<NShrinkable<T>> shrink() {
			int index = Arrays.asList(values).indexOf(valueToShrink);
			if (index <= 0)
				return Collections.emptySet();
			T shrunkValue = values[index - 1];
			NShrinker<T> shrinker = new WheelShrinker(shrunkValue);
			NShrinkableValue<T> shrinkableValue = new NShrinkableValue<>(shrunkValue, index, shrinker);
			return Collections.singleton(shrinkableValue);
		}
	}
}