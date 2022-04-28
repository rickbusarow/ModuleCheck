/*
 * Copyright (C) 2021-2022 Rick Busarow
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

package modulecheck.reporting.sarif

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Static Analysis Results Format (SARIF) Version 2.1.0 JSON Schema: a standard format for the
 * output of static analysis tools.
 */
@JsonClass(generateAdapter = true)
data class SarifReport(
  /** The URI of the JSON schema corresponding to the version. */
  @Json(name = "\$schema")
  val schema: String,

  /** The SARIF format version of this log file. */
  @Json(name = "version")
  val version: Version,

  /** References to external property files that share data between runs. */
  @Json(name = "inlineExternalProperties")
  val inlineExternalProperties: List<ExternalProperties>? = null,

  /** Key/value pairs that provide additional information about the log file. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** The set of runs contained in this log file. */
  @Json(name = "runs")
  val runs: List<Run>
)

/** The top-level element of an external property file. */
@JsonClass(generateAdapter = true)
data class ExternalProperties(
  /** Addresses that will be merged with a separate run. */
  @Json(name = "addresses")
  val addresses: List<Address>? = null,

  /** An array of artifact objects that will be merged with a separate run. */
  @Json(name = "artifacts")
  val artifacts: List<Artifact>? = null,

  /** A conversion object that will be merged with a separate run. */
  @Json(name = "conversion")
  val conversion: Conversion? = null,

  /** The analysis tool object that will be merged with a separate run. */
  @Json(name = "driver")
  val driver: SarifDriver? = null,

  /** Tool extensions that will be merged with a separate run. */
  @Json(name = "extensions")
  val extensions: List<SarifDriver>? = null,

  /**
   * Key/value pairs that provide additional information that will be merged with a separate run.
   */
  @Json(name = "externalizedProperties")
  val externalizedProperties: PropertyBag? = null,

  /** An array of graph objects that will be merged with a separate run. */
  @Json(name = "graphs")
  val graphs: List<Graph>? = null,

  /** A stable, unique identifer for this external properties object, in the form of a GUID. */
  @Json(name = "guid")
  val guid: String? = null,

  /** Describes the invocation of the analysis tool that will be merged with a separate run. */
  @Json(name = "invocations")
  val invocations: List<Invocation>? = null,

  /**
   * An array of logical locations such as namespaces, types or functions that will be merged with a
   * separate run.
   */
  @Json(name = "logicalLocations")
  val logicalLocations: List<LogicalLocation>? = null,

  /** Tool policies that will be merged with a separate run. */
  @Json(name = "policies")
  val policies: List<SarifDriver>? = null,

  /** Key/value pairs that provide additional information about the external properties. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** An array of result objects that will be merged with a separate run. */
  @Json(name = "results")
  val results: List<SarifResult>? = null,

  /**
   * A stable, unique identifer for the run associated with this external properties object, in the
   * form of a GUID.
   */
  @Json(name = "runGuid")
  val runGUID: String? = null,

  /**
   * The URI of the JSON schema corresponding to the version of the external property file format.
   */
  @Json(name = "schema")
  val schema: String? = null,

  /** Tool taxonomies that will be merged with a separate run. */
  @Json(name = "taxonomies")
  val taxonomies: List<SarifDriver>? = null,

  /** An array of threadFlowLocation objects that will be merged with a separate run. */
  @Json(name = "threadFlowLocations")
  val threadFlowLocations: List<ThreadFlowLocation>? = null,

  /** Tool translations that will be merged with a separate run. */
  @Json(name = "translations")
  val translations: List<SarifDriver>? = null,

  /** The SARIF format version of this external properties object. */
  @Json(name = "version")
  val version: Version? = null,

  /** Requests that will be merged with a separate run. */
  @Json(name = "webRequests")
  val webRequests: List<WebRequest>? = null,

  /** Responses that will be merged with a separate run. */
  @Json(name = "webResponses")
  val webResponses: List<WebResponse>? = null
)

/**
 * A physical or virtual address, or a range of addresses, in an 'addressable region' (memory or a
 * binary file).
 *
 * The address of the location.
 */
@JsonClass(generateAdapter = true)
data class Address(
  /** The address expressed as a byte offset from the start of the addressable region. */
  @Json(name = "absoluteAddress")
  val absoluteAddress: Int? = null,

  /** A human-readable fully qualified name that is associated with the address. */
  @Json(name = "fullyQualifiedName")
  val fullyQualifiedName: String? = null,

  /** The index within run.addresses of the cached object for this address. */
  @Json(name = "index")
  val index: Int? = null,

  /**
   * An open-ended string that identifies the address kind. 'data', 'function',
   * 'header','instruction', 'module', 'page', 'section', 'segment', 'stack', 'stackFrame', 'table'
   * are well-known values.
   */
  @Json(name = "kind")
  val kind: String? = null,

  /** The number of bytes in this range of addresses. */
  @Json(name = "length")
  val length: Int? = null,

  /** A name that is associated with the address, e.g., '.text'. */
  @Json(name = "name")
  val name: String? = null,

  /**
   * The byte offset of this address from the absolute or relative address of the parent object.
   */
  @Json(name = "offsetFromParent")
  val offsetFromParent: Int? = null,

  /** The index within run.addresses of the parent object. */
  @Json(name = "parentIndex")
  val parentIndex: Int? = null,

  /** Key/value pairs that provide additional information about the address. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /**
   * The address expressed as a byte offset from the absolute address of the top-most parent object.
   */
  @Json(name = "relativeAddress")
  val relativeAddress: Int? = null
)

/** Key/value pairs. */
@JsonClass(generateAdapter = true)
data class PropertyBag(
  /** A set of distinct strings that provide additional information. */
  @Json(name = "tags")
  val tags: List<String>? = null
)

/** A single artifact. In some cases, this artifact might be nested within another artifact. */
@JsonClass(generateAdapter = true)
data class Artifact(
  /** The contents of the artifact. */
  @Json(name = "contents")
  val contents: ArtifactContent? = null,

  /** A short description of the artifact. */
  @Json(name = "description")
  val description: Message? = null,

  /** Specifies the encoding for an artifact object that refers to a text file. */
  @Json(name = "encoding")
  val encoding: String? = null,

  /**
   * A dictionary, each of whose keys is the name of a hash function and each of whose values is the
   * hashed value of the artifact produced by the specified hash function.
   */
  @Json(name = "hashes")
  val hashes: Map<String, String>? = null,

  /**
   * The Coordinated Universal Time (UTC) date and time at which the artifact was most recently
   * modified. See "Date/time properties" in the SARIF spec for the required format.
   */
  @Json(name = "lastModifiedTimeUtc")
  val lastModifiedTimeUTC: String? = null,

  /** The length of the artifact in bytes. */
  @Json(name = "length")
  val length: Int? = null,

  /** The location of the artifact. */
  @Json(name = "location")
  val location: ArtifactLocation? = null,

  /** The MIME type (RFC 2045) of the artifact. */
  @Json(name = "mimeType")
  val mimeType: String? = null,

  /** The offset in bytes of the artifact within its containing artifact. */
  @Json(name = "offset")
  val offset: Int? = null,

  /** Identifies the index of the immediate parent of the artifact, if this artifact is nested. */
  @Json(name = "parentIndex")
  val parentIndex: Int? = null,

  /** Key/value pairs that provide additional information about the artifact. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** The role or roles played by the artifact in the analysis. */
  @Json(name = "roles")
  val roles: List<Role>? = null,

  /**
   * Specifies the source language for any artifact object that refers to a text file that contains
   * source code.
   */
  @Json(name = "sourceLanguage")
  val sourceLanguage: String? = null
)

/**
 * The contents of the artifact.
 *
 * Represents the contents of an artifact.
 *
 * The portion of the artifact contents within the specified region.
 *
 * The body of the request.
 *
 * The body of the response.
 *
 * The content to insert at the location specified by the 'deletedRegion' property.
 */
@JsonClass(generateAdapter = true)
data class ArtifactContent(
  /**
   * MIME Base64-encoded content from a binary artifact, or from a text artifact in its original
   * encoding.
   */
  @Json(name = "binary")
  val binary: String? = null,

  /** Key/value pairs that provide additional information about the artifact content. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /**
   * An alternate rendered representation of the artifact (e.g., a decompiled representation of a
   * binary region).
   */
  @Json(name = "rendered")
  val rendered: MultiformatMessageString? = null,

  /** UTF-8-encoded content from a text artifact. */
  @Json(name = "text")
  val text: String? = null
)

/**
 * An alternate rendered representation of the artifact (e.g., a decompiled representation of a
 * binary region).
 *
 * A message string or message format string rendered in multiple formats.
 *
 * A comprehensive description of the tool component.
 *
 * A description of the report. Should, as far as possible, provide details sufficient to enable
 * resolution of any problem indicated by the result.
 *
 * Provides the primary documentation for the report, useful when there is no online documentation.
 *
 * A concise description of the report. Should be a single sentence that is understandable when
 * visible space is limited to a single line of text.
 *
 * A brief description of the tool component.
 *
 * A comprehensive description of the translation metadata.
 *
 * A brief description of the translation metadata.
 */
@JsonClass(generateAdapter = true)
data class MultiformatMessageString(
  /** A Markdown message string or format string. */
  @Json(name = "markdown")
  val markdown: String? = null,

  /** Key/value pairs that provide additional information about the message. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** A plain text message string or format string. */
  @Json(name = "text")
  val text: String
)

