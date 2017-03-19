package net.jqwik.discovery;

import net.jqwik.descriptor.SkipExecutionDecorator;
import net.jqwik.descriptor.ContainerClassDescriptor;
import net.jqwik.discovery.specs.TestableMethodDiscoverySpec;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

abstract class AbstractMethodResolver implements ElementResolver {

	private final TestableMethodDiscoverySpec methodSpec;

	AbstractMethodResolver(TestableMethodDiscoverySpec methodSpec) {
		this.methodSpec = methodSpec;
	}

	@Override
	public Set<TestDescriptor> resolveElement(AnnotatedElement element, TestDescriptor parent) {
		if (!(element instanceof Method))
			return Collections.emptySet();

		if (!(parent instanceof ContainerClassDescriptor))
			return Collections.emptySet();

		Method method = (Method) element;
		if (!isRelevantMethod(method))
			return Collections.emptySet();

		return Collections.singleton(createTestDescriptor(parent, method));
	}

	@Override
	public Optional<TestDescriptor> resolveUniqueId(UniqueId.Segment segment, TestDescriptor parent) {
		if (!segment.getType().equals(getSegmentType()))
			return Optional.empty();

		if (!(parent instanceof ContainerClassDescriptor))
			return Optional.empty();

		Optional<Method> optionalMethod = findMethod(segment, (ContainerClassDescriptor) parent);
		if (!optionalMethod.isPresent())
			return Optional.empty();

		Method method = optionalMethod.get();
		if (!isRelevantMethod(method))
			return Optional.empty();

		return Optional.of(createTestDescriptor(parent, method));
	}

	private boolean isRelevantMethod(Method candidate) {
		return methodSpec.shouldBeDiscovered(candidate);
	}

	private Optional<Method> findMethod(UniqueId.Segment segment, ContainerClassDescriptor parent) {
		return JqwikUniqueIDs.findMethodBySegment(segment, parent.getContainerClass());
	}

	private TestDescriptor createTestDescriptor(TestDescriptor parent, Method method) {
		UniqueId uniqueId = createUniqueId(method, parent);
		Class<?> testClass = ((ContainerClassDescriptor) parent).getContainerClass();
		TestDescriptor newDescriptor = createTestDescriptor(uniqueId, testClass, method);
		if (methodSpec.butSkippedOnExecution(method)) {
			return new SkipExecutionDecorator(newDescriptor, methodSpec.skippingReason(method));
		} else {
			return newDescriptor;
		}
	}

	protected abstract String getSegmentType();

	protected abstract UniqueId createUniqueId(Method method, TestDescriptor parent);

	protected abstract TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method);

}
