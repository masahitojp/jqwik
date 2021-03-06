package net.jqwik.engine.properties.shrinking;

import java.util.*;
import java.util.function.*;

import org.junit.platform.engine.reporting.*;
import org.mockito.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.engine.*;
import net.jqwik.engine.properties.*;
import net.jqwik.engine.properties.shrinking.ShrinkableTypesForTest.*;

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PropertyShrinkerTests {

	@SuppressWarnings("unchecked")
	private final Consumer<ReportEntry> reporter = Mockito.mock(Consumer.class);
	@SuppressWarnings("unchecked")
	private final Consumer<List<Object>> falsifiedSampleReporter = Mockito.mock(Consumer.class);

	@Example
	void ifThereIsNothingToShrinkReturnOriginalValue() {
		List<Shrinkable<Object>> unshrinkableParameters = asList(Shrinkable.unshrinkable(1), Shrinkable.unshrinkable("hello"));
		PropertyShrinker shrinker = new PropertyShrinker(unshrinkableParameters, ShrinkingMode.FULL, reporter, falsifiedSampleReporter);

		Throwable originalError = new RuntimeException("original error");
		PropertyShrinkingResult result = shrinker.shrink(ignore -> TryExecutionResult.falsified(null), originalError);

		assertThat(result.sample()).isEqualTo(asList(1, "hello"));
		assertThat(result.steps()).isEqualTo(0);
		assertThat(result.throwable()).isPresent();
		assertThat(result.throwable().get()).isSameAs(originalError);

		verifyNoInteractions(falsifiedSampleReporter);
	}

	@Example
	void ifShrinkingIsOffReturnOriginalValue() {
		List<Shrinkable<Object>> parameters = toList(5, 10);

		PropertyShrinker shrinker = new PropertyShrinker(parameters, ShrinkingMode.OFF, reporter, falsifiedSampleReporter);

		Throwable originalError = new RuntimeException("original error");
		PropertyShrinkingResult result = shrinker.shrink(ignore -> TryExecutionResult.falsified(null), originalError);

		assertThat(result.sample()).isEqualTo(asList(5, 10));
		assertThat(result.steps()).isEqualTo(0);
		assertThat(result.throwable()).isPresent();
		assertThat(result.throwable().get()).isSameAs(originalError);

		verifyNoInteractions(falsifiedSampleReporter);
	}

	@Example
	void shrinkAllParameters() {
		List<Shrinkable<Object>> parameters = toList(5, 10);

		PropertyShrinker shrinker = new PropertyShrinker(parameters, ShrinkingMode.FULL, reporter, falsifiedSampleReporter);

		TestingFalsifier<List<Object>> listFalsifier = params -> {
			if (((int) params.get(0)) == 0) return true;
			return ((int) params.get(1)) <= 1;
		};
		PropertyShrinkingResult result = shrinker.shrink(listFalsifier, null);

		assertThat(result.sample()).isEqualTo(asList(1, 2));
		assertThat(result.throwable()).isNotPresent();

		assertThat(result.steps()).isEqualTo(12);
	}

	@Property(tries = 100, edgeCases = EdgeCasesMode.NONE)
	@ExpectFailure(checkResult = ShrinkToEmptyList0.class)
	boolean shrinkDependentParameters(
		@ForAll @Size(min = 0, max = 10) List<Integer> list,
		@ForAll @IntRange(min = 0, max = 100) int size
	) {
		return list.size() < size;
	}

	private class ShrinkToEmptyList0 extends ShrinkToChecker {
		@Override
		public Iterable<?> shrunkValues() {
			return Arrays.asList(Collections.emptyList(), 0);
		}
	}

	@Example
	void reportFalsifiedParameters() {
		List<Shrinkable<Object>> parameters = toList(5, 10);

		PropertyShrinker shrinker = new PropertyShrinker(parameters, ShrinkingMode.FULL, reporter, falsifiedSampleReporter);
		shrinker.shrink(ignore -> TryExecutionResult.falsified(null), null);

		verify(falsifiedSampleReporter, times(15)).accept(any(List.class));
	}

	@Example
	void resultThrowableComesFromActualShrunkValue() {
		List<Shrinkable<Object>> parameters = toList(5, 10);

		PropertyShrinker shrinker = new PropertyShrinker(parameters, ShrinkingMode.FULL, reporter, falsifiedSampleReporter);

		TestingFalsifier<List<Object>> listFalsifier = params -> {
			if (((int) params.get(0)) == 0) return true;
			if (((int) params.get(1)) <= 1) return true;
			throw new RuntimeException(String.format("%s:%s", params.get(0), params.get(1)));
		};
		PropertyShrinkingResult result = shrinker.shrink(listFalsifier, null);

		assertThat(result.sample()).isEqualTo(asList(1, 2));
		assertThat(result.throwable()).isPresent();
	}

	@Example
	void resultSampleConsistsOfActualUsedObjects_notOfValuesGeneratedByShrinkable() {
		List<Shrinkable<Object>> parameters = toList(5, 10);

		PropertyShrinker shrinker = new PropertyShrinker(parameters, ShrinkingMode.FULL, reporter, falsifiedSampleReporter);

		Falsifier<List<Object>> listFalsifier = params -> {
			params.add(42);
			if (((int) params.get(0)) == 0) return TryExecutionResult.satisfied();
			if (((int) params.get(1)) <= 1) return TryExecutionResult.satisfied();
			return TryExecutionResult.falsified(null);
		};
		PropertyShrinkingResult result = shrinker.shrink(listFalsifier, null);

		assertThat(result.sample()).isEqualTo(asList(1, 2, 42));
	}

	@Example
	void withBoundedShrinkingBreakOffAfter1000Steps() {
		List<Shrinkable<Object>> parameters = toList(900, 1000);

		PropertyShrinker shrinker = new PropertyShrinker(parameters, ShrinkingMode.BOUNDED, reporter, falsifiedSampleReporter);

		PropertyShrinkingResult result = shrinker.shrink(ignore -> TryExecutionResult.falsified(null), null);

		assertThat(result.sample()).isEqualTo(asList(0, 900));

		ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass(ReportEntry.class);
		verify(reporter, times(1)).accept(entryCaptor.capture());

		assertThat(entryCaptor.getValue().getKeyValuePairs()).containsKeys("shrinking bound reached");
	}

	private List<Shrinkable<Object>> toList(int i, int i2) {
		return asList(
			new OneStepShrinkable(i).asGeneric(),
			new OneStepShrinkable(i2).asGeneric()
		);
	}

	@Group
	class Duplicates {

		@Property(tries = 10000)
		@ExpectFailure(checkResult = ShrinkTo77.class)
		boolean shrinkDuplicateIntegersTogether(
			@ForAll @IntRange(min = 1, max = 100) int int1,
			@ForAll @IntRange(min = 1, max = 100) int int2
		) {
			return int1 < 7 || int1 != int2;
		}

		private class ShrinkTo77 extends ShrinkToChecker {
			@Override
			public Iterable<?> shrunkValues() {
				return Arrays.asList(7, 7);
			}
		}

		@Property(tries = 10000)
		@ExpectFailure(checkResult = ShrunkToAA.class)
		void shrinkingDuplicateStringsTogether(@ForAll("aString") String first, @ForAll("aString") String second) {
			assertThat(first).isNotEqualTo(second);
		}

		private class ShrunkToAA extends ShrinkToChecker {
			@Override
			public Iterable<?> shrunkValues() {
				return Arrays.asList("aa", "aa");
			}
		}

		@Provide
		Arbitrary<String> aString() {
			return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(2).ofMaxLength(5);
		}
	}


}