/**
 * A short description of the artifact.
 *
 * A short description of the artifact location.
 *
 * A message relevant to the region.
 *
 * A message relevant to the location.
 *
 * A description of the location relationship.
 *
 * A message relevant to this call stack.
 *
 * A message that describes the condition that was encountered.
 *
 * A description of the reporting descriptor relationship.
 *
 * A description of the graph.
 *
 * A short description of the edge.
 *
 * A short description of the node.
 *
 * A message describing the role played by the attachment.
 *
 * A message relevant to the rectangle.
 *
 * A message relevant to the code flow.
 *
 * A message relevant to the thread flow.
 *
 * A message that describes the proposed fix, enabling viewers to present the proposed change to an
 * end user.
 *
 * A description of this graph traversal.
 *
 * A message to display to the user as the edge is traversed.
 *
 * A message that describes the result. The first sentence of the message only will be displayed
 * when visible space is limited.
 *
 * A description of the identity and role played within the engineering system by this object's
 * containing run object.
 *
 * Encapsulates a message intended to be read by the end user.
 */
@JsonClass(generateAdapter = true)
data class Message(
  /** An array of strings to substitute into the message string. */
  @Json(name = "arguments")
  val arguments: List<String>? = null,

  /** The identifier for this message. */
  @Json(name = "id")
  val id: String? = null,

  /** A Markdown message string. */
  @Json(name = "markdown")
  val markdown: String? = null,

  /** Key/value pairs that provide additional information about the message. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** A plain text message string. */
  @Json(name = "text")
  val text: String? = null
)

/**
 * The location of the artifact.
 *
 * Specifies the location of an artifact.
 *
 * An absolute URI specifying the location of the analysis tool's executable.
 *
 * A file containing the standard error stream from the process that was invoked.
 *
 * A file containing the standard input stream to the process that was invoked.
 *
 * A file containing the standard output stream from the process that was invoked.
 *
 * A file containing the interleaved standard output and standard error stream from the process that
 * was invoked.
 *
 * The working directory for the analysis tool run.
 *
 * Identifies the artifact that the analysis tool was instructed to scan. This need not be the same
 * as the artifact where the result actually occurred.
 *
 * The location of the attachment.
 *
 * The location of the artifact to change.
 *
 * The location of the external property file.
 *
 * Provides a suggestion to SARIF consumers to display file paths relative to the specified
 * location.
 *
 * The location in the local file system to which the root of the repository was mapped at the time
 * of the analysis.
 */
@JsonClass(generateAdapter = true)
data class ArtifactLocation(
  /** A short description of the artifact location. */
  @Json(name = "description")
  val description: Message? = null,

  /**
   * The index within the run artifacts array of the artifact object associated with the artifact
   * location.
   */
  @Json(name = "index")
  val index: Int? = null,

  /** Key/value pairs that provide additional information about the artifact location. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** A string containing a valid relative or absolute URI. */
  @Json(name = "uri")
  val uri: String? = null,

  /**
   * A string which indirectly specifies the absolute URI with respect to which a relative URI in
   * the "uri" property is interpreted.
   */
  @Json(name = "uriBaseId")
  val uriBaseID: String? = null
)

enum class Role {
  @Json(name = "added")
  Added,

  @Json(name = "analysisTarget")
  AnalysisTarget,

  @Json(name = "attachment")
  Attachment,

  @Json(name = "debugOutputFile")
  DebugOutputFile,

  @Json(name = "deleted")
  Deleted,

  @Json(name = "directory")
  Directory,

  @Json(name = "driver")
  Driver,

  @Json(name = "extension")
  Extension,

  @Json(name = "memoryContents")
  MemoryContents,

  @Json(name = "modified")
  Modified,

  @Json(name = "policy")
  Policy,

  @Json(name = "referencedOnCommandLine")
  ReferencedOnCommandLine,

  @Json(name = "renamed")
  Renamed,

  @Json(name = "responseFile")
  ResponseFile,

  @Json(name = "resultFile")
  ResultFile,

  @Json(name = "standardStream")
  StandardStream,

  @Json(name = "taxonomy")
  Taxonomy,

  @Json(name = "toolSpecifiedConfiguration")
  ToolSpecifiedConfiguration,

  @Json(name = "tracedFile")
  TracedFile,

  @Json(name = "translation")
  Translation,

  @Json(name = "uncontrolled")
  Uncontrolled,

  @Json(name = "unmodified")
  Unmodified,

  @Json(name = "userSpecifiedConfiguration")
  UserSpecifiedConfiguration
}

/**
 * A conversion object that will be merged with a separate run.
 *
 * Describes how a converter transformed the output of a static analysis tool from the analysis
 * tool's native output format into the SARIF format.
 *
 * A conversion object that describes how a converter transformed an analysis tool's native
 * reporting format into the SARIF format.
 */
@JsonClass(generateAdapter = true)
data class Conversion(
  /** The locations of the analysis tool's per-run log files. */
  @Json(name = "analysisToolLogFiles")
  val analysisToolLogFiles: List<ArtifactLocation>? = null,

  /** An invocation object that describes the invocation of the converter. */
  @Json(name = "invocation")
  val invocation: Invocation? = null,

  /** Key/value pairs that provide additional information about the conversion. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** A tool object that describes the converter. */
  @Json(name = "tool")
  val tool: Tool
)

/**
 * An invocation object that describes the invocation of the converter.
 *
 * The runtime environment of the analysis tool run.
 */
@JsonClass(generateAdapter = true)
data class Invocation(
  /** The account that ran the analysis tool. */
  @Json(name = "account")
  val account: String? = null,

  /**
   * An array of strings, containing in order the command line arguments passed to the tool from the
   * operating system.
   */
  @Json(name = "arguments")
  val arguments: List<String>? = null,

  /** The command line used to invoke the tool. */
  @Json(name = "commandLine")
  val commandLine: String? = null,

  /**
   * The Coordinated Universal Time (UTC) date and time at which the run ended. See "Date/time
   * properties" in the SARIF spec for the required format.
   */
  @Json(name = "endTimeUtc")
  val endTimeUTC: String? = null,

  /**
   * The environment variables associated with the analysis tool process, expressed as key/value
   * pairs.
   */
  @Json(name = "environmentVariables")
  val environmentVariables: Map<String, String>? = null,

  /** An absolute URI specifying the location of the analysis tool's executable. */
  @Json(name = "executableLocation")
  val executableLocation: ArtifactLocation? = null,

  /** Specifies whether the tool's execution completed successfully. */
  @Json(name = "executionSuccessful")
  val executionSuccessful: Boolean,

  /** The process exit code. */
  @Json(name = "exitCode")
  val exitCode: Int? = null,

  /** The reason for the process exit. */
  @Json(name = "exitCodeDescription")
  val exitCodeDescription: String? = null,

  /** The name of the signal that caused the process to exit. */
  @Json(name = "exitSignalName")
  val exitSignalName: String? = null,

  /** The numeric value of the signal that caused the process to exit. */
  @Json(name = "exitSignalNumber")
  val exitSignalNumber: Int? = null,

  /** The machine that hosted the analysis tool run. */
  @Json(name = "machine")
  val machine: String? = null,

  /**
   * An array of configurationOverride objects that describe notifications related runtime
   * overrides.
   */
  @Json(name = "notificationConfigurationOverrides")
  val notificationConfigurationOverrides: List<ConfigurationOverride>? = null,

  /** The process id for the analysis tool run. */
  @Json(name = "processId")
  val processID: Int? = null,

  /** The reason given by the operating system that the process failed to start. */
  @Json(name = "processStartFailureMessage")
  val processStartFailureMessage: String? = null,

  /** Key/value pairs that provide additional information about the invocation. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** The locations of any response files specified on the tool's command line. */
  @Json(name = "responseFiles")
  val responseFiles: List<ArtifactLocation>? = null,

  /** An array of configurationOverride objects that describe rules related runtime overrides. */
  @Json(name = "ruleConfigurationOverrides")
  val ruleConfigurationOverrides: List<ConfigurationOverride>? = null,

  /**
   * The Coordinated Universal Time (UTC) date and time at which the run started. See "Date/time
   * properties" in the SARIF spec for the required format.
   */
  @Json(name = "startTimeUtc")
  val startTimeUTC: String? = null,

  /** A file containing the standard error stream from the process that was invoked. */
  @Json(name = "stderr")
  val stderr: ArtifactLocation? = null,

  /** A file containing the standard input stream to the process that was invoked. */
  @Json(name = "stdin")
  val stdin: ArtifactLocation? = null,

  /** A file containing the standard output stream from the process that was invoked. */
  @Json(name = "stdout")
  val stdout: ArtifactLocation? = null,

  /**
   * A file containing the interleaved standard output and standard error stream from the process
   * that was invoked.
   */
  @Json(name = "stdoutStderr")
  val stdoutStderr: ArtifactLocation? = null,

  /** A list of conditions detected by the tool that are relevant to the tool's configuration. */
  @Json(name = "toolConfigurationNotifications")
  val toolConfigurationNotifications: List<Notification>? = null,

  /** A list of runtime conditions detected by the tool during the analysis. */
  @Json(name = "toolExecutionNotifications")
  val toolExecutionNotifications: List<Notification>? = null,

  /** The working directory for the analysis tool run. */
  @Json(name = "workingDirectory")
  val workingDirectory: ArtifactLocation? = null
)

