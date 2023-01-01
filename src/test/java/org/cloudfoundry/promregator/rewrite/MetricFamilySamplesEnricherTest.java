package org.cloudfoundry.promregator.rewrite;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.cloudfoundry.promregator.JUnitTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.Collector.Type;

public class MetricFamilySamplesEnricherTest {
	@AfterAll
	static void cleanupEnvironment() {
		JUnitTestUtils.cleanUpAll();
	}

	@Test
	void testAllLabelsSimple() {
		AbstractMetricFamilySamplesEnricher subject = new CFAllLabelsMetricFamilySamplesEnricher("testOrgName", "testSpaceName", "testComponent", "testInstance:42");

		List<Sample> samples = new LinkedList<>();
		Sample s = new Sample("dummyname", Arrays.asList(new String[] { "labelName" }), Arrays.asList(new String[] {"labelValue"}), 1.0);
		samples.add(s);
		
		MetricFamilySamples mfs = new MetricFamilySamples("dummyname", Type.GAUGE, "dummyHelp", samples);
		
		HashMap<String, MetricFamilySamples> map = new HashMap<>();
		map.put("metricName", mfs);
		
		HashMap<String, MetricFamilySamples> result = subject.determineEnumerationOfMetricFamilySamples(map);
		
		Assertions.assertEquals(1, result.size());
		
		MetricFamilySamples testMFS = result.get("metricName");
		Assertions.assertNotNull(testMFS);
		
		Assertions.assertEquals(1, testMFS.samples.size());
		
		Sample testSample = testMFS.samples.get(0);
		Assertions.assertNotNull(testSample);
		
		List<String> labelNamesList = testSample.labelNames;
		String[] labelNames = labelNamesList.toArray(new String[0]);
		
		Assertions.assertEquals("labelName", labelNames[0]);
		Assertions.assertEquals("org_name", labelNames[1]);
		Assertions.assertEquals("space_name", labelNames[2]);
		Assertions.assertEquals("app_name", labelNames[3]);
		Assertions.assertEquals("cf_instance_id", labelNames[4]);
		Assertions.assertEquals("cf_instance_number", labelNames[5]);
		
		List<String> labelValuesList = testSample.labelValues;
		String[] labelValues = labelValuesList.toArray(new String[0]);

		Assertions.assertEquals("labelValue", labelValues[0]);
		Assertions.assertEquals("testOrgName", labelValues[1]);
		Assertions.assertEquals("testSpaceName", labelValues[2]);
		Assertions.assertEquals("testComponent", labelValues[3]);
		Assertions.assertEquals("testInstance:42", labelValues[4]);
		Assertions.assertEquals("42", labelValues[5]);
	}

	@Test
	void testNullEnricherSimple() {
		AbstractMetricFamilySamplesEnricher subject = new NullMetricFamilySamplesEnricher();

		List<Sample> samples = new LinkedList<>();
		Sample s = new Sample("dummyname", Arrays.asList(new String[] { "labelName" }), Arrays.asList(new String[] {"labelValue"}), 1.0);
		samples.add(s);
		
		MetricFamilySamples mfs = new MetricFamilySamples("dummyname", Type.GAUGE, "dummyHelp", samples);
		
		HashMap<String, MetricFamilySamples> map = new HashMap<>();
		map.put("metricName", mfs);
		
		HashMap<String, MetricFamilySamples> result = subject.determineEnumerationOfMetricFamilySamples(map);
		
		Assertions.assertEquals(1, result.size());
		
		MetricFamilySamples testMFS = result.get("metricName");
		Assertions.assertNotNull(testMFS);
		
		Assertions.assertEquals(1, testMFS.samples.size());
		
		Sample testSample = testMFS.samples.get(0);
		Assertions.assertNotNull(testSample);
		
		List<String> labelNamesList = testSample.labelNames;
		String[] labelNames = labelNamesList.toArray(new String[0]);
		
		Assertions.assertEquals(1, labelNames.length);
		Assertions.assertEquals("labelName", labelNames[0]);
		
		List<String> labelValuesList = testSample.labelValues;
		String[] labelValues = labelValuesList.toArray(new String[0]);

		Assertions.assertEquals(1, labelValues.length);
		Assertions.assertEquals("labelValue", labelValues[0]);
	}

}
