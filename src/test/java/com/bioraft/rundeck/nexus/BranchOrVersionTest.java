/*
 * Copyright 2019 BioRAFT, Inc. (https://bioraft.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bioraft.rundeck.nexus;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for Nexus3OptionProvider.
 *
 * @author Karl DeBisschop <kdebisschop@gmail.com>
 * @since 2019-12-26
 */
@RunWith(MockitoJUnitRunner.class)
public class BranchOrVersionTest {

	@Test
	public void testOne() {
		final String COMPONENT = "COMPONENT";
		runTest(COMPONENT, "0.0.0", "_", "1");
		runTest(COMPONENT, "0.1.2", "_", "3");
		runTest(COMPONENT, "1.2.1.2", "-", "4");
		runTest(COMPONENT, "v1.2.1.5", "-", "6");
		runTest(COMPONENT, "v2.2.1.", "_", "16");
		runTest(COMPONENT, "v2.2.0.", "_", "beta6");
		runTest(COMPONENT, "v2.2.0.", "+", "alpha1");
		runTest(COMPONENT, "", "", "");
	}

	@Test
	public void testComparison() {
		BranchOrVersion version;
		final String COMPONENT = "example.org/component";
		final String EMPTY = "";
		version = subject("1.2.3-3");
		assertEquals(1, version.compareTo(null));
		assertEquals(1, version.compareTo(subject("1.2.3")));
		assertEquals(1, version.compareTo(subject("1.2.3-2")));
		assertEquals(1, version.compareTo(subject("1.2.2-3")));
		assertEquals(1, version.compareTo(subject("v1.2.2-3")));
		assertEquals(1, version.compareTo(subject("1.2-3")));
		assertEquals(1, version.compareTo(subject("1.2.2.2-3")));
		assertEquals(1, version.compareTo(subject("rc1.2.3-3")));
		assertEquals(0, version.compareTo(subject("v1.2.3-3")));
		assertEquals(0, version.compareTo(subject("1.2.3-3")));
		assertEquals(-1, version.compareTo(subject("1.2.3.1-3")));
		assertEquals(-1, version.compareTo(subject("1.2.3-4")));
		assertEquals(-1, version.compareTo(subject("1.2.3-14")));
		assertEquals(-1, version.compareTo(subject("2.2.3-1")));
		assertEquals(-1, version.compareTo(subject("v1.2.4-3")));
		assertEquals(-1, version.compareTo(subject("rc1.2.4-3")));
		assertEquals(-1, version.compareTo(subject("20240720.1-3")));
		assertEquals(-1, version.compareTo(subject("20240720.1")));

		version = subject("1.2.3-3");
		assertEquals(1, version.compareTo(subject("rc1.2.2-4")));
		assertEquals(-1, version.compareTo(subject("20240720.1")));

		assertEquals(0, subject(COMPONENT).compareTo(subject(COMPONENT)));

		assertEquals(0, subject(EMPTY).compareTo(subject(EMPTY)));
	}

	public void runTest(String component, String version, String separator, String build) {
		BranchOrVersion subject;
		subject = new BranchOrVersion("v2/" + component + "/manifests/" + version + separator + build);
		assertEquals(component, subject.getArtifactId());
		assertEquals(version, subject.getVersion());
		assertEquals(build, subject.getBuild());
	}

	public BranchOrVersion subject(String componentVersion) {
		return new BranchOrVersion("v2/component/manifests/" + componentVersion);
	}

	public BranchOrVersion subject(String componentVersion, String build) {
		return new BranchOrVersion("v2/component/manifests/" + componentVersion + "-" + build);
	}

	public BranchOrVersion subject(String componentVersion, String separator, String build) {
		return new BranchOrVersion("v2/component/manifests/" + componentVersion + separator + build);
	}
}