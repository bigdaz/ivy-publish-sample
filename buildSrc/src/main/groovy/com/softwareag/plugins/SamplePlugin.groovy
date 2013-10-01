package com.softwareag.plugins;

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyArtifact
import org.gradle.api.publish.ivy.IvyConfiguration
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.internal.dependency.DefaultIvyDependency
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.PublishArtifact

class PluginExtension {
    def buildVersion = "unspecified"
}


class SamplePlugin implements Plugin<Project> {

	public void apply(Project project) {
		project.extensions.create("setup", PluginExtension)
		configureVersion(project)
		//Apply Ivy Publish Plugin
		project.plugins.apply(IvyPublishPlugin)
		configurePublish(project)
	}
 
	private void configureVersion(Project project) {
		project.afterEvaluate {
			project.version = project.setup.buildVersion
		}
	}
	private void configurePublish(Project project) {
		project.publishing {
		publications {
				ivy(IvyPublication) { IvyPublication ivyPublication ->
					Map ivyArtifacts = [:]
					Map ivyDependencies = [:]
					
					project.configurations.all { Configuration conf ->
						IvyConfiguration ivyConf = ivyPublication.configurations.create(conf.name)
						conf.extendsFrom.each {
							ivyConf.extend(it.name)
						}

						conf.artifacts.all { PublishArtifact art ->
							if (ivyArtifacts[art.file] == null) {
								ivyArtifacts[art.file] = [ art, conf.name]
							} else if (!ivyArtifacts[art.file][1].contains(conf.name)) {
								ivyArtifacts[art.file] = [ art, "${ivyArtifacts[art.file][1]},$conf.name"]
							}
						}
						
						conf.dependencies.withType(ModuleDependency) { ModuleDependency dep ->
							if (ivyDependencies[dep] == null) {
								ivyDependencies[dep] = conf.name
							} else {
								ivyDependencies[dep] += ",$conf.name"
							}
						}
					}
					
					ivyArtifacts.each { file, values ->
						IvyArtifact ivyArtifact = ivyPublication.artifact( values[0])
						ivyArtifact.name = values[0].name
						ivyArtifact.type = values[0].type
						ivyArtifact.conf = values[1]
					}
					
					ivyDependencies.each { ModuleDependency moduleDependency, conf ->
						ivyPublication.dependencies.add(new DefaultIvyDependency(moduleDependency.getGroup(),moduleDependency.getName(),moduleDependency.getVersion(), conf + '->' + moduleDependency.configuration))
					}

					
				ivyPublication.descriptor.withXml {
                       def configurations = asNode().configurations[0]
                        project.configurations.all { Configuration conf ->

                            def confNode = configurations.conf.find { it.@name == conf.name }
                           
                            confNode.@description = conf.description
                            if (!conf.visible) {
                                confNode.@visibility = "private"
                            }
                        }
                    }
                }   
			}
		}
	}
}
	