/** Information about how a specific rule or notification was reconfigured at runtime. */
@JsonClass(generateAdapter = true)
data class ConfigurationOverride(
  /** Specifies how the rule or notification was configured during the scan. */
  @Json(name = "configuration")
  val configuration: ReportingConfiguration,

  /** A reference used to locate the descriptor whose configuration was overridden. */
  @Json(name = "descriptor")
  val descriptor: ReportingDescriptorReference,

  /** Key/value pairs that provide additional information about the configuration override. */
  @Json(name = "properties")
  val properties: PropertyBag? = null
)

/**
 * Specifies how the rule or notification was configured during the scan.
 *
 * Information about a rule or notification that can be configured at runtime.
 *
 * Default reporting configuration information.
 */
@JsonClass(generateAdapter = true)
data class ReportingConfiguration(
  /** Specifies whether the report may be produced during the scan. */
  @Json(name = "enabled")
  val enabled: Boolean? = null,

  /** Specifies the failure level for the report. */
  @Json(name = "level")
  val level: Level? = null,

  /** Contains configuration information specific to a report. */
  @Json(name = "parameters")
  val parameters: PropertyBag? = null,

  /** Key/value pairs that provide additional information about the reporting configuration. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** Specifies the relative priority of the report. Used for analysis output only. */
  @Json(name = "rank")
  val rank: Double? = null
)

/**
 * Specifies the failure level for the report.
 *
 * A value specifying the severity level of the notification.
 *
 * A value specifying the severity level of the result.
 */
enum class Level(val value: String) {
  @Json(name = "error")
  Error("error"),

  @Json(name = "none")
  None("none"),

  @Json(name = "note")
  Note("note"),

  @Json(name = "warning")
  Warning("warning")
}

/**
 * A reference used to locate the descriptor whose configuration was overridden.
 *
 * A reference used to locate the rule descriptor associated with this notification.
 *
 * A reference used to locate the descriptor relevant to this notification.
 *
 * A reference to the related reporting descriptor.
 *
 * A reference used to locate the rule descriptor relevant to this result.
 *
 * Information about how to locate a relevant reporting descriptor.
 */
@JsonClass(generateAdapter = true)
data class ReportingDescriptorReference(
  /** A guid that uniquely identifies the descriptor. */
  @Json(name = "guid")
  val guid: String? = null,

  /** The id of the descriptor. */
  @Json(name = "id")
  val id: String? = null,

  /**
   * The index into an array of descriptors in toolComponent.ruleDescriptors,
   * toolComponent.notificationDescriptors, or toolComponent.taxonomyDescriptors, depending on
   * context.
   */
  @Json(name = "index")
  val index: Int? = null,

  /**
   * Key/value pairs that provide additional information about the reporting descriptor reference.
   */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** A reference used to locate the toolComponent associated with the descriptor. */
  @Json(name = "toolComponent")
  val toolComponent: ToolComponentReference? = null
)

/**
 * A reference used to locate the toolComponent associated with the descriptor.
 *
 * Identifies a particular toolComponent object, either the driver or an extension.
 *
 * The component which is strongly associated with this component. For a translation, this refers to
 * the component which has been translated. For an extension, this is the driver that provides the
 * extension's plugin model.
 */
@JsonClass(generateAdapter = true)
data class ToolComponentReference(
  /** The 'guid' property of the referenced toolComponent. */
  @Json(name = "guid")
  val guid: String? = null,

  /** An index into the referenced toolComponent in tool.extensions. */
  @Json(name = "index")
  val index: Int? = null,

  /** The 'name' property of the referenced toolComponent. */
  @Json(name = "name")
  val name: String? = null,

  /** Key/value pairs that provide additional information about the toolComponentReference. */
  @Json(name = "properties")
  val properties: PropertyBag? = null
)

/**
 * Describes a condition relevant to the tool itself, as opposed to being relevant to a target being
 * analyzed by the tool.
 */
@JsonClass(generateAdapter = true)
data class Notification(
  /** A reference used to locate the rule descriptor associated with this notification. */
  @Json(name = "associatedRule")
  val associatedRule: ReportingDescriptorReference? = null,

  /** A reference used to locate the descriptor relevant to this notification. */
  @Json(name = "descriptor")
  val descriptor: ReportingDescriptorReference? = null,

  /** The runtime exception, if any, relevant to this notification. */
  @Json(name = "exception")
  val exception: Exception? = null,

  /** A value specifying the severity level of the notification. */
  @Json(name = "level")
  val level: Level? = null,

  /** The locations relevant to this notification. */
  @Json(name = "locations")
  val locations: List<Location>? = null,

  /** A message that describes the condition that was encountered. */
  @Json(name = "message")
  val message: Message,

  /** Key/value pairs that provide additional information about the notification. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** The thread identifier of the code that generated the notification. */
  @Json(name = "threadId")
  val threadID: Int? = null,

  /**
   * The Coordinated Universal Time (UTC) date and time at which the analysis tool generated the
   * notification.
   */
  @Json(name = "timeUtc")
  val timeUTC: String? = null
)

/**
 * The runtime exception, if any, relevant to this notification.
 *
 * Describes a runtime exception encountered during the execution of an analysis tool.
 */
@JsonClass(generateAdapter = true)
data class Exception(
  /** An array of exception objects each of which is considered a cause of this exception. */
  @Json(name = "innerExceptions")
  val innerExceptions: List<Exception>? = null,

  /**
   * A string that identifies the kind of exception, for example, the fully qualified type name of
   * an object that was thrown, or the symbolic name of a signal.
   */
  @Json(name = "kind")
  val kind: String? = null,

  /** A message that describes the exception. */
  @Json(name = "message")
  val message: String? = null,

  /** Key/value pairs that provide additional information about the exception. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** The sequence of function calls leading to the exception. */
  @Json(name = "stack")
  val stack: Stack? = null
)

/**
 * The sequence of function calls leading to the exception.
 *
 * A call stack that is relevant to a result.
 *
 * The call stack leading to this location.
 */
@JsonClass(generateAdapter = true)
data class Stack(
  /**
   * An array of stack frames that represents a sequence of calls, rendered in reverse chronological
   * order, that comprise the call stack.
   */
  @Json(name = "frames")
  val frames: List<StackFrame>,

  /** A message relevant to this call stack. */
  @Json(name = "message")
  val message: Message? = null,

  /** Key/value pairs that provide additional information about the stack. */
  @Json(name = "properties")
  val properties: PropertyBag? = null
)

/** A function call within a stack trace. */
@JsonClass(generateAdapter = true)
data class StackFrame(
  /** The location to which this stack frame refers. */
  @Json(name = "location")
  val location: Location? = null,

  /** The name of the module that contains the code of this stack frame. */
  @Json(name = "module")
  val module: String? = null,

  /** The parameters of the call that is executing. */
  @Json(name = "parameters")
  val parameters: List<String>? = null,

  /** Key/value pairs that provide additional information about the stack frame. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** The thread identifier of the stack frame. */
  @Json(name = "threadId")
  val threadID: Int? = null
)

/**
 * The location to which this stack frame refers.
 *
 * A location within a programming artifact.
 *
 * A code location associated with the node.
 *
 * The code location.
 *
 * Identifies the location associated with the suppression.
 */
@JsonClass(generateAdapter = true)
data class Location(
  /** A set of regions relevant to the location. */
  @Json(name = "annotations")
  val annotations: List<Region>? = null,

  /**
   * Value that distinguishes this location from all other locations within a single result object.
   */
  @Json(name = "id")
  val id: Int? = null,

  /** The logical locations associated with the result. */
  @Json(name = "logicalLocations")
  val logicalLocations: List<LogicalLocation>? = null,

  /** A message relevant to the location. */
  @Json(name = "message")
  val message: Message? = null,

  /** Identifies the artifact and region. */
  @Json(name = "physicalLocation")
  val physicalLocation: PhysicalLocation? = null,

  /** Key/value pairs that provide additional information about the location. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** An array of objects that describe relationships between this location and others. */
  @Json(name = "relationships")
  val relationships: List<LocationRelationship>? = null
)

/**
 * A region within an artifact where a result was detected.
 *
 * Specifies a portion of the artifact that encloses the region. Allows a viewer to display
 * additional context around the region.
 *
 * Specifies a portion of the artifact.
 *
 * The region of the artifact to delete.
 */
@JsonClass(generateAdapter = true)
data class Region(
  /** The length of the region in bytes. */
  @Json(name = "byteLength")
  val byteLength: Int? = null,

  /** The zero-based offset from the beginning of the artifact of the first byte in the region. */
  @Json(name = "byteOffset")
  val byteOffset: Int? = null,

  /** The length of the region in characters. */
  @Json(name = "charLength")
  val charLength: Int? = null,

  /**
   * The zero-based offset from the beginning of the artifact of the first character in the region.
   */
  @Json(name = "charOffset")
  val charOffset: Int? = null,

  /** The column number of the character following the end of the region. */
  @Json(name = "endColumn")
  val endColumn: Int? = null,

  /** The line number of the last character in the region. */
  @Json(name = "endLine")
  val endLine: Int? = null,

  /** A message relevant to the region. */
  @Json(name = "message")
  val message: Message? = null,

  /** Key/value pairs that provide additional information about the region. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** The portion of the artifact contents within the specified region. */
  @Json(name = "snippet")
  val snippet: ArtifactContent? = null,

  /**
   * Specifies the source language, if any, of the portion of the artifact specified by the region
   * object.
   */
  @Json(name = "sourceLanguage")
  val sourceLanguage: String? = null,

  /** The column number of the first character in the region. */
  @Json(name = "startColumn")
  val startColumn: Int? = null,

  /** The line number of the first character in the region. */
  @Json(name = "startLine")
  val startLine: Int? = null
)

