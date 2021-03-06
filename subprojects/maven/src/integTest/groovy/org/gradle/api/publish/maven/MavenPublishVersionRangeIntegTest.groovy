/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.publish.maven

import org.gradle.integtests.fixtures.publish.maven.AbstractMavenPublishIntegTest
import spock.lang.Issue

class MavenPublishVersionRangeIntegTest extends AbstractMavenPublishIntegTest {
    def mavenModule = mavenRepo.module("org.gradle.test", "publishTest", "1.9")

    public void "version range is mapped to maven syntax in published pom file"() {
        given:
        settingsFile << "rootProject.name = 'publishTest' "
        buildFile << """
            apply plugin: 'maven-publish'
            apply plugin: 'java'

            group = 'org.gradle.test'
            version = '1.9'

            publishing {
                repositories {
                    maven { url "${mavenRepo.uri}" }
                }
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            dependencies {
                compile "group:projectA:latest.release"
                compile "group:projectB:latest.integration"
                compile "group:projectC:1.+"
                compile "group:projectD:[1.0,2.0)"
                compile "group:projectE:[1.0]"
            }"""

        when:
        run "publish"

        then:
        mavenModule.assertPublishedAsJavaModule()

        mavenModule.parsedPom.scopes.keySet() == ["compile"] as Set
        mavenModule.parsedPom.scopes.compile.assertDependsOn(
            "group:projectA:RELEASE",
            "group:projectB:LATEST",
            "group:projectC:1.+",
            "group:projectD:[1.0,2.0)",
            "group:projectE:[1.0]"
        )
    }

    @Issue("GRADLE-3233")
    def "publishes POM dependency for Gradle dependency with empty version"() {
        settingsFile << "rootProject.name = 'publishTest' "
        buildFile << """

            apply plugin: 'maven-publish'
            apply plugin: 'java'

            group = 'org.gradle.test'
            version = '1.9'

            publishing {
                repositories {
                    maven { url "${mavenRepo.uri}" }
                }
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }

            dependencies {
                compile "group:projectA"
                compile group:"group", name:"projectB", version:null
            }"""

        when:
        run "publish"

        then:
        mavenModule.assertPublishedAsJavaModule()
        mavenModule.parsedPom.scopes.compile.assertDependsOn("group:projectA:", "group:projectB:")
    }

}
