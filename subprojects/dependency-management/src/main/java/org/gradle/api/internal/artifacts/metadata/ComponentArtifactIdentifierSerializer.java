/*
 * Copyright 2013 the original author or authors.
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
package org.gradle.api.internal.artifacts.metadata;

import com.google.common.base.Objects;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.ComponentIdentifierSerializer;
import org.gradle.internal.component.external.model.DefaultModuleComponentArtifactIdentifier;
import org.gradle.internal.component.model.IvyArtifactName;
import org.gradle.internal.serialize.AbstractSerializer;
import org.gradle.internal.serialize.Decoder;
import org.gradle.internal.serialize.Encoder;

public class ComponentArtifactIdentifierSerializer extends AbstractSerializer<ComponentArtifactIdentifier> {
    private final ComponentIdentifierSerializer componentIdentifierSerializer = new ComponentIdentifierSerializer();

    public void write(Encoder encoder, ComponentArtifactIdentifier value) throws Exception {
        if (value instanceof DefaultModuleComponentArtifactIdentifier) {
            DefaultModuleComponentArtifactIdentifier moduleComponentArtifactIdentifier = (DefaultModuleComponentArtifactIdentifier) value;
            componentIdentifierSerializer.write(encoder, moduleComponentArtifactIdentifier.getComponentIdentifier());
            IvyArtifactName ivyArtifactName = moduleComponentArtifactIdentifier.getName();
            encoder.writeString(ivyArtifactName.getName());
            encoder.writeString(ivyArtifactName.getType());
            encoder.writeNullableString(ivyArtifactName.getExtension());
            encoder.writeNullableString(ivyArtifactName.getClassifier());
        } else {
            throw new IllegalArgumentException("Unknown identifier type.");
        }
    }

    public ComponentArtifactIdentifier read(Decoder decoder) throws Exception {
        ModuleComponentIdentifier componentIdentifier = (ModuleComponentIdentifier) componentIdentifierSerializer.read(decoder);
        String artifactName = decoder.readString();
        String type = decoder.readString();
        String extension = decoder.readNullableString();
        String classifier = decoder.readNullableString();
        return new DefaultModuleComponentArtifactIdentifier(componentIdentifier, artifactName, type, extension, classifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        ComponentArtifactIdentifierSerializer rhs = (ComponentArtifactIdentifierSerializer) obj;
        return Objects.equal(componentIdentifierSerializer, rhs.componentIdentifierSerializer);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), componentIdentifierSerializer);
    }
}