/** A logical location of a construct that produced a result. */
@JsonClass(generateAdapter = true)
data class LogicalLocation(
  /**
   * The machine-readable name for the logical location, such as a mangled function name provided by
   * a C++ compiler that encodes calling convention, return type and other details along with the
   * function name.
   */
  @Json(name = "decoratedName")
  val decoratedName: String? = null,

  /** The human-readable fully qualified name of the logical location. */
  @Json(name = "fullyQualifiedName")
  val fullyQualifiedName: String? = null,

  /** The index within the logical locations array. */
  @Json(name = "index")
  val index: Int? = null,

  /**
   * The type of construct this logical location component refers to. Should be one of 'function',
   * 'member', 'module', 'namespace', 'parameter', 'resource', 'returnType', 'type', 'variable',
   * 'object', 'array', 'property', 'value', 'element', 'text', 'attribute', 'comment',
   * 'declaration', 'dtd' or 'processingInstruction', if any of those accurately describe the
   * construct.
   */
  @Json(name = "kind")
  val kind: String? = null,

  /**
   * Identifies the construct in which the result occurred. For example, this property might contain
   * the name of a class or a method.
   */
  @Json(name = "name")
  val name: String? = null,

  /**
   * Identifies the index of the immediate parent of the construct in which the result was detected.
   * For example, this property might point to a logical location that represents the namespace that
   * holds a type.
   */
  @Json(name = "parentIndex")
  val parentIndex: Int? = null,

  /** Key/value pairs that provide additional information about the logical location. */
  @Json(name = "properties")
  val properties: PropertyBag? = null
)

/**
 * Identifies the artifact and region.
 *
 * A physical location relevant to a result. Specifies a reference to a programming artifact
 * together with a range of bytes or characters within that artifact.
 */
@JsonClass(generateAdapter = true)
data class PhysicalLocation(
  /** The address of the location. */
  @Json(name = "address")
  val address: Address? = null,

  /** The location of the artifact. */
  @Json(name = "artifactLocation")
  val artifactLocation: ArtifactLocation? = null,

  /**
   * Specifies a portion of the artifact that encloses the region. Allows a viewer to display
   * additional context around the region.
   */
  @Json(name = "contextRegion")
  val contextRegion: Region? = null,

  /** Key/value pairs that provide additional information about the physical location. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** Specifies a portion of the artifact. */
  @Json(name = "region")
  val region: Region? = null
)

/** Information about the relation of one location to another. */
@JsonClass(generateAdapter = true)
data class LocationRelationship(
  /** A description of the location relationship. */
  @Json(name = "description")
  val description: Message? = null,

  /**
   * A set of distinct strings that categorize the relationship. Well-known kinds include
   * 'includes', 'isIncludedBy' and 'relevant'.
   */
  @Json(name = "kinds")
  val kinds: List<String>? = null,

  /** Key/value pairs that provide additional information about the location relationship. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** A reference to the related location. */
  @Json(name = "target")
  val target: Int
)

/**
 * A tool object that describes the converter.
 *
 * The analysis tool that was run.
 *
 * Information about the tool or tool pipeline that generated the results in this run. A run can
 * only contain results produced by a single tool or tool pipeline. A run can aggregate results from
 * multiple log files, as long as context around the tool run (tool command-line arguments and the
 * like) is identical for all aggregated files.
 */
@JsonClass(generateAdapter = true)
data class Tool(
  /** The analysis tool that was run. */
  @Json(name = "driver")
  val driver: SarifDriver,

  /** Tool extensions that contributed to or reconfigured the analysis tool that was run. */
  @Json(name = "extensions")
  val extensions: List<SarifDriver>? = null,

  /** Key/value pairs that provide additional information about the tool. */
  @Json(name = "properties")
  val properties: PropertyBag? = null
)

/**
 * The analysis tool that was run.
 *
 * A component, such as a plug-in or the driver, of the analysis tool that was run.
 *
 * The analysis tool object that will be merged with a separate run.
 */
@JsonClass(generateAdapter = true)
data class SarifDriver(
  /**
   * The component which is strongly associated with this component. For a translation, this refers
   * to the component which has been translated. For an extension, this is the driver that provides
   * the extension's plugin model.
   */
  @Json(name = "associatedComponent")
  val associatedComponent: ToolComponentReference? = null,

  /** The kinds of data contained in this object. */
  @Json(name = "contents")
  val contents: List<Content>? = null,

  /**
   * The binary version of the tool component's primary executable file expressed as four
   * non-negative integers separated by a period (for operating systems that express file versions
   * in this way).
   */
  @Json(name = "dottedQuadFileVersion")
  val dottedQuadFileVersion: String? = null,

  /** The absolute URI from which the tool component can be downloaded. */
  @Json(name = "downloadUri")
  val downloadURI: String? = null,

  /** A comprehensive description of the tool component. */
  @Json(name = "fullDescription")
  val fullDescription: MultiformatMessageString? = null,

  /**
   * The name of the tool component along with its version and any other useful identifying
   * information, such as its locale.
   */
  @Json(name = "fullName")
  val fullName: String? = null,

  /**
   * A dictionary, each of whose keys is a resource identifier and each of whose values is a
   * multiformatMessageString object, which holds message strings in plain text and (optionally)
   * Markdown format. The strings can include placeholders, which can be used to construct a message
   * in combination with an arbitrary number of additional string arguments.
   */
  @Json(name = "globalMessageStrings")
  val globalMessageStrings: Map<String, MultiformatMessageString>? = null,

  /** A unique identifer for the tool component in the form of a GUID. */
  @Json(name = "guid")
  val guid: String? = null,

  /**
   * The absolute URI at which information about this version of the tool component can be found.
   */
  @Json(name = "informationUri")
  val informationURI: String? = null,

  /**
   * Specifies whether this object contains a complete definition of the localizable and/or
   * non-localizable data for this component, as opposed to including only data that is relevant to
   * the results persisted to this log file.
   */
  @Json(name = "isComprehensive")
  val isComprehensive: Boolean? = null,

  /**
   * The language of the messages emitted into the log file during this run (expressed as an ISO
   * 639-1 two-letter lowercase language code) and an optional region (expressed as an ISO 3166-1
   * two-letter uppercase subculture code associated with a country or region). The casing is
   * recommended but not required (in order for this data to conform to RFC5646).
   */
  @Json(name = "language")
  val language: String? = null,

  /**
   * The semantic version of the localized strings defined in this component; maintained by
   * components that provide translations.
   */
  @Json(name = "localizedDataSemanticVersion")
  val localizedDataSemanticVersion: String? = null,

  /** An array of the artifactLocation objects associated with the tool component. */
  @Json(name = "locations")
  val locations: List<ArtifactLocation>? = null,

  /**
   * The minimum value of localizedDataSemanticVersion required in translations consumed by this
   * component; used by components that consume translations.
   */
  @Json(name = "minimumRequiredLocalizedDataSemanticVersion")
  val minimumRequiredLocalizedDataSemanticVersion: String? = null,

  /** The name of the tool component. */
  @Json(name = "name")
  val name: String,

  /**
   * An array of reportingDescriptor objects relevant to the notifications related to the
   * configuration and runtime execution of the tool component.
   */
  @Json(name = "notifications")
  val notifications: List<SarifRule>? = null,

  /** The organization or company that produced the tool component. */
  @Json(name = "organization")
  val organization: String? = null,

  /** A product suite to which the tool component belongs. */
  @Json(name = "product")
  val product: String? = null,

  /**
   * A localizable string containing the name of the suite of products to which the tool component
   * belongs.
   */
  @Json(name = "productSuite")
  val productSuite: String? = null,

  /** Key/value pairs that provide additional information about the tool component. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** A string specifying the UTC date (and optionally, the time) of the component's release. */
  @Json(name = "releaseDateUtc")
  val releaseDateUTC: String? = null,

  /** The tool component version in the format specified by Semantic Versioning 2.0. */
  @Json(name = "semanticVersion")
  val semanticVersion: String? = null,

  /** A brief description of the tool component. */
  @Json(name = "shortDescription")
  val shortDescription: MultiformatMessageString? = null,

  /**
   * An array of toolComponentReference objects to declare the taxonomies supported by the tool
   * component.
   */
  @Json(name = "supportedTaxonomies")
  val supportedTaxonomies: List<ToolComponentReference>? = null,

  /**
   * An array of reportingDescriptor objects relevant to the definitions of both standalone and
   * tool-defined taxonomies.
   */
  @Json(name = "taxa")
  val taxa: List<SarifRule>? = null,

  /** Translation metadata, required for a translation, not populated by other component types. */
  @Json(name = "translationMetadata")
  val translationMetadata: TranslationMetadata? = null,

  /** The tool component version, in whatever format the component natively provides. */
  @Json(name = "version")
  val version: String? = null,

  /**
   * An array of reportingDescriptor objects relevant to the analysis performed by the tool
   * component.
   */
  @Json(name = "rules")
  val rules: List<SarifRule>? = null
)

enum class Content(val value: String) {
  LocalizedData("localizedData"),
  NonLocalizedData("nonLocalizedData")
}

/**
 * Metadata that describes a specific report produced by the tool, as part of the analysis it
 * provides or its runtime reporting.
 */
