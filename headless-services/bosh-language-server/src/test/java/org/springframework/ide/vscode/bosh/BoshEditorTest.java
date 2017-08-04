/*******************************************************************************
 * Copyright (c) 2016 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.bosh;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.ide.vscode.languageserver.testharness.Editor.*;
import static org.springframework.ide.vscode.languageserver.testharness.TestAsserts.assertContains;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.ide.vscode.bosh.mocks.MockCloudConfigProvider;
import org.springframework.ide.vscode.bosh.models.BoshCommandReleasesProvider;
import org.springframework.ide.vscode.bosh.models.BoshCommandStemcellsProvider;
import org.springframework.ide.vscode.bosh.models.DynamicModelProvider;
import org.springframework.ide.vscode.bosh.models.ReleaseData;
import org.springframework.ide.vscode.bosh.models.ReleasesModel;
import org.springframework.ide.vscode.bosh.models.StemcellData;
import org.springframework.ide.vscode.bosh.models.StemcellsModel;
import org.springframework.ide.vscode.commons.util.ExternalCommand;
import org.springframework.ide.vscode.commons.util.text.LanguageId;
import org.springframework.ide.vscode.commons.yaml.reconcile.YamlSchemaProblems;
import org.springframework.ide.vscode.languageserver.testharness.Editor;
import org.springframework.ide.vscode.languageserver.testharness.LanguageServerHarness;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;

public class BoshEditorTest {

	LanguageServerHarness<BoshLanguageServer> harness;

	private BoshCliConfig cliConfig = new BoshCliConfig();
	private MockCloudConfigProvider cloudConfigProvider = new MockCloudConfigProvider(cliConfig);
	private DynamicModelProvider<StemcellsModel> stemcellsProvider = mock(DynamicModelProvider.class);
	private DynamicModelProvider<ReleasesModel> releasesProvider = mock(DynamicModelProvider.class);

	@Before public void setup() throws Exception {
		harness = new LanguageServerHarness<BoshLanguageServer>(() -> {
				return new BoshLanguageServer(cliConfig, cloudConfigProvider,
						(dc) -> stemcellsProvider.getModel(dc),
						(dc) -> releasesProvider.getModel(dc)
				)
				.setMaxCompletions(100);
			},
			LanguageId.BOSH_DEPLOYMENT
		);
		harness.intialize(null);
		System.setProperty("lsp.yaml.completions.errors.disable", "false");
	}

	@Test public void toplevelV2PropertyNamesKnown() throws Exception {
		cloudConfigProvider.readWith(() -> { throw new IOException("Can't read cloud config"); });
		Editor editor = harness.newEditor(
				"name: some-name\n" +
				"director_uuid: cf8dc1fc-9c42-4ffc-96f1-fbad983a6ce6\n" +
				"releases:\n" +
				"- name: redis\n" +
				"  version: 12\n" +
				"stemcells:\n" +
				"- alias: default\n" +
				"  os: ubuntu-trusty\n" +
				"  version: 3421.11\n" +
				"update:\n" +
				"  canaries: 1\n" +
				"  max_in_flight: 10\n" +
				"  canary_watch_time: 1000-30000\n" +
				"  update_watch_time: 1000-30000\n" +
				"instance_groups:\n" +
				"- name: redis-master\n" +
				"  instances: 1\n" +
				"  azs: [z1, z2]\n" +
				"  jobs:\n" +
				"  - name: redis-server\n" +
				"    release: redis\n" +
				"    properties:\n" +
				"      port: 3606\n" +
				"  vm_type: large\n" +
				"  vm_extensions: [public-lbs]\n" +
				"  stemcell: default\n" +
				"  persistent_disk_type: large\n" +
				"  networks:\n" +
				"  - name: default\n" +
				"- name: redis-slave\n" +
				"  instances: 2\n" +
				"  azs: [z1, z2]\n" +
				"  jobs:\n" +
				"  - name: redis-server\n" +
				"    release: redis\n" +
				"    properties: {}\n" +
				"  vm_type: large\n" +
				"  stemcell: default\n" +
				"  persistent_disk_type: large\n" +
				"  networks:\n" +
				"  - name: default\n" +
				"variables:\n" +
				"- name: admin_password\n" +
				"  type: password\n" +
				"- name: default_ca\n" +
				"  type: certificate\n" +
				"  options:\n" +
				"    is_ca: true\n" +
				"    common_name: some-ca\n" +
				"- name: director_ssl\n" +
				"  type: certificate\n" +
				"  options:\n" +
				"    ca: default_ca\n" +
				"    common_name: cc.cf.internal\n" +
				"    alternative_names: [cc.cf.internal]\n" +
				"properties:\n" +
				"  a-property: the-value\n" +
				"tags:\n" +
				"  project: cf\n" +
				"blah: hoooo\n"
		);
		editor.assertProblems(
				"director_uuid|bosh v2 CLI no longer checks or requires",
				"properties|Deprecated in favor of job level properties and links",
				"blah|Unknown property"
		);

		editor.assertHoverContains("name", "The name of the deployment");
		editor.assertHoverContains("director_uuid", "This string must match the UUID of the currently targeted Director");
		editor.assertHoverContains("releases", "The name and version of each release in the deployment");
		editor.assertHoverContains("stemcells", "The name and version of each stemcell");
		editor.assertHoverContains("update", "This specifies instance update properties");
		editor.assertHoverContains("instance_groups", "Specifies the mapping between release [jobs](https://bosh.io/docs/terminology.html#job) and instance groups.");
		editor.assertHoverContains("properties", 3, "Describes global properties. Deprecated");
		editor.assertHoverContains("variables", "Describes variables");
		editor.assertHoverContains("tags", "Specifies key value pairs to be sent to the CPI for VM tagging");
	}

	@Test public void reconcileCfManifest() throws Exception {
		Editor editor = harness.newEditorFromClasspath("/workspace/cf-deployment-manifest.yml");
		cloudConfigProvider.executeCommandWith(() -> {
			throw new IOException("Couldn't contact the director");
		});
		editor.assertProblems(/*NONE*/);
	}

	@Test public void toplevelPropertyCompletions() throws Exception {
		harness.getServer().enableSnippets(false);
		Editor editor = harness.newEditor(
				"<*>"
		);
		editor.assertCompletions(
				"name: <*>"
		);

		editor = harness.newEditor(
				"name: blah\n" +
				"<*>"
		);

		editor.assertCompletions(
				"name: blah\n" +
				"instance_groups:\n" +
				"- name: <*>"
				, // ============
				"name: blah\n" +
				"releases:\n" +
				"- name: <*>"
				, // ============
				"name: blah\n" +
				"stemcells:\n- <*>"
				, // ============
				"name: blah\n" +
				"tags:\n  <*>"
				, // ============
				"name: blah\n" +
				"update:\n  <*>"
				, // ============
				"name: blah\n" +
				"variables:\n" +
				"- name: <*>"
// Below completions are suppressed because they are deprecated
//				, // ============
//				"name: blah\n" +
//				"director_uuid: <*>"
//				, // ============
//				"name: blah\n" +
//				"properties:\n  <*>"
		);
	}

	@Test public void stemcellCompletions() throws Exception {
		harness.getServer().enableSnippets(false);
		Editor editor = harness.newEditor(
				"stemcells:\n" +
				"- <*>"
		);
		editor.assertCompletions(
				"stemcells:\n" +
				"- alias: <*>"
				, // ==========
				"stemcells:\n" +
				"- name: <*>"
				, // ==========
				"stemcells:\n" +
				"- os: <*>"
				, // ==========
				"stemcells:\n" +
				"- version: <*>"
		);

		editor = harness.newEditor(
				"stemcells:\n" +
				"- alias<*>"
		);
	}

	@Test public void stemcellReconciling() throws Exception {
		Editor editor = harness.newEditor(
				"stemcells:\n" +
				"- {}"
		);
		editor.assertProblems(
				"-|One of [name, os] is required",
				"-|[alias, version] are required",
				"}|[instance_groups, name, releases, update] are required"
		);
	}

	@Test public void stemcellHovers() throws Exception {
		Editor editor = harness.newEditor(
				"stemcells:\n" +
				"- alias: default\n" +
				"  os: ubuntu-trusty\n" +
				"  version: 3147\n" +
				"  name: bosh-aws-xen-hvm-ubuntu-trusty-go_agent"
		);
		editor.assertHoverContains("alias", "Name of a stemcell used in the deployment");
		editor.assertHoverContains("os", "Operating system of a matching stemcell");
		editor.assertHoverContains("version", "The version of a matching stemcell");
		editor.assertHoverContains("name", "Full name of a matching stemcell. Either `name` or `os` keys can be specified.");
	}

	@Test public void releasesBlockCompletions() throws Exception {
		harness.getServer().enableSnippets(false);
		Editor editor = harness.newEditor(
				"releases:\n" +
				"- <*>"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertCompletions(
				"releases:\n" +
				"- name: <*>"
		);

		editor = harness.newEditor(
				"releases:\n" +
				"- name: foo\n" +
				"  <*>"
		);
		editor.assertCompletions(PLAIN_COMPLETION,
				"releases:\n" +
				"- name: foo\n" +
				"  sha1: <*>"
				, // ============
				"releases:\n" +
				"- name: foo\n" +
				"  url: <*>"
				, // ============
				"releases:\n" +
				"- name: foo\n" +
				"  version: <*>"
		);
	}

	@Test public void releasesAdvancedValidations() throws Exception {
		Editor editor = harness.newEditor(
				"releases:\n" +
				"- name: some-release\n" +
				"  url: https://my.releases.com/funky.tar.gz\n" +
				"- name: other-relase\n" +
				"  version: other-version\n" +
				"  url: file:///root/releases/a-nice-file.tar.gz\n" +
				"- name: bad-url\n" +
				"  version: more-version\n" +
				"  url: proto://something.com\n" +
				"#x"
		);
		editor.assertProblems(
				"^-^ name: some-release|'version' is required",
				"url|'sha1' is recommended when the 'url' is http(s)",
				"proto|Url scheme must be one of [http, https, file]",
				"x|are required"
		);
		Diagnostic missingSha1Problem = editor.assertProblem("url");
		assertContains("'sha1' is recommended", missingSha1Problem.getMessage());
		assertEquals(DiagnosticSeverity.Warning, missingSha1Problem.getSeverity());
	}

	@Test public void releasesBlockPropertyReconcileAndHovers() throws Exception {
		Editor editor = harness.newEditor(
				"releases:\n" +
				"- name: some-release\n" +
				"  version: some-version\n" +
				"  url: https://my.releases.com/funky.tar.gz\n" +
				"  sha1: 440248a31253296b1626ad52886e58900730f32e\n" +
				"  woot: dunno\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems(
				"woot|Unknown property"
		);

		editor.assertHoverContains("name", "Name of a release used in the deployment");
		editor.assertHoverContains("version", "The version of the release to use");
		editor.assertHoverContains("url", "URL of the release to use");
		editor.assertHoverContains("sha1", "The SHA1 of the release tarball");

		editor = harness.newEditor(
				"releases:\n" +
				"- name: some-release\n" +
				"  version: <*>\n"
		);
		editor.assertCompletionLabels("latest");
	}

	@Test public void instanceGroupsCompletions() throws Exception {
		harness.getServer().enableSnippets(false);
		Editor editor = harness.newEditor(
				"instance_groups:\n" +
				"- <*>"
		);
		editor.assertCompletions(
				"instance_groups:\n" +
				"- name: <*>"
		);

		editor = harness.newEditor(
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  <*>"
		);
		editor.assertCompletions(PLAIN_COMPLETION,
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  azs:\n" +
				"  - <*>"
				, // =============
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  env:\n" +
				"    <*>"
				, // =============
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  instances: <*>"
				, // =============
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  jobs:\n" +
				"  - <*>"
				, // =============
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  lifecycle: <*>"
				, // =============
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  migrated_from:\n" +
				"  - <*>"
				, // =============
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  networks:\n" +
				"  - name: <*>"
				, // =============
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  persistent_disk_type: <*>"
				, // =============
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  stemcell: <*>"
				, // =============
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  update:\n" +
				"    <*>"
				, // =============
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  vm_extensions:\n" +
				"  - <*>"
				, // =============
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  vm_type: <*>"

//				, // =============
// Not suggested because its deprecated:
//				"instance_groups:\n" +
//				"- name: foo-group\n" +
//				"  properties:\n" +
//				"    <*>"
		);
	}

	@Test public void instanceGroupsHovers() throws Exception {
		Editor editor = harness.newEditor(
				"instance_groups:\n" +
				"- name: redis-master\n" +
				"  properties: {}\n" +
				"  instances: 1\n" +
				"  azs: [z1, z2]\n" +
				"  jobs:\n" +
				"  - name: redis-server\n" +
				"    release: redis\n" +
				"    properties:\n" +
				"      port: 3606\n" +
				"  vm_type: medium\n" +
				"  vm_extensions: [public-lbs]\n" +
				"  stemcell: default\n" +
				"  persistent_disk_type: medium\n" +
				"  networks:\n" +
				"  - name: default\n" +
				"\n" +
				"- name: redis-slave\n" +
				"  instances: 2\n" +
				"  azs: [z1, z2]\n" +
				"  jobs:\n" +
				"  - name: redis-server\n" +
				"    release: redis\n" +
				"    properties: {}\n" +
				"  update:\n" +
				"    canaries: 2\n" +
				"  lifecycle: errand\n" +
				"  migrated_from: []\n" +
				"  env: {}\n" +
				"  vm_type: medium\n" +
				"  stemcell: default\n" +
				"  persistent_disk_type: medium\n" +
				"  networks:\n" +
				"  - name: default\n"
		);
		editor.assertHoverContains("name", "A unique name used to identify and reference the instance group.");
		editor.assertHoverContains("azs", "List of AZs associated with this instance group");
		editor.assertHoverContains("instances", "The number of instances in this group");
		editor.assertHoverContains("jobs", "Specifies the name and release of jobs that will be installed on each instance.");
		editor.assertHoverContains("vm_type", "A valid VM type name from the cloud config");
		editor.assertHoverContains("vm_extensions", "A valid list of VM extension names from the cloud config");
		editor.assertHoverContains("stemcell", "A valid stemcell alias from the Stemcells Block");
		editor.assertHoverContains("persistent_disk_type", "A valid disk type name from the cloud config.");
		editor.assertHoverContains("networks", "Specifies the networks this instance requires");
		editor.assertHoverContains("update", "Specific update settings for this instance group");
		editor.assertHoverContains("migrated_from", "Specific migration settings for this instance group.");
		editor.assertHoverContains("lifecycle", "Specifies the kind of workload");
		editor.assertHoverContains("properties", "Specifies instance group properties");
		editor.assertHoverContains("env", "Specifies advanced BOSH Agent configuration");
	}

	@Test public void instanceGroups_job_hovers() throws Exception {
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"instance_groups:\n" +
				"- name: foo\n" +
				"  jobs:\n" +
				"  - name: the-job\n" +
				"    release: the-jobs-release\n" +
				"    properties:\n" +
				"      blah: blah\n" +
				"    consumes:\n" +
				"      blah: blah \n" +
				"    provides:\n" +
				"      blah: blah\n"
		);
		editor.assertHoverContains("name", 3, "The job name");
		editor.assertHoverContains("release", "The release where the job exists");
		editor.assertHoverContains("consumes", "Links consumed by the job");
		editor.assertHoverContains("provides", "Links provided by the job");
		editor.assertHoverContains("properties", "Specifies job properties");
	}

	@Test public void instanceGroups_network_hovers() throws Exception {
		Editor editor = harness.newEditor(
			"name: foo\n" +
			"instance_groups:\n" +
			"- name: foo\n" +
			"  networks:\n" +
			"  - name: the-network\n" +
			"    static_ips: []\n" +
			"    default: []\n"
		);
		editor.assertHoverContains("name", 3, "A valid network name from the cloud config");
		editor.assertHoverContains("static_ips", "Array of IP addresses");
		editor.assertHoverContains("default", "Specifies which network components");
	}

	@Test public void instanceGroups_env_hovers() throws Exception {
		Editor editor = harness.newEditor(
			"name: foo\n" +
			"instance_groups:\n" +
			"- name: foo\n" +
			"  env:\n" +
			"    bosh: {}\n" +
			"    password: []\n"
		);
		editor.assertHoverContains("bosh", "no description");
		editor.assertHoverContains("password", "Crypted password");
	}

	@Test public void updateBlockCompletions() throws Exception {
		harness.getServer().enableSnippets(false);
		Editor editor = harness.newEditor(
				"update:\n" +
				"  <*>"
		);
		editor.assertCompletions(PLAIN_COMPLETION,
				"update:\n" +
				"  canaries: <*>"
				, // =====
				"update:\n" +
				"  canary_watch_time: <*>"
				, // =====
				"update:\n" +
				"  max_in_flight: <*>"
				, // =====
				"update:\n" +
				"  serial: <*>"
				, // =====
				"update:\n" +
				"  update_watch_time: <*>"
		);
	}

	@Test public void updateBlockHovers() throws Exception {
		Editor editor = harness.newEditor(
				"update:\n" +
				"  canaries: 1\n" +
				"  max_in_flight: 10\n" +
				"  canary_watch_time: 1000-30000\n" +
				"  update_watch_time: 1000-30000\n" +
				"  serial: false"
		);
		editor.assertHoverContains("canaries", "The number of [canary]");
		editor.assertHoverContains("max_in_flight", "maximum number of non-canary instances");
		editor.assertHoverContains("canary_watch_time", "checks whether the canary instances");
		editor.assertHoverContains("update_watch_time", "checks whether the instances");
		editor.assertHoverContains("serial", "deployed in parallel");
	}

	@Test public void variablesBlockCompletions() throws Exception {
		harness.getServer().enableSnippets(false);
		Editor editor = harness.newEditor(
				"variables:\n" +
				"- <*>"
		);
		editor.assertCompletions(
				"variables:\n" +
				"- name: <*>"
		);

		editor = harness.newEditor(
				"variables:\n" +
				"- name: foo\n" +
				"  <*>"
		);
		editor.assertCompletions(PLAIN_COMPLETION,
				"variables:\n" +
				"- name: foo\n" +
				"  options:\n" +
				"    <*>"
				, // ===============
				"variables:\n" +
				"- name: foo\n" +
				"  type: <*>"
		);

		editor = harness.newEditor(
				"variables:\n" +
				"- name: foo\n" +
				"  type: <*>"
		);
		editor.assertCompletionLabels("certificate", "password", "rsa", "ssh");
	}

	@Test public void variablesBlockReconciling() throws Exception {
		Editor editor = harness.newEditor(
				"variables:\n" +
				"- name: admin-passcode\n" +
				"  type: something-that-might-work-in-theory\n" + //shouldn't be a warning/error
				"  bogus-propt: bah\n" +
				"  options: {}"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems(
				"bogus-propt|Unknown property"
		);
	}

	@Test public void variablesBlockHovers() throws Exception {
		Editor editor = harness.newEditor(
				"variables:\n" +
				"- name: admin_password\n" +
				"  type: password\n" +
				"- name: default_ca\n" +
				"  type: certificate\n" +
				"  options:\n" +
				"    is_ca: true\n" +
				"    common_name: some-ca\n" +
				"- name: director_ssl\n" +
				"  type: certificate\n" +
				"  options:\n" +
				"    ca: default_ca\n" +
				"    common_name: cc.cf.internal\n" +
				"    alternative_names: [cc.cf.internal]"
		);
		editor.assertHoverContains("name", "Unique name used to identify a variable");
		editor.assertHoverContains("type", "Type of a variable");
		editor.assertHoverContains("options", "Specifies generation options");
	}

	@Test public void tolerateV1Manifests() throws Exception {
		Editor editor = harness.newEditor(
				"---\n" +
				"name: my-redis-deployment\n" +
				"director_uuid: 1234abcd-5678-efab-9012-3456cdef7890\n" +
				"\n" +
				"releases:\n" +
				"- {name: redis, version: 12}\n" +
				"\n" +
				"resource_pools:\n" +
				"- name: redis-servers\n" +
				"  network: default\n" +
				"  stemcell:\n" +
				"    name: bosh-aws-xen-ubuntu-trusty-go_agent\n" +
				"    version: 2708\n" +
				"  cloud_properties:\n" +
				"    instance_type: m1.small\n" +
				"    availability_zone: us-east-1c\n" +
				"\n" +
				"disk_pools: []\n" +
				"\n" +
				"networks:\n" +
				"- name: default\n" +
				"  type: manual\n" +
				"  subnets:\n" +
				"  - range: 10.10.0.0/24\n" +
				"    gateway: 10.10.0.1\n" +
				"    static:\n" +
				"    - 10.10.0.16 - 10.10.0.18\n" +
				"    reserved:\n" +
				"    - 10.10.0.2 - 10.10.0.15\n" +
				"    dns: [10.10.0.6]\n" +
				"    cloud_properties:\n" +
				"      subnet: subnet-d597b993\n" +
				"\n" +
				"compilation:\n" +
				"  workers: 2\n" +
				"  network: default\n" +
				"  reuse_compilation_vms: true\n" +
				"  cloud_properties:\n" +
				"    instance_type: c1.medium\n" +
				"    availability_zone: us-east-1c\n" +
				"\n" +
				"update:\n" +
				"  canaries: 1\n" +
				"  max_in_flight: 3\n" +
				"  canary_watch_time: 15000-30000\n" +
				"  update_watch_time: 15000-300000\n" +
				"\n" +
				"jobs:\n" +
				"- name: redis-master\n" +
				"  instances: 1\n" +
				"  templates:\n" +
				"  - {name: redis-server, release: redis}\n" +
				"  persistent_disk: 10_240\n" +
				"  resource_pool: redis-servers\n" +
				"  networks:\n" +
				"  - name: default\n" +
				"\n" +
				"- name: redis-slave\n" +
				"  instances: 2\n" +
				"  templates:\n" +
				"  - {name: redis-server, release: redis}\n" +
				"  persistent_disk: 10_240\n" +
				"  resource_pool: redis-servers\n" +
				"  networks:\n" +
				"  - name: default\n" +
				"\n" +
				"properties:\n" +
				"  redis:\n" +
				"    max_connections: 10\n" +
				"\n" +
				"cloud_provider: {}"
		);
		editor.ignoreProblem(YamlSchemaProblems.DEPRECATED_PROPERTY);
		editor.assertProblems(/*NONE*/);

		editor = harness.newEditor(
				"name: foo\n" +
				"director_uuid: dca5480a-6b0e-11e7-907b-a6006ad3dba0\n" +
				"networks: {}" //This makes it a V1 schema
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems(
				"director_uuid|bosh v2 CLI no longer checks or requires",
				"networks|Deprecated: 'networks' is a V1 schema property"
		);
	}

	@Test public void documentSymbols() throws Exception {
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"variables:\n" +
				"- name: blobstore_admin_users_password\n" +
				"  type: password\n" +
				"- name: blobstore_secure_link_secret\n" +
				"  type: password\n" +
				"stemcells:\n" +
				"- alias: default\n" +
				"  os: ubuntu-trusty\n" +
				"  version: '3421.11'\n" +
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  networks:\n" +
				"  - name: the-network\n" +
				"    static_ips: []\n" +
				"    default: []\n" +
				"- name: bar-group\n" +
				"  networks:\n" +
				"  - name: the-network\n" +
				"    static_ips: []\n" +
				"    default: []\n" +
				"releases:\n" +
				"- name: one-release\n" +
				"- name: other-release\n"
		);
		editor.assertDocumentSymbols(
				"default|StemcellAlias",
				"foo-group|InstanceGroup",
				"bar-group|InstanceGroup",
				"one-release|Release",
				"other-release|Release",
				"blobstore_admin_users_password|Variable",
				"blobstore_secure_link_secret|Variable"
		);
	}

	@Test public void duplicateSymbolChecking() throws Exception {
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"variables:\n" +
				"- name: dup_var\n" +
				"  type: password\n" +
				"- name: blobstore_admin_users_password\n" +
				"  type: password\n" +
				"- name: blobstore_secure_link_secret\n" +
				"  type: password\n" +
				"- name: dup_var\n" +
				"  type: ssh\n" +
				"stemcells:\n" +
				"- alias: default\n" +
				"  os: ubuntu-trusty\n" +
				"  version: '3421.11'\n" +
				"- alias: dup_cell\n" +
				"  os: ubuntu-trusty\n" +
				"  version: '3421.11'\n" +
				"- alias: dup_cell\n" +
				"  os: ubuntu-trusty\n" +
				"  version: '3421.11'\n" +
				"instance_groups:\n" +
				"- name: foo-group\n" +
				"  networks:\n" +
				"  - name: default\n" +
				"    static_ips: []\n" +
				"    default: []\n" +
				"- name: bar-group\n" +
				"  networks:\n" +
				"  - name: default\n" +
				"    static_ips: []\n" +
				"    default: []\n" +
				"- name: bar-group\n" +
				"  networks:\n" +
				"  - name: default\n" +
				"    static_ips: []\n" +
				"    default: []\n" +
				"releases:\n" +
				"- name: one-release\n" +
				"- name: other-release\n" +
				"- name: one-release\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);

		editor.assertProblems(
				"dup_var|Duplicate 'VariableName'",
				"dup_var|Duplicate 'VariableName'",
				"dup_cell|Duplicate 'StemcellAlias'",
				"dup_cell|Duplicate 'StemcellAlias'",
				"bar-group|Duplicate 'InstanceGroupName'",
				"bar-group|Duplicate 'InstanceGroupName'",
				"one-release|Duplicate 'ReleaseName'",
				"one-release|Duplicate 'ReleaseName'"
		);
	}

	@Test public void contentAssistReleaseReference() throws Exception {
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"instance_groups: \n" +
				"- name: some-server\n" +
				"  jobs:\n" +
				"  - release: <*>\n" +
				"releases: \n" +
				"- name: some-release\n" +
				"  url: https://release-hub.info/some-release.tar.gz?version=99.3.2\n" +
				"  sha1: asddsfsd\n" +
				"- name: other-release\n" +
				"  url: https://release-hub.info/other-release.tar.gz?version=99.3.2\n" +
				"  sha1: asddsfsd\n"
		);
		editor.assertContextualCompletions("<*>"
				, // ==>
				"other-release<*>", "some-release<*>"
		);
	}

	@Test public void reconcileReleaseReference() throws Exception {
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"instance_groups: \n" +
				"- name: some-server\n" +
				"  jobs:\n" +
				"  - release: some-release\n" +
				"- name: some-other-server\n" +
				"  jobs:\n" +
				"  - release: bogus-release\n" +
				"releases: \n" +
				"- name: some-release\n" +
				"  url: https://release-hub.info/some-release.tar.gz?version=99.3.2\n" +
				"  sha1: asddsfsd\n" +
				"- name: other-release\n" +
				"  url: https://release-hub.info/other-release.tar.gz?version=99.3.2\n" +
				"  sha1: asddsfsd\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems(
				"bogus-release|unknown 'ReleaseName'. Valid values are: [other-release, some-release]"
		);
	}

	@Test public void contentAssistStemcellReference() throws Exception {
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"instance_groups: \n" +
				"- name: some-server\n" +
				"  stemcell: <*>\n" +
				"stemcells:\n" +
				"- alias: default\n" +
				"  os: ubuntu\n" +
				"  version: 1346.77.1\n" +
				"- alias: windoze\n" +
				"  os: windows\n" +
				"  version: 678.9.1\n"
		);
		editor.assertContextualCompletions("<*>"
				, // ==>
				"default<*>", "windoze<*>"
		);
	}

	@Test public void reconcileStemcellReference() throws Exception {
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"instance_groups: \n" +
				"- name: some-server\n" +
				"  stemcell: default\n" +
				"- name: windoze-server\n" +
				"  stemcell: windoze\n" +
				"- name: bad-server\n" +
				"  stemcell: bogus-stemcell\n" +
				"stemcells:\n" +
				"- alias: default\n" +
				"  os: ubuntu\n" +
				"  version: 1346.77.1\n" +
				"- alias: windoze\n" +
				"  os: windows\n" +
				"  version: 678.9.1\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems(
				"bogus-stemcell|unknown 'StemcellAlias'. Valid values are: [default, windoze]"
		);
	}

	@Test public void dynamicStemcellNamesFromDirector() throws Exception {
		StemcellsModel stemcellsModel = mock(StemcellsModel.class);
		when(stemcellsProvider.getModel(any())).thenReturn(stemcellsModel);
		when(stemcellsModel.getStemcellNames()).thenReturn(ImmutableMultiset.of(
				"bosh-vsphere-esxi-centos-7-go_agent",
				"bosh-vsphere-esxi-ubuntu-trusty-go_agent"
		));

		//content assist
		Editor editor = harness.newEditor(
				"stemcells:\n" +
				"- alias: blah\n" +
				"  name: <*>"
		);
		editor.assertContextualCompletions("<*>",
				"bosh-vsphere-esxi-centos-7-go_agent<*>",
				"bosh-vsphere-esxi-ubuntu-trusty-go_agent<*>"
		);

		//reconcile
		editor = harness.newEditor(
				"stemcells:\n" +
				"- alias: good\n" +
				"  name: bosh-vsphere-esxi-centos-7-go_agent\n" +
				"- alias: not-so-good\n" +
				"  name: bogus<*>"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems(
				"bogus|unknown 'StemcellName'. Valid values are: [bosh-vsphere-esxi-centos-7-go_agent, bosh-vsphere-esxi-ubuntu-trusty-go_agent]"
		);
	}

	@Test public void dynamicStemcellOssFromDirector() throws Exception {
		StemcellsModel stemcellsModel = mock(StemcellsModel.class);
		when(stemcellsProvider.getModel(any())).thenReturn(stemcellsModel);
		when(stemcellsModel.getStemcellOss()).thenReturn(ImmutableSet.of("ubuntu", "centos"));

		//content assist
		Editor editor = harness.newEditor(
				"stemcells:\n" +
				"- os: <*>"
		);
		editor.assertContextualCompletions("<*>",
				"centos<*>",
				"ubuntu<*>"
		);

		//reconcile
		editor = harness.newEditor(
				"stemcells:\n" +
				"- alias: good\n" +
				"  os: ubuntu\n" +
				"- alias: not-so-good\n" +
				"  os: bogus<*>"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems(
				"bogus|unknown 'StemcellOs'. Valid values are: [ubuntu, centos]"
		);
	}

	@Test public void contentAssistStemcellNameNoDirector() throws Exception {
		Editor editor;
		stemcellsProvider = mock(DynamicModelProvider.class);
		when(stemcellsProvider.getModel(any())).thenThrow(new IOException("Couldn't connect to bosh"));
		editor = harness.newEditor(
				"stemcells:\n" +
				"- alias: good\n" +
				"  name: <*>\n"
		);
		List<CompletionItem> completions = editor.getCompletions();
		assertEquals(1, completions.size());
		CompletionItem c = completions.get(0);
		c = harness.resolveCompletionItem(c);
		assertContains("Couldn't connect to bosh", c.getDocumentation());
		System.out.println("label  = " + c.getLabel());
		System.out.println("detail = " + c.getDetail());
		System.out.println("doc    = " + c.getDocumentation());
	}

	@SuppressWarnings("unchecked")
	@Test public void contentAssistStemcellVersionNoDirector() throws Exception {
		Editor editor;
		stemcellsProvider = mock(DynamicModelProvider.class);
		when(stemcellsProvider.getModel(any())).thenThrow(new IOException("Couldn't connect to bosh"));
		editor = harness.newEditor(
				"stemcells:\n" +
				"- alias: good\n" +
				"  name: centos-agent\n" +
				"  version: <*>\n"
		);
		editor.assertContextualCompletions("<*>",
				"latest<*>"
		);
	}

	@Test public void contentAssistStemcellVersionFromDirector() throws Exception {
		Editor editor;
		stemcellsProvider = provideStemcellsFrom(
				new StemcellData("ubuntu-agent", "123.4", "ubuntu"),
				new StemcellData("ubuntu-agent", "222.2", "ubuntu"),
				new StemcellData("centos-agent", "222.2", "centos"),
				new StemcellData("centos-agent", "333.3", "centos")
		);

		editor = harness.newEditor(
				"stemcells:\n" +
				"- alias: good\n" +
				"  name: centos-agent\n" +
				"  version: <*>\n"
		);
		editor.assertContextualCompletions("<*>",
				"222.2<*>", "333.3<*>", "latest<*>"
		);

		editor = harness.newEditor(
				"stemcells:\n" +
				"- alias: good\n" +
				"  os: ubuntu\n" +
				"  version: <*>\n"
		);
		editor.assertContextualCompletions("<*>",
				"123.4<*>", "222.2<*>", "latest<*>"
		);

		editor = harness.newEditor(
				"stemcells:\n" +
				"- alias: good\n" +
				"  version: <*>\n"
		);
		editor.assertContextualCompletions("<*>",
				"123.4<*>", "222.2<*>", "333.3<*>", "latest<*>"
		);

		//when os and name are 'bogus' at least suggest proposals based on other prop
		editor = harness.newEditor(
				"stemcells:\n" +
				"- alias: good\n" +
				"  name: centos-agent\n" +
				"  os: bogus\n" +
				"  version: <*>\n"
		);
		editor.assertContextualCompletions("<*>",
				"222.2<*>", "333.3<*>", "latest<*>"
		);

		editor = harness.newEditor(
				"stemcells:\n" +
				"- alias: good\n" +
				"  os: centos\n" +
				"  name: bogus\n" +
				"  version: <*>\n"
		);
		editor.assertContextualCompletions("<*>",
				"222.2<*>", "333.3<*>", "latest<*>"
		);

		//when the os and name disagree, merge the proposals for both:
		editor = harness.newEditor(
				"stemcells:\n" +
				"- alias: good\n" +
				"  os: centos\n" + //Contradicts the name
				"  name: ubuntu-agent\n" + //Contradicts the os
				"  version: <*>\n"
		);
		editor.assertContextualCompletions("<*>",
				"123.4<*>", "222.2<*>", "333.3<*>", "latest<*>"
		);
	}

	@SuppressWarnings("unchecked")
	@Test public void reconcileStemcellVersionNoDirector() throws Exception {
		Editor editor;
		stemcellsProvider = mock(DynamicModelProvider.class);
		when(stemcellsProvider.getModel(any())).thenThrow(new IOException("Couldn't connect to bosh"));
		editor = harness.newEditor(
				"stemcells:\n" +
				"- alias: aaa\n" +
				"  name: centos-agent\n" +
				"  version: 222.2\n" +
				"- alias: bbb\n" +
				"  name: centos-agent\n" +
				"  version: 123.4\n" +
				"- alias: ddd\n" +
				"  os: ubuntu\n" +
				"  version: 333.3\n" +
				"- alias: eee\n" +
				"  os: ubuntu\n" +
				"  version: latest\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems(/*NONE*/);
	}

	@Test public void reconcileStemcellVersionFromDirector() throws Exception {
		Editor editor;
		stemcellsProvider = provideStemcellsFrom(
				new StemcellData("ubuntu-agent", "123.4", "ubuntu"),
				new StemcellData("ubuntu-agent", "222.2", "ubuntu"),
				new StemcellData("centos-agent", "222.2", "centos"),
				new StemcellData("centos-agent", "333.3", "centos")
		);

		editor = harness.newEditor(
				"stemcells:\n" +
				"- alias: aaa\n" +
				"  name: centos-agent\n" +
				"  version: 222.2\n" +
				"- alias: bbb\n" +
				"  name: centos-agent\n" +
				"  version: 123.4\n" +
				"- alias: ddd\n" +
				"  os: ubuntu\n" +
				"  version: 333.3\n" +
				"- alias: eee\n" +
				"  os: ubuntu\n" +
				"  version: latest\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems(
				"123.4|unknown 'StemcellVersion[name=centos-agent]'. Valid values are: [222.2, 333.3, latest]",
				"333.3|unknown 'StemcellVersion[os=ubuntu]'. Valid values are: [123.4, 222.2, latest]"
		);
	}

	private DynamicModelProvider<StemcellsModel> provideStemcellsFrom(StemcellData... stemcellData) {
		return new BoshCommandStemcellsProvider(cliConfig) {
			@Override
			protected String executeCommand(ExternalCommand command) throws Exception {
				String rows = mapper.writeValueAsString(stemcellData);
				return "{\n" +
						"    \"Tables\": [\n" +
						"        {\n" +
						"            \"Rows\": " + rows +
						"        }\n" +
						"    ]\n" +
						"}";
			}
		};
	}

	@Test public void contentAssistReleaseNameDef() throws Exception {
		releasesProvider = provideReleasesFrom(
				new ReleaseData("foo", "123.4"),
				new ReleaseData("foo", "222.2"),
				new ReleaseData("bar", "222.2"),
				new ReleaseData("bar", "333.3")
		);

		Editor editor = harness.newEditor(
				"releases:\n" +
				"- name: <*>"
		);
		editor.assertContextualCompletions("<*>",
				"bar<*>",
				"foo<*>"
		);
	}

	@Test public void reconcileReleaseNameDef() throws Exception {
		releasesProvider = provideReleasesFrom(
				new ReleaseData("foo", "123.4"),
				new ReleaseData("foo", "222.2"),
				new ReleaseData("bar", "222.2"),
				new ReleaseData("bar", "333.3")
		);

		Editor editor = harness.newEditor(
				"releases:\n" +
				"- name: foo\n" +
				"- name: bar\n" +
				"- name: bogus\n" +
				"- name: url-makes-this-ok\n" +
				"  url: file://blah"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems("bogus|unknown 'ReleaseName'. Valid values are: [foo, bar]");
	}

	@Test public void contentAssistReleaseVersion() throws Exception {
		releasesProvider = provideReleasesFrom(
				new ReleaseData("foo", "123.4"),
				new ReleaseData("foo", "222.2"),
				new ReleaseData("bar", "222.2"),
				new ReleaseData("bar", "333.3")
		);

		Editor editor = harness.newEditor(
				"releases:\n" +
				"- version: <*>"
		);
		editor.assertContextualCompletions("<*>",
				"123.4<*>",
				"222.2<*>",
				"333.3<*>",
				"latest<*>"
		);

		editor = harness.newEditor(
				"releases:\n" +
				"- name: foo\n" +
				"  version: <*>"
		);
		editor.assertContextualCompletions("<*>",
				"123.4<*>",
				"222.2<*>",
				"latest<*>"
		);

		//Still get all suggestions even when 'url' property is added
		editor = harness.newEditor(
				"releases:\n" +
				"- version: <*>\n" +
				"  url: blah"
		);
		editor.assertContextualCompletions("<*>",
				"123.4<*>",
				"222.2<*>",
				"333.3<*>",
				"latest<*>"
		);

		editor = harness.newEditor(
				"releases:\n" +
				"- name: foo\n" +
				"  url: blah\n" +
				"  version: <*>"
		);
		editor.assertContextualCompletions("<*>",
				"123.4<*>",
				"222.2<*>",
				"333.3<*>",
				"latest<*>"
		);

	}

	@Test public void reconcileReleaseVersion() throws Exception {
		Editor editor;
		releasesProvider = provideReleasesFrom(
				new ReleaseData("foo", "123.4"),
				new ReleaseData("foo", "222.2"),
				new ReleaseData("bar", "222.2"),
				new ReleaseData("bar", "333.3")
		);

		editor = harness.newEditor(
				"releases:\n" +
				"- version: bogus\n" +
				"- version: url-makes-this-possibly-correct\n" +
				"  url: file:///relesease-folder/blah-release.tar.gz\n"+
				"- name: bar\n" +
				"  version: url-makes-this-also-possibly-correct\n" +
				"  url: file:///relesease-folder/other-release.tar.gz"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems("bogus|unknown 'ReleaseVersion'. Valid values are: [123.4, 222.2, 333.3, latest]");

		editor = harness.newEditor(
				"releases:\n" +
				"- version: 123.4\n" +
				"- version: latest\n" +
				"- version: bogus\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems("bogus|unknown 'ReleaseVersion'. Valid values are: [123.4, 222.2, 333.3, latest]");

		editor = harness.newEditor(
				"releases:\n" +
				"- name: foo\n" +
				"  version: 123.4\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems(/*NONE*/);

		editor = harness.newEditor(
				"releases:\n" +
				"- name: foo\n" +
				"  version: 222.2\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems(/*NONE*/);

		editor = harness.newEditor(
				"releases:\n" +
				"- name: foo\n" +
				"  version: 333.3\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems("333.3|unknown 'ReleaseVersion[name=foo]'. Valid values are: [123.4, 222.2, latest]");

	}


	private DynamicModelProvider<ReleasesModel> provideReleasesFrom(ReleaseData... stemcellData) {
		return new BoshCommandReleasesProvider(cliConfig) {
			@Override
			protected String executeCommand(ExternalCommand command) throws Exception {
				String rows = mapper.writeValueAsString(stemcellData);
				return "{\n" +
						"    \"Tables\": [\n" +
						"        {\n" +
						"            \"Rows\": " + rows +
						"        }\n" +
						"    ]\n" +
						"}";
			}
		};
	}

	@Test public void contentAssistVMtype() throws Exception {
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"instance_groups: \n" +
				"- name: some-server\n" +
				"  stemcell: windoze\n" +
				"  vm_type: <*>"
		);
		editor.assertContextualCompletions(
				"<*>"
				, // ==>
				"default<*>",
				"large<*>"
		);

		//Verify that the cache is working. Shouldn't read the cc provier more than once, evem for multiple CA requests.
		editor.assertCompletionLabels("default", "large");
		editor.assertCompletionLabels("default", "large");
		assertEquals(1, cloudConfigProvider.getReadCount());
	}

	@Test public void reconcileVMtype() throws Exception {
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"instance_groups: \n" +
				"- name: some-server\n" +
				"  vm_type: bogus-vm\n" +
				"- name: other-server\n" +
				"  vm_type: large"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems(
				"bogus-vm|unknown 'VMType'. Valid values are: [default, large]"
		);
		assertEquals(1, cloudConfigProvider.getReadCount());
	}

	@Test public void reconcileVMTypeWhenCloudConfigUnavailable() throws Exception {
		cloudConfigProvider.executeCommandWith(() -> null);
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"instance_groups: \n" +
				"- name: some-server\n" +
				"  vm_type: bogus-vm\n" +
				"- name: other-server\n" +
				"  vm_type: large"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems(/*NONE*/); //Should not complain about unknown vm_types, if we can't determine what valid vm_types actually exist.
	}

	@Test public void reconcileVMTypeWhenCloudConfigThrows() throws Exception {
		cloudConfigProvider.executeCommandWith(() -> { throw new TimeoutException("Reading cloud config timed out"); });
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"instance_groups: \n" +
				"- name: some-server\n" +
				"  vm_type: bogus-vm\n" +
				"- name: other-server\n" +
				"  vm_type: large"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems(/*NONE*/); //Should not complain about unknown vm_types, if we can't determine what valid vm_types actually exist.
	}

	@Test public void contentAssistShowsWarningWhenCloudConfigThrows() throws Exception {
		cloudConfigProvider.executeCommandWith(() -> {
			throw new TimeoutException("Reading cloud config timed out");
		});
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"instance_groups: \n" +
				"- name: some-server\n" +
				"  vm_type: <*>"
		);
		CompletionItem completion = editor.assertCompletionLabels("TimeoutException").get(0);
		completion = harness.resolveCompletionItem(completion);
		assertContains("Reading cloud config timed out", completion.getDocumentation());
	}

	@Test public void reconcileNetworkName() throws Exception {
		Editor editor = harness.newEditor(
				"name: my-first-deployment\n" +
				"instance_groups:\n" +
				"- name: my-server\n" +
				"  networks:\n" +
				"  - name: default\n" +
				"  - name: bogus-nw\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems("bogus-nw|unknown 'NetworkName'. Valid values are: [default]");
	}

	@Test public void reconcileNetworkName2() throws Exception {
		cloudConfigProvider.readWith(() ->
			"networks:\n" +
			"- name: public-nw\n" +
			"- name: local-nw"
		);

		Editor editor = harness.newEditor(
				"name: my-first-deployment\n" +
				"instance_groups:\n" +
				"- name: my-server\n" +
				"  networks:\n" +
				"  - name: public-nw\n" +
				"  - name: local-nw\n" +
				"  - name: bogus-nw\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems("bogus-nw|unknown 'NetworkName'. Valid values are: [public-nw, local-nw]");
	}

	@Test public void contentAssistNetworkName() throws Exception {
		cloudConfigProvider.readWith(() ->
			"networks:\n" +
			"- name: public-nw\n" +
			"- name: local-nw"
		);

		Editor editor = harness.newEditor(
				"name: my-first-deployment\n" +
				"instance_groups:\n" +
				"- name: my-server\n" +
				"  networks:\n" +
				"  - name: <*>"
		);
		editor.assertContextualCompletions("<*>",
				"local-nw<*>", "public-nw<*>"
		);
	}

	@Test public void reconcileAZs() throws Exception {
		Editor editor = harness.newEditor(
				"name: my-first-deployment\n" +
				"instance_groups:\n" +
				"- name: my-server\n" +
				"  azs: [z1, z2, z-bogus]\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems("z-bogus|unknown 'AvailabilityZone'. Valid values are: [z1, z2, z3]");
	}

	@Test public void contentAssistAZ() throws Exception {
		Editor editor = harness.newEditor(
				"name: my-first-deployment\n" +
				"instance_groups:\n" +
				"- name: my-server\n" +
				"  azs:\n" +
				"  - <*>"
		);
		editor.assertContextualCompletions("<*>",
				"z1<*>", "z2<*>", "z3<*>"
		);
	}

	@Test public void reconcilePersistentDiskType() throws Exception {
		Editor editor = harness.newEditor(
				"name: my-first-deployment\n" +
				"instance_groups:\n" +
				"- name: my-server1\n" +
				"  persistent_disk_type: default\n" +
				"- name: my-server2\n" +
				"  persistent_disk_type: bogus\n" +
				"- name: my-server3\n" +
				"  persistent_disk_type: large\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems("bogus|unknown 'DiskType'. Valid values are: [default, large]");
	}

	@Test public void reconcilePersistentDiskType2() throws Exception {
		cloudConfigProvider.readWith(() ->
			"disk_types:\n" +
			"- name: small-disk\n" +
			"- name: large-disk\n"
		);
		Editor editor = harness.newEditor(
				"name: my-first-deployment\n" +
				"instance_groups:\n" +
				"- name: my-server1\n" +
				"  persistent_disk_type: small-disk\n" +
				"- name: my-server2\n" +
				"  persistent_disk_type: bogus\n" +
				"- name: my-server3\n" +
				"  persistent_disk_type: large-disk\n"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems("bogus|unknown 'DiskType'. Valid values are: [small-disk, large-disk]");
	}

	@Test public void reconcileVMExtensions() throws Exception {
		Editor editor = harness.newEditor(
				"name: my-first-deployment\n" +
				"instance_groups:\n" +
				"- name: my-server\n" +
				"  vm_extensions:\n" +
				"  - blah"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems("blah|unknown 'VMExtension'. Valid values are: []");
	}

	@Test public void reconcileVMExtensions2() throws Exception {
		cloudConfigProvider.readWith(() ->
			"vm_extensions:\n" +
			"- name: pub-lbs\n" +
			"  cloud_properties:\n" +
			"    elbs: [main]"
		);
		Editor editor = harness.newEditor(
				"name: my-first-deployment\n" +
				"instance_groups:\n" +
				"- name: my-server\n" +
				"  vm_extensions:\n" +
				"  - pub-lbs\n" +
				"  - bogus"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);
		editor.assertProblems("bogus|unknown 'VMExtension'. Valid values are: [pub-lbs]");
	}

	@Test public void gotoReleaseDefinition() throws Exception {
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"instance_groups: \n" +
				"- name: some-server\n" +
				"  jobs:\n" +
				"  - release: some-release\n" +
				"- name: some-other-server\n" +
				"  jobs:\n" +
				"  - release: bogus-release\n" +
				"releases:\n" +
				"- name: some-release\n" +
				"  url: https://release-hub.info/some-release.tar.gz?version=99.3.2\n" +
				"  sha1: asddsfsd\n" +
				"- name: other-release\n" +
				"  url: https://release-hub.info/other-release.tar.gz?version=99.3.2\n" +
				"  sha1: asddsfsd\n"
		);

		editor.assertGotoDefinition(editor.positionOf("some-release"),
			editor.rangeOf(
				"releases:\n" +
				"- name: some-release\n"
				,
				"some-release"
			)
		);
	}

	@Test public void gotoStemcellDefinition() throws Exception {
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"instance_groups: \n" +
				"- name: some-server\n" +
				"  stemcell: default\n" +
				"- name: windoze-server\n" +
				"  stemcell: windoze\n" +
				"- name: bad-server\n" +
				"  stemcell: bogus-stemcell\n" +
				"stemcells:\n" +
				"- alias: default\n" +
				"  os: ubuntu\n" +
				"  version: 1346.77.1\n" +
				"- alias: windoze\n" +
				"  os: windows\n" +
				"  version: 678.9.1\n"
		);

		editor.assertGotoDefinition(
			editor.positionOf("stemcell: windoze", "windoze"),
			editor.rangeOf("- alias: windoze", "windoze")
		);
	}

	@Test public void bug_149769913() throws Exception {
		Editor editor = harness.newEditor(
				"releases:\n" +
				"- name: learn-bosh\n" +
				"  url: file:///blah\n" +
				"  version:\n" +
				"- name: blah-blah\n" +
				"  version:"
		);
		editor.ignoreProblem(YamlSchemaProblems.MISSING_PROPERTY);

		editor.assertProblems(
				"version:^^|cannot be blank",
				"version:^^|cannot be blank"
		);
	}

	@Test public void snippet_toplevel() throws Exception {
		Editor editor = harness.newEditor("<*>");
		editor.assertCompletions(SNIPPET_COMPLETION,
				"name: $1\n" +
				"releases:\n" +
				"- name: $2\n" +
				"  version: $3\n" +
				"stemcells:\n" +
				"- alias: $4\n" +
				"  version: $5\n" +
				"update:\n" +
				"  canaries: $6\n" +
				"  max_in_flight: $7\n" +
				"  canary_watch_time: $8\n" +
				"  update_watch_time: $9\n" +
				"instance_groups:\n" +
				"- name: $10\n" +
				"  azs:\n" +
				"  - $11\n" +
				"  instances: $12\n" +
				"  jobs:\n" +
				"  - name: $13\n" +
				"    release: $14\n" +
				"  vm_type: $15\n" +
				"  stemcell: $16\n" +
				"  networks:\n" +
				"  - name: $17<*>"
				, // ------------------
				"instance_groups:\n" +
				"- name: $1\n" +
				"  azs:\n" +
				"  - $2\n" +
				"  instances: $3\n" +
				"  jobs:\n" +
				"  - name: $4\n" +
				"    release: $5\n" +
				"  vm_type: $6\n" +
				"  stemcell: $7\n" +
				"  networks:\n" +
				"  - name: $8<*>"
				, // ----------------
				"releases:\n" +
				"- name: $1\n" +
				"  version: $2<*>"
				, // ----------------
				"stemcells:\n" +
				"- alias: $1\n" +
				"  version: $2<*>"
				, // ----------------
				"update:\n" +
				"  canaries: $1\n" +
				"  max_in_flight: $2\n" +
				"  canary_watch_time: $3\n" +
				"  update_watch_time: $4<*>"
				, // -----------------
				"variables:\n" +
				"- name: $1\n" +
				"  type: $2<*>"
		);
	}

	@Test public void snippet_disabledWhenPropertiesAlreadyDefined() throws Exception {
		Editor editor = harness.newEditor(
				"name:\n" +
				"releases:\n" +
				"stemcells:\n" +
				"<*>"
		);

		editor.assertCompletionLabels(SNIPPET_COMPLETION,
				//"BoshDeploymentManifest Snippet",
				"instance_groups Snippet",
//				"releases Snippet",
//				"stemcells Snippet"
				"update Snippet",
				"variables Snippet",
				"- Stemcell Snippet"
		);
	}

	@Test public void snippet_nested_plain() throws Exception {
		Editor editor;
		//Plain exact completion
		editor = harness.newEditor(
				"instance_groups:\n" +
				"- name: blah\n" +
				"  <*>"
		);
		editor.assertContextualCompletions(LanguageId.BOSH_DEPLOYMENT, c -> c.getLabel().equals("jobs Snippet"),
				"jo<*>"
				, // ------
				"jobs:\n" +
				"  - name: $1\n" +
				"    release: $2<*>"
		);
		editor.assertCompletionWithLabel("jobs Snippet",
				"instance_groups:\n" +
				"- name: blah\n" +
				"  jobs:\n" +
				"  - name: $1\n" +
				"    release: $2<*>"
		);
	}

	@Test public void snippet_nested_indenting() throws Exception {
		Editor editor;
		//With extra indent:
		editor = harness.newEditor(
				"instance_groups:\n" +
				"- name: blah\n" +
				"<*>"
		);
		editor.assertCompletionWithLabel("→ jobs Snippet",
				"instance_groups:\n" +
				"- name: blah\n" +
				"  jobs:\n" +
				"  - name: $1\n" +
				"    release: $2<*>"
		);
		editor.assertContextualCompletions(LanguageId.BOSH_DEPLOYMENT, c -> c.getLabel().equals("→ jobs Snippet"),
				"jo<*>"
				, // ------
				"  jobs:\n" +
				"  - name: $1\n" +
				"    release: $2<*>"
		);
	}

	@Test public void snippet_dedented() throws Exception {
		Editor editor;
		editor = harness.newEditor(
				"name: \n" +
				"variables:\n" +
				"- name: voo\n" +
				"  type: aaa\n" +
				"<*>"
		);
		editor.assertContextualCompletions(LanguageId.BOSH_DEPLOYMENT, DEDENTED_COMPLETION.and(SNIPPET_COMPLETION),
				"  <*>"
				, // ==>
				"instance_groups:\n" +
				"- name: $1\n" +
				"  azs:\n" +
				"  - $2\n" +
				"  instances: $3\n" +
				"  jobs:\n" +
				"  - name: $4\n" +
				"    release: $5\n" +
				"  vm_type: $6\n" +
				"  stemcell: $7\n" +
				"  networks:\n" +
				"  - name: $8<*>"
				, //=========
				"releases:\n" +
				"- name: $1\n" +
				"  version: $2<*>"
				, //========
				"stemcells:\n" +
				"- alias: $1\n" +
				"  version: $2<*>"
				, //========
				"update:\n" +
				"  canaries: $1\n" +
				"  max_in_flight: $2\n" +
				"  canary_watch_time: $3\n" +
				"  update_watch_time: $4<*>"
				, //========
				"- name: $1\n" +
				"  type: $2<*>"
		);
	}

	@Test public void relaxedCALessSpaces() throws Exception {
		Editor editor;
		editor = harness.newEditor(
				"name: \n" +
				"variables:\n" +
				"- name: voo\n" +
				"  type: aaa\n" +
				"<*>"
		);
		editor.assertContextualCompletions(LanguageId.BOSH_DEPLOYMENT, DEDENTED_COMPLETION.and(SNIPPET_COMPLETION.negate()),
				"  <*>"
				, //==>
				"instance_groups:\n" +
				"- name: <*>"
				, //----
				"releases:\n" +
				"- name: <*>"
				, //----
				"stemcells:\n" +
				"- <*>"
				, //---
				"tags:\n" +
				"  <*>"
				, //---
				"update:\n" +
				"  <*>"
				, //---
				"- name: <*>"
		);
	}

	@Test public void relaxedCAmoreSpaces() throws Exception {
		Editor editor = harness.newEditor(
			"name: foo\n" +
			"instance_groups:\n" +
			"- name: \n" +
			"<*>"
		);
		editor.assertContextualCompletions(LanguageId.BOSH_DEPLOYMENT, c -> c.getLabel().equals("→ jobs"),
				"jo<*>"
				, // ==>
				"  jobs:\n" +
				"  - <*>"
		);
	}

	@Test public void keyCompletionThatNeedsANewline() throws Exception {
		Editor editor = harness.newEditor(
				"name: foo\n" +
				"update: canwa<*>"
		);
		editor.assertCompletions(
				"name: foo\n" +
				"update: \n" +
				"  canary_watch_time: <*>"
		);
	}

	@Test public void cloudconfigReconcile() throws Exception {
		Editor editor = harness.newEditor(LanguageId.BOSH_CLOUD_CONFIG,
				"bogus: bad\n" +
				"azs:\n" +
				"- name: z1\n" +
				"  cloud_properties: {availability_zone: us-east-1a}\n" +
				"- name: z2\n" +
				"  cloud_properties: {availability_zone: us-east-1b}\n" +
				"\n" +
				"vm_types:\n" +
				"- name: small\n" +
				"  cloud_properties:\n" +
				"    instance_type: t2.micro\n" +
				"    ephemeral_disk: {size: 3000, type: gp2}\n" +
				"- name: medium\n" +
				"  cloud_properties:\n" +
				"    instance_type: m3.medium\n" +
				"    ephemeral_disk: {size: 30000, type: gp2}\n" +
				"\n" +
				"disk_types:\n" +
				"- name: small\n" +
				"  disk_size: 3000\n" +
				"  cloud_properties: {type: gp2}\n" +
				"- name: large\n" +
				"  disk_size: 50_000\n" +
				"  cloud_properties: {type: gp2}\n" +
				"\n" +
				"networks:\n" +
				"- name: private\n" +
				"  type: manual\n" +
				"  subnets:\n" +
				"  - range: 10.10.0.0/24\n" +
				"    gateway: 10.10.0.1\n" +
				"    az: z1\n" +
				"    static: [10.10.0.62]\n" +
				"    dns: [10.10.0.2]\n" +
				"    cloud_properties: {subnet: subnet-f2744a86}\n" +
				"  - range: 10.10.64.0/24\n" +
				"    gateway: 10.10.64.1\n" +
				"    az: z2\n" +
				"    static: [10.10.64.121, 10.10.64.122]\n" +
				"    dns: [10.10.0.2]\n" +
				"    cloud_properties: {subnet: subnet-eb8bd3ad}\n" +
				"- name: vip\n" +
				"  type: vip\n" +
				"\n" +
				"compilation:\n" +
				"  workers: 5\n" +
				"  reuse_compilation_vms: true\n" +
				"  az: z1\n" +
				"  vm_type: medium\n" +
				"  network: private\n"
		);

		editor.assertProblems("bogus|Unknown property");
	}

}