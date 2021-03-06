/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.api.provider

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import spock.lang.Unroll

import static PropertyStateProjectUnderTest.Language
import static org.gradle.util.TextUtil.normaliseFileSeparators

class PropertyStateIntegrationTest extends AbstractIntegrationSpec {

    private final PropertyStateProjectUnderTest projectUnderTest = new PropertyStateProjectUnderTest(testDirectory)

    @Unroll
    def "can create and use property state by custom task written as #language class"() {
        given:
        projectUnderTest.writeCustomTaskTypeToBuildSrc(language)
        buildFile << """
            task myTask(type: MyTask)
        """

        when:
        succeeds('myTask')

        then:
        projectUnderTest.assertDefaultOutputFileDoesNotExist()

        when:
        buildFile << """
             myTask {
                enabled = true
                outputFiles = files("${normaliseFileSeparators(projectUnderTest.customOutputFile.canonicalPath)}")
            }
        """
        succeeds('myTask')

        then:
        projectUnderTest.assertDefaultOutputFileDoesNotExist()
        projectUnderTest.assertCustomOutputFileContent()

        where:
        language << [Language.GROOVY, Language.JAVA]
    }

    def "can lazily map extension property state to task property with convention mapping"() {
        given:
        projectUnderTest.writeCustomGroovyBasedTaskTypeToBuildSrc()
        projectUnderTest.writePluginWithExtensionMappingUsingConventionMapping()

        when:
        succeeds('myTask')

        then:
        projectUnderTest.assertDefaultOutputFileDoesNotExist()
        projectUnderTest.assertCustomOutputFileContent()
    }

    def "can lazily map extension property state to task property with property state"() {
        given:
        projectUnderTest.writeCustomGroovyBasedTaskTypeToBuildSrc()
        projectUnderTest.writePluginWithExtensionMappingUsingPropertyState()

        when:
        succeeds('myTask')

        then:
        projectUnderTest.assertDefaultOutputFileDoesNotExist()
        projectUnderTest.assertCustomOutputFileContent()
    }

    def "can set property value from DSL using a value or a provider"() {
        given:
        buildFile << """
class SomeExtension {
    final PropertyState<String> prop
    
    @javax.inject.Inject
    SomeExtension(ProviderFactory providers) {
        prop = providers.property(String)
    }
}

class SomeTask extends DefaultTask {
    final PropertyState<String> prop = project.providers.property(String)
}

extensions.create('custom', SomeExtension, providers)
custom.prop = "value"
assert custom.prop.get() == "value"

custom.prop = providers.provider { "new value" }
assert custom.prop.get() == "new value"

tasks.create('t', SomeTask)
tasks.t.prop = custom.prop
assert tasks.t.prop.get() == "new value"

custom.prop = "changed"
assert custom.prop.get() == "changed"
assert tasks.t.prop.get() == "changed"

"""

        expect:
        succeeds()
    }

    def "reports failure to set property value using incompatible type"() {
        given:
        buildFile << """
class SomeExtension {
    final PropertyState<String> prop
    
    @javax.inject.Inject
    SomeExtension(ProviderFactory providers) {
        prop = providers.property(String)
    }
}

extensions.create('custom', SomeExtension, providers)

task wrongValueType {
    doLast {
        custom.prop = 123
    }
}

task wrongPropertyStateType {
    doLast {
        custom.prop = providers.property(Integer)
    }
}

task wrongRuntimeType {
    doLast {
        custom.prop = providers.provider { 123 }
        custom.prop.get()
    }
}
"""

        when:
        fails("wrongValueType")

        then:
        failure.assertHasDescription("Execution failed for task ':wrongValueType'.")
        failure.assertHasCause("Cannot set the value of a property of type java.lang.String using an instance of type java.lang.Integer.")

        when:
        fails("wrongPropertyStateType")

        then:
        failure.assertHasDescription("Execution failed for task ':wrongPropertyStateType'.")
        failure.assertHasCause("Cannot set the value of a property of type java.lang.String using a provider of type java.lang.Integer.")

        when:
        fails("wrongRuntimeType")

        then:
        failure.assertHasDescription("Execution failed for task ':wrongRuntimeType'.")
        failure.assertHasCause("Cannot get the value of a property of type java.lang.String as the provider associated with this property returned a value of type java.lang.Integer.")
    }
}
