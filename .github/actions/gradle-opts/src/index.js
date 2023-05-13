/*
 * Copyright (C) 2021-2023 Rick Busarow
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

const core = require('@actions/core');
const github = require('@actions/github');

try {
  const runnerOS = process.env.RUNNER_OS;
  let gradleOpts;

  switch (runnerOS) {
    case 'macOS':
      gradleOpts = process.env.GRADLE_OPTS_MACOS;
      break;
    case 'Linux':
      gradleOpts = process.env.GRADLE_OPTS_UBUNTU;
      break;
    case 'Windows':
      gradleOpts = process.env.GRADLE_OPTS_WINDOWS;
      break;
    default:
      throw new Error(`Unsupported runner OS: ${runnerOS}`);
  }

  core.exportVariable('GRADLE_OPTS', gradleOpts);
} catch (error) {
  core.setFailed(error.message);
}
