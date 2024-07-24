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

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Puts branches or versions followed by a build designator into a uniform structure.
 *
 * <p>Parses versions and branches where there is a build specifier attached by "-",
 * "_" or "+".
 *
 * <p>Also puts branches into a similar structure. In particular, branches like
 * ISSUE-1234-bug-description will be sorted numerically.
 *
 * <p>In particular, the algorithm assumes each componentVersion or docker image tag
 * is composed of a version or branch followed by a build designator. The build
 * designator is first extracted by looking for a part of the componentVersion
 * after either "-", "_", or "+". If there is more than one [_+-] the match looks
 * for the last one.
 *
 * <p>The part before the build designator (and separator) is considered the version
 * or branch. If there are two consecutive integers separated by a decimal, then it
 * is considered a version specifier. Otherwise, it is considered a branch name. In
 * either case, it is split into parts at each period (i.e., ".").
 *
 * <p>Each part of the versionOrBranch and the build specifier are considered in 3 parts:
 *  - Zero or more non-numeric initial characters
 *  - Zero or more integers
 *  - Zero or more concluding characters that may be a mix of integers and non-integers
 *
 * <p>These fields in order are used to sort the assets.
 * 
 * @author Karl DeBisschop <kdebisschop@gmail.com>
 * @since 2019-12-26
 */
public class BranchOrVersion {

	public static final String BUILD_SEPARATOR_REGEX = "[_+-]";

	private final String artifactId;
	private final String versionOrBranch;
	private String build;
    private final String componentVersion;
	private final String comparator;

	public String getArtifactId() {
		return artifactId;
	}

	public String getBuild() {
		return build;
	}

	public String getVersion() {
		return versionOrBranch;
	}

	public BranchOrVersion(String path) {
		artifactId = component(path);
		componentVersion = tag(path);
        String sep;
        if (componentVersion.matches("^.+" + BUILD_SEPARATOR_REGEX + "[a-zA-Z0-9]+$")) {
			versionOrBranch = componentVersion.replaceFirst("^(.+)" + BUILD_SEPARATOR_REGEX + "[a-zA-Z0-9]+$", "$1");
			build = componentVersion.replaceFirst("^" + versionOrBranch + BUILD_SEPARATOR_REGEX + "([a-zA-Z0-9]+)$", "$1");
			sep = componentVersion.replaceFirst("^" + versionOrBranch + "(" + BUILD_SEPARATOR_REGEX + ")" + build + "$", "$1");
		} else {
			build = "";
			versionOrBranch = componentVersion;
			sep = "";
		}
		if (componentVersion.matches("^rc(\\d+[.].*)")) {
			build = build + "rc";
			if (sep.isEmpty()) {
				sep = "-";
			}
		}
		comparator = componentVersion.replaceFirst("^(v|rc)(\\d+[.].*)", "$2") + sep + build;
	}

	/**
	 * Consider a component version to be anything with 2 consecutive integer parts.
	 * Anything else is considered to be a branch.
	 */
	public boolean isVersion() {
		return versionOrBranch.matches("^(v|rc)?\\d+[.]\\d+?.*+$");
	}

	/**
	 * Decorate maven ComparableVersion to handle a few edge cases.
	 *
	 * @param other The object to compare against.
	 * @return Return 0 if equal, 1 if this is greater, -1 if that is greater.
	 */
	public int compareTo(Object other) {
		if (other == null) {
			return 1;
		}
		BranchOrVersion that = (BranchOrVersion) other;
		ComparableVersion thisVersion = new ComparableVersion(comparator);
		ComparableVersion thatVersion = new ComparableVersion(that.comparator);
		return Integer.signum(thisVersion.compareTo(thatVersion));
	}

	public String toString() {
		return artifactId + ":" + componentVersion;
	}

	/**
	 * Extracts the component name from a path string.
	 */
	private String component(String path) {
		return path.replaceFirst("^[^/]+/", "").replaceFirst("/.*", "");
	}

	/**
	 * Extracts tag part of a path. The tag (or componentVersion in the Nexus query)
	 * can reflect either a branch name or a semantic version. Either branches or
	 * versions may be suffixed by a build specifier, which can be numeric or
	 * string-valued.
	 */
	private String tag(String path) {
		return path.replaceAll("/?[^/]{0,199}/", "");
	}

}
