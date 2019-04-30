/*
 * Copyright © 2016-2018 Cask Data, Inc.
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

 /**
  * OPENMARKET - Event emitted to hub to open the market
  * CLOSEMARKET - Event emitted to hub to close the market
  */
export default {
  APPUPLOAD: 'APPICATION_UPLOAD',
  ARTIFACTUPLOAD: 'ARTIFACT_UPLOAD',
  CLOSEMARKET: 'CLOSE_MARKET',
  CREATENAMESPACE: 'CREATE_NAMESPACE',
  DELETEENTITY: 'DELETE_ENTITY',
  DIRECTIVEUPLOAD: 'DIRECTIVE_UPLOAD',
  NONAMESPACE: 'NO_NAMESPACE',
  OPENMARKET: 'OPEN_MARKET',
  PUBLISHPIPELINE: 'PUBLISH_PIPELINE',
  NSPREFERENCESSAVED: 'NS_PREFERENCES_SAVED',
  NAMESPACECREATED: 'NAMESPACE_CREATED',
};
