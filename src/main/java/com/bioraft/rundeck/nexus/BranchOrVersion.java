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

import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang.builder.CompareToBuilder;

/**
 * Puts branches or versions followed by a build designator into a uniform structure.
 * 
 * Expands three-part semantic versions to handle cases where there are more
 * than 3 parts and where build specifier may be attached by "_" or "+". (Although
 * it would really just be better if people only used semantic versions.)
 * 
 * Also puts branches into a similar structure. In particular, branches like
 * ISSUE-1234-bug-description will be sorted numerically.
 * 
 * In particular, the algorithm assumes each componentVersion or docker image tag
 * is composed of a version or branch followed by a build designator. The build
 * designator is first extracted by looking for a part of the componentVersion
 * after either "-", "_", or "+". If there is more than one [_+-] the match looks
 * for the last one.
 * 
 * The part before the build designator (and separator) is considered the version
 * or branch. If there are two consecutive integers separated by a decimal, then it
 * is considered a version specifier. Otherwise it is considered a branch name. In
 * either case, it is split into parts at each period (i.e., ".").
 * 
 * Each part of the versionOrBranch and the build specifier are considered in 3 parts:
 *  - Zero or more non-numeric initial characters
 *  - Zero or more integers
 *  - Zero or more concluding characters that may be a mix of integers and non-integers
 *  
 * These fields in order are used to sort the assets.
 * 
 * @author Karl DeBisschop <kdebisschop@gmail.com>
 * @since 2019-12-26
 */
public class BranchOrVersion {

	public static final String BUILD_SEPARATOR_REGEX = "[_+-]";

	String artifactId;
	String versionOrBranch;
	String build;
	String sep;
	String[] parts;

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
		String tag = tag(path);
		if (tag.matches("^.*" + BUILD_SEPARATOR_REGEX + "([a-zA-Z0-9]+)$")) {
			build = tag.replaceFirst("^.*" + BUILD_SEPARATOR_REGEX + "([a-zA-Z0-9]+)$", "$1");
			versionOrBranch = tag.replaceFirst("^(.*)" + BUILD_SEPARATOR_REGEX + build + "$", "$1");
			sep = tag.replaceFirst("^.*(" + BUILD_SEPARATOR_REGEX + ")" + build + "$", "$1");
		} else {
			build = "";
			versionOrBranch = tag;
			sep = "";
		}
		this.parts = versionOrBranch.split("[.]");
	}

	public Iterator<String> getIterator() {
		return Arrays.asList(parts).iterator();
	}

	/**
	 * Consider a component version to be anything with 2 consecutive integer parts.
	 * Anything else is considered to be a branch.
	 */
	public boolean isVersion() {
		return versionOrBranch.matches(".*[0-9]+[.][0-9]+.*");
	}

	/**
	 * Use array of parts to do comparison so it can compare regardless of number of
	 * parts in a version specifier.
	 * 
	 * @param other The object to compare against.
	 * @return Return 0 if equal, 1 if this is greater, -1 if that is greater.
	 */
	public int compareTo(Object other) {
		if (other == null) {
			return 1;
		}
		BranchOrVersion that = (BranchOrVersion) other;
		CompareToBuilder comparator = new CompareToBuilder();
		Iterator<String> thatIterator = that.getIterator();
		Iterator<String> thisIterator = getIterator();
		while (thatIterator.hasNext()) {
			if (!thisIterator.hasNext()) {
				if (comparator.toComparison() == 0) {
					return -1;
				} else {
					break;
				}
			}
			String thisOne = thisIterator.next();
			String thatOne = thatIterator.next();
			comparator.append(partOne(thisOne), partOne(thatOne));
			comparator.append(partTwo(thisOne), partTwo(thatOne));
			comparator.append(partThree(thisOne), partThree(thatOne));
		}
		if (comparator.toComparison() == 0 && thisIterator.hasNext()) {
			return 1;
		}
		comparator.append(partOne(this.build), partOne(that.build));
		comparator.append(partTwo(this.build), partTwo(that.build));
		comparator.append(partThree(this.build), partThree(that.build));
		return comparator.toComparison();
	}

	private String partOne(String string) {
		return string.replaceFirst("^([^0-9]*).*$", "$1");
	}

	private Integer partTwo(String string) {
		String integerPart = string.replaceFirst("^[^0-9]*([0-9]*).*$", "$1");
		if (integerPart.length() == 0) {
			return 0;
		}
		return Integer.parseInt(integerPart);
	}

	private String partThree(String string) {
		return string.replaceFirst("^[^0-9]*[0-9]*", "");		
	}

	public String toString() {
		return artifactId + ":" + String.join(".", parts) + sep + build;
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
	 * versions may be suffixed by a build specified, which can be numeric or
	 * string-valued.
	 */
	private String tag(String path) {
		return path.replaceAll(".*/", "");
	}

}
