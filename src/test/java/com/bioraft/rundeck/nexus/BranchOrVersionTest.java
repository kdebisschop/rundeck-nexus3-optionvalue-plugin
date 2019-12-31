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
		String component = "COMPONENT";
		runTest(component, "0.0.0", "_", "1");
		runTest(component, "0.1.2", "_", "3");
		runTest(component, "1.2.1.2", "-", "4");
		runTest(component, "v1.2.1.5", "-", "6");
		runTest(component, "v2.2.1.", "_", "16");
		runTest(component, "v2.2.0.", "_", "beta6");
		runTest(component, "v2.2.0.", "+", "alpha1");
	}

	@Test
	public void testComparison() {
		BranchOrVersion subject;
		subject = subject(path("COMP", "1.2.3", "-", "3"));

		assertEquals(-1, subject.compareTo(subject(path("COMP", "1.2.3", "-", "4"))));
		assertEquals(-1, subject.compareTo(subject(path("COMP", "1.2.3", "-", "14"))));
		assertEquals(-1, subject.compareTo(subject(path("COMP", "2.2.3", "-", "1"))));
	}

	public void runTest(String component, String version, String separator, String build) {
		BranchOrVersion subject;
		subject = new BranchOrVersion("v2/" + component + "/manifests/" + version + separator + build);
		assertEquals(component, subject.getArtifactId());
		assertEquals(version, subject.getVersion());
		assertEquals(build, subject.getBuild());
	}

	public String path(String component, String version, String separator, String build) {
		return "v2/" + component + "/manifests/" + version + separator + build;
	}

	public BranchOrVersion subject(String path) {
		return new BranchOrVersion(path);
	}
}