@JsonClass(generateAdapter = true)
data class SarifRule(

  /** A URI where the primary documentation for the report can be found. */
  @Json(name = "helpUri")
  val helpURI: String? = null,

  /** A stable, opaque identifier for the report. */
  @Json(name = "id")
  val id: String,

  /** A report identifier that is understandable to an end user. */
  @Json(name = "name")
  val name: String? = null,

  /** Default reporting configuration information. */
  @Json(name = "defaultConfiguration")
  val defaultConfiguration: ReportingConfiguration? = null,

  /**
   * A concise description of the report. Should be a single sentence that is understandable when
   * visible space is limited to a single line of text.
   */
  @Json(name = "shortDescription")
  val shortDescription: MultiformatMessageString? = null,

  /**
   * A description of the report. Should, as far as possible, provide details sufficient to enable
   * resolution of any problem indicated by the result.
   */
  @Json(name = "fullDescription")
  val fullDescription: MultiformatMessageString? = null,

  /**
   * An array of unique identifies in the form of a GUID by which this report was known in some
   * previous version of the analysis tool.
   */
  @Json(name = "deprecatedGuids")
  val deprecatedGuids: List<String>? = null,

  /**
   * An array of stable, opaque identifiers by which this report was known in some previous version
   * of the analysis tool.
   */
  @Json(name = "deprecatedIds")
  val deprecatedIDS: List<String>? = null,

  /**
   * An array of readable identifiers by which this report was known in some previous version of the
   * analysis tool.
   */
  @Json(name = "deprecatedNames")
  val deprecatedNames: List<String>? = null,

  /** A unique identifer for the reporting descriptor in the form of a GUID. */
  @Json(name = "guid")
  val guid: String? = null,

  /**
   * Provides the primary documentation for the report, useful when there is no online
   * documentation.
   */
  @Json(name = "help")
  val help: MultiformatMessageString? = null,

  /**
   * A set of name/value pairs with arbitrary names. Each value is a multiformatMessageString
   * object, which holds message strings in plain text and (optionally) Markdown format. The strings
   * can include placeholders, which can be used to construct a message in combination with an
   * arbitrary number of additional string arguments.
   */
  @Json(name = "messageStrings")
  val messageStrings: Map<String, MultiformatMessageString>? = null,

  /** Key/value pairs that provide additional information about the report. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /**
   * An array of objects that describe relationships between this reporting descriptor and others.
   */
  @Json(name = "relationships")
  val relationships: List<ReportingDescriptorRelationship>? = null
)

/** Information about the relation of one reporting descriptor to another. */
@JsonClass(generateAdapter = true)
data class ReportingDescriptorRelationship(
  /** A description of the reporting descriptor relationship. */
  @Json(name = "description")
  val description: Message? = null,

  /**
   * A set of distinct strings that categorize the relationship. Well-known kinds include
   * 'canPrecede', 'canFollow', 'willPrecede', 'willFollow', 'superset', 'subset', 'equal',
   * 'disjoint', 'relevant', and 'incomparable'.
   */
  @Json(name = "kinds")
  val kinds: List<String>? = null,

  /**
   * Key/value pairs that provide additional information about the reporting descriptor reference.
   */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** A reference to the related reporting descriptor. */
  @Json(name = "target")
  val target: ReportingDescriptorReference
)

/**
 * Translation metadata, required for a translation, not populated by other component types.
 *
 * Provides additional metadata related to translation.
 */
@JsonClass(generateAdapter = true)
data class TranslationMetadata(
  /** The absolute URI from which the translation metadata can be downloaded. */
  @Json(name = "downloadUri")
  val downloadURI: String? = null,

  /** A comprehensive description of the translation metadata. */
  @Json(name = "fullDescription")
  val fullDescription: MultiformatMessageString? = null,

  /** The full name associated with the translation metadata. */
  @Json(name = "fullName")
  val fullName: String? = null,

  /**
   * The absolute URI from which information related to the translation metadata can be downloaded.
   */
  @Json(name = "informationUri")
  val informationURI: String? = null,

  /** The name associated with the translation metadata. */
  @Json(name = "name")
  val name: String,

  /** Key/value pairs that provide additional information about the translation metadata. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** A brief description of the translation metadata. */
  @Json(name = "shortDescription")
  val shortDescription: MultiformatMessageString? = null
)

/**
 * A network of nodes and directed edges that describes some aspect of the structure of the code
 * (for example, a call graph).
 */
@JsonClass(generateAdapter = true)
data class Graph(
  /** A description of the graph. */
  @Json(name = "description")
  val description: Message? = null,

  /** An array of edge objects representing the edges of the graph. */
  @Json(name = "edges")
  val edges: List<Edge>? = null,

  /** An array of node objects representing the nodes of the graph. */
  @Json(name = "nodes")
  val nodes: List<Node>? = null,

  /** Key/value pairs that provide additional information about the graph. */
  @Json(name = "properties")
  val properties: PropertyBag? = null
)

/** Represents a directed edge in a graph. */
@JsonClass(generateAdapter = true)
data class Edge(
  /** A string that uniquely identifies the edge within its graph. */
  @Json(name = "id")
  val id: String,

  /** A short description of the edge. */
  @Json(name = "label")
  val label: Message? = null,

  /** Key/value pairs that provide additional information about the edge. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** Identifies the source node (the node at which the edge starts). */
  @Json(name = "sourceNodeId")
  val sourceNodeID: String,

  /** Identifies the target node (the node at which the edge ends). */
  @Json(name = "targetNodeId")
  val targetNodeID: String
)

/** Represents a node in a graph. */
@JsonClass(generateAdapter = true)
data class Node(
  /** Array of child nodes. */
  @Json(name = "children")
  val children: List<Node>? = null,

  /** A string that uniquely identifies the node within its graph. */
  @Json(name = "id")
  val id: String,

  /** A short description of the node. */
  @Json(name = "label")
  val label: Message? = null,

  /** A code location associated with the node. */
  @Json(name = "location")
  val location: Location? = null,

  /** Key/value pairs that provide additional information about the node. */
  @Json(name = "properties")
  val properties: PropertyBag? = null
)

/** A result produced by an analysis tool. */
@JsonClass(generateAdapter = true)
data class SarifResult(
  /**
   * Identifies the artifact that the analysis tool was instructed to scan. This need not be the
   * same as the artifact where the result actually occurred.
   */
  @Json(name = "analysisTarget")
  val analysisTarget: ArtifactLocation? = null,

  /** A set of artifacts relevant to the result. */
  @Json(name = "attachments")
  val attachments: List<Attachment>? = null,

  /** The state of a result relative to a baseline of a previous run. */
  @Json(name = "baselineState")
  val baselineState: BaselineState? = null,

  /** An array of 'codeFlow' objects relevant to the result. */
  @Json(name = "codeFlows")
  val codeFlows: List<CodeFlow>? = null,

  /**
   * A stable, unique identifier for the equivalence class of logically identical results to which
   * this result belongs, in the form of a GUID.
   */
  @Json(name = "correlationGuid")
  val correlationGUID: String? = null,

  /**
   * A set of strings each of which individually defines a stable, unique identity for the result.
   */
  @Json(name = "fingerprints")
  val fingerprints: Map<String, String>? = null,

  /**
   * An array of 'fix' objects, each of which represents a proposed fix to the problem indicated by
   * the result.
   */
  @Json(name = "fixes")
  val fixes: List<Fix>? = null,

  /** An array of zero or more unique graph objects associated with the result. */
  @Json(name = "graphs")
  val graphs: List<Graph>? = null,

  /** An array of one or more unique 'graphTraversal' objects. */
  @Json(name = "graphTraversals")
  val graphTraversals: List<GraphTraversal>? = null,

  /** A stable, unique identifer for the result in the form of a GUID. */
  @Json(name = "guid")
  val guid: String? = null,

  /** An absolute URI at which the result can be viewed. */
  @Json(name = "hostedViewerUri")
  val hostedViewerURI: String? = null,

  /** A value that categorizes results by evaluation state. */
  @Json(name = "kind")
  val kind: ResultKind? = null,

  /** A value specifying the severity level of the result. */
  @Json(name = "level")
  val level: Level? = null,

  /**
   * The set of locations where the result was detected. Specify only one location unless the
   * problem indicated by the result can only be corrected by making a change at every specified
   * location.
   */
  @Json(name = "locations")
  val locations: List<Location>? = null,

  /**
   * A message that describes the result. The first sentence of the message only will be displayed
   * when visible space is limited.
   */
  @Json(name = "message")
  val message: Message,

  /**
   * A positive integer specifying the number of times this logically unique result was observed in
   * this run.
   */
  @Json(name = "occurrenceCount")
  val occurrenceCount: Int? = null,

  /** A set of strings that contribute to the stable, unique identity of the result. */
  @Json(name = "partialFingerprints")
  val partialFingerprints: Map<String, String>? = null,

  /** Key/value pairs that provide additional information about the result. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** Information about how and when the result was detected. */
  @Json(name = "provenance")
  val provenance: ResultProvenance? = null,

  /** A number representing the priority or importance of the result. */
  @Json(name = "rank")
  val rank: Double? = null,

  /** A set of locations relevant to this result. */
  @Json(name = "relatedLocations")
  val relatedLocations: List<Location>? = null,

  /** A reference used to locate the rule descriptor relevant to this result. */
  @Json(name = "rule")
  val rule: ReportingDescriptorReference? = null,

  /**
   * The stable, unique identifier of the rule, if any, to which this notification is relevant. This
   * member can be used to retrieve rule metadata from the rules dictionary, if it exists.
   */
  @Json(name = "ruleId")
  val ruleID: String? = null,

  /**
   * The index within the tool component rules array of the rule object associated with this result.
   */
  @Json(name = "ruleIndex")
  val ruleIndex: Int? = null,

  /** An array of 'stack' objects relevant to the result. */
  @Json(name = "stacks")
  val stacks: List<Stack>? = null,

  /** A set of suppressions relevant to this result. */
  @Json(name = "suppressions")
  val suppressions: List<Suppression>? = null,

  /**
   * An array of references to taxonomy reporting descriptors that are applicable to the result.
   */
  @Json(name = "taxa")
  val taxa: List<ReportingDescriptorReference>? = null,

  /** A web request associated with this result. */
  @Json(name = "webRequest")
  val webRequest: WebRequest? = null,

  /** A web response associated with this result. */
  @Json(name = "webResponse")
  val webResponse: WebResponse? = null,

  /** The URIs of the work items associated with this result. */
  @Json(name = "workItemUris")
  val workItemUris: List<String>? = null
)

