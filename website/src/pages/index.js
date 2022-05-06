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

import React from "react";
import clsx from "clsx";
import Layout from "@theme/Layout";
import Link from "@docusaurus/Link";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import useBaseUrl from "@docusaurus/useBaseUrl";
import styles from "./styles.module.css";

const features = [
  {
    title: 'Tree shaking',
    imageUrl: 'img/modulecheck_diagram.png',
    description: (
      <>
        Blah blah blah.

        <br/><br/>

        More text.

        <br/><br/>

      </>
    ),
  },
];

function Feature({imageUrl, title, description}) {
  const imgUrl = useBaseUrl(imageUrl);
  return (
    <div>
      {imgUrl && (
        <div className="text--center">
          <img className={styles.featureImage} src={imgUrl} alt={title}/>
        </div>
      )}
      {/*<h1 align="center">{title}</h1>*/}

      {/*<p>{description}</p>*/}
    </div>
  );
}

function Home() {
  const context = useDocusaurusContext();
  const {siteConfig = {}} = context;
  return (
    <Layout
      title={`${siteConfig.title}`}
      description="Fast Gradle dependency graph validation"
    >
      <header className={clsx('hero hero--primary', styles.heroBanner)}>
        <div className="container">
          <p className={clsx(styles.heroSlogan)}>
            <strong>ModuleCheck</strong> removes unused module dependencies from your gradle
            project.
          </p>
          <div className={styles.buttons}>
            <Link
              className={clsx(
                'button button--outline button--secondary button--lg',
                styles.gettingStartedButton,
              )}
              to={useBaseUrl('docs')}>
              Get Started
            </Link>

            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;

            {
              // <iframe
              //   src="https://ghbtns.com/github-btn.html?user=rbusarow&repo=ModuleCheck&type=star&count=true&size=large"
              //   width="170"
              //   height="30"
              //   title="GitHub"/>
            }

          </div>
        </div>
      </header>
      <main>
        <div className={styles.badges}>
          <div className="container">
            <a href="https://search.maven.org/search?q=g:com.rickbusarow.modulecheck">
              <img
                src="https://img.shields.io/maven-central/v/com.rickbusarow.modulecheck/modulecheck-api.svg?label=release&style=for-the-badge&color=aa0055"
                alt="version badge"/>
            </a>

            &nbsp;

            <a href="https://plugins.gradle.org/plugin/com.rickbusarow.module-check">
              <img
                src="https://img.shields.io/gradle-plugin-portal/v/com.rickbusarow.module-check?style=for-the-badge"
                alt="Gradle Plugin Portal"/>
            </a>

            &nbsp;

            <a href="https://oss.sonatype.org/#nexus-search;quick~com.rickbusarow.modulecheck">
              <img
                src="https://img.shields.io/nexus/s/com.rickbusarow.modulecheck/modulecheck-api?label=snapshots&server=https%3A%2F%2Foss.sonatype.org&style=for-the-badge"
                alt="Snapshot"/>
            </a>

            &nbsp;

            <a href="https://github.com/rbusarow/ModuleCheck/blob/main/LICENSE">
              <img
                src="https://img.shields.io/badge/license-apache2.0-blue?style=for-the-badge"
                alt="license"/>
            </a>
          </div>
        </div>
      </main>
      <main>
        <section className={styles.features}>
          <div className="container">
            {/*<div className="row">*/}
            {features.map((props, idx) => (
              <Feature key={idx} {...props} />
            ))}
            {/*</div>*/}
          </div>
        </section>
      </main>
    </Layout>
  );
}

export default Home;
