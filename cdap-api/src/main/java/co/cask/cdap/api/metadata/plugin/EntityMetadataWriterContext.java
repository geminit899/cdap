/*
 * Copyright © 2018 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package co.cask.cdap.api.metadata.plugin;

import co.cask.cdap.api.metadata.MetadataEntity;

import java.util.List;
import java.util.Map;

/**
 * Interfaces for methods exposed in pipeline to write metadata. This writer is tied to an
 * entity and can only write metadata for the specific entity or its derivatives
 */
public interface EntityMetadataWriterContext {
  /**
   * Adds the specified {@link Map} to the metadata. Existing keys will be updated with new values.
   */
  void addProperties(Map<String, String> properties);

  /**
   * Adds the specified {@link Map} to the metadata of the {@link MetadataEntity} created by calling
   * {@link co.cask.cdap.api.metadata.MetadataEntity#append(String, String)} on subParts to the
   * {@link co.cask.cdap.api.metadata.MetadataEntity} associated with this {@link EntityMetadataWriterContext}.
   * Existing keys will be updated with new values.
   *
   * @param subParts the key-value subparts
   * @param properties the properties to be added
   */
  void addProperties(List<MetadataEntity.KeyValue> subParts, Map<String, String> properties);

  /**
   * Adds the specified tags. If a given tag already exists for the metadata entity it will be skipped.
   */
  void addTags(String... tags);

  /**
   * Adds all the specified tags. If a given tag already exists for the metadata entity it will be skipped.
   */
  void addTags(Iterable<String> tags);

  /**
   * Adds all the specified tags to the {@link MetadataEntity} created by calling
   * {@link co.cask.cdap.api.metadata.MetadataEntity#append(String, String)} on subParts to the
   * {@link co.cask.cdap.api.metadata.MetadataEntity} associated with this {@link EntityMetadataWriterContext}. If a
   * given tag already exists for the metadata entity it will be skipped.
   *
   * @param subParts the key-value subparts
   * @param tags the tags to be added
   */
  void addTags(List<MetadataEntity.KeyValue> subParts, Iterable<String> tags);

  /**
   * Removes all the user metadata (including properties and tags)
   */
  void removeMetadata();

  /**
   * Removes all user metadata (including properties and tags) from the {@link MetadataEntity} created by calling
   * {@link co.cask.cdap.api.metadata.MetadataEntity#append(String, String)} on subParts to the
   * {@link co.cask.cdap.api.metadata.MetadataEntity} associated with this {@link EntityMetadataWriterContext}.
   *
   * @param subParts the key-value subparts
   */
  void removeMetadata(List<MetadataEntity.KeyValue> subParts);

  /**
   * Removes all properties from the user metadata
   */
  void removeProperties();

  /**
   * Removes all user properties from the {@link MetadataEntity} created by calling
   * {@link co.cask.cdap.api.metadata.MetadataEntity#append(String, String)} on subParts to the
   * {@link co.cask.cdap.api.metadata.MetadataEntity} associated with this {@link EntityMetadataWriterContext}.
   *
   * @param subParts the key-value subparts
   */
  void removeProperties(List<MetadataEntity.KeyValue> subParts);

  /**
   * Removes the specified keys from the user metadata properties
   *
   * @param keys the metadata property keys to remove
   */
  void removeProperties(String... keys);

  /**
   * Removes the specified keys from the user metadata properties from the {@link MetadataEntity} created by calling
   * {@link co.cask.cdap.api.metadata.MetadataEntity#append(String, String)} on subParts to the
   * {@link co.cask.cdap.api.metadata.MetadataEntity} associated with this {@link EntityMetadataWriterContext}.
   *
   * @param subParts the key-value subparts
   * @param keys the metadata property keys to remove
   */
  void removeProperties(List<MetadataEntity.KeyValue> subParts, String... keys);

  /**
   * Removes all user tags
   */
  void removeTags();

  /**
   * Removes all user tags from the {@link MetadataEntity} created by calling
   * {@link co.cask.cdap.api.metadata.MetadataEntity#append(String, String)} on subParts to the
   * {@link co.cask.cdap.api.metadata.MetadataEntity} associated with this {@link EntityMetadataWriterContext}.
   *
   * @param subParts the key-value subparts
   */
  void removeTags(List<MetadataEntity.KeyValue> subParts);

  /**
   * Removes the specified user tags
   *
   * @param tags the tags to remove
   */
  void removeTags(String... tags);

  /**
   * Removes the specified user tags from the {@link MetadataEntity} created by calling
   * {@link co.cask.cdap.api.metadata.MetadataEntity#append(String, String)} on subParts to the
   * {@link co.cask.cdap.api.metadata.MetadataEntity} associated with this {@link EntityMetadataWriterContext}.
   *
   * @param tags the tags to remove
   * @param subParts the key-value subparts
   */
  void removeTags(List<MetadataEntity.KeyValue> subParts, String... tags);
}