/** An artifact relevant to a result. */
@JsonClass(generateAdapter = true)
data class Attachment(
  /** The location of the attachment. */
  @Json(name = "artifactLocation")
  val artifactLocation: ArtifactLocation,

  /** A message describing the role played by the attachment. */
  @Json(name = "description")
  val description: Message? = null,

  /** Key/value pairs that provide additional information about the attachment. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** An array of rectangles specifying areas of interest within the image. */
  @Json(name = "rectangles")
  val rectangles: List<Rectangle>? = null,

  /** An array of regions of interest within the attachment. */
  @Json(name = "regions")
  val regions: List<Region>? = null
)

/** An area within an image. */
@JsonClass(generateAdapter = true)
data class Rectangle(
  /**
   * The Y coordinate of the bottom edge of the rectangle, measured in the image's natural units.
   */
  @Json(name = "bottom")
  val bottom: Double? = null,

  /** The X coordinate of the left edge of the rectangle, measured in the image's natural units. */
  @Json(name = "left")
  val left: Double? = null,

  /** A message relevant to the rectangle. */
  @Json(name = "message")
  val message: Message? = null,

  /** Key/value pairs that provide additional information about the rectangle. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /**
   * The X coordinate of the right edge of the rectangle, measured in the image's natural units.
   */
  @Json(name = "right")
  val right: Double? = null,

  /** The Y coordinate of the top edge of the rectangle, measured in the image's natural units. */
  @Json(name = "top")
  val top: Double? = null
)

/** The state of a result relative to a baseline of a previous run. */
enum class BaselineState(val value: String) {
  Absent("absent"),
  New("new"),
  Unchanged("unchanged"),
  Updated("updated")
}

/**
 * A set of threadFlows which together describe a pattern of code execution relevant to detecting a
 * result.
 */
@JsonClass(generateAdapter = true)
data class CodeFlow(
  /** A message relevant to the code flow. */
  @Json(name = "message")
  val message: Message? = null,

  /** Key/value pairs that provide additional information about the code flow. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /**
   * An array of one or more unique threadFlow objects, each of which describes the progress of a
   * program through a thread of execution.
   */
  @Json(name = "threadFlows")
  val threadFlows: List<ThreadFlow>
)

/**
 * Describes a sequence of code locations that specify a path through a single thread of execution
 * such as an operating system or fiber.
 */
@JsonClass(generateAdapter = true)
data class ThreadFlow(
  /** An string that uniquely identifies the threadFlow within the codeFlow in which it occurs. */
  @Json(name = "id")
  val id: String? = null,

  /** Values of relevant expressions at the start of the thread flow that remain constant. */
  @Json(name = "immutableState")
  val immutableState: Map<String, MultiformatMessageString>? = null,

  /**
   * Values of relevant expressions at the start of the thread flow that may change during thread
   * flow execution.
   */
  @Json(name = "initialState")
  val initialState: Map<String, MultiformatMessageString>? = null,

  /**
   * A temporally ordered array of 'threadFlowLocation' objects, each of which describes a location
   * visited by the tool while producing the result.
   */
  @Json(name = "locations")
  val locations: List<ThreadFlowLocation>,

  /** A message relevant to the thread flow. */
  @Json(name = "message")
  val message: Message? = null,

  /** Key/value pairs that provide additional information about the thread flow. */
  @Json(name = "properties")
  val properties: PropertyBag? = null
)

/**
 * A location visited by an analysis tool while simulating or monitoring the execution of a program.
 */
@JsonClass(generateAdapter = true)
data class ThreadFlowLocation(
  /** An integer representing the temporal order in which execution reached this location. */
  @Json(name = "executionOrder")
  val executionOrder: Int? = null,

  /** The Coordinated Universal Time (UTC) date and time at which this location was executed. */
  @Json(name = "executionTimeUtc")
  val executionTimeUTC: String? = null,

  /**
   * Specifies the importance of this location in understanding the code flow in which it occurs.
   * The order from most to least important is "essential", "important", "unimportant". Default:
   * "important".
   */
  @Json(name = "importance")
  val importance: Importance? = null,

  /** The index within the run threadFlowLocations array. */
  @Json(name = "index")
  val index: Int? = null,

  /**
   * A set of distinct strings that categorize the thread flow location. Well-known kinds include
   * 'acquire', 'release', 'enter', 'exit', 'call', 'return', 'branch', 'implicit', 'false',
   * 'true', 'caution', 'danger', 'unknown', 'unreachable', 'taint', 'function', 'handler', 'lock',
   * 'memory', 'resource', 'scope' and 'value'.
   */
  @Json(name = "kinds")
  val kinds: List<String>? = null,

  /** The code location. */
  @Json(name = "location")
  val location: Location? = null,

  /** The name of the module that contains the code that is executing. */
  @Json(name = "module")
  val module: String? = null,

  /** An integer representing a containment hierarchy within the thread flow. */
  @Json(name = "nestingLevel")
  val nestingLevel: Int? = null,

  /** Key/value pairs that provide additional information about the threadflow location. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** The call stack leading to this location. */
  @Json(name = "stack")
  val stack: Stack? = null,

  /**
   * A dictionary, each of whose keys specifies a variable or expression, the associated value of
   * which represents the variable or expression value. For an annotation of kind 'continuation',
   * for example, this dictionary might hold the current assumed values of a set of global
   * variables.
   */
  @Json(name = "state")
  val state: Map<String, MultiformatMessageString>? = null,

  /**
   * An array of references to rule or taxonomy reporting descriptors that are applicable to the
   * thread flow location.
   */
  @Json(name = "taxa")
  val taxa: List<ReportingDescriptorReference>? = null,

  /** A web request associated with this thread flow location. */
  @Json(name = "webRequest")
  val webRequest: WebRequest? = null,

  /** A web response associated with this thread flow location. */
  @Json(name = "webResponse")
  val webResponse: WebResponse? = null
)

/**
 * Specifies the importance of this location in understanding the code flow in which it occurs.
 * The order from most to least important is "essential", "important", "unimportant". Default:
 * "important".
 */
enum class Importance(val value: String) {
  Essential("essential"),
  Important("important"),
  Unimportant("unimportant")
}

/**
 * A web request associated with this thread flow location.
 *
 * Describes an HTTP request.
 *
 * A web request associated with this result.
 */
@JsonClass(generateAdapter = true)
data class WebRequest(
  /** The body of the request. */
  @Json(name = "body")
  val body: ArtifactContent? = null,

  /** The request headers. */
  @Json(name = "headers")
  val headers: Map<String, String>? = null,

  /**
   * The index within the run.webRequests array of the request object associated with this result.
   */
  @Json(name = "index")
  val index: Int? = null,

  /**
   * The HTTP method. Well-known values are 'GET', 'PUT', 'POST', 'DELETE', 'PATCH', 'HEAD',
   * 'OPTIONS', 'TRACE', 'CONNECT'.
   */
  @Json(name = "method")
  val method: String? = null,

  /** The request parameters. */
  @Json(name = "parameters")
  val parameters: Map<String, String>? = null,

  /** Key/value pairs that provide additional information about the request. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** The request protocol. Example: 'http'. */
  @Json(name = "protocol")
  val protocol: String? = null,

  /** The target of the request. */
  @Json(name = "target")
  val target: String? = null,

  /** The request version. Example: '1.1'. */
  @Json(name = "version")
  val version: String? = null
)

/**
 * A web response associated with this thread flow location.
 *
 * Describes the response to an HTTP request.
 *
 * A web response associated with this result.
 */
@JsonClass(generateAdapter = true)
data class WebResponse(
  /** The body of the response. */
  @Json(name = "body")
  val body: ArtifactContent? = null,

  /** The response headers. */
  @Json(name = "headers")
  val headers: Map<String, String>? = null,

  /**
   * The index within the run.webResponses array of the response object associated with this result.
   */
  @Json(name = "index")
  val index: Int? = null,

  /** Specifies whether a response was received from the server. */
  @Json(name = "noResponseReceived")
  val noResponseReceived: Boolean? = null,

  /** Key/value pairs that provide additional information about the response. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** The response protocol. Example: 'http'. */
  @Json(name = "protocol")
  val protocol: String? = null,

  /** The response reason. Example: 'Not found'. */
  @Json(name = "reasonPhrase")
  val reasonPhrase: String? = null,

  /** The response status code. Example: 451. */
  @Json(name = "statusCode")
  val statusCode: Int? = null,

  /** The response version. Example: '1.1'. */
  @Json(name = "version")
  val version: String? = null
)

