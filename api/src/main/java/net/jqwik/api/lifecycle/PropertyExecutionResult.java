package net.jqwik.api.lifecycle;

import java.util.*;

import org.apiguardian.api.*;
import org.opentest4j.*;

import static org.apiguardian.api.API.Status.*;

/**
 * Represents the result of running a property.
 */
@API(status = EXPERIMENTAL, since = "1.0")
public interface PropertyExecutionResult {

	/**
	 * Status of executing a single test or container.
	 */
	enum Status {

		/**
		 * Indicates that the execution of a property was
		 * <em>successful</em>.
		 */
		SUCCESSFUL,

		/**
		 * Indicates that the execution of a property was
		 * <em>aborted</em> before the actual property method could be run.
		 */
		ABORTED,

		/**
		 * Indicates that the execution of a property has
		 * <em>failed</em>.
		 */
		FAILED
	}

	/**
	 * The seed used to generate randomized parameters.
	 *
	 * @return an optional String
	 */
	Optional<String> seed();

	/**
	 * The potentially shrunk set of parameters that falsified this property.
	 *
	 * @return an optional list of parameters
	 */
	Optional<List<Object>> falsifiedSample();

	/**
	 * The final status of this property
	 *
	 * @return status enum
	 */
	Status status();

	/**
	 * Will return {@code Optional.empty()} if status is anything but FAILED.
	 * If FAILED the optional MUST contain a throwable.
	 */
	Optional<Throwable> throwable();

	/**
	 * The number of tries for which parameters were created
	 * and the property method run.
	 *
	 * @return an number equal to or greater than 0
	 */
	@API(status = EXPERIMENTAL, since = "1.2.4")
	int countChecks();

	/**
	 * The number of tries for which parameters were created and the property method run
	 * and which were not aborted, e.g. through a failing assumption.
	 *
	 * @return an number equal to or greater than 0
	 */
	@API(status = EXPERIMENTAL, since = "1.2.4")
	int countTries();

	/**
	 * Use to change the {@linkplain Status status} of a property execution result in a
	 * {@linkplain AroundPropertyHook}.
	 *
	 * @param newStatus Status enum
	 * @param throwable Throwable object or null
	 * @return the changed result object
	 */
	@API(status = EXPERIMENTAL, since = "1.2.4")
	PropertyExecutionResult mapTo(Status newStatus, Throwable throwable);

	/**
	 * Use to change the status of a failed property to {@linkplain Status#SUCCESSFUL}
	 * in a {@linkplain AroundPropertyHook}.
	 *
	 * @return the changed result object
	 */
	@API(status = EXPERIMENTAL, since = "1.2.4")
	default PropertyExecutionResult mapToSuccessful() {
		if (status() == Status.SUCCESSFUL) {
			return this;
		}
		return mapTo(Status.SUCCESSFUL, null);
	}

	/**
	 * Use to change the status of a successful property execution to {@linkplain Status#FAILED}
	 * in a {@linkplain AroundPropertyHook}.
	 *
	 * @param throwable Throwable object or null
	 * @return the changed result object
	 */
	@API(status = EXPERIMENTAL, since = "1.2.4")
	default PropertyExecutionResult mapToFailed(Throwable throwable) {
		return mapTo(Status.FAILED, throwable);
	}

	/**
	 * Use to change the status of a successful property execution to {@linkplain Status#FAILED}
	 * in a {@linkplain AroundPropertyHook}.
	 *
	 * @param message a String that serves as message of an assertion error
	 * @return the changed result object
	 */
	@API(status = EXPERIMENTAL, since = "1.2.4")
	default PropertyExecutionResult mapToFailed(String message) {
		return mapToFailed(new AssertionFailedError(message));
	}

	/**
	 * Use to change the status of a property execution to {@linkplain Status#ABORTED}
	 * in a {@linkplain AroundPropertyHook}.
	 *
	 * @param throwable Throwable object or null
	 * @return the changed result object
	 */
	@API(status = EXPERIMENTAL, since = "1.2.4")
	default PropertyExecutionResult mapToAborted(Throwable throwable) {
		return mapTo(Status.ABORTED, throwable);
	}

}
