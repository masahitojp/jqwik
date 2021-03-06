package net.jqwik.engine.execution.reporting;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

public class SampleReporter {

	private static final int MAX_LINE_LENGTH = 100;

	public static void reportSample(
		StringBuilder reportLines,
		Method propertyMethod,
		List<Object> sample,
		String headline
	) {
		List<String> parameterNames = Arrays.stream(propertyMethod.getParameters())
											.map(Parameter::getName)
											.collect(Collectors.toList());
		SampleReporter sampleReporter = new SampleReporter(headline, sample, parameterNames);
		LineReporter lineReporter = new LineReporterImpl(reportLines);
		sampleReporter.reportTo(lineReporter);
	}

	private final String headline;
	private final List<Object> sample;

	private final List<String> parameterNames;

	public SampleReporter(String headline, List<Object> sample, List<String> parameterNames) {
		if (sample.size() != parameterNames.size()) {
			throw new IllegalArgumentException("Number of sample parameters must be equal to number of parameter names");
		}
		this.headline = headline;
		this.sample = sample;
		this.parameterNames = parameterNames;
	}

	void reportTo(LineReporter lineReporter) {
		lineReporter.addLine(0, "");
		reportHeadline(lineReporter);
		reportParameters(lineReporter);
	}

	private void reportParameters(LineReporter lineReporter) {
		for (int i = 0; i < parameterNames.size(); i++) {
			String parameterName = parameterNames.get(i);
			Object parameterValue = sample.get(i);
			ValueReport sampleReport = createReport(parameterValue);
			if (sampleReport.singleLineLength() + parameterName.length() < MAX_LINE_LENGTH) {
				String line = String.format("%s: %s", parameterName, sampleReport.singleLineReport());
				lineReporter.addLine(1, line);
			} else {
				String line = String.format("%s:", parameterName);
				lineReporter.addLine(1, line);
				sampleReport.report(lineReporter, 2, "");
			}
		}
	}

	private ValueReport createReport(Object value) {
		return ValueReport.of(value);
	}

	private void reportHeadline(LineReporter lineReporter) {
		if (headline == null) {
			return;
		}
		lineReporter.addLine(0, headline);
		lineReporter.addUnderline(0, headline.length());
	}
}