/**
 * A proposed fix for the problem represented by a result object. A fix specifies a set of artifacts
 * to modify. For each artifact, it specifies a set of bytes to remove, and provides a set of new
 * bytes to replace them.
 */
@JsonClass(generateAdapter = true)
data class Fix(
  /** One or more artifact changes that comprise a fix for a result. */
  @Json(name = "artifactChanges")
  val artifactChanges: List<ArtifactChange>,

  /**
   * A message that describes the proposed fix, enabling viewers to present the proposed change to
   * an end user.
   */
  @Json(name = "description")
  val description: Message? = null,

  /** Key/value pairs that provide additional information about the fix. */
  @Json(name = "properties")
  val properties: PropertyBag? = null
)

/** A change to a single artifact. */
@JsonClass(generateAdapter = true)
data class ArtifactChange(
  /** The location of the artifact to change. */
  @Json(name = "artifactLocation")
  val artifactLocation: ArtifactLocation,

  /** Key/value pairs that provide additional information about the change. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /**
   * An array of replacement objects, each of which represents the replacement of a single region in
   * a single artifact specified by 'artifactLocation'.
   */
  @Json(name = "replacements")
  val replacements: List<Replacement>
)

/** The replacement of a single region of an artifact. */
@JsonClass(generateAdapter = true)
data class Replacement(
  /** The region of the artifact to delete. */
  @Json(name = "deletedRegion")
  val deletedRegion: Region,

  /** The content to insert at the location specified by the 'deletedRegion' property. */
  @Json(name = "insertedContent")
  val insertedContent: ArtifactContent? = null,

  /** Key/value pairs that provide additional information about the replacement. */
  @Json(name = "properties")
  val properties: PropertyBag? = null
)

/** Represents a path through a graph. */
@JsonClass(generateAdapter = true)
data class GraphTraversal(
  /** A description of this graph traversal. */
  @Json(name = "description")
  val description: Message? = null,

  /** The sequences of edges traversed by this graph traversal. */
  @Json(name = "edgeTraversals")
  val edgeTraversals: List<EdgeTraversal>? = null,

  /**
   * Values of relevant expressions at the start of the graph traversal that remain constant for the
   * graph traversal.
   */
  @Json(name = "immutableState")
  val immutableState: Map<String, MultiformatMessageString>? = null,

  /**
   * Values of relevant expressions at the start of the graph traversal that may change during graph
   * traversal.
   */
  @Json(name = "initialState")
  val initialState: Map<String, MultiformatMessageString>? = null,

  /** Key/value pairs that provide additional information about the graph traversal. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** The index within the result.graphs to be associated with the result. */
  @Json(name = "resultGraphIndex")
  val resultGraphIndex: Int? = null,

  /** The index within the run.graphs to be associated with the result. */
  @Json(name = "runGraphIndex")
  val runGraphIndex: Int? = null
)

/** Represents the traversal of a single edge during a graph traversal. */
@JsonClass(generateAdapter = true)
data class EdgeTraversal(
  /** Identifies the edge being traversed. */
  @Json(name = "edgeId")
  val edgeID: String,

  /** The values of relevant expressions after the edge has been traversed. */
  @Json(name = "finalState")
  val finalState: Map<String, MultiformatMessageString>? = null,

  /** A message to display to the user as the edge is traversed. */
  @Json(name = "message")
  val message: Message? = null,

  /** Key/value pairs that provide additional information about the edge traversal. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** The number of edge traversals necessary to return from a nested graph. */
  @Json(name = "stepOverEdgeCount")
  val stepOverEdgeCount: Int? = null
)

/** A value that categorizes results by evaluation state. */
enum class ResultKind(val value: String) {
  Fail("fail"),
  Informational("informational"),
  NotApplicable("notApplicable"),
  Open("open"),
  Pass("pass"),
  Review("review")
}

/**
 * Information about how and when the result was detected.
 *
 * Contains information about how and when a result was detected.
 */
@JsonClass(generateAdapter = true)
data class ResultProvenance(
  /**
   * An array of physicalLocation objects which specify the portions of an analysis tool's output
   * that a converter transformed into the result.
   */
  @Json(name = "conversionSources")
  val conversionSources: List<PhysicalLocation>? = null,

  /**
   * A GUID-valued string equal to the automationDetails.guid property of the run in which the
   * result was first detected.
   */
  @Json(name = "firstDetectionRunGuid")
  val firstDetectionRunGUID: String? = null,

  /**
   * The Coordinated Universal Time (UTC) date and time at which the result was first detected. See
   * "Date/time properties" in the SARIF spec for the required format.
   */
  @Json(name = "firstDetectionTimeUtc")
  val firstDetectionTimeUTC: String? = null,

  /**
   * The index within the run.invocations array of the invocation object which describes the tool
   * invocation that detected the result.
   */
  @Json(name = "invocationIndex")
  val invocationIndex: Int? = null,

  /**
   * A GUID-valued string equal to the automationDetails.guid property of the run in which the
   * result was most recently detected.
   */
  @Json(name = "lastDetectionRunGuid")
  val lastDetectionRunGUID: String? = null,

  /**
   * The Coordinated Universal Time (UTC) date and time at which the result was most recently
   * detected. See "Date/time properties" in the SARIF spec for the required format.
   */
  @Json(name = "lastDetectionTimeUtc")
  val lastDetectionTimeUTC: String? = null,

  /** Key/value pairs that provide additional information about the result. */
  @Json(name = "properties")
  val properties: PropertyBag? = null
)

/** A suppression that is relevant to a result. */
@JsonClass(generateAdapter = true)
data class Suppression(
  /** A stable, unique identifer for the suprression in the form of a GUID. */
  @Json(name = "guid")
  val guid: String? = null,

  /** A string representing the justification for the suppression. */
  @Json(name = "justification")
  val justification: String? = null,

  /** A string that indicates where the suppression is persisted. */
  @Json(name = "kind")
  val kind: SuppressionKind,

  /** Identifies the location associated with the suppression. */
  @Json(name = "location")
  val location: Location? = null,

  /** Key/value pairs that provide additional information about the suppression. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** A string that indicates the state of the suppression. */
  @Json(name = "state")
  val state: State? = null
)

/** A string that indicates where the suppression is persisted. */
enum class SuppressionKind {
  @Json(name = "external")
  External,

  @Json(name = "inSource")
  InSource
}

/** A string that indicates the state of the suppression. */
enum class State {
  @Json(name = "accepted")
  Accepted,

  @Json(name = "rejected")
  Rejected,

  @Json(name = "underReview")
  UnderReview
}

/**
 * The SARIF format version of this external properties object.
 *
 * The SARIF format version of this log file.
 */
enum class Version {
  @Json(name = "2.1.0")
  The210
}

/** Describes a single run of an analysis tool, and contains the reported output of that run. */
@JsonClass(generateAdapter = true)
data class Run(
  /** Addresses associated with this run instance, if any. */
  @Json(name = "addresses")
  val addresses: List<Address>? = null,

  /** An array of artifact objects relevant to the run. */
  @Json(name = "artifacts")
  val artifacts: List<Artifact>? = null,

  /** Automation details that describe this run. */
  @Json(name = "automationDetails")
  val automationDetails: RunAutomationDetails? = null,

  /**
   * The 'guid' property of a previous SARIF 'run' that comprises the baseline that was used to
   * compute result 'baselineState' properties for the run.
   */
  @Json(name = "baselineGuid")
  val baselineGUID: String? = null,

  /** Specifies the unit in which the tool measures columns. */
  @Json(name = "columnKind")
  val columnKind: ColumnKind? = null,

  /**
   * A conversion object that describes how a converter transformed an analysis tool's native
   * reporting format into the SARIF format.
   */
  @Json(name = "conversion")
  val conversion: Conversion? = null,

  /** Specifies the default encoding for any artifact object that refers to a text file. */
  @Json(name = "defaultEncoding")
  val defaultEncoding: String? = null,

  /**
   * Specifies the default source language for any artifact object that refers to a text file that
   * contains source code.
   */
  @Json(name = "defaultSourceLanguage")
  val defaultSourceLanguage: String? = null,

  /**
   * References to external property files that should be inlined with the content of a root log
   * file.
   */
  @Json(name = "externalPropertyFileReferences")
  val externalPropertyFileReferences: ExternalPropertyFileReferences? = null,

  /** An array of zero or more unique graph objects associated with the run. */
  @Json(name = "graphs")
  val graphs: List<Graph>? = null,

  /** Describes the invocation of the analysis tool. */
  @Json(name = "invocations")
  val invocations: List<Invocation>? = null,

  /**
   * The language of the messages emitted into the log file during this run (expressed as an ISO
   * 639-1 two-letter lowercase culture code) and an optional region (expressed as an ISO 3166-1
   * two-letter uppercase subculture code associated with a country or region). The casing is
   * recommended but not required (in order for this data to conform to RFC5646).
   */
  @Json(name = "language")
  val language: String? = null,

  /** An array of logical locations such as namespaces, types or functions. */
  @Json(name = "logicalLocations")
  val logicalLocations: List<LogicalLocation>? = null,

  /**
   * An ordered list of character sequences that were treated as line breaks when computing region
   * information for the run.
   */
  @Json(name = "newlineSequences")
  val newlineSequences: List<String>? = null,

  /**
   * The artifact location specified by each uriBaseId symbol on the machine where the tool
   * originally ran.
   */
  @Json(name = "originalUriBaseIds")
  val originalURIBaseIDS: Map<String, ArtifactLocation>? = null,

  /**
   * Contains configurations that may potentially override both
   * reportingDescriptor.defaultConfiguration (the tool's default severities) and
   * invocation.configurationOverrides (severities established at run-time from the command line).
   */
  @Json(name = "policies")
  val policies: List<SarifDriver>? = null,

  /** Key/value pairs that provide additional information about the run. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** An array of strings used to replace sensitive information in a redaction-aware property. */
  @Json(name = "redactionTokens")
  val redactionTokens: List<String>? = null,

  /**
   * The set of results contained in an SARIF log. The results array can be omitted when a run is
   * solely exporting rules metadata. It must be present (but may be empty) if a log file represents
   * an actual scan.
   */
  @Json(name = "results")
  val results: List<SarifResult>? = null,

  /** Automation details that describe the aggregate of runs to which this run belongs. */
  @Json(name = "runAggregates")
  val runAggregates: List<RunAutomationDetails>? = null,

  /**
   * A specialLocations object that defines locations of special significance to SARIF consumers.
   */
  @Json(name = "specialLocations")
  val specialLocations: SpecialLocations? = null,

  /** An array of toolComponent objects relevant to a taxonomy in which results are categorized. */
  @Json(name = "taxonomies")
  val taxonomies: List<SarifDriver>? = null,

  /** An array of threadFlowLocation objects cached at run level. */
  @Json(name = "threadFlowLocations")
  val threadFlowLocations: List<ThreadFlowLocation>? = null,

  /**
   * Information about the tool or tool pipeline that generated the results in this run. A run can
   * only contain results produced by a single tool or tool pipeline. A run can aggregate results
   * from multiple log files, as long as context around the tool run (tool command-line arguments
   * and the like) is identical for all aggregated files.
   */
  @Json(name = "tool")
  val tool: Tool,

  /** The set of available translations of the localized data provided by the tool. */
  @Json(name = "translations")
  val translations: List<SarifDriver>? = null,

  /** Specifies the revision in version control of the artifacts that were scanned. */
  @Json(name = "versionControlProvenance")
  val versionControlProvenance: List<VersionControlDetails>? = null,

  /** An array of request objects cached at run level. */
  @Json(name = "webRequests")
  val webRequests: List<WebRequest>? = null,

  /** An array of response objects cached at run level. */
  @Json(name = "webResponses")
  val webResponses: List<WebResponse>? = null
)

/**
 * Automation details that describe this run.
 *
 * Information that describes a run's identity and role within an engineering system process.
 */
@JsonClass(generateAdapter = true)
data class RunAutomationDetails(
  /**
   * A stable, unique identifier for the equivalence class of runs to which this object's containing
   * run object belongs in the form of a GUID.
   */
  @Json(name = "correlationGuid")
  val correlationGUID: String? = null,

  /**
   * A description of the identity and role played within the engineering system by this object's
   * containing run object.
   */
  @Json(name = "description")
  val description: Message? = null,

  /** A stable, unique identifer for this object's containing run object in the form of a GUID. */
  @Json(name = "guid")
  val guid: String? = null,

  /** A hierarchical string that uniquely identifies this object's containing run object. */
  @Json(name = "id")
  val id: String? = null,

  /** Key/value pairs that provide additional information about the run automation details. */
  @Json(name = "properties")
  val properties: PropertyBag? = null
)

/** Specifies the unit in which the tool measures columns. */
enum class ColumnKind(val value: String) {
  UnicodeCodePoints("unicodeCodePoints"),
  Utf16CodeUnits("utf16CodeUnits")
}

/**
 * References to external property files that should be inlined with the content of a root log file.
 */
@JsonClass(generateAdapter = true)
data class ExternalPropertyFileReferences(
  /**
   * An array of external property files containing run.addresses arrays to be merged with the root
   * log file.
   */
  @Json(name = "addresses")
  val addresses: List<ExternalPropertyFileReference>? = null,

  /**
   * An array of external property files containing run.artifacts arrays to be merged with the root
   * log file.
   */
  @Json(name = "artifacts")
  val artifacts: List<ExternalPropertyFileReference>? = null,

  /**
   * An external property file containing a run.conversion object to be merged with the root log
   * file.
   */
  @Json(name = "conversion")
  val conversion: ExternalPropertyFileReference? = null,

  /**
   * An external property file containing a run.driver object to be merged with the root log file.
   */
  @Json(name = "driver")
  val driver: ExternalPropertyFileReference? = null,

  /**
   * An array of external property files containing run.extensions arrays to be merged with the root
   * log file.
   */
  @Json(name = "extensions")
  val extensions: List<ExternalPropertyFileReference>? = null,

  /**
   * An external property file containing a run.properties object to be merged with the root log
   * file.
   */
  @Json(name = "externalizedProperties")
  val externalizedProperties: ExternalPropertyFileReference? = null,

  /**
   * An array of external property files containing a run.graphs object to be merged with the root
   * log file.
   */
  @Json(name = "graphs")
  val graphs: List<ExternalPropertyFileReference>? = null,

  /**
   * An array of external property files containing run.invocations arrays to be merged with the
   * root log file.
   */
  @Json(name = "invocations")
  val invocations: List<ExternalPropertyFileReference>? = null,

  /**
   * An array of external property files containing run.logicalLocations arrays to be merged with
   * the root log file.
   */
  @Json(name = "logicalLocations")
  val logicalLocations: List<ExternalPropertyFileReference>? = null,

  /**
   * An array of external property files containing run.policies arrays to be merged with the root
   * log file.
   */
  @Json(name = "policies")
  val policies: List<ExternalPropertyFileReference>? = null,

  /** Key/value pairs that provide additional information about the external property files. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /**
   * An array of external property files containing run.results arrays to be merged with the root
   * log file.
   */
  @Json(name = "results")
  val results: List<ExternalPropertyFileReference>? = null,

  /**
   * An array of external property files containing run.taxonomies arrays to be merged with the root
   * log file.
   */
  @Json(name = "taxonomies")
  val taxonomies: List<ExternalPropertyFileReference>? = null,

  /**
   * An array of external property files containing run.threadFlowLocations arrays to be merged with
   * the root log file.
   */
  @Json(name = "threadFlowLocations")
  val threadFlowLocations: List<ExternalPropertyFileReference>? = null,

  /**
   * An array of external property files containing run.translations arrays to be merged with the
   * root log file.
   */
  @Json(name = "translations")
  val translations: List<ExternalPropertyFileReference>? = null,

  /**
   * An array of external property files containing run.requests arrays to be merged with the root
   * log file.
   */
  @Json(name = "webRequests")
  val webRequests: List<ExternalPropertyFileReference>? = null,

  /**
   * An array of external property files containing run.responses arrays to be merged with the root
   * log file.
   */
  @Json(name = "webResponses")
  val webResponses: List<ExternalPropertyFileReference>? = null
)

/**
 * An external property file containing a run.conversion object to be merged with the root log file.
 *
 * An external property file containing a run.driver object to be merged with the root log file.
 *
 * An external property file containing a run.properties object to be merged with the root log file.
 *
 * Contains information that enables a SARIF consumer to locate the external property file that
 * contains the value of an externalized property associated with the run.
 */
@JsonClass(generateAdapter = true)
data class ExternalPropertyFileReference(
  /** A stable, unique identifer for the external property file in the form of a GUID. */
  @Json(name = "guid")
  val guid: String? = null,

  /**
   * A non-negative integer specifying the number of items contained in the external property file.
   */
  @Json(name = "itemCount")
  val itemCount: Int? = null,

  /** The location of the external property file. */
  @Json(name = "location")
  val location: ArtifactLocation? = null,

  /** Key/value pairs that provide additional information about the external property file. */
  @Json(name = "properties")
  val properties: PropertyBag? = null
)

/**
 * A specialLocations object that defines locations of special significance to SARIF consumers.
 *
 * Defines locations of special significance to SARIF consumers.
 */
@JsonClass(generateAdapter = true)
data class SpecialLocations(
  /**
   * Provides a suggestion to SARIF consumers to display file paths relative to the specified
   * location.
   */
  @Json(name = "displayBase")
  val displayBase: ArtifactLocation? = null,

  /** Key/value pairs that provide additional information about the special locations. */
  @Json(name = "properties")
  val properties: PropertyBag? = null
)

/**
 * Specifies the information necessary to retrieve a desired revision from a version control system.
 */
@JsonClass(generateAdapter = true)
data class VersionControlDetails(
  /**
   * A Coordinated Universal Time (UTC) date and time that can be used to synchronize an enlistment
   * to the state of the repository at that time.
   */
  @Json(name = "asOfTimeUtc")
  val asOfTimeUTC: String? = null,

  /** The name of a branch containing the revision. */
  @Json(name = "branch")
  val branch: String? = null,

  /**
   * The location in the local file system to which the root of the repository was mapped at the
   * time of the analysis.
   */
  @Json(name = "mappedTo")
  val mappedTo: ArtifactLocation? = null,

  /** Key/value pairs that provide additional information about the version control details. */
  @Json(name = "properties")
  val properties: PropertyBag? = null,

  /** The absolute URI of the repository. */
  @Json(name = "repositoryUri")
  val repositoryURI: String,

  /** A string that uniquely and permanently identifies the revision within the repository. */
  @Json(name = "revisionId")
  val revisionID: String? = null,

  /** A tag that has been applied to the revision. */
  @Json(name = "revisionTag")
  val revisionTag: String? = null
)